package org.apache.dubbo.demo.consumer.comp.asyncWithPrimaryRes;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.demo.WrapperAndPrimaryResService;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Service;

@Service
public class AsyncInvokeService {

    @DubboReference(injvm = false)
    private WrapperAndPrimaryResService wrapperAndPrimaryResService;

    public void async() {
        // 异步调用包装类型返回值的方法
        System.out.println("开始远程调用1");
        RpcContext.getContext().asyncCall(() -> wrapperAndPrimaryResService.wrapper())
                .handle((r, t) -> {
                    System.out.println("1 返回结果是：" + r);
                    return r;
                });
        System.out.println("结束注册异步调用1");

        // 异步调用原生类型返回值的方法 这里有个bug 会直接返回原生类型的默认值
        System.out.println("开始远程调用2");
        RpcContext.getContext().asyncCall(() -> wrapperAndPrimaryResService.primary())
                .handle((r, t) -> {
                    System.out.println("2 返回结果是：" + r);
                    return r;
                });
        System.out.println("结束注册异步调用2");

    }
}
