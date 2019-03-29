package com.ljz.agent.agent;

import com.ljz.agent.model.AgentServerResponse;
import com.ljz.agent.model.HttpRequestFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ConcurrentHashMap;

public class ConsumerAgentClientHandler extends ChannelInboundHandlerAdapter{
   private ConcurrentHashMap<Long, HttpRequestFuture> requestHolder;

   public ConsumerAgentClientHandler(ConcurrentHashMap<Long, HttpRequestFuture> requestHolder) {
       this.requestHolder = requestHolder;
   }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AgentServerResponse response = (AgentServerResponse) msg;
        long requestId = response.getRequestId();
        HttpRequestFuture future = requestHolder.get(requestId);
        requestHolder.remove(requestId);
        future.done(response);
    }
}
