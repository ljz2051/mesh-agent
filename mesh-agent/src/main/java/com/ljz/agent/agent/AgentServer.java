package com.ljz.agent.agent;

import com.ljz.agent.registry.EtcdRegistry;
import com.ljz.agent.registry.IRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class AgentServer {
    private static Logger logger = LoggerFactory.getLogger(AgentServer.class);
    private IRegistry registry;
    private static final String SERVICE_NAME = "com.alibaba.dubbo.performance.demo.provider.IHelloService";

    private int port;
    private ConcurrentHashMap<Long, Channel> requestHolder;
    private EventLoopGroup workerGroup;
//
    public AgentServer(int port, ConcurrentHashMap<Long, Channel> requestHolder, EventLoopGroup workerGroup) {
        this.port = port;
        this.requestHolder = requestHolder;
        this.workerGroup = workerGroup;
    }

    public void run() throws Exception {
        logger.info("Starting netty server for agent .....");
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        //EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ProviderAgentDecoder());
                        //ch.pipeline().addLast(new ProviderAgentOutHandler());
                        ch.pipeline().addLast(new ProviderAgentServerHandler(requestHolder, workerGroup));
                    }
                })
                //.option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        b.bind(port).sync();

        /*向etcd注册服务*/
        logger.info("Register service!");
        registry = new EtcdRegistry(System.getProperty("etcd.url"));
        try {
            registry.register(SERVICE_NAME, this.port, System.getProperty("load.level"));
        } catch (Exception e) {
            logger.error("Failed to register service!:{}", e);
            return;
        }
        logger.info("Register success!");
    }
}
