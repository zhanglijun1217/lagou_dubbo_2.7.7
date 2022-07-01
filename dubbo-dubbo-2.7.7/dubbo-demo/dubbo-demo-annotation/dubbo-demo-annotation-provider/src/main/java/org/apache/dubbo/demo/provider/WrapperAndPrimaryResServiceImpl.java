package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.demo.WrapperAndPrimaryResService;

@DubboService
public class WrapperAndPrimaryResServiceImpl implements WrapperAndPrimaryResService {

    @Override
    public Boolean wrapper() {
        System.out.println("调用wrapper");
        return true;
    }

    @Override
    public boolean primary() {
        System.out.println("调用primary");
        return true;
    }
}
