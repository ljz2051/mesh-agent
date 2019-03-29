package com.ljz.agent.agent;

import com.ljz.agent.httpserver.HttpServer;
import com.ljz.agent.model.HttpRequestFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;


public class AgentClient {

    private static Logger logger = LoggerFactory.getLogger(AgentClient.class);
    private String name;
    private String host;
    private int port;
    private int loadLevel;
    private ConcurrentHashMap<Long, HttpRequestFuture> requestHolder;

    public void setLoadLevel(int loadLevel) {
        this.loadLevel = loadLevel;
    }

    private Channel channel;

    private EventLoopGroup workGroup;

    public AgentClient(String host, int port, EventLoopGroup workGroup) {
        this.name = host + ":" + String.valueOf(port);
        this.host= host;
        this.port = port;
        requestHolder = new ConcurrentHashMap<>(10000);
        this.workGroup = workGroup;
    }

    public ConcurrentHashMap<Long, HttpRequestFuture> getRequestHolder() {
        return requestHolder;
    }

    public void run() throws InterruptedException {
        logger.info("start run agent.....");
        /* try {*/
            Bootstrap b = new Bootstrap();

            b.group(workGroup)
                    .channel(EpollSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    //.option(ChannelOption.SO_BACKLOG, 1024)
                    //.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(16 * 1024, 32 * 1024))
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ConsumerAgentDecoder());
                            ch.pipeline().addLast(new ConsumerAgentClientHandler(requestHolder));
                        }
                    });

              channel = b.connect(host, port).channel();
              HttpServer.eventLoopAgentClientMap.put(channel.eventLoop(), this);

       /* } finally {
            workGroup.shutdownGracefully();
        }*/
    }

    public Channel getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.name + ":" + this.loadLevel;
    }

}
