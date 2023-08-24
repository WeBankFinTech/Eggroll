package com.webank.eggroll.clustermanager.job;

import com.eggroll.core.constant.SessionStatus;
import com.eggroll.core.grpc.NodeManagerClient;
import com.eggroll.core.pojo.*;
import com.webank.eggroll.clustermanager.cluster.ClusterResourceManager;
import com.webank.eggroll.clustermanager.dao.impl.ServerNodeService;
import com.webank.eggroll.clustermanager.dao.impl.SessionMainService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class JobServiceHandler {
    Logger log = LoggerFactory.getLogger(JobServiceHandler.class);

    @Autowired
    ClusterResourceManager clusterResourceManager;
    @Autowired
    SessionMainService sessionMainService;
    @Autowired
    ServerNodeService serverNodeService;

    public void killJob(String sessionId) {
        log.info("killing job {}", sessionId);
        try {
            clusterResourceManager.lockSession(sessionId);
            clusterResourceManager.getKillJobMap().put(sessionId, System.currentTimeMillis());
            if (sessionMainService.getById(sessionId) == null) {
                return;
            }
            ErSessionMeta sessionMeta = sessionMainService.getSession(sessionId);
            if (StringUtils.equalsAny(sessionMeta.getStatus(), SessionStatus.KILLED.name(), SessionStatus.CLOSED.name(), SessionStatus.ERROR.name())) {
                return;
            }
            Map<Long, List<ErProcessor>> groupMap = sessionMeta.getProcessors().stream().collect(Collectors.groupingBy(ErProcessor::getServerNodeId));
            Map<ErServerNode, List<ErProcessor>> nodeAndProcessors = new HashMap<>();
            groupMap.forEach((nodeId, processors) -> {
                ErServerNode erServerNode = serverNodeService.getById(nodeId).toErServerNode();
                KillContainersRequest killContainersRequest = new KillContainersRequest();
                killContainersRequest.setSessionId(sessionId);
                List<Long> processorIdList = new ArrayList<>();
                for (ErProcessor processor : processors) {
                    processorIdList.add(processor.getId());
                }
                try {
                    killContainersRequest.setContainers(processorIdList);
                    new NodeManagerClient(erServerNode.getEndpoint()).killJobContainers(killContainersRequest);
                } catch (Exception e) {
                    log.error("killContainers error : ", e);
                }
            });
        } finally {
            clusterResourceManager.unlockSession(sessionId);
        }
    }

//    public SubmitJobResponse handleSubmit(SubmitJobRequest submitJobMeta) {
//        if (JobProcessorTypes.DeepSpeed.name().equals(submitJobMeta.getJobType())) {
//            return handleDeepspeedSubmit(submitJobMeta);
//        } else {
//            throw new IllegalArgumentException("unsupported job type: " + submitJobMeta.getJobType());
//        }
//    }


    public QueryJobStatusResponse handleJobStatusQuery(QueryJobStatusRequest queryJobStatusRequest) {
        String sessionId = queryJobStatusRequest.getSessionId();
        ErSessionMeta sessionMain = sessionMainService.getSessionMain(sessionId);
        QueryJobStatusResponse queryJobStatusResponse = new QueryJobStatusResponse();
        queryJobStatusResponse.setSessionId(sessionId);
        queryJobStatusResponse.setStatus(sessionMain==null?null:sessionMain.getStatus());
        return queryJobStatusResponse;
    }

    public KillJobResponse handleJobKill(KillJobRequest killJobRequest) {
        String sessionId = killJobRequest.getSessionId();
        killJob(sessionId);
        KillJobResponse response = new KillJobResponse();
        response.setSessionId(sessionId);
        return response;
    }

    public StopJobResponse handleJobStop(StopJobRequest stopJobRequest) {
        String sessionId = stopJobRequest.getSessionId();
        killJob(sessionId);
        StopJobResponse response = new StopJobResponse();
        response.setSessionId(sessionId);
        return response;
    }

//    private SubmitJobResponse handleDeepspeedSubmit(SubmitJobRequest submitJobRequest) throws InterruptedException {
//        String sessionId = submitJobRequest.getSessionId();
//        int worldSize = submitJobRequest.getWorldSize();
//
//        // prepare processors
//        List<ErProcessor> prepareProcessors = new ArrayList<>();
//        for (int i = 0; i < worldSize; i++) {
//            ErProcessor erProcessor = new ErProcessor();
//            erProcessor.setProcessorType(JobProcessorTypes.DeepSpeed.name());
//            erProcessor.setStatus(ProcessorStatus.NEW.name());
//
//            ErResource erResource = new ErResource();
//            erResource.setResourceType(Dict.VGPU_CORE);
//            erResource.setAllocated(1L);
//            erResource.setStatus(ResourceStatus.PRE_ALLOCATED.name());
//            erProcessor.getResources().add(erResource);
//
//            prepareProcessors.add(erProcessor);
//        }
//
//        ResourceApplication resourceApplication = new ResourceApplication();
//        resourceApplication.setSortByResourceType(Dict.VCPU_CORE);
//        resourceApplication.setProcessors(prepareProcessors);
//        resourceApplication.setResourceExhaustedStrategy(Dict.WAITING);
//        resourceApplication.setTimeout(submitJobRequest.getResourceOptions().getTimeoutSeconds() * 1000);
//        resourceApplication.setSessionId(sessionId);
//        resourceApplication.setSessionName(JobProcessorTypes.DeepSpeed.toString());
//        log.info("submitting resource request: " + resourceApplication + ", " + resourceApplication.hashCode());
//
//        clusterResourceManager.submitResourceRequest(resourceApplication);
//        List<Map<ErProcessor, ErServerNode>> dispatchedProcessorList = resourceApplication.getResult();
//        log.info("submitted resource request: " + resourceApplication + ", " + resourceApplication.hashCode());
//        log.info("dispatchedProcessor: " + JsonUtil.object2Json(dispatchedProcessorList));
//
//        try {
//            //锁不能移到分配资源之前，会造成死锁
//            clusterResourceManager.lockSession(sessionId);
//            if (!clusterResourceManager.getKillJobMap().containsKey(sessionId)) {
//                ErSessionMeta registeredSessionMeta = sessionMainService.getSession(submitJobRequest.getSessionId());
//                List<Pair<Map<ErProcessor, ErServerNode>, ErProcessor>> pariList = new ArrayList<>();
//                for (int i = 0; i < dispatchedProcessorList.size(); i++) {
//                    Map<ErProcessor, ErServerNode> erProcessorErServerNodeMap = dispatchedProcessorList.get(i);
//                    ErProcessor registeredProcessor = registeredSessionMeta.getProcessors().get(i);
//                    erProcessorErServerNodeMap.forEach((processor, node) -> {
//                        processor.setId(registeredProcessor.getId());
//                    });
//                    pariList.add(new Pair<>(erProcessorErServerNodeMap, registeredProcessor));
//                }
//
//                List<Pair<Long, ErServerNode>> deepspeedConfigsWithNode = new ArrayList<>();
//                List<DeepspeedContainerConfig> deepspeedContainerConfigList = new ArrayList<>();
//                AtomicInteger globalRank = new AtomicInteger(0);
//                AtomicInteger crossSize = new AtomicInteger(0);
//                AtomicInteger crossRank = new AtomicInteger(0);
//                Map<ErProcessor, List<Pair<Map<ErProcessor, ErServerNode>, ErProcessor>>> collect = pariList.stream()
//                        .collect(Collectors.groupingBy(Pair::getValue));
//                collect.forEach((node,processorAndNodeArray)->{
//                    crossSize.getAndIncrement();
//                    int localSize = processorAndNodeArray.size();
//                    List<Integer> cudaVisibleDevices = new ArrayList<>();
//                    processorAndNodeArray.forEach(pair -> {
//                        String[] devicesForProcessor = pair.getValue().getOptions().getOrDefault("cudaVisibleDevices", "-1").split(",");
//                        for (String devicesStr : devicesForProcessor) {
//                            int device = Integer.parseInt(devicesStr);
//                            cudaVisibleDevices.add(device);
//                            if (device < 0) {
//                                throw new IllegalArgumentException("cudaVisibleDevices is not set or invalid: " + JsonUtil.object2Json(pair.getKey().getOptions().get("cudaVisibleDevices")));
//                            }
//                        }
//                    });
//
//                    if (cudaVisibleDevices.stream().distinct().count() != cudaVisibleDevices.size()) {
//                        throw new IllegalArgumentException("duplicate cudaVisibleDevices: " + JsonUtil.object2Json(cudaVisibleDevices));
//                    }
//
//                    int localRank = 0;
//                    for (Pair<Map<ErProcessor, ErServerNode>, ErProcessor> pair : processorAndNodeArray) {
//                        DeepspeedContainerConfig deepspeedContainerConfig = new DeepspeedContainerConfig();
//                        deepspeedContainerConfig.setCudaVisibleDevices(cudaVisibleDevices);
//                        deepspeedContainerConfig.setWorldSize(worldSize);
//                        deepspeedContainerConfig.setCrossRank(crossRank.get());
//                        deepspeedContainerConfig.setCrossSize(crossSize.get());
//                        deepspeedContainerConfig.setLocalSize(localSize);
//                        deepspeedContainerConfig.setLocalRank(localRank);
//                        deepspeedContainerConfig.setRank(globalRank.get());
//                        deepspeedContainerConfig.setStorePrefix(sessionId);
//                        deepspeedConfigsWithNode.add(new Pair<Long, ErServerNode>(
//                                pair.getValue().getId(),
//                                node));
//                        deepspeedContainerConfigList.add(deepspeedContainerConfig);
//                        localRank++;
//                        globalRank.addAndGet(1);
//                    }
//
//                    crossRank.addAndGet(1);
//                });
//                long[] ranksProcessorId = deepspeedConfigsWithNode.stream().mapToLong(Triple::getFirst).toArray();
//                long[] ranksNodeId = deepspeedConfigsWithNode.stream().mapToLong(triple -> triple.getSecond().getId()).toArray();
//                int[] ranksLocalRank = deepspeedConfigsWithNode.stream().mapToInt(Triple::getThird).toArray();
//                int[] ranksRank = deepspeedConfigsWithNode.stream().mapToInt(triple -> triple.getThird().getRank()).toArray();
//
//                smDao.registerRanks(sessionId, ranksProcessorId, ranksNodeId, ranksLocalRank, ranksRank);
//
//                deepspeedConfigsWithNode.parallelStream().forEach(nodeAndConfigs -> {
//                    ErServerNode node = nodeAndConfigs.getSecond();
//                    Map<Long, DeepspeedContainerConfig> deepspeedConfigs = nodeAndConfigs.getThird().stream()
//                            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
//
//                    NodeManagerClient nodeManagerClient = new NodeManagerClient(node.getEndpoint());
//                    nodeManagerClient.startJobContainers(new StartDeepspeedContainerRequest(
//                            sessionId,
//                            submitJobRequest.getName(),
//                            submitJobRequest.getCommandArguments(),
//                            submitJobRequest.getEnvironmentVariables(),
//                            submitJobRequest.getFiles(),
//                            submitJobRequest.getZippedFiles(),
//                            deepspeedConfigs,
//                            submitJobRequest.getOptions()
//                    ));
//                });
//
//                long startTimeout = System.currentTimeMillis() + SessionConfKeys.EGGROLL_SESSION_START_TIMEOUT_MS.get();
//                ErProcessor[] activeProcessors = waitSubmittedContainers(sessionId, worldSize, startTimeout);
//
//                Map<Long, Map<String, String>> idToOptions = Arrays.stream(dispatchedProcessors)
//                        .collect(Collectors.toMap(ErProcessor::getId, ErProcessor::getOptions));
//
//                for (ErProcessor processor : activeProcessors) {
//                    Map<String, String> options = idToOptions.get(processor.getId());
//                    processor.getOptions().putAll(options);
//                }
//
//                smDao.updateSessionStatus(sessionId, SessionStatus.ACTIVE, SessionStatus.NEW);
//
//                return new SubmitJobResponse(sessionId, Arrays.asList(activeProcessors));
//            } else {
//                logError("kill session " + sessionId + " request was found");
//                throw new ErSessionException("kill session " + sessionId + " request was found");
//            }
//        } catch (Exception e) {
//            killJob(sessionId, false);
//            throw e;
//        } finally {
//            ClusterResourceManager.unlockSession(sessionId);
//        }
//    }
}
