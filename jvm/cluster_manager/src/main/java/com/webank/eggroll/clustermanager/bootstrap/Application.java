package com.webank.eggroll.clustermanager.bootstrap;


import com.webank.eggroll.core.ContextHolder;
import com.webank.eggroll.core.resourcemanager.ClusterManagerBootstrap;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.webank.eggroll.clustermanager.dao.mapper")
@SpringBootApplication
@ConfigurationProperties
//@PropertySource(value = "classpath:eggroll.properties1", ignoreResourceNotFound = false)
@EnableScheduling
public class Application {
    static Logger logger = LoggerFactory.getLogger(Application.class);

    public static ApplicationContext context  ;

    public static void main(String[] args) {
        System.setProperty("spring.config.name","eggroll");
        ClusterManagerBootstrap clusterManagerBootstrap = new ClusterManagerBootstrap();
        context=  new SpringApplicationBuilder(Application.class).run(args);
        Runtime.getRuntime().addShutdownHook(new Thread(() ->{

        }));


        ContextHolder.context_$eq(context);
        clusterManagerBootstrap.init(args);
        clusterManagerBootstrap.start();
        synchronized(context) {
            try {
                context.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }




}