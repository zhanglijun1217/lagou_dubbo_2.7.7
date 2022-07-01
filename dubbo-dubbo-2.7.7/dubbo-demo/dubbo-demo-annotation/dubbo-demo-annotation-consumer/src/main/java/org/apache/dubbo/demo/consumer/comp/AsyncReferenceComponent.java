package org.apache.dubbo.demo.consumer.comp;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AsyncReferenceComponent {

    @DubboReference
    private DemoService demoService;

    public void echo(String name) {
        // 调用异步方法 接口上声明CompletableFuture
        // stringCompletableFuture 异步接口定义的返回值
        CompletableFuture<String> stringCompletableFuture = demoService.sayHelloAsync(name);

        // 上下文中的future在invoker调用完成之后设置进去 @See AbstractInvoker invoke方法会维护
        CompletableFuture<Object> contextFuture = RpcContext.getContext().getCompletableFuture();

        // 两个是一个futureO
        System.out.println(stringCompletableFuture);
        System.out.println(contextFuture);

        // 注册完成时的回调
        stringCompletableFuture.whenComplete((s, t) -> {
            if (t == null) {
                // 此处是收到结果响应之后委托给DubboClientHandler线程来完成的
                System.out.println("异步接口调用成功，收到回调。");
                System.out.println("当前线程名称" + Thread.currentThread().getName());
            }
        });
    }
}
