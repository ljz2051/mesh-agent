package com.ljz.agent.model;

import java.net.URLDecoder;

public class AgentClientDetailedReq {

    private long requestId;

    private String interfaceName;     //interface

    private String method;         //method

    private String parameterTypes;   //parameterTypesString
    private String parameter;       //parameter

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public static final String INTERFACE_KEY = "interface";
    public static final String METHOD_KEY = "method";
    public static final String PARAMETER_TYPE_KEY = "parameterTypesString";
    public static final String PARAMETER_KEY = "parameter";

    public void processParam(String key, String value) throws Exception {
        if (INTERFACE_KEY.equals(key)) {
            setInterfaceName(value);
        } else if (METHOD_KEY.equals(key)) {
            setMethod(value);
        } else if (PARAMETER_TYPE_KEY.equals(key))  {
            setParameterTypes(URLDecoder.decode(value, "utf-8"));
        } else if (PARAMETER_KEY.equals(key)) {
            setParameter(URLDecoder.decode(value, "utf-8"));
        }
    }
}
