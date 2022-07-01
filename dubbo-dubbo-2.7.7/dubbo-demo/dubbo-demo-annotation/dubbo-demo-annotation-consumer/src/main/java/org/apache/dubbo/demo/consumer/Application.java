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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.consumer.comp.AsyncReferenceComponent;
import org.apache.dubbo.demo.consumer.comp.DemoServiceComponent;

import org.apache.dubbo.demo.consumer.comp.ResourceDemo;
import org.apache.dubbo.demo.consumer.comp.asyncWithPrimaryRes.AsyncInvokeService;
import org.apache.dubbo.rpc.Protocol;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.CountDownLatch;

public class Application {
    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) throws Exception{
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        context.start();
//        DemoService service = context.getBean("demoServiceComponent", DemoServiceComponent.class);
//        String hello = service.sayHello("world");
//        System.out.println("result :" + hello);

        // 查看dubbo引用对象 在Spring容器中的对线下 是个FactoryBean 会调用getObject方法也就是dubbo的代理对象
        // 所以@Resource也是可以直接引用dubbo reference的
//        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("registry");
//        ResourceDemo resourceDemo = context.getBean("resourceDemo", ResourceDemo.class);
//        resourceDemo.s();

        // 异步调用demo
//        AsyncReferenceComponent bean = context.getBean(AsyncReferenceComponent.class);
//        // 内部是async异步引用
//        bean.echo("world");


        // 测试异步调用 包装类和基本类型返回结果
        AsyncInvokeService asyncInvokeService = context.getBean(AsyncInvokeService.class);
        asyncInvokeService.async();
        new CountDownLatch(1).await();
    }

    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.demo.consumer.comp")
    @PropertySource("classpath:/spring/dubbo-consumer.properties")
    @ComponentScan(value = {"org.apache.dubbo.demo.consumer.comp"})
    static class ConsumerConfiguration {

    }
}
