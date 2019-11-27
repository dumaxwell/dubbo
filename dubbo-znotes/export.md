 
#### 导出服务到本地
   
#### 导出服务到远程
##### 导出
###### 目标
 - 消费方：DemoService 就是图中服务消费端的 proxy，消费方通过该 proxy 调用对应的 invoker ,invoker实现真正的网络调用。
 - 生产方：DemoServiceImpl 会被封装成 wrapper 实例，进而封装到 abstractProxyInvoker 实例，并生成一个 exporter 实例。
 - ![消费方调用生产方流程图](http://dubbo.apache.org/docs/zh-cn/dev/sources/images/dubbo_rpc_invoke.jpg)
 - 通过URL：dubbo: + ip + port + interface，能找到server上的invoker，并通过 implName + methodName + args 调用该方法
###### 导出过程

        
0. 由 ServiceBean 中的 onApplicationEvent 进入  //onApplicationEvent 是一个事件响应方法，该方法会在收到 Spring 上下文刷新事件后执行服务导出操作
    1. 是否已导出，是否不用导出
    1. ServiceBean 的 export(){super.export()}
        2. ServiceConfig 的 export()
            3. checkAndUpdateSubConfigs();// 检查，设置子项配置
            3. ServiceConfig 的 doExport()
            ######正文由此开始
                1. ServiceConfig 的 doExportUrls()
                    2. loadRegistries() //加载要注册的目标地址，返回url
                    ```
                           registryURLs.get(0)
                           registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?
                               application=demo-provider&
                               dubbo=2.0.2&
                               pid=20308&
                               qos.port=22222&
                               registry=zookeeper&
                               timestamp=1572827648546
                               
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
                        3. 生成URL
                        3. 本地导出一个exporter // exportLocal(url) //injvm://127.0.0.1/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bean.name=org.apache.dubbo.demo.DemoService&bind.ip=10.75.16.91&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,ha&pid=17736&qos.port=22222&release=&side=provider&timestamp=1572997276343
                            4. Wrapper——>Wrapper0(由dubbo代码生成，重写invoke()方法，统一impl的调用，通过传入：实现类实例，方法名，方法入参数组，调用实现类中的方法)package org.apache.dubbo.common.bytecode.Wrapper0.java。
                            4. Invoker——>AbstractProxyInvoker(invoke()调用doInvoker())——>匿名类(重写doInvoke()方法，该方法调用Wrapper0中的invoke()方法)
                            4. Exporter——>AbstractExporter——>InjvmExporter。injvmExporter实例保存了上面的invoker实例到实例的exportedMap中 //todo 为啥要搞个map，难道有多个？
                            4. Protocol(将invoker放入exporter中)——>AbstractProtocol——>InjvmProtocol(主要重写export()方法，返回上面的injvmExporter实例)
                                5. 这里制造exporter的方法如下：
                                    - ServiceConfig 中的 doExportUrlsFor1Protocol() 有一句 protocol.export(DelegateProviderMetaDataInvoker invoker)
                                    - protocol=Protocol$Adaptive
                                    - 进而调用 ProtocolListenerWrapper.export()
                                    - 进而调用 ProtocolFilterWrapper.export()
                                     - 如果是注册协议，则调用 RegistryProtocol.export()
                                     - 其他协议，则构造对应 filter 链，调用对应 Protocol。这里是 InjvmProtocol.export()
                                       // todo 如何构造filter链的
                                5. 层层返回 exporter
                            4. ServiceConfig.exporters中保存上面的exporter实例 //todo 为啥用list，怎么拿出来用，遍历吗？
                        3. 远程导出n个exporter // n是注册中心数量
                            - 遍历每个注册中心，该impl实例对每个注册中心生成一个exporter，添加到 ServiceConfig 实例中去
                            4. 生成invoker(impl实例,接口类型,${url}) // registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F10.75.16.91%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddemo-provider%26bean.name%3Dorg.apache.dubbo.demo.DemoService%26bind.ip%3D10.75.16.91%26bind.port%3D20880%26deprecated%3Dfalse%26dubbo%3D2.0.2%26dynamic%3Dtrue%26generic%3Dfalse%26interface%3Dorg.apache.dubbo.demo.DemoService%26methods%3DsayHello%2Cha%26pid%3D17736%26qos.port%3D22222%26release%3D%26side%3Dprovider%26timestamp%3D1572997276343&pid=17736&qos.port=22222&registry=zookeeper&timestamp=1572997027574
                            4. 将 ServiceConfig 实例，invoker实例，包装到 DelegateProviderMetaDataInvoker 实例中
                            4. exporters.add(protocol.export(delegateProviderMetaDataInvoker)) //默认是DubboProtocol.export()  
                                5. 这里调用url如下： // todo 为啥是registry开头？
                                   ```
                                    registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F10.75.16.91%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddemo-provider%26bean.name%3Dorg.apache.dubbo.demo.DemoService%26bind.ip%3D10.75.16.91%26bind.port%3D20880%26deprecated%3Dfalse%26dubbo%3D2.0.2%26dynamic%3Dtrue%26generic%3Dfalse%26interface%3Dorg.apache.dubbo.demo.DemoService%26methods%3DsayHello%2Cha%26pid%3D4956%26qos.port%3D22222%26release%3D%26side%3Dprovider%26timestamp%3D1572999638839&pid=4956&qos.port=22222&registry=zookeeper&timestamp=1572999634953
                                   ```
                                   调用栈依然如下：
                                    - Protocol.export(invoker) // ServiceConfig 接口
                                    - Protocol$Adaptive.export(invoker) // 适配器
                                    - ProtocolListenerWrapper.export(invoker) // 由于是registry协议，没有调用监听绑定，直接调下面的
                                    - ProtocolFilterWrapper.export(invoker) // 由于是registry协议，没有buildInvokerChain，直接调用RegistryProtocol.export(invoker)
                                    - RegistryProtocol.export(invoker)
                                5. DubboExporter exporter = new DubboExporter(invoker,key,exportMap) // key=demoGroup/org.apache.dubbo.demo.DemoService:1.0.1:20880
                                5. openServer(url) // 启动服务器
                                    6. isServer == true，创建服务端 createServer(url)
                                        7. url.get("server") // 默认netty
                                        7. 通过 SPI 检测是否存在 server 参数所代表的 Transporter 拓展，不存在则抛出异常
                                        7. 获取 ExchangeServer 实例：Exchangers.bind(url, ExchangeHandlerAdapter匿名内部类实例) // 该类是静态工具类 todo 这个handler干嘛用
                     
                                                                                                TelnetHandler ——> |
                                            ChannelHandler——>ChannelHandlerAdapter ——> | TelnetHandlerAdapter——>|
                                                ChannelHandler, TelnetHandler————————>  ExchangeHandler————> |ExchangeHandlerAdapter
                     
                                            8. 获取Exchange,默认为HeaderExchanger
                                            8. HeaderExchanger 的bind(url, handler) 方法创建ExchangeServer实例
                     
                                                Endpoint,Resetable,IdeleSensible——>Server——>ExchangeServer——>HeaderExchangeServer
                     
                                                9. 用handler创建HeaderExchangeHandler实例
                                                9. 用HeaderExchangeHandler实例创建DecodeHandler实例
                                                9. 创建Server实例，Transporters.bind(url,DecodeHandler实例)
                                                    10. 获取 Transporter 自适应实例：transporter$Adaptive
                                                    10. transporter$Adaptive 根据传入的 url 决定加载什么类型的Transporter，默认/netty4/NettyTransporter
                                                        11. 返回 new NettyServer(url, handler/listener)
                                                            12. 返回 super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)))
                                                                13. 后面这个参数待分析
                                                                13. AbstractServer <—— Server <—— Endpoint, Resetable, IdleSensible
                                                                                               <—— AbstractEndpoint <—— AbstractPeer <—— Endpoint, ChannelHandler
                                                                                                                                       <—— Resetable
                                                                     14. new 出 AbstractServer 实例，handler存入该实例中，并且为属性 ExecutorService 赋值
                                                                        15. doOpen() //模板方法，启动服务器
                                                                            16. 调用 /netty4/NettyServer 的doOpen()方法，启动服务器
                                                                                17. netty : todo
                                                                                    - 获取channel，绑定ip、端口，添加到channelFuture中
                                                                                    - 关键词：ServerBootStrap，bossGroup，workerGroup,NettyServerHandler
                                                                        15. log : main  INFO transport.AbstractServer:  [DUBBO] Start NettyServer bind /0.0.0.0:20880, export /10.75.16.91:20880, dubbo version: , current host: 10.75.16.91
                                                                        15. 为 AbstractServer 实例的 线程池属性 ExecutorService 赋值
                                                                    14. 返回 AbstractServer 实例
                                                                13. 返回 AbstractServer 实例
                                                            12. 返回 NettyServer 实例
                                                        11. 返回 Server 实例
                                                    10. 返回 Server 实例
                                                9. 将创建的 Server 实例放入 HeaderExchangeServer 实例
                                            8. HeaderExchanger 的bind, 返回 (ExchangeServer) HeaderExchangeServer 实例
                                        7. 返回 ExchangeServer 实例
                                    6. 此时创建 server成功，服务已经启动，返回 ExchangeServer 实例
                                // ————————————————————————————————
                                 5. optimizeSerialization(url) // 优化序列化
    1. ServiceBean 的 publishExportEvent()

       
##### 注册
没什么可说的，目标是在zk创建文件节点/${group}/${interface}/providers/${url}
group:dubbo
interface:org.apache.dubbo.demo.DemoService
url:dubbo://
可使用ZooInspector查看
总结：创建zk客户端实例，在用实例创建文件路径节点。都是zk的Curator框架的使用。

##### Filter链
###### 生成

###### 调用


#### 总结
本节没有什么难点，主要是各种父类子类、包装类、工厂类看的比较难受。再就是Netty的框架、zk框架的代码调用。明天整理下调用顺序，进入下一节的学习了！
 1. 生成url
 1. 根据url生成exporter
 1. 启动服务或者reset服务，将handler绑定到端口
 1. 将路径注册到zk
 
 
#### 遗留问题
 1. ProtocalFilterWrapper.buildInvokerChain() 中 new CallbackRegistrationInvoker作用。服务调用时看看，全异步调用链https://dubbo.apache.org/zh-cn/blog/dubbo-new-async.html
 next指针形成的那个链表用于对请求的处理，另一个ArrayList<Filter> 就是在此时执行,拿到结果之后，遍历这个ArrayList，执行其onResponse或者onError方法，如此一来，请求和响应应就会经过所有生效的Filter处理。
 