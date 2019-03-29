package com.ljz.agent;

import com.ljz.agent.agent.AgentClient;
import com.ljz.agent.httpserver.HttpServer;
import com.ljz.agent.registry.Endpoint;
import com.ljz.agent.registry.EtcdRegistry;
import com.ljz.agent.registry.IRegistry;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务发现（包含负载均衡）
 */
public class ServiceDiscovery {
    private static Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private IRegistry registry;
    private ConcurrentHashMap<String, HashSet<String>> serviceNameToAgentClientMap;
    private ConcurrentHashMap<String, AgentClient> clientNameToAgentClientMap;  //保证单线程write
    private Object lock = new Object();

    private static boolean overFlag = false;  //服务发现是否进行完毕

    private String[] loadBalanceList;

    public ServiceDiscovery(String registryAddr) {
        registry = new EtcdRegistry(registryAddr);
        serviceNameToAgentClientMap = new ConcurrentHashMap<>();
        clientNameToAgentClientMap = new ConcurrentHashMap<>();
    }

    /**
     * 发现最优的AgentClient
     */
    public AgentClient findOptimalAgentClient(String serviceName, EventLoopGroup workGroup, EventLoop eventLoop) throws Exception {
        HashSet<String> agentClients;
        do {
            agentClients = serviceNameToAgentClientMap.getOrDefault(serviceName, null);
            if (agentClients == null || agentClients.size() == 0) {
                synchronized (lock) {
                    agentClients = serviceNameToAgentClientMap.getOrDefault(serviceName, null);
                    if (agentClients == null || agentClients.size() == 0) {
                        findService(serviceName, workGroup);
                    }
                }
            }
        } while (agentClients == null || agentClients.size() == 0 || !overFlag);

        /**
         * 负载均衡策略：eventLoopAgentClientMap存储了eventloop和AgentClient的映射，当请求到来时，优先根据线程对应的
         * eventloop选择相应的AgentClient作转发，以减少线程切换; 在eventLoopAgentClientMap中找不到Agentclient时，采用
         * 加权随机的方式，转发请求
         */
        AgentClient agentClient;
        if ((agentClient = HttpServer.eventLoopAgentClientMap.get(eventLoop)) != null) {
            return agentClient;
        }

        int random = ThreadLocalRandom.current().nextInt(loadBalanceList.length);

        return clientNameToAgentClientMap.get(loadBalanceList[random]);
    }

    /**
     * 服务发现
     * @param serviceName
     * @return
     * @throws Exception
     */
    private boolean findService(String serviceName, EventLoopGroup workGroup) throws Exception {
        List<Endpoint> endpoints = registry.find(serviceName);
        if (endpoints == null) {
            return false;
        }

        //Iterator<EventExecutor> iterator = workGroup.iterator();
        for (Endpoint endpoint : endpoints) {
            AgentClient agentClient = createAgentClient(endpoint, workGroup);
            HashSet<String> agentClients = serviceNameToAgentClientMap.get(serviceName);
            if (agentClients == null) {
                agentClients = new HashSet<>();
                serviceNameToAgentClientMap.put(serviceName, agentClients);
            }
            agentClients.add(agentClient.getName());

        }
        loadBalanceList = getLoadBanlanceList(endpoints);
        overFlag = true;
        return true;
    }

    /**
     * 构建AgentClient
     * @param endpoint
     * @return
     * @throws InterruptedException
     */
    public AgentClient createAgentClient(Endpoint endpoint, EventLoopGroup workGroup) throws InterruptedException {
        String clientName = endpoint.getName();
        AgentClient agentClient = clientNameToAgentClientMap.getOrDefault(clientName, null);
        if (agentClient == null) {
            agentClient = new AgentClient(endpoint.getHost(), endpoint.getPort(), workGroup);
            agentClient.setLoadLevel(endpoint.getLoadLevel());
            clientNameToAgentClientMap.put(clientName, agentClient);
            agentClient.run();
        }

        return agentClient;
    }

    /**
     * 构造用于负载均衡轮询的list
     * @param endpointList
     * @return
     */
    private String[] getLoadBanlanceList(final List<Endpoint> endpointList) {
        List<Endpoint> endpoints = endpointList;
        Collections.sort(endpoints, new Comparator<Endpoint>() {
            @Override
            public int compare(Endpoint e1, Endpoint e2) {
                return e2.getLoadLevel() - e1.getLoadLevel();
            }
        });
        int totalLoad = 0;
        int productOfLoad = 1;
        for (Endpoint endpoint : endpoints) {
            totalLoad += endpoint.getLoadLevel();
            productOfLoad *= endpoint.getLoadLevel();
        }

        int[] loadPerReq = new int[endpoints.size()];
        int[] loadlevelList = new int[endpoints.size()];
        String[] loadBalanceList = new String[totalLoad];
        int loadBalanceListndex = 0;
        int times = totalLoad;
        for (int index = 0; index < endpoints.size(); index++) {
            Endpoint endpoint = endpoints.get(index);
            loadBalanceList[loadBalanceListndex++] = endpoint.getHost() + ":" + endpoint.getPort();

            loadPerReq[index] = productOfLoad / endpoint.getLoadLevel();
            loadlevelList[index] = loadPerReq[index];
            times--;
        }

        while (times > 0) {
            int small = Integer.MAX_VALUE;
            int index = 0;
            for (int i = 0; i < loadlevelList.length; i++) {
                if (loadlevelList[i] < small) {
                    small = loadlevelList[i];
                    index = i;
                }
            }
            loadBalanceList[loadBalanceListndex++] = endpoints.get(index).getHost() + ":" + endpoints.get(index).getPort();
            loadlevelList[index] *= loadPerReq[index];
            times--;
        }

        return loadBalanceList;
    }
}
