package org.apache.dubbo.demo.consumer.comp;

import org.apache.dubbo.demo.DemoService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

// 测试@Referencen注解的bean 能否直接@Resoure注入代理对象
@Component
public class ResourceDemo {

    @Resource(name = "@Reference org.apache.dubbo.demo.DemoService")
    private DemoService demoService;

    public void s() {
        System.out.println(demoService.getClass());
        System.out.println(demoService.sayHello("1"));
    }


}
