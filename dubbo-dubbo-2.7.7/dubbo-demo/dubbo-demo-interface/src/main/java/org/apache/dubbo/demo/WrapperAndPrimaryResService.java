package org.apache.dubbo.demo;

public interface WrapperAndPrimaryResService {

    /**
     * 返回包装Boolean的方法
     * @return
     */
    Boolean wrapper();

    /**
     * 返回原生boolean的方法
     * @return
     */
    boolean primary();
}
