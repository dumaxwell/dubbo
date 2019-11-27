#### 导出

##### 目标

 - 消费方：DemoService 就是图中服务消费端的 proxy，消费方通过该 proxy 调用对应的 invoker ,invoker实现真正的网络调用。
 - 生产方：DemoServiceImpl 会被封装成 wrapper 实例，进而封装到 abstractProxyInvoker 实例，并生成一个 exporter 实例。
 - ![消费方调用生产方流程图](http://dubbo.apache.org/docs/zh-cn/dev/sources/images/dubbo_rpc_invoke.jpg)
 - 通过URL：dubbo: + ip + port + interface，能找到server上的invoker，并通过 implName + methodName + args 调用该方法

##### 导出过程

0. 由 ServiceBean 中的 onApplicationEvent 进入  //onApplicationEvent 是一个事件响应方法，该方法会在收到 Spring 上下文刷新事件后执行服务导出操作
    1. 是否已导出，是否不用导出
    1. ServiceBean 的 export(){super.export()}
        2. ServiceConfig 的 export()
            3. checkAndUpdateSubConfigs();// 检查，设置子项配置
            3. ServiceConfig 的 doExport()
            ----- 正文由此开始 -----
                1. ServiceConfig 的 doExportUrls()
                    2. 加载要注册中心连接，可能有多个
                        ```
                           registryURLs.get(0)
                           registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?
                               application=demo-provider&
                               dubbo=2.0.2&
                               pid=20308&
                               qos.port=22222&
                               registry=zookeeper&
                               timestamp=1572827648546
                        ```   
                    2. 遍历所有协议，将每个协议的 exporter 注册到上面的注册中心，下面是最常见的协议配置
                        ```
                           protocolConfig
                           <dubbo:
                                service beanName="org.apache.dubbo.demo.DemoService"
                                path="org.apache.dubbo.demo.DemoService"
                                ref="org.apache.dubbo.demo.provider.DemoServiceImpl@3918c187"
                                prefix="dubbo.service.org.apache.dubbo.demo.DemoService"
                                generic="false"
                                interface="org.apache.dubbo.demo.DemoService"
                                unexported="false"
                                exported="true"
                                deprecated="false"
                                dynamic="true"
                                id="org.apache.dubbo.demo.DemoService"
                                valid="true"
                            />
                        ```
                    2. ServiceConfig 的 doExportUrlsFor1Protocol(protocolConfig, registryURLs)

                        3. 生成URL  // todo 后续查看这里到底做了哪些配置

                        3. 本地导出一个exporter // exportLocal(url) 
                            ```
                                injvm://127.0.0.1/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&bind.ip=10.75.16.91&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.deprecatedmo.DemoService&methods=sayHello,ha&pid=17736&qos.port=22222&release=&side=provider&timestamp=1572997276343
                            ```
                            4. Wrapper——>Wrapper0(由dubbo代码生成，重写invoke()方法，统一impl的调用，通过传入：实现类实例，方法名，方法入参数组，调用实现类中的方法)package org.apache.dubbo.common.bytecode.Wrapper0.java。
                            4. Invoker——>AbstractProxyInvoker(invoke()调用doInvoker())——>匿名类(重写doInvoke()方法，该方法调用Wrapper0中的invoke()方法)
                            4. Exporter——>AbstractExporter——>InjvmExporter。injvmExporter实例保存了上面的invoker实例到实例的exportedMap中 //todo 为啥要搞个map，难道有多个？
                            4. Protocol(将invoker放入exporter中)——>AbstractProtocol——>InjvmProtocol(主要重写export()方法，返回上面的injvmExporter实例)
                                5. 这里制造exporter的方法如下：
                                    - ServiceConfig 中的 doExportUrlsFor1Protocol() 有一句 protocol.export(DelegateProviderMetaDataInvoker invoker)
                                    - protocol=Protocol$Adaptive
                                    - 进而调用 ProtocolFilterWrapper.export()
                                    - 进而调用 ProtocolListenerWrapper.export()
                                     - 如果是注册协议，则调用 RegistryProtocol.export()
                                     - 其他协议，则构造对应 filter 链，调用对应 Protocol。这里是 InjvmProtocol.export()
                                       // 构造filter链，见文末
                                5. 层层返回 exporter
                            4. ServiceConfig.exporters中保存上面的exporter实例 //todo 为啥用list，怎么拿出来用，遍历吗？

                        3. 远程导出n个exporter // n是注册中心数量
                            - 遍历每个注册中心，该impl实例对每个注册中心生成一个exporter，添加到 ServiceConfig 实例中去，
                                ``` 注册中心 url：
                                    registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&export=dubbo://10.75.16.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&bind.ip=10.75.16.91&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,ha&pid=17736&qos.port=22222&release=&side=provider&timestamp=1572997276343&pid=17736&qos.port=22222&registry=zookeeper&timestamp=1572997027574
                                ```
                            4. 前置处理。如：添加监视器链接到url中
                            4. 生成invoker(impl实例,接口类型,${url}) 
                                5. ProxyFactory.getInvoke(impl实例, impl.class, ${url})，通过该方法生成 Invoker ，默认 JavassistProxyFactory 
                                    6. Wrapper.makeWrapper(impl.class) 根据类对象生成class文件，impl中所有方法，皆可通过class文件的 invokeMethod(impl实例, 方法名, 参数类型列表, 参数列表) 方法调用。
                                    6. 将上一步的class文件的invokeMethod调用，封装到 invoker 中。
                                5. 返回 Invoker 实例
                            
                            4. DelegateProviderMetaDataInvoker，对 invoker 实例及元数据（ServiceConfig 实例）包装

                            4. protocol.export(delegateProviderMetaDataInvoker) // Protocal$Adaptive.export
                                5. 这里调用url如下：
                                   ```
                                    registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&export=dubbo://10.75.16.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&bind.ip=10.75.16.91&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,ha&pid=4956&qos.port=22222&release=&side=provider&timestamp=1572999638839&pid=4956&qos.port=22222&registry=zookeeper&timestamp=1572999634953
                                   ```
                                   调用栈依然如下：delegateProviderMetaDataInvoker 以下简称 dInvoker
                                    - Protocol$Adaptive.export(dInvoker) // 适配器
                                    - ProtocolFilterWrapper.export(dInvoker) // 用于生成过滤链。由于是 registry 协议，没有 buildInvokerChain ，而是直接调用 ProtocolFilterWrapper.export(invoker)
                                    - ProtocolListenerWrapper.export(dInvoker) // todo用于干嘛。的由于是registry协议，没有调用监听绑定，直接调下面的
                                    - RegistryProtocol.export(dInvoker)
                                        6. RegistryProtocol.export(dInvoker, url)
                                        ----- 启动 dubbo 服务，获取 exporter -----
                                            7. 获取 dInvoker 中的 provider url  // dubbo:
                                            7. ExporterChangeableWrapper exporter = doLocalExport(dInvoker, provider url) // todo 该对象会有一个单线程的线程池、
                                                8. // todo lambda表达式没看懂
                                                8. RegistryProtocol 中调用 export
                                                    调用栈依然如下：
                                                    - Protocol$Adaptive.export(dInvoker) // 适配器
                                                    - ProtocolFilterWrapper.export(dInvoker) // 用于生成过滤链。buildInvokerChain ，得到 fInvoker 。
                                                    - ProtocolListenerWrapper.export(fInvoker) // todo用于干嘛。包装
                                                    - exporter = DubboProtocol.export(fInvoker)

                                                    9. 主要内容 DubboProtocol.export(fInvoker)
                                                        10. DubboExporter exporter = new DubboExporter(invoker,key,exportMap) // key=demoGroup/org.apache.dubbo.demo.DemoService:1.0.1:20880
                                                        ----- 启动服务器开始 -----
                                                        10. openServer(url) // 启动服务器
                                                            11. isServer == true，创建服务端 createServer(url)
                                                                12. url.get("server") // 默认netty
                                                                12. 通过 SPI 检测是否存在 server 参数所代表的 Transporter 拓展，不存在则抛出异常
                                                                12. 获取 ExchangeServer 实例：Exchangers.bind(url, ExchangeHandlerAdapter匿名内部类实例) // 该类是静态工具类 todo 这个handler干嘛用
                                                                ```
                                                                                                                      TelnetHandler ——> |
                                                                    ChannelHandler——>ChannelHandlerAdapter ——> | TelnetHandlerAdapter——>|
                                                                           ChannelHandler, TelnetHandler————————>  ExchangeHandler————> |ExchangeHandlerAdapter
                                                                ```
                                                                    13. 获取Exchange,默认为HeaderExchanger
                                                                    13. HeaderExchanger 的bind(url, handler) 方法创建ExchangeServer实例
                                             
                                                                        Endpoint,Resetable,IdeleSensible——>Server——>ExchangeServer——>HeaderExchangeServer
                                             
                                                                        14. 用handler创建HeaderExchangeHandler实例
                                                                        14. 用HeaderExchangeHandler实例创建DecodeHandler实例
                                                                        14. 创建Server实例，Transporters.bind(url,DecodeHandler实例)
                                                                            15. 获取 Transporter 自适应实例：transporter$Adaptive
                                                                            15. transporter$Adaptive 根据传入的 url 决定加载什么类型的Transporter，默认/netty4/NettyTransporter
                                                                                16. 返回 new NettyServer(url, handler/listener)
                                                                                    17. 返回 super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)))
                                                                                        18. 后面这个参数待分析
                                                                                        18. AbstractServer <—— Server <—— Endpoint, Resetable, IdleSensible
                                                                                                                       <—— AbstractEndpoint <—— AbstractPeer <—— Endpoint, ChannelHandler
                                                                                                                                                               <—— Resetable
                                                                                            19. new 出 AbstractServer 实例，handler存入该实例中，并且为属性 ExecutorService 赋值
                                                                                                20. doOpen() //模板方法，启动服务器
                                                                                                    21. 调用 /netty4/NettyServer 的doOpen()方法，启动服务器
                                                                                                        22. netty : todo
                                                                                                            - 获取channel，绑定ip、端口，添加到channelFuture中
                                                                                                            - 关键词：ServerBootStrap，bossGroup，workerGroup,NettyServerHandler
                                                                                                20. log : main  INFO transport.AbstractServer:  [DUBBO] Start NettyServer bind /0.0.0.0:20880, export /10.75.16.91:20880, dubbo version: , current host: 10.75.16.91
                                                                                                20. 为 AbstractServer 实例的 线程池属性 ExecutorService 赋值
                                                                                            19. 返回 AbstractServer 实例
                                                                                        18. 返回 AbstractServer 实例
                                                                                    17. 返回 NettyServer 实例
                                                                                16. 返回 Server 实例
                                                                            15. 返回 Server 实例
                                                                        14. 将创建的 Server 实例放入 HeaderExchangeServer 实例
                                                                    13. HeaderExchanger 的bind, 返回 (ExchangeServer) HeaderExchangeServer 实例
                                                                12. 返回 ExchangeServer 实例
                                                            11. 此时创建 server成功，服务已经启动，返回 ExchangeServer 实例
                                                            11. 如果 server 已经启动，则 resetUrl // todo 这里什么作用
                                                                ```
                                                                    原来url:
                                                                        dubbo://10.75.16.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&bind.ip=10.75.16.91&bind.port=20880&channel.readonly.sent=true&codec=dubbo&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&heartbeat=60000&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,ha&pid=5832&qos.port=22222&release=&side=provider&timestamp=1574753107555
                                                                     
                                                                    重置后url：
                                                                        dubbo://10.75.16.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.MaxwellService&bind.ip=10.75.16.91&bind.port=20880&channel.readonly.sent=true&codec=dubbo&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&heartbeat=60000&interface=org.apache.dubbo.demo.MaxwellService&methods=invokeDemoSayHello&pid=5832&qos.port=22222&release=&side=provider&timestamp=1574753108406
                                                                ```
                                                        10. optimizeSerialization(url) // 优化序列化 todo
                                                        ----- 启动服务器结束 -----
                                                    9. 返回 exporter。 这里主要是启动服务器、重置url，exporter只是个封装

                                                    9. ProtocolListenerWrapper 将 exporter 与 ExporterListener 的实现类list包装返回 exporter 。
                                            7. 获取 exporter   
                                            ----- 启动 dubbo 服务，获取 exporter -----
                                            ----- 下面开始注册 -----
                                            - 没什么可说的，目标是在zk创建文件节点/${group}/${interface}/providers/${url}
                                            - ![zk注册后节点查看 by ZooInspector](http://dubbo.apache.org/docs/zh-cn/source_code_guide/sources/images/service-registry.png)
                                                8. 通过 Invoker 的 registryUrl 获取 registry 实例
                                                8. register(registryUrl, registeredProviderUrl) 向 registryUrl 注册 registeredProviderUrl
                                                    ```
                                                        zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&export=dubbo://10.75.16.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&bind.ip=10.75.16.91&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,ha&pid=27476&qos.port=22222&release=&side=provider&timestamp=1574824953000&pid=27476&qos.port=22222&timestamp=1574824931543
                                                        
                                                        dubbo://10.75.16.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,ha&pid=27476&release=&side=provider&timestamp=1574824953000
                                                    ```
                                                    group:dubbo
                                                    interface:org.apache.dubbo.demo.DemoService
                                                    url:dubbo://    
                                                    可使用ZooInspector查看
                                                    总结：创建zk客户端实例，在用实例创建文件路径节点。都是zk的Curator框架的使用。
                                                8. 向注册中心进行订阅 override 数据，并更新到 exporter 中
                                            ----- 注册结束 -----                 
                                            7. 将 exporter 添加到 List<Exporter> 中 // todo 写到list中，怎么使用
                                4. 将 url 添加到 ServiceConfig urls 中
                1.2.3 void

---

##### 注册
没什么可说的，目标是在zk创建文件节点/${group}/${interface}/providers/${url}
group:dubbo
interface:org.apache.dubbo.demo.DemoService
url:dubbo://
可使用ZooInspector查看
总结：创建zk客户端实例，在用实例创建文件路径节点。都是zk的Curator框架的使用。

---

##### Filter链
###### 生成

###### 调用

---

#### 总结
本节没有什么难点，主要是各种父类子类、包装类、工厂类看的比较难受。再就是Netty的框架、zk框架的代码调用。
 1. 检查配置是否齐全
 1. 生成url
 1. 导出服务 
    2. 根据url，针对每一种协议，导出到injvm
    2. 根据url，针对每一种协议，导出到每一个注册中心，获取 exporter 
        3. 先调用 RegistryProtocol，而后再 DubboProtocol。
            4. RegistryProtocol 
                5. filterChain
                5. 监听器（admin用）
                5. dubboProtocol处理完后，获取注册中心实例，并注册
            4. DubboProtocol 做这些
                5. 启动服务
                5. 绑定处理器
                5. resetUrl
 
 ---

#### 遗留问题
 1. ProtocalFilterWrapper.buildInvokerChain() 中 new CallbackRegistrationInvoker的作用。服务调用时看看
 全异步调用链https://dubbo.apache.org/zh-cn/blog/dubbo-new-async.html
 next指针形成的那个链表用于对请求的处理，另一个ArrayList<Filter> 就是在此时执行,拿到结果之后，遍历这个ArrayList，执行其onResponse或者onError方法，如此一来，请求和响应应就会经过所有生效的Filter处理。
 1. 实际没碰到过injvm的调用
 1. ProtocolListenerWrapper 是如何做到监听服务调用的，是否异步，是否影响性能
 1. ExporterChangeableWrapper exporter = doLocalExport(dInvoker, provider url) 该对象会有一个单线程的线程池 做咩？
 1. resetUrl的用处是什么
 1. 优化序列化 干嘛用
 1. netty细节实现，如何做到指定url调用指定handler，这里所有handler是否公用一个cacheThreadPool，能否按照接口核心程度进行隔离