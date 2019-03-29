package com.ljz.agent.agent;

import com.ljz.agent.ServiceSwitcher;
import com.ljz.agent.model.AgentClientDetailedReq;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

//@ChannelHandler.Sharable
public class ProviderAgentServerHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);
    private ConcurrentHashMap<Long, Channel> requestHolder;
    private ServiceSwitcher serviceSwitcher = new ServiceSwitcher();

    public ProviderAgentServerHandler(ConcurrentHashMap<Long, Channel> requestHolder,  EventLoopGroup workerGroup) {
        this.requestHolder = requestHolder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AgentClientDetailedReq) {
            AgentClientDetailedReq agentClientDetailedReq = (AgentClientDetailedReq) msg;

            serviceSwitcher.switchToDubbo(agentClientDetailedReq);    //转换成dubbo

            requestHolder.put(agentClientDetailedReq.getRequestId(), ctx.channel());
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
    }
}
