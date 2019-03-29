package com.ljz.agent;

import com.ljz.agent.dubbo.model.Request;
import com.ljz.agent.dubbo.model.RpcInvocation;
import com.ljz.agent.model.AgentClientDetailedReq;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 协议转换 ， 单例
 */
public class ServiceSwitcher {
    private static Logger  logger = LoggerFactory.getLogger(ServiceSwitcher.class);
    private static Channel rpcClientChannel = null;
    private static CountDownLatch rpcChannelReady = new CountDownLatch(1);

    public static void setRpcClientChannel(Channel clientChannel) {
        rpcClientChannel = clientChannel;
        rpcChannelReady.countDown();
    }

    /**
     * 服务协议转换至dubbo
     */
    public void switchToDubbo(AgentClientDetailedReq detailedReq) throws Exception {

        Channel channel = rpcClientChannel;
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(detailedReq.getMethod());
        invocation.setAttachment("path", detailedReq.getInterfaceName());
        invocation.setParameterTypes(detailedReq.getParameterTypes());

        invocation.setArguments(detailedReq.getParameter());

        Request request = new Request(detailedReq.getRequestId(), detailedReq.getInterfaceName(),
                detailedReq.getMethod(), detailedReq.getParameterTypes());
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);

        channel.writeAndFlush(request);
    }

}
