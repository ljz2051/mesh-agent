package com.ljz.agent.dubbo;

import com.ljz.agent.dubbo.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class RpcClientHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<Long, Channel> requestHolder;

    public RpcClientHandler(ConcurrentHashMap<Long, Channel> requestHolder) {
        this.requestHolder = requestHolder;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcResponse) {
            RpcResponse response = (RpcResponse) msg;
            ByteBuf header = ctx.alloc().directBuffer(12);
            long  requestId = response.getRequestId();

            header.writeLong(requestId);
            header.writeInt(response.getValue().readableBytes());

            Channel channel = requestHolder.get(requestId);

            channel.write(header);
            channel.writeAndFlush(response.getValue());

            requestHolder.remove(requestId);

            ReferenceCountUtil.release(msg);

        }
    }

}
