package com.ljz.agent.agent;

import com.ljz.agent.model.AgentClientDetailedReq;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProviderAgentDecoder extends ByteToMessageDecoder {

    private static Logger logger = LoggerFactory.getLogger(ProviderAgentDecoder.class);

    private static final int HEADER_LENGTH = 12;

    private boolean isHeader = true;

    private AgentClientDetailedReq agentClientDetailedReq;

    private int dataLength;

    private static final byte CHAR_EQUAL = 61;  //=
    private static final byte CHAR_AND = 38;    //&

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception{

        if (isHeader) {
            if (in.readableBytes() < HEADER_LENGTH) {
                return;
            }
            agentClientDetailedReq = new AgentClientDetailedReq();

            agentClientDetailedReq.setRequestId(in.readLong());

            dataLength = in.readInt();

            isHeader = false;
        }

        if (in.readableBytes() < dataLength) {
            return;
        }

        decodeAgentClientReq(agentClientDetailedReq, in);
        out.add(agentClientDetailedReq);

        isHeader = true;
    }

    /**
     * 解析服务名、方法名、参数类型和参数
     * @param agentReq
     * @param reqData
     * @throws Exception
     */
    private void decodeAgentClientReq(AgentClientDetailedReq agentReq, ByteBuf reqData)throws Exception {
        int originIndex = reqData.readerIndex();
        int remaiderlength = dataLength;
        int lastIndex = originIndex;
        boolean nextIsKey = true;
        String key = "";
        String value;

        while(true) {
            int i = reqData.forEachByte(lastIndex, remaiderlength, (ch) -> {

                    if(ch == CHAR_EQUAL || ch == CHAR_AND) {
                        return false;
                    }
                    return true;
            });
            if (nextIsKey) {
                key = reqData.toString(lastIndex, i - lastIndex, CharsetUtil.UTF_8);
                nextIsKey = false;
               /* if (key.equals(PARAMETER_KEY)) {
                    //ByteBuf parametetr = UnpooledByteBufAllocator.DEFAULT.directBuffer(1024);
                    ByteBuf parametetr = reqData.copy(i+ 1, remaiderlength-i-1+lastIndex);
                    //reqData.writeBytes(parametetr, i + 1, remaiderlength-i-1+lastIndex);
                    agentReq.setParameter(parametetr);
                    //logger.info(parametetr.toString(CharsetUtil.UTF_8));
                    break;
                }*/
            } else {
                if (i == -1) {
                    value = reqData.toString(lastIndex, remaiderlength, CharsetUtil.UTF_8);
                    agentReq.processParam(key, value);
                    reqData.readerIndex(originIndex + dataLength);
                    break;
                } else {
                    value = reqData.toString(lastIndex, i - lastIndex, CharsetUtil.UTF_8);
                    agentReq.processParam(key, value);
                    nextIsKey = true;
                }

            }
            remaiderlength = remaiderlength - (i + 1 - lastIndex);
            lastIndex = i + 1;
        }
    }
}
