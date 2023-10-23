package com.webank.eggroll.clustermanager.cluster;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eggroll.core.config.Dict;
import com.eggroll.core.config.MetaInfo;
import com.eggroll.core.constant.*;
import com.eggroll.core.context.Context;
import com.eggroll.core.exceptions.ErSessionException;
import com.eggroll.core.grpc.NodeManagerClient;
import com.eggroll.core.pojo.*;
import com.eggroll.core.postprocessor.ApplicationStartedRunner;
import com.eggroll.core.utils.JsonUtil;
import com.eggroll.core.utils.LockUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.webank.eggroll.clustermanager.dao.impl.*;
import com.webank.eggroll.clustermanager.entity.ProcessorResource;
import com.webank.eggroll.clustermanager.schedule.ClusterManagerTask;
import com.webank.eggroll.clustermanager.session.SessionManager;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


@Data
@Singleton
public class ClusterResourceManager implements ApplicationStartedRunner {

    Logger log = LoggerFactory.getLogger(ClusterResourceManager.class);

    private ConcurrentHashMap<String, ReentrantLock> sessionLockMap = new ConcurrentHashMap<>();
    private Map<String, Long> killJobMap = new ConcurrentHashMap<>();
    private FifoBroker<ResourceApplication> applicationQueue = new FifoBroker<>();
    @Inject
    SessionMainService sessionMainService;
    @Inject
    SessionProcessorService sessionProcessorService;
    @Inject
    ServerNodeService serverNodeService;
    @Inject
    NodeResourceService nodeResourceService;
    @Inject
    ProcessorResourceService processorResourceService;
    @Inject
    SessionManager sessionManager;


    BlockingQueue<Long> nodeResourceUpdateQueue = new ArrayBlockingQueue(100);

    public void countAndUpdateNodeResource(Long serverNodeId) {
        this.nodeResourceUpdateQueue.add(serverNodeId);
    }

    public void countAndUpdateNodeResourceInner(Long serverNodeId) {

        List<ProcessorResource> resourceList = this.processorResourceService.list(new LambdaQueryWrapper<ProcessorResource>()
                .eq(ProcessorResource::getServerNodeId, serverNodeId).in(ProcessorResource::getResourceType, Lists.newArrayList(ResourceStatus.ALLOCATED.getValue(), ResourceStatus.PRE_ALLOCATED.getValue())));
        List<ErResource> prepareUpdateResource = Lists.newArrayList();
        if (resourceList != null) {
            Map<String, ErResource> resourceMap = Maps.newHashMap();
            resourceList.forEach(processorResource -> {
                String status = processorResource.getStatus();
                ErResource nodeResource = resourceMap.get(processorResource.getResourceType());
                if (nodeResource == null) {
                    resourceMap.put(processorResource.getResourceType(), new ErResource());
                    nodeResource = resourceMap.get(processorResource.getResourceType());
                }
                if (status.equals(ResourceStatus.ALLOCATED.getValue())) {
                    if (nodeResource.getAllocated() != null)
                        nodeResource.setAllocated(nodeResource.getAllocated() + processorResource.getAllocated());
                    else
                        nodeResource.setAllocated(processorResource.getAllocated());

                } else {
                    if (nodeResource.getPreAllocated() != null)
                        nodeResource.setPreAllocated(nodeResource.getPreAllocated() + processorResource.getAllocated());
                    else
                        nodeResource.setPreAllocated(processorResource.getAllocated());
                }
            });
            prepareUpdateResource.addAll(resourceMap.values());
        } else {
            ErResource gpuResource = new ErResource();
            gpuResource.setServerNodeId(serverNodeId);
            gpuResource.setResourceType(ResourceType.VGPU_CORE.name());
            gpuResource.setAllocated(0L);
            gpuResource.setPreAllocated(0L);
            ErResource cpuResource = new ErResource();
            cpuResource.setServerNodeId(serverNodeId);
            cpuResource.setResourceType(ResourceType.VCPU_CORE.name());
            cpuResource.setAllocated(0L);
            cpuResource.setPreAllocated(0L);
            prepareUpdateResource.add(gpuResource);
            prepareUpdateResource.add(cpuResource);
        }
        nodeResourceService.doUpdateNodeResource(serverNodeId, prepareUpdateResource);
    }


    /**
     * @param context
     * @param request
     * @return
     */
    public CheckResourceEnoughResponse checkResourceEnoughForFlow(Context context, CheckResourceEnoughRequest request) {
        log.info("checkResourceEnoughForFlow request info: {}", request.toString());
        CheckResourceEnoughResponse response = new CheckResourceEnoughResponse();
        boolean result = false;
        Long globalRemainResource = 0L;
        List<ErServerNode> erServerNodes = getServerNodeWithResource();
        for (ErServerNode n : erServerNodes) {
            for (ErResource r : n.getResources()) {
                if (r.getResourceType().equals(request.getResourceType())) {
                    long nodeResourceCount = r.getUnAllocatedResource();
                    if (Dict.CHECK_RESOURCE_ENOUGH_CHECK_TYPE_NODE.equals(request.getCheckType()) && nodeResourceCount >= request.getRequiredResourceCount()) {
                        log.info("the node {} have enough resource for required", n.getEndpoint().getHost());
                        result = true;
                    }
                    globalRemainResource += nodeResourceCount;
                }
            }
        }
        log.info("globalRemainResource: {}", globalRemainResource);

        if (Dict.CHECK_RESOURCE_ENOUGH_CHECK_TYPE_CLUSTER.equals(request.getCheckType())) {
            result = globalRemainResource >= request.getRequiredResourceCount();
        }
        response.setEnough(result);
        return response;
    }

    Thread countNodeResourceThread = new Thread(() -> {
        while (true) {
            try {
                List<Long> nodeList = Lists.newArrayList();
                nodeResourceUpdateQueue.drainTo(nodeList);
                Set<Long> nodeSet = new HashSet<>();
                nodeSet.addAll(nodeList);
                for (Long nodeId : nodeSet) {
                    countAndUpdateNodeResourceInner(nodeId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }, "RESOURCE-COUNT-THREAD");


    Thread lockCleanThread = new Thread(() -> {
        while (true) {
            log.info("lock clean thread , prepare to run");
            long now = System.currentTimeMillis();
            sessionLockMap.forEach((k, v) -> {
                try {
                    ErSessionMeta es = sessionMainService.getSessionMain(k);
                    if (es.getUpdateTime() != null) {
                        long updateTime = es.getUpdateTime().getTime();
                        if (now - updateTime > MetaInfo.EGGROLL_RESOURCE_LOCK_EXPIRE_INTERVAL
                                && (SessionStatus.KILLED.name().equals(es.getStatus())
                                || SessionStatus.ERROR.name().equals(es.getStatus())
                                || SessionStatus.CLOSED.name().equals(es.getStatus())
                                || SessionStatus.FINISHED.name().equals(es.getStatus()))) {
                            sessionLockMap.remove(es.getId());
                            killJobMap.remove(es.getId());
                        }
                    }
                } catch (Throwable e) {
                    log.error("lock clean error: " + e.getMessage());
                    // e.printStackTrace();
                }
            });
            try {
                Thread.sleep(MetaInfo.EGGROLL_RESOURCE_LOCK_EXPIRE_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }, "LOCK-CLEAN-THREAD");

    Thread dispatchThread = new Thread(() -> {
        log.info("resource dispatch thread start !!!");
        boolean flag = true;
        while (flag) {
            ResourceApplication resourceApplication = null;
            if (applicationQueue.getBroker().size() > 0) {
                resourceApplication = applicationQueue.getBroker().peek();
                log.info("resource application queue size {}", applicationQueue.getBroker().size());
            } else {
                try {
                    Thread.sleep(MetaInfo.EGGROLL_RESOURCE_DISPATCH_INTERVAL);
                } catch (Exception e) {
                    log.error("Thread.sleep error");
                }
            }

            ErSessionMeta erSessionMeta = new ErSessionMeta();
            boolean resourceDispartcherFlag = true;
            while (resourceDispartcherFlag) {
                try {
                    if (resourceApplication != null) {
                        long now = System.currentTimeMillis();
                        List<ErServerNode> serverNodes = null;
                        try {
                            lockSession(resourceApplication.getSessionId());
                            if (killJobMap.containsKey(resourceApplication.getSessionId())) {
                                log.error("session " + resourceApplication.getSessionId() + " is already canceled, drop it");
                                applicationQueue.getBroker().remove();
                                break;
                            }
                            if (resourceApplication.getWaitingCount().get() == 0) {
                                //过期资源申请
                                log.error("expired resource request: " + resourceApplication + " !!!");
                                applicationQueue.getBroker().remove();
                                break;
                            }
                            int tryCount = 0;
                            do {
                                serverNodes = getServerNodeWithResource();
                                tryCount += 1;
                                if (serverNodes == null || serverNodes.size() == 0) {
                                    try {
                                        Thread.sleep(MetaInfo.CONFKEY_NODE_MANAGER_HEARTBEAT_INTERVAL);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } while ((serverNodes == null || serverNodes.size() == 0) && tryCount < 2);
                            boolean enough = checkResourceEnough(serverNodes, resourceApplication);
                            log.info("check resource is enough ? " + enough);
                            if (!enough) {
                                switch (resourceApplication.getResourceExhaustedStrategy()) {
                                    case Dict.IGNORE:
                                        continue;
                                    case Dict.WAITING:
                                        Thread.sleep(MetaInfo.EGGROLL_RESOURCE_DISPATCH_INTERVAL);
                                        log.info("resource is not enough, waiting next loop");
                                        continue;
                                    case Dict.THROW_ERROR:
                                        resourceApplication.getStatus().set(1);
                                        resourceApplication.countDown();
                                        applicationQueue.getBroker().remove();
                                        flag = false;
                                        return;
                                }
                            }
                            switch (resourceApplication.getDispatchStrategy()) {
                                case Dict.REMAIN_MOST_FIRST:
                                    remainMostFirstDispatch(serverNodes, resourceApplication);
                                    break;
                                case Dict.RANDOM:
                                    randomDispatch(serverNodes, resourceApplication);
                                    break;
                                case Dict.FIX:
                                    fixDispatch(serverNodes, resourceApplication);
                                    break;
                                case Dict.SINGLE_NODE_FIRST:
                                    remainMostFirstDispatch(serverNodes, resourceApplication);
                                    break;
                            }
                            List<MutablePair<ErProcessor, ErServerNode>> dispatchedProcessors = resourceApplication.getResourceDispatch();
                            erSessionMeta.setId(resourceApplication.getSessionId());
                            erSessionMeta.setName(resourceApplication.getSessionName());
                            List<ErProcessor> processorList = new ArrayList<>();
                            for (MutablePair<ErProcessor, ErServerNode> dispatchedProcessor : dispatchedProcessors) {
                                processorList.add(dispatchedProcessor.getKey());
                            }
                            erSessionMeta.setProcessors(processorList);
                            erSessionMeta.setTotalProcCount(dispatchedProcessors.size());
                            erSessionMeta.setStatus(SessionStatus.NEW.name());

                            sessionMainService.registerWithResource(erSessionMeta);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            unlockSession(resourceApplication.getSessionId());
                        }
//                    ErSessionMeta registeredSessionMeta = sessionMainService.getSession(resourceApplication.getSessionId());
                        Map<Long, List<ErServerNode>> serverNodeMap = serverNodes.stream().collect(Collectors.groupingBy(ErServerNode::getId));
                        resourceApplication.getResourceDispatch().clear();
                        for (ErProcessor processor : erSessionMeta.getProcessors()) {
                            resourceApplication.getResourceDispatch().add(new MutablePair<>(processor, serverNodeMap.get(processor.getServerNodeId()).get(0)));
                        }
                        resourceApplication.countDown();
                        applicationQueue.getBroker().remove();

                    }
                } catch (Exception e) {
                    log.error("dispatch resource error: ", e);
                }
            }
        }
        log.error("!!!!!!!!!!!!!!!!!!!resource dispatch thread quit!!!!!!!!!!!!!!!!");
    }, "RESOURCE_DISPATCH_THREAD");

    private List<ErServerNode> getServerNodeWithResource() {
        ErServerNode erServerNode = new ErServerNode();
        erServerNode.setStatus(ServerNodeStatus.HEALTHY.name());
        erServerNode.setNodeType(ServerNodeTypes.NODE_MANAGER.name());
        return serverNodeService.getServerNodesWithResource(erServerNode);
    }

    private Boolean checkResourceEnough(List<ErServerNode> erServerNodes, ResourceApplication resourceApplication) {
        boolean result = true;
        Map<String, Long> globalRemainResourceMap = new HashMap<>();
        Map<Long, Map<String, Long>> nodeRemainResourceMap = new HashMap<>();

        for (ErServerNode n : erServerNodes) {
            Map<String, Long> nodeMap = nodeRemainResourceMap.getOrDefault(n.getId(), new HashMap<>());
            for (ErResource r : n.getResources()) {
                long remain = nodeMap.getOrDefault(r.getResourceType(), 0L);
                long unAllocated = r.getUnAllocatedResource();
                nodeMap.put(r.getResourceType(), remain + unAllocated);
            }
            nodeRemainResourceMap.put(n.getId(), nodeMap);
        }

        nodeRemainResourceMap.forEach((k, v) -> v.forEach((str, lon) -> {
            Long count = globalRemainResourceMap.getOrDefault(str, 0L);
            globalRemainResourceMap.put(str, count + lon);
        }));

        if (!resourceApplication.isAllowExhausted()) {
            assert erServerNodes.size() > 0;
            if (Dict.FIX.equals(resourceApplication.getDispatchStrategy())) {
                int eggsPerNode = Integer.parseInt(resourceApplication.getOptions()
                        .getOrDefault(Dict.CONFKEY_SESSION_PROCESSORS_PER_NODE, MetaInfo.CONFKEY_SESSION_PROCESSORS_PER_NODE.toString()));
                String resourceType = resourceApplication.getOptions().getOrDefault("resourceType", Dict.VCPU_CORE);
                int types = resourceApplication.getProcessorTypes().size();
                result = nodeRemainResourceMap.entrySet().stream().allMatch(n -> {
                    long exist = n.getValue().getOrDefault(resourceType, 0L);
                    return exist >= eggsPerNode * types;
                });
            } else {
                List<ErProcessor> processors = resourceApplication.getProcessors();
//                    ErServerNode erServerNode = erServerNodes.stream().reduce((x, y) -> {
//                                x.getResources().addAll(y.getResources());
//                                return x;
//                            }
//                    ).orElse(null);
                Map<String, Long> requestResourceMap = new HashMap<>();

                ErProcessor erProcessor = processors.stream().reduce((x, y) -> {
                            x.getResources().addAll(y.getResources());
                            return x;
                        }
                ).orElse(null);

                if (erProcessor != null && erProcessor.getResources() != null) {
                    Map<String, List<ErResource>> collect = erProcessor.getResources().stream().collect(Collectors.groupingBy(ErResource::getResourceType));
                    collect.forEach((k, erResourceList) -> {
                        long sum = 0;
                        for (ErResource resource : erResourceList) {
                            sum += resource.getAllocated();
                        }
                        requestResourceMap.put(k, sum);
                    });

                }
                for (Map.Entry<String, Long> r : requestResourceMap.entrySet()) {
                    Long globalResourceRemain = globalRemainResourceMap.getOrDefault(r.getKey(), -1L);
                    if (globalResourceRemain.intValue() > -1) {
                        log.info("check resource " + r.getKey() + " request " + r.getValue() + " remain " + globalResourceRemain);
                        if (r.getValue() > globalResourceRemain) {
                            result = false;
                            break;
                        }
                    } else {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }

    //    public ResourceApplication remainMostFirstDispatch(List<ErServerNode> serverNodes, ResourceApplication resourceApplication) {
//        List<ErProcessor> requiredProcessors = resourceApplication.getProcessors();
//        List<ErServerNode> sortedNodes = serverNodes.stream().sorted(Comparator.comparingLong(node -> getFirstUnAllocatedResource(node, resourceApplication))).collect(Collectors.toList());
//        List<MutablePair<ErServerNode, ErResource>> nodeResourceTupes = new ArrayList<>();
//        for (ErServerNode sortedNode : sortedNodes) {
//            nodeResourceTupes.add(new MutablePair<>(sortedNode, sortedNode.getResources().get(0)));
//        }
//        Map<ErServerNode, Long> sortMap = new HashMap<>();
//        for (ErServerNode node : sortedNodes) {
//            sortMap.put(node, getFirstUnAllocatedResource(node, resourceApplication));
//        }
//        Map<ErServerNode, List<ErProcessor>> nodeToProcessors = new HashMap<>();
//
//        for (int index = 0; index < requiredProcessors.size(); index++) {
//            ErProcessor requiredProcessor = requiredProcessors.get(index);
//            ErServerNode node = nodeResourceTupes.get(0).getKey();
//
//            int nextGpuIndex = -1;
//            List<ErResource> newResources = new ArrayList<>();
//            for (ErResource r : requiredProcessor.getResources()) {
//                if (Dict.VGPU_CORE.equals(r.getResourceType())) {
//                    List<ErResource> gpuResourcesInNodeArray = node.getResources().stream()
//                            .filter(res -> Dict.VGPU_CORE.equals(res.getResourceType()))
//                            .collect(Collectors.toList());
//                    if (!gpuResourcesInNodeArray.isEmpty()) {
//                        ErResource gpuResourcesInNode = gpuResourcesInNodeArray.get(0);
//
//                        List<String> extentionCache = new ArrayList<>();
//                        if (gpuResourcesInNode.getExtention() != null) {
//                            extentionCache = Arrays.asList(gpuResourcesInNode.getExtention().split(","));
//                        }
//                        nextGpuIndex = getNextGpuIndex(gpuResourcesInNode.getTotal(), extentionCache);
//                        extentionCache.add(String.valueOf(nextGpuIndex));
//                        r.setExtention(String.valueOf(nextGpuIndex));
//                    }
//                }
//                newResources.add(r);
//            }
//            String host = node.getEndpoint().getHost();
//            requiredProcessor.setServerNodeId(node.getId());
//            requiredProcessor.setCommandEndpoint(new ErEndpoint(host, 0));
//            requiredProcessor.setResources(newResources);
//            Map<String, String> optionsMap = new HashMap<>();
//            optionsMap.put("cudaVisibleDevices", nextGpuIndex + "");
//            requiredProcessor.setOptions(optionsMap);
//            nodeToProcessors.computeIfAbsent(node, k -> new ArrayList<>()).add(requiredProcessor);
//        }
//
//        nodeToProcessors.forEach((node, processors) -> {
//            for (ErProcessor processor : processors) {
//                resourceApplication.getResourceDispatch().add(new MutablePair<>(processor, node));
//            }
//        });
//
//        return resourceApplication;
//
//    }
    private void remainMostFirstDispatch(List<ErServerNode> serverNodes, ResourceApplication resourceApplication) throws InvocationTargetException, IllegalAccessException {
        List<ErProcessor> requiredProcessors = resourceApplication.getProcessors();
        List<ErServerNode> sortedNodes = serverNodes.stream().sorted(Comparator.comparingLong(node -> getFirstUnAllocatedResource(node, resourceApplication))).collect(Collectors.toList());
        List<MutablePair<ErServerNode, Long>> nodeResourceTupes = new ArrayList<>();
        for (ErServerNode sortedNode : sortedNodes) {
            nodeResourceTupes.add(new MutablePair<>(sortedNode, sortedNode.getResources().get(0).getUnAllocatedResource()));
        }

        List<String> allocatedGpuIndex = new ArrayList<>();
        Map<ErServerNode, List<ErProcessor>> nodeToProcessors = new HashMap<>();

        for (int index = 0; index < requiredProcessors.size(); index++) {

            ErProcessor requiredProcessor = resourceApplication.getProcessors().get(index);

            MutablePair<ErServerNode, Long> nodeTupe = nodeResourceTupes.get(0);
            ErServerNode node = nodeTupe.getLeft();

            int nextGpuIndex = -1;
            List<ErResource> newResources = new ArrayList<>();
            for (ErResource r : requiredProcessor.getResources()) {
                ErResource changedResource = r;
                if (r.getResourceType().equals(Dict.VGPU_CORE)) {
                    List<ErResource> gpuResourcesInNodeList = node.getResources().stream()
                            .filter(res -> res.getResourceType().equals(Dict.VGPU_CORE))
                            .collect(Collectors.toList());

                    if (!gpuResourcesInNodeList.isEmpty()) {
                        ErResource gpuResourcesInNode = gpuResourcesInNodeList.get(0);

                        List<String> extentionCache = new ArrayList<>();
                        if (gpuResourcesInNode.getExtention() != null) {
                            extentionCache.addAll(Arrays.asList(gpuResourcesInNode.getExtention().split(",")));
                        }

                        nextGpuIndex = getNextGpuIndex(gpuResourcesInNode.getTotal(), extentionCache);
                        extentionCache.add(String.valueOf(nextGpuIndex));
                        ErResource newChangedResource = new ErResource();
                        BeanUtils.copyProperties(newChangedResource, changedResource);
                        newChangedResource.setExtention(String.valueOf(nextGpuIndex));
                        changedResource = newChangedResource;
                    }
                }
                newResources.add(changedResource);
            }
            nodeResourceTupes = new ArrayList<>(nodeResourceTupes.subList(1, nodeResourceTupes.size()));
            final String host = node.getEndpoint().getHost();
            int globalRank = index;
            int localRank = nodeToProcessors.getOrDefault(node, new ArrayList<>()).size();
            requiredProcessor.setServerNodeId(node.getId());
            requiredProcessor.setCommandEndpoint(new ErEndpoint(host, 0));
            requiredProcessor.setResources(newResources);
            requiredProcessor.getOptions().put("cudaVisibleDevices", String.valueOf(nextGpuIndex));

            nodeToProcessors.computeIfAbsent(node, k -> new ArrayList<>()).add(requiredProcessor);

//            ErProcessor newRequiredProcessor = new ErProcessor();
//            BeanUtils.copyProperties(newRequiredProcessor,requiredProcessor);
//            newRequiredProcessor.setServerNodeId(node.getId());
//            newRequiredProcessor.setCommandEndpoint(new ErEndpoint(node.getEndpoint().getHost(), 0));
//            newRequiredProcessor.setResources(newResources);
//            newRequiredProcessor.getOptions().put("cudaVisibleDevices",String.valueOf(nextGpuIndex));
//            requiredProcessor = newRequiredProcessor;

        }

        List<MutablePair<ErProcessor, ErServerNode>> result = nodeToProcessors.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(p -> new MutablePair<>(p, entry.getKey())))
                .collect(Collectors.toList());
        resourceApplication.getResourceDispatch().addAll(result);
    }

//    private ResourceApplication singleNodeFirstDispatch(List<ErServerNode> serverNodes, ResourceApplication resourceApplication) throws InvocationTargetException, IllegalAccessException {
//        List<ErProcessor> requiredProcessors = resourceApplication.getProcessors();
//        List<MutablePair<ErServerNode, Integer>> nodeResourceTupes = serverNodes.stream()
//                .map(n -> new MutablePair(n, n.getResources().stream()
//                        .filter(r -> r.getResourceType().equals(resourceApplication.getSortByResourceType()))
//                        .map(ErResource::getUnAllocatedResource)
//                        .findFirst()
//                        .orElse(0L)))
//                .sorted(Comparator.comparingLong(node -> getFirstUnAllocatedResource((ErServerNode)node.getLeft(), resourceApplication)))
//                .collect(Collectors.toCollection(ArrayList::new));
//
//        List<ErServerNode> nodeList = new ArrayList<>();
//        nodeResourceTupes.forEach(t -> {
//            for (int index = 0; index < t.getRight(); index++) {
//                nodeList.add(t.getLeft());
//            }
//        });
//
//        Map<ErServerNode, List<ErProcessor>> nodeToProcessors = new HashMap<>();
//
//        for (int index = 0; index < requiredProcessors.size(); index++) {
//
//            ErProcessor requiredProcessor = resourceApplication.getProcessors().get(index);
//            ErServerNode node = nodeList.get(index);
//
//            int nextGpuIndex = -1;
//            List<ErResource> newResources = new ArrayList<>();
//            for (ErResource r : requiredProcessor.getResources()) {
//                ErResource changedResource = r;
//                if (r.getResourceType().equals(Dict.VGPU_CORE) ) {
//                    List<ErResource> gpuResourcesInNodeList = node.getResources().stream()
//                            .filter(res -> res.getResourceType().equals(Dict.VGPU_CORE))
//                            .collect(Collectors.toList());
//
//                    if (!gpuResourcesInNodeList.isEmpty()) {
//                        ErResource gpuResourcesInNode = gpuResourcesInNodeList.get(0);
//
//                        List<String> extentionCache = new ArrayList<>();
//                        if (gpuResourcesInNode.getExtention() != null) {
//                            extentionCache.addAll(Arrays.asList(gpuResourcesInNode.getExtention().split(",")));
//                        }
//
//                        nextGpuIndex = getNextGpuIndex(gpuResourcesInNode.getTotal(), extentionCache);
//                        extentionCache.add(String.valueOf(nextGpuIndex));
//                        ErResource newChangedResource = new ErResource();
//                        BeanUtils.copyProperties(newChangedResource,changedResource);
//                        newChangedResource.setExtention(String.valueOf(nextGpuIndex));
//                        changedResource = newChangedResource;
//                    }
//                }
//                newResources.add(changedResource);
//            }
//
//            ErProcessor newRequiredProcessor = new ErProcessor();
//            BeanUtils.copyProperties(newRequiredProcessor,requiredProcessor);
//            newRequiredProcessor.setServerNodeId(node.getId());
//            newRequiredProcessor.setCommandEndpoint(new ErEndpoint(node.getEndpoint().getHost(), 0));
//            newRequiredProcessor.setResources(newResources);
//            newRequiredProcessor.getOptions().put("cudaVisibleDevices",String.valueOf(nextGpuIndex));
//            requiredProcessor = newRequiredProcessor;
//
//            nodeToProcessors.computeIfAbsent(node, k -> new ArrayList<>()).add(requiredProcessor);
//        }
//
//        List<MutablePair<ErProcessor, ErServerNode>> result = nodeToProcessors.entrySet().stream()
//                .flatMap(entry -> entry.getValue().stream().map(p -> new MutablePair<>(p, entry.getKey())))
//                .collect(Collectors.toList());
//
//        resourceApplication.getResourceDispatch().addAll(result);
//        return resourceApplication;
//    }

    private Long getFirstUnAllocatedResource(ErServerNode serverNode, ResourceApplication resourceApplication) {
        for (ErResource resource : serverNode.getResources()) {
            if (resource.getResourceType().equals(resourceApplication.getSortByResourceType())) {
                return resource.getUnAllocatedResource();
            }
        }
        return 0L;
    }

    private int getNextGpuIndex(Long size, List<String> alreadyAllocated) {
        int result = -1;

        for (int index = 0; index < size; index++) {
            boolean isAllocated = false;
            for (String allocated : alreadyAllocated) {
                if (allocated.equals(String.valueOf(index))) {
                    isAllocated = true;
                    break;
                }
            }
            if (!isAllocated) {
                result = index;
                break;
            }
        }

        log.info("get next gpu index, size: " + size + " alreadyAllocated: " + JsonUtil.object2Json(alreadyAllocated) + " return: " + result);
        return result;
    }

    private static ResourceApplication randomDispatch(List<ErServerNode> serverNodes, ResourceApplication resourceApplication) {
        List<ErProcessor> requiredProcessors = resourceApplication.getProcessors();
        List<ErServerNode> shuffledNodes = new ArrayList<>(serverNodes);
        Collections.shuffle(shuffledNodes);
        Map<ErServerNode, List<ErProcessor>> nodeToProcessors = new HashMap<>();

        for (int index = 0; index < requiredProcessors.size(); index++) {
            ErProcessor requiredProcessor = resourceApplication.getProcessors().get(index);
            ErServerNode node = shuffledNodes.get(0);

            String host = node.getEndpoint().getHost();
            int globalRank = index;
            int localRank = nodeToProcessors.getOrDefault(node, new ArrayList<>()).size();
            requiredProcessor.setServerNodeId(node.getId());
            requiredProcessor.setCommandEndpoint(new ErEndpoint(host, 0));
            if (nodeToProcessors.containsKey(node)) {
                nodeToProcessors.get(node).add(requiredProcessor);
            } else {
                nodeToProcessors.put(node, new ArrayList<>(Collections.singletonList(requiredProcessor)));
            }

            shuffledNodes.remove(0);
        }

        nodeToProcessors.forEach((node, processors) -> {
            for (ErProcessor processor : processors) {
                resourceApplication.getResourceDispatch().add(new MutablePair<>(processor, node));
            }
        });
        return resourceApplication;
    }

    public ResourceApplication fixDispatch(List<ErServerNode> serverNodes, ResourceApplication resourceApplication) {
        int eggsPerNode = Integer.parseInt(resourceApplication.getOptions().getOrDefault(Dict.CONFKEY_SESSION_PROCESSORS_PER_NODE, MetaInfo.CONFKEY_SESSION_PROCESSORS_PER_NODE.toString()));
        String resourceType = resourceApplication.getOptions().getOrDefault("resourceType", Dict.VCPU_CORE);
        Map<ErServerNode, List<ErProcessor>> nodeToProcessors = new HashMap<>();

        for (String elem : resourceApplication.getProcessorTypes()) {
            for (ErServerNode node : serverNodes) {
                for (int i = 0; i < eggsPerNode; i++) {
                    ErResource resource = new ErResource();
                    resource.setResourceType(resourceType);
                    resource.setAllocated(1L);
                    resource.setStatus(ResourceStatus.PRE_ALLOCATED.name());

                    ErProcessor requiredProcessor = new ErProcessor();
                    requiredProcessor.setServerNodeId(node.getId());
                    requiredProcessor.setProcessorType(elem);
                    requiredProcessor.setCommandEndpoint(new ErEndpoint(node.getEndpoint().getHost(), 0));
                    requiredProcessor.setStatus(ProcessorStatus.NEW.name());
                    requiredProcessor.setResources(Collections.singletonList(resource));
                    if (nodeToProcessors.containsKey(node)) {
                        nodeToProcessors.get(node).add(requiredProcessor);
                    } else {
                        nodeToProcessors.put(node, Collections.singletonList(requiredProcessor));
                    }
                }
            }
        }

        nodeToProcessors.forEach((node, processors) -> {
            for (ErProcessor processor : processors) {
                resourceApplication.getResourceDispatch().add(new MutablePair<>(processor, node));
            }
        });

        return resourceApplication;
    }

    public void submitResourceRequest(ResourceApplication resourceRequest) throws InterruptedException {
        applicationQueue.getBroker().put(resourceRequest);
    }

    public ErSessionMeta submitJodDownload(ResourceApplication resourceApplication) {
        String sessionId = resourceApplication.getSessionId();
        List<ErProcessor> processors = resourceApplication.getProcessors();
        Integer totalProcCount = CollectionUtils.isEmpty(processors) ? 0 : processors.size();

        try {
            lockSession(sessionId);
            try {
                ErSessionMeta session = sessionMainService.getSessionMain(sessionId);
                if (null == session) {
                    ErSessionMeta registerInfo = new ErSessionMeta(sessionId, resourceApplication.getSessionName(), processors, totalProcCount, SessionStatus.NEW.name());
                    sessionMainService.register(registerInfo, true);
                }
                log.info("register session over");
            } catch (Exception e) {
                throw new ErSessionException("session is already created");
            }

            ErSessionMeta erSessionInDb = sessionMainService.getSession(sessionId);

            processors.forEach(processor -> {
                ErSessionMeta erSessionMeta = new ErSessionMeta();
                Map<String, String> newOptions = new HashMap<>(processor.getOptions());
                try {
                    BeanUtils.copyProperties(erSessionMeta, erSessionInDb);
                    erSessionMeta.setOptions(newOptions);
                    String host = processor.getOptions().get(Dict.IP);
                    Integer port = Integer.valueOf(processor.getOptions().get(Dict.PORT));
                    ErEndpoint nodeManagerEndpoint = new ErEndpoint(host, port);
                    NodeManagerClient nodeManagerClient = new NodeManagerClient(nodeManagerEndpoint);
                    nodeManagerClient.startContainers(new Context(), erSessionMeta);
                } catch (Exception e) {
                    log.error("start container error {}", e.getMessage());
                    e.printStackTrace();
                    return;
                }
            });

            long startTimeout = System.currentTimeMillis() + MetaInfo.EGGROLL_SESSION_START_TIMEOUT_MS;
            boolean isStarted = false;

            while (System.currentTimeMillis() <= startTimeout) {
                ErSessionMeta lastSession = sessionMainService.getSession(sessionId);
                if (lastSession.getActiveProcCount() == null || lastSession.getActiveProcCount() < resourceApplication.getProcessors().size()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    isStarted = true;
                    break;
                }
            }

            if (!isStarted) {
                sessionManager.killSession(new Context(), erSessionInDb, SessionStatus.ERROR.name());
                throw new ErSessionException("create download session failed");
            }

            ErSessionMeta erSessionMetaUpdate = new ErSessionMeta();
            try {
                BeanUtils.copyProperties(erSessionMetaUpdate, erSessionInDb);
                erSessionMetaUpdate.setStatus(SessionStatus.ACTIVE.name());
                erSessionMetaUpdate.setActiveProcCount(totalProcCount);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new ErSessionException("session copyProperties  failed");
            }

            sessionMainService.updateSessionMain(erSessionMetaUpdate, sessionMeta -> {
                if (SessionStatus.KILLED.name().equals(sessionMeta.getStatus())) {
                    sessionProcessorService.batchUpdateBySessionId(sessionMeta, sessionId);
                }
            });
            return sessionMainService.getSession(sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            unlockSession(sessionId);
        }
        return null;
    }


    public void lockSession(String sessionId) {
        LockUtils.lock(sessionLockMap, sessionId);
//        ReentrantLock lock = sessionLockMap.get(sessionId);
//        if (lock == null) {
//            sessionLockMap.putIfAbsent(sessionId, new ReentrantLock());
//            lock = sessionLockMap.get(sessionId);
//        }
////        log.debug("lock session {}", sessionId);
//        lock.lock();
    }

    public void unlockSession(String sessionId) {
        LockUtils.unLock(sessionLockMap, sessionId);
//        ReentrantLock lock = sessionLockMap.get(sessionId);
//        if (lock != null) {
////            log.info("unlock session {}", sessionId);
//            lock.unlock();
//        }
    }

    @Override
    public void run(String[] args) throws Exception {
        ClusterManagerTask.runTask(dispatchThread);
        ClusterManagerTask.runTask(lockCleanThread);
        log.info("{} run() end !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", this.getClass().getSimpleName());
    }
}
