package com.ljz.agent.agent;

import com.ljz.agent.model.AgentServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ConsumerAgentDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 12;

    private AgentServerResponse response;
    private int dataLength;
    private boolean isHeader = true;

    /*接收响应解码*/
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        /*接收头部*/
        if (isHeader) {
            if(in.readableBytes() <  HEADER_LENGTH){
                return;
            }
            /*解析头部*/
            response = new AgentServerResponse();
            response.setRequestId(in.readLong());
            dataLength = in.readInt();

            isHeader = false;
        }

        /*接收返回值*/
        if (in.readableBytes() < dataLength) {
            return;
        }

        ByteBuf returnValue = ctx.alloc().directBuffer(256);

        in.readBytes(returnValue, dataLength);

        response.setValue(returnValue);

        out.add(response);

        /*下一个字节开始是头*/
        isHeader = true;
    }
}
