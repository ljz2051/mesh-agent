package com.ljz.agent;

import com.ljz.agent.agent.AgentServer;
import com.ljz.agent.dubbo.DubboConnectManager;
import com.ljz.agent.httpserver.HttpServer;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class AgentApp {
    private static Logger logger = LoggerFactory.getLogger(AgentApp.class);

    public static void main(String[] args) throws Exception {
        String type = System.getProperty("type");
        logger.info("================================================================");

        if ("consumer".equals(type)) {
            new HttpServer().bind(Integer.parseInt(System.getProperty("server.port")));
        } else if("provider".equals(type)) {
            ConcurrentHashMap<Long, Channel> requestHolder = new ConcurrentHashMap<Long, Channel>(10000);
            EventLoopGroup workerGroup = new EpollEventLoopGroup(1);
            //((EpollEventLoopGroup) workerGroup).setIoRatio(65);
            /*先与Dubbo进行连接*/
            DubboConnectManager dubboConnectManager = new DubboConnectManager(requestHolder,workerGroup);
            Channel dubboChannel = null;
            while(dubboChannel == null) {
                logger.info("Connecting to Dubbo..");
                try{
                    dubboChannel = dubboConnectManager.getChannel();
                } catch (Exception e){
                    logger.error(e.getMessage());
                    Thread.sleep(500);
                }
            }

            ServiceSwitcher.setRpcClientChannel(dubboChannel);

            int port = Integer.parseInt(System.getProperty("server.port"));

            new AgentServer(port, requestHolder, workerGroup).run();
        } else {
            logger.error("Environment variable type is needed to set to provider or consumer.");
        }

    }
}
