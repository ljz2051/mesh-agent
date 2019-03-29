package com.ljz.agent.dubbo.model;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * @author ken.lj
 * @date 02/04/2018
 */
public class JsonUtils {

    public static String lineSeparator = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("line.separator"));

    public static void writeObject(Object obj, PrintWriter writer) throws IOException {
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);
        serializer.config(SerializerFeature.WriteEnumUsingToString, true);
        serializer.write(obj);
        out.writeTo(writer);
        out.close(); // for reuse SerializeWriter buf
        writer.println();
        writer.flush();
    }

    public static void writeBytes(byte[] b, PrintWriter writer) {
        writer.print(new String(b));
        writer.flush();
    }

    /*public static void writeDobboRpcInvocation(RpcInvocation inv, PrintWriter writer) throws IOException {
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);
        serializer.config(SerializerFeature.WriteEnumUsingToString, true);
        serializer.write(inv.getAttachment("dubbo", "2.0.1"));
        serializer.write(lineSeparator);
        serializer.write(inv.getAttachment("path"));
        serializer.write(lineSeparator);
        serializer.write(inv.getAttachment("version"));
        serializer.write(lineSeparator);
        serializer.write(inv.getMethodName());
        serializer.write(lineSeparator);
        serializer.write(inv.getParameterTypes());
        serializer.write(lineSeparator);
        serializer.write(inv.getArguments());
        serializer.write(lineSeparator);
        serializer.write(inv.getAttachments());
        serializer.write(lineSeparator);
        out.writeTo(writer);
        out.close(); // for reuse SerializeWriter buf
        //writer.println();
        writer.flush();
    }*/
}
