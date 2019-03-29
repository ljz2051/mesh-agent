# mesh-agent
2018年阿里中间件比赛初赛代码由阿里云code迁移至github

初赛题目地址：https://tianchi.aliyun.com/competition/entrance/231657/information    

详细说明：https://code.aliyun.com/middlewarerace2018/docs   需要登陆阿里云code

初赛解题思路：《Service Mesh Agent for Apache Dubbo (Incubating) 》

### （1）赛题解读
赛题要求实现一个高性能的Agent，Agent必须具有服务注册与发现、负载均衡和协议转换的功能，并且要具有一定的通用性。赛题限定的系统架构如下所示：  
![赛题系统架构图](/images/structure.png)    
首先，Provider Agent启动时会将Provider提供的服务注册到ETCD上。然后Consumer处发起http请求，Consumer Agent处拦截到Consumer的请求后，依赖服务名到ETCD上做服务发现，然后采用一定的负载均衡算法，将消息发送到provider侧。Provider侧由Provider Agent 接收消息，经过处理后，将消息转发到Provider，等候Provider响应。Provider响应完成后，由Provider Agent将响应消息回传到相应的Consumer侧。Consumer侧由Consumer Agent接收响应并处理后，回传到相应的Consumer。
整个过程，Consumer侧的Agent需要完成的功能有：服务发现、负载均衡、消息传输和处理、协议转换，Provider侧的Agent需要完成的功能有：服务注册、协议转换、消息传输和处理。

### （2）核心思路
基本的解题思路如下图所示：  
![基本解题思路](/images/dataflow.png)  
①HttpServer: 基于netty实现的一个http服务器。它接收到consumer的http请求后，解析出http的body部分（Bytebuf形式），从Bytebuf形式的消息中解析出服务名后，到etcd上做服务发现，此处会缓存服务发现的结果。然后根据负载均衡策略（具体策略后面讲），将消息通过AgentClient转发。 当消息响应返回后，它也负责将返回相应的http Response。

②AgentClient：第一个作用是转发消息至Provider， 转发消息的格式为  
![消息格式](/images/filed.png)    
其中message会透传HttpServer解析出的ByteBuf，减少内存拷贝的开销。
第二个作用是回传Provider响应至HttpServer，其传递的消息格式和上述类似，不再赘述。

③AgentServer：第一个作用是接收consumer Agent发来的请求，解析出其中的服务名、方法名、参数类型和具体参数，将其转换成dubbo协议后，转发至DubboConnectManager。第二个作用是，接收DubboConnectManager返回的响应，并将其透传到Consumer一侧。

④DubboConnectManager:用于发送dubbo格式的消息至provider，以及接收Provider的响应，解析出结果部分后发送至AgentServer。

### （3）主要优化点
① Http Server, Agent Client, Agent Server和DubboConnectManager都是基于netty实现，几个地方均采用异步的方式，实现无阻塞传输。例如，在HttpServer出接收到http请求后，会注册一个回调函数，待响应返回后，执行相应的回调函数。  

② 使用了池化直接内存，线程申请内存时会优先从线程栈缓存的内存池中获取，获取不到时，才从公共的内存池中获取，以最大程度的减少线程抢锁。   

③ 共用EventLoopGroup。HttpServer和AgentClient会共用一个EventLoopGroup, AgentServer和DubboConnectManager会共用一个EventLoopGroup。共用线程池的好处，一是可以减少线程切换，二是可以最大化内存池的收益（HttpServer处从池中获取内存，AgentClient处归还相应的池，且会优先使用线程缓存的内存池。Agent Server和DubboConnectManager处类似）。   

④ 利用共用EventLoopGroup的特性，做负载均衡。  
![负载均衡](/images/commongroup.png)  
负载均衡策略：对于每一个请求消息，Http Server会有一个处理线程（EventLoop), 转发消息时，会优先将此消息转发到该处理线程所在的Agent Client上。
```Java

/**
* 负载均衡策略：eventLoopAgentClientMap存储了eventloop和AgentClient的
*映射，当请求到来时，优先根据线程对应的eventloop选择相应的AgentClient作转
*发，以减少线程切换; 在eventLoopAgentClientMap中找不到Agentclient时，采用
*加权随机的方式，转发请求
*/
    AgentClient agentClient;
    if ((agentClient = HttpServer.eventLoopAgentClientMap.get(eventLoop)!= null) { 
        return agentClient;
    }

    int random = ThreadLocalRandom.current().nextInt(loadBalanceList.length);

    return clientNameToAgentClientMap.get(loadBalanceList[random]);
```

这样的负载均衡策略， 目的是为了最大程度的减少线程切换的开销，事实也证明这样的负载均衡要比普通的加权轮询或者随机轮询有效很多（本地测试相差大约1000qps）。  

⑤ 内存零拷贝。HttpServer到AgentClient转发消息处，DubboConnectManager转发响应到AgentServer等一些地方，在使用直接内存的情况下，尽量透传ByteBuf，通过内存零拷贝的方式, 最大程度的减少内存的拷贝的开销。  

⑥ 使用Epoll替换NIO。在Consumer Agent和Provider Agent处均使用EpollSocketChannel代替NioSocketChannel。由于是linux环境，故直接指定使用EpollSocketChannel。

⑦ 减少线程数。HttpServer处使用一个接收线程，使用和AgentCient数量相同的处理线程（3个），AgentClient处使用一个处理线程，AgentClient使用一个接收线程，一个处理线程，DubboConnectManager处使用一个处理线程。实践表明，过多的线程会造成频繁的线程切换，增加线程抢锁的概率，占用较多CPU，不利于提高性能。采用上述数量的线程，足以处理所有请求。  

⑧ 针对特定的场景，实现的自己的http协议解码，在保证通用性的前提下，减少不适合本场景的解码处理过程，以提高性能。

### （4）通用性考虑
在优化性能的同时，最大程度的保证通用性。例如，服务提供者的发现，依赖于ETCD的服务注册和发现功能；Provider Agent的数量， 依赖于服务发现的结果；HttpServer处使用的处理线程数量和AgentClient的数量挂钩；根据每条消息的具体服务名参数，做服务发现，具体方法名和参数类型依赖于消息传参， 尽量避免硬编码等等。

### （5）经验感悟

系统调优时，一定要找系统的性能瓶颈，不在性能瓶颈上的调优，一般是收效甚微的。


