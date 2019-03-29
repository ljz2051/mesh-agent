package com.ljz.agent.dubbo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ConcurrentHashMap;

public class DubboConnectManager {
    //private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

    private Bootstrap bootstrap;

    private Channel channel;
    private Object lock = new Object();
    private ConcurrentHashMap<Long, Channel> requestHolder;

    private EventLoopGroup workerGroup;

    public DubboConnectManager(ConcurrentHashMap<Long, Channel> requestHolder, EventLoopGroup workerGroup) {
        this.requestHolder = requestHolder;
        this.workerGroup = workerGroup;
    }

    public Channel getChannel() throws Exception {
        if (null != channel) {
            return channel;
        }

        if (null == bootstrap) {
            synchronized (lock) {
                if (null == bootstrap) {
                    initBootstrap();
                }
            }
        }

        if (null == channel) {
            synchronized (lock){
                if (null == channel){
                    int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));
                    channel = bootstrap.connect("127.0.0.1", port).sync().channel();
                }
            }
        }

        return channel;
    }

    public void initBootstrap() {

        bootstrap = new Bootstrap()
                .group(workerGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.SO_BACKLOG, 1024)
                .channel(EpollSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>()  {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //pipeline.addLast(new DubboRpcOutHandler());
                        pipeline.addLast(new DubboRpcEncoder());
;                        pipeline.addLast(new DubboRpcDecoder());
                        pipeline.addLast(new RpcClientHandler(requestHolder));
                    }
                });
    }
}
