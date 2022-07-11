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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.Node;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

/**
 * Directory. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Directory_service">Directory Service</a>
 *
 * @see org.apache.dubbo.rpc.cluster.Cluster#join(Directory)
 *
 * 目录，代表多个Invoker集合 后续路由规则、负载均衡及集群容错的基础
 */
public interface Directory<T> extends Node {

    /**
     * get service type.
     * 服务接口类型
     *
     * @return service type.
     */
    Class<T> getInterface();

    /**
     * list invokers.
     * list会根据invocation参数，过滤自身维护的Invoker集合，返回符合条件的Invoker集合
     * @return invokers
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;

    // 获取当前Directory对象维护的全部Invoker对象
    List<Invoker<T>> getAllInvokers();

    // consumer端的url
    URL getConsumerUrl();

}