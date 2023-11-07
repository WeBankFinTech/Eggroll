package com.eggroll.core.boostrap;

import com.eggroll.core.config.ExtendEnvConf;
import com.eggroll.core.config.MetaInfo;
import com.eggroll.core.utils.CommandArgsUtils;
import com.eggroll.core.utils.PropertiesUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class CommonBoostrap {

    static Logger logger = LoggerFactory.getLogger(CommonBoostrap.class);

    public static void init(String[] args, String module) {
        if (StringUtils.isBlank(System.getProperty("module"))) {
            System.setProperty("module", module);
        }
        CommandLine cmd = CommandArgsUtils.parseArgs(args);
        String confPath;
        if (cmd != null) {
            confPath = cmd.getOptionValue('c', "./conf/eggroll.properties");
        } else {
            confPath = "./conf/eggroll.properties";
        }
        logger.info("load eggroll config file {}", confPath);
        File file = new File(confPath);
        MetaInfo.STATIC_CONF_PATH = file.getAbsolutePath();
        Properties environment = PropertiesUtil.getProperties(confPath);
        MetaInfo.init(environment);

        String extendConfPath =confPath.replace("eggroll.properties","node-extend-env.properties");
        logger.info("load extend config file {}", confPath);
        Properties extendPro = PropertiesUtil.getProperties(extendConfPath);
        ExtendEnvConf.initToMap(extendPro);

    }
}
