package com.ljz.agent.dubbo;

import com.ljz.agent.dubbo.model.Bytes;
import com.ljz.agent.dubbo.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder {
    private static Logger logger = LoggerFactory.getLogger(DubboRpcDecoder.class);
    // header length.
    protected static final int HEADER_LENGTH = 16;
    private Boolean isHeader = true;
    private long requestId;
    private int dataLength;


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        if (isHeader) {
            if (byteBuf.readableBytes() < HEADER_LENGTH) {
                return;
            }
            /*解析报头*/
            byte[] header = new byte[HEADER_LENGTH];
            byteBuf.readBytes(header, 0, HEADER_LENGTH);
            requestId = Bytes.bytes2long(header, 4);
            dataLength = Bytes.bytes2int(header, 12);

            isHeader = false;
        }

        if (byteBuf.readableBytes() < dataLength) {
            return;
        }

        ByteBuf value = channelHandlerContext.alloc().directBuffer(2048);

        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);

        byteBuf.readBytes(value, dataLength);

        value.readerIndex(value.readerIndex() + 2);
        value.writerIndex(value.writerIndex() - 1);

        response.setValue(value);

        list.add(response);

        isHeader = true;
    }

}
