/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.demo.DemoService;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;
import java.util.concurrent.*;

public class Application {
    public static List<Future<Object>> futureList = new LinkedList<>();

    static ExecutorService executor = Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-consumer.xml");
        context.start();
        DemoService demoService = context.getBean("demoService", DemoService.class);

//        startMainThread();

        int i=1;
        while (true) {
//            CompletableFuture<String> future = demoService.sayHelloAsync("world");
//            future.whenComplete((retValue, exception) -> {
//                if (exception == null) {
//                    System.out.println(retValue);
//                } else {
//                    exception.printStackTrace();
//                }
//            });

            i++;

            demoService.sayHello(""+i);

            CompletableFuture<Object> future = RpcContext.getContext().getCompletableFuture();

            future.whenComplete((result, throwable)->{
                if (throwable != null) {
                    System.out.println(throwable);
                } else {
                    System.out.println(result);
                }
            });
//            synchronized (futureList) {
//                futureList.add(future);
//            }

            Thread.sleep(200l);
        }

    }

    private static void startMainThread() {
        new Thread(()->{
            while (true) {

                List<Future<Object>> localFutureList;
                synchronized (futureList) {
                    localFutureList = new LinkedList<>(futureList);
                    futureList.clear();
                }

                Iterator it = localFutureList.iterator();
                while (it.hasNext()) {
                    Future<Object> future = (Future<Object>) it.next();
                    if (future.isDone()) {
                        executor.submit(new Runnable() {
                            public void run() {
                                try {
                                    Object result = future.get(10l, TimeUnit.MILLISECONDS);
                                    System.out.println(Thread.currentThread().getName() + result);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (TimeoutException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        it.remove();
                    }
                }
                synchronized (futureList) {
                    futureList.addAll(localFutureList);
                }
            }
        }).start();
    }
}
