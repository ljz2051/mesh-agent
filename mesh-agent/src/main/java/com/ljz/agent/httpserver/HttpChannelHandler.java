package com.ljz.agent.httpserver;

import com.ljz.agent.ServiceDiscovery;
import com.ljz.agent.agent.AgentClient;
import com.ljz.agent.model.AgentServerResponse;
import com.ljz.agent.model.HttpRequestFuture;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class HttpChannelHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(HttpChannelHandler.class);
    private static ServiceDiscovery serviceDiscovery = new ServiceDiscovery(System.getProperty("etcd.url"));

    private static AtomicLong requestId = new AtomicLong(0);

    private static final byte CHAR_EQUAL = 61;  //=
    private static final byte CHAR_AND = 38;    //&
    private static final String INTERFACE_KEY = "interface";
    private static final int HEADER_LENGTH = 12;

    private EventLoopGroup workGroup;
    private  ByteBuf header;

    public HttpChannelHandler(EventLoopGroup workGroup) {
        this.workGroup = workGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf httpContent = (ByteBuf) msg;

        HttpRequestFuture httpRequestFuture = new HttpRequestFuture();
        String interfaceName = getInterfaceName(httpContent);
        long curRequestId = requestId.getAndIncrement();

        /*调用服务发现，找到要转发的AgentClient*/
        AgentClient agentClient = serviceDiscovery.findOptimalAgentClient(interfaceName, workGroup, ctx.channel().eventLoop());

        agentClient.getRequestHolder().put(curRequestId, httpRequestFuture);

        /*用CompositeByteBuf构造要转发的数据*/
        CompositeByteBuf contentByteBuf = PooledByteBufAllocator.DEFAULT.compositeDirectBuffer();
        header = PooledByteBufAllocator.DEFAULT.directBuffer(HEADER_LENGTH);
        header.writeLong(curRequestId);
        header.writeInt(httpContent.readableBytes());
        contentByteBuf.addComponent(true, header).addComponent(true, httpContent);

        //logger.info(contentByteBuf.toString(CharsetUtil.UTF_8));

        Channel channel = agentClient.getChannel();
        channel.writeAndFlush(contentByteBuf);

        /*注册回调函数*/
        httpRequestFuture.addListener(() ->{
            try {
                AgentServerResponse response = httpRequestFuture.get();

                DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, response.getValue());
                setHeaders(httpResponse);
                ctx.writeAndFlush(httpResponse);

             } catch (Exception e) {
                e.printStackTrace();
             }
        }, ctx.executor());

    }

    /**
     * 设置响应头
     * @param response
     */
    private void setHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        //response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    /**
     * 解析服务名
     * @param contentByteBuf
     * @return
     */
    private String getInterfaceName(ByteBuf contentByteBuf) {
        int originIndex = contentByteBuf.readerIndex();
        int lastIndex = originIndex;
        boolean nextIsKey = true;
        boolean nextIsInterfaceName = false;
        String interfaceName;
        while(true) {
            int i = contentByteBuf.forEachByte((value) ->
            {
               if (value == CHAR_EQUAL || value == CHAR_AND) {
                   return false;
               }
               return true;
            });
            if (nextIsKey) {
                String key = contentByteBuf.toString(lastIndex, i - lastIndex, CharsetUtil.UTF_8);
                if (INTERFACE_KEY.equals(key)) {
                    nextIsInterfaceName = true;
                }
                nextIsKey = false;
            } else {
                if(nextIsInterfaceName) {
                    interfaceName = contentByteBuf.toString(lastIndex, i - lastIndex, CharsetUtil.UTF_8);
                    contentByteBuf.readerIndex(originIndex);
                    //contentByteBuf.clear();
                    break;
                }
                nextIsKey = true;
            }
            lastIndex = i + 1;
            contentByteBuf.readerIndex(lastIndex);
        }

        return interfaceName;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
    }
}
