

#### 导出服务到本地
1. Wrapper——>Wrapper0(由dubbo代码生成，主要并主要重写invoke()方法，包装方法调用。逻辑是通过传入的参数：实现类实例，方法名，方法入参数组，调用实现类中的方法)
1. Invoker——>AbstractProxyInviker(invoke()调用doInvoker())——>匿名类(重写doInvoke()方法，该方法调用Wrapper0中的invoke()方法)
1. Exporter——>AbstractExporter——>InjvmExporter。injvmExporter实例保存了上面的invoker实例到实例的exportedMap中 _todo 为啥要搞个map，难道有多个？_
1. Protocol(将invoker放入exporter中)——>AbstractProtocol——>InjvmProtocol(主要重写export()方法，返回上面的injvmExporter实例)
1. ServiceConfig.exporters中保存上面的exporter实例 todo 为啥用list，怎么拿出来用，遍历吗？
    
#### 导出服务到远程
##### 导出
###### 目标
通过URL("dubbo:" + ip + port + interface)，能找到server上的invoker，并通过 implName + methodName + args 调用该方法
###### 导出过程
1. doExport()
    1. doExportUrls()
        2. loadRegistries() //加载要注册的目标地址，返回url
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
        
                registryURLs.get(0)
                registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?
                     application=demo-provider&
                     dubbo=2.0.2&
                     pid=20308&
                     qos.port=22222&
                     registry=zookeeper&
                     timestamp=1572827648546
         ```   
        2. doExportUrlsFor1Protocol(protocolConfig, registryURLs)
            3. 生成URL
            3. 导出到本地
            3. 导出到远程
                4.
                4. 生成invoker
                4. exporters.add(protocol.export(invoker)) //默认是DubboProtocol.export()  
                    5. DubboExporter exporter = new DubboExporter(invoker,key,exportMap) // key=demoGroup/org.apache.dubbo.demo.DemoService:1.0.1:20880
                    5. openServer(url) // 启动服务器
                        6. if(isServer == true) createServer(url)
                            7. url.get("server") // 默认netty
                            7. 通过 SPI 检测是否存在 server 参数所代表的 Transporter 拓展，不存在则抛出异常
                            7. 创建ExchangeServer：Exchangers.bind(url, ExchangeHandlerAdapter匿名内部类实例) // 该类是静态工具类 todo 这个handler干嘛用
                                8. 获取Exchange,默认为HeaderExchanger
                                8. 调用HeaderExchanger的bind(url, handler)方法创建ExchangeServer实例
                                    Endpoint,Resetable,IdeleSensible——>Server——>ExchangeServer——>HeaderExchangeServer
                                    9. 用handler创建HeaderExchangeHandler实例
                                    9. 用HeaderExchangeHandler实例创建DecodeHandler实例
                                    9. 创建Server实例，Transporters.bind(url,DecodeHandler实例)
                                        10. 获取 Transporter 自适应实例：transporter$Adaptive
                                        10. transporter$Adaptive 根据传入的 url 决定加载什么类型的Transporter，默认/netty4/NettyTransporter
                                            11. new NettyServer(url, handler/listener)
                    5. optimizeSerialization(url) // 优化序列化
       
##### 注册