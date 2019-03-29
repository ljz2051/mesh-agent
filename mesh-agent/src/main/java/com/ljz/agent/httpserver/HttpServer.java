package com.ljz.agent.httpserver;

import com.ljz.agent.agent.AgentClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {
    private static Logger logger = LoggerFactory.getLogger(HttpServer.class);

    public static ConcurrentHashMap<EventLoop, AgentClient> eventLoopAgentClientMap = new ConcurrentHashMap<>(4);

    public void bind(int port) throws Exception {
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EventLoopGroup workGroup = new EpollEventLoopGroup(3);
        //((NioEventLoopGroup) workGroup).setIoRatio(45);
        try {
            ServerBootstrap b = new ServerBootstrap();
            logger.info("httpServer start1 ...");
            b.group(bossGroup, workGroup)
                    .channel(EpollServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 512)
                    //.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 *1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            sc.config().setAllocator(PooledByteBufAllocator.DEFAULT);
                            sc.pipeline().addLast(new HttpAdvanceRequestDecoder());
                            sc.pipeline().addLast(new HttpResponseEncoder());
                            sc.pipeline().addLast(new HttpChannelHandler(workGroup));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
                    //.childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)

            ChannelFuture f = b.bind(port).sync();
            logger.info("httpServer start2 ...");
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
