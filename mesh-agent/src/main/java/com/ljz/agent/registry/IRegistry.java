package com.ljz.agent.registry;

import java.util.List;

public interface IRegistry {

    // 注册服务
    void register(String serviceName) throws Exception;

    void register(String serviceName, int port, String loadLevel) throws Exception;

    List<Endpoint> find(String serviceName) throws Exception;
}
