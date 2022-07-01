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

package com.alibaba.dubbo.remoting.exchange;


import com.alibaba.dubbo.remoting.RemotingException;

/**
 * 2019-06-20
 * duubo2.7版本之前 对异步调用是设置一个future get()阻塞唤醒
 * 为了实现非阻塞 设置了ResponseFuture来设置callback机制 来注册future结果返回之后 客户端收到返回结果之后 客户端的线程来回调
 * Dubbo2.7之后使用CompletableFuture来实现异步调用 很轻松的实现的异步调用且非阻塞 还可以优雅的在正常返回和异常时返回。所以这里过时了。
 */
@Deprecated
public interface ResponseFuture {
    /**
     * get result.
     *
     * @return result.
     */
    Object get() throws RemotingException;

    /**
     * get result with the specified timeout.
     *
     * @param timeoutInMillis timeout.
     * @return result.
     */
    Object get(int timeoutInMillis) throws RemotingException;

    /**
     * set callback.
     *
     * @param callback
     */
    void setCallback(ResponseCallback callback);

    /**
     * check is done.
     *
     * @return done or not.
     */
    boolean isDone();
}
