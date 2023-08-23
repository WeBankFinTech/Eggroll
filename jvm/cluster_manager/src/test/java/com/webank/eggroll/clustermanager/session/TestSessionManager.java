package com.webank.eggroll.clustermanager.session;

import com.eggroll.core.config.Dict;
import com.eggroll.core.grpc.ClusterManagerClient;
import com.eggroll.core.pojo.ErEndpoint;
import com.eggroll.core.pojo.ErSessionMeta;
import com.webank.eggroll.clustermanager.grpc.GrpcServer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

//@SpringBootTest(classes = Application.class)
//@RunWith(SpringJUnit4ClassRunner.class)
public class TestSessionManager {

    Logger logger = LoggerFactory.getLogger(TestSessionManager.class);

    ErEndpoint endpoint = new ErEndpoint("localhost:4670");

    @Test
    public void  testCreateSession(){
    }

    @Test
    public void testGetOrCreate() {
        //new ErSessionMeta(id = "testing_reg"+System.currentTimeMillis()+"_"+scala.util.Random.nextInt(100).toString, options = Map(SessionConfKeys.CONFKEY_SESSION_PROCESSORS_PER_NODE -> "2"))
        ErSessionMeta getOrCreateSessionMeta = new ErSessionMeta();
        getOrCreateSessionMeta.setId("testx_"+System.currentTimeMillis());
        Map<String,String> options = new HashMap<>();
        options.put(Dict.CONFKEY_SESSION_PROCESSORS_PER_NODE ,"2");
        getOrCreateSessionMeta.setOptions(options);
        getOrCreateSessionMeta.setName("Test1");
        getOrCreateSessionMeta.setActiveProcCount(1);
        getOrCreateSessionMeta.setTotalProcCount(4);
        ClusterManagerClient clusterManagerClient = new ClusterManagerClient(endpoint);
        ErSessionMeta result = clusterManagerClient.getOrCreateSession(getOrCreateSessionMeta);
        logger.info("====================>result.id = {} , result.status = {}" ,result.getId(),result.getStatus());
    }

    @Test
    public void testGetSession() {
        ErSessionMeta getOrCreateSessionMeta = new ErSessionMeta();
        getOrCreateSessionMeta.setId("testx_1692327334950");
        Map<String,String> options = new HashMap<>();
        options.put(Dict.CONFKEY_SESSION_PROCESSORS_PER_NODE ,"2");
        getOrCreateSessionMeta.setOptions(options);
        getOrCreateSessionMeta.setActiveProcCount(1);
        getOrCreateSessionMeta.setTotalProcCount(4);
        ClusterManagerClient clusterManagerClient = new ClusterManagerClient(endpoint);
        ErSessionMeta result = clusterManagerClient.getSession(getOrCreateSessionMeta);
        logger.info("====================>result.id = {} , result.status = {}" ,result.getId(),result.getStatus());
    }

    @Test
    public void testKillSession() {
        ErSessionMeta getOrCreateSessionMeta = new ErSessionMeta();
        getOrCreateSessionMeta.setId("testx_1692342316761");
        Map<String,String> options = new HashMap<>();
        options.put(Dict.CONFKEY_SESSION_PROCESSORS_PER_NODE ,"2");
        getOrCreateSessionMeta.setOptions(options);
        getOrCreateSessionMeta.setActiveProcCount(1);
        getOrCreateSessionMeta.setTotalProcCount(4);
        ClusterManagerClient clusterManagerClient = new ClusterManagerClient(endpoint);
        ErSessionMeta result = clusterManagerClient.killSession(getOrCreateSessionMeta);
        logger.info("====================>result.id = {} , result.status = {}" ,result.getId(),result.getStatus());
    }

    @Test
    public void testKillAllSession() {
        ErSessionMeta getOrCreateSessionMeta = new ErSessionMeta();
        getOrCreateSessionMeta.setId("testx_1692263572251");
        Map<String,String> options = new HashMap<>();
        options.put(Dict.CONFKEY_SESSION_PROCESSORS_PER_NODE ,"2");
        getOrCreateSessionMeta.setOptions(options);
        getOrCreateSessionMeta.setActiveProcCount(1);
        getOrCreateSessionMeta.setTotalProcCount(4);
        ClusterManagerClient clusterManagerClient = new ClusterManagerClient(endpoint);
        ErSessionMeta result = clusterManagerClient.killAllSession(getOrCreateSessionMeta);
        logger.info("====================>result.id = {} , result.status = {}" ,result.getId(),result.getStatus());
    }
}
