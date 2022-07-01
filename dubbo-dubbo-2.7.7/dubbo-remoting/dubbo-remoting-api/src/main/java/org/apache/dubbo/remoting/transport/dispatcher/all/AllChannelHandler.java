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
package org.apache.dubbo.remoting.transport.dispatcher.all;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.ExecutionException;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;
import org.apache.dubbo.remoting.transport.dispatcher.WrappedChannelHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

// dispatcher的默认实现 dubbo的线程模型中的一种 代表io线程收到所有的事件 都转发给对应的线程池去处理 io线程可以立即返回
public class AllChannelHandler extends WrappedChannelHandler {

    public AllChannelHandler(ChannelHandler handler, URL url) {
        super(handler, url);
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        ExecutorService executor = getExecutorService();
        try {
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CONNECTED));
        } catch (Throwable t) {
            throw new ExecutionException("connect event", channel, getClass() + " error when process connected event .", t);
        }
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        ExecutorService executor = getExecutorService();
        try {
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.DISCONNECTED));
        } catch (Throwable t) {
            throw new ExecutionException("disconnect event", channel, getClass() + " error when process disconnected event .", t);
        }
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        // received处理一个请求 可能是服务端处理的Request请求 也可能是客户端处理的结果Response返回请求
        // 然后去丢给dubbo业务线程池去执行。 这里之后就Dubbo的IO线程就返回了

        // 在服务端处理Request请求场景： 交给DubboServerHandler业务线程执行
        //      内部的的Invoker执行完成后 通过channel到NettyServerHandler向客户端发送返回信息（HeaderExchangeHandler）。

        // 在客户端处理Response返回场景：交给DubboClientHandler业务线程执行
        //      在之前客户端注册的 id——>DefaultFuture缓存中找到DefaultFutrue 执行complete方法将Response绑定（HeaderExchangeHandler)，
        //      然后唤醒异步转同步阻塞的用户线程（AsyncToSyncInvoker），返回Response的结果


        // 获取一个线程池
        ExecutorService executor = getPreferredExecutorService(message);
        try {
            // 线程池中处理请求

            // 比如 对于服务处理Request请求：
            // handler就是一路包装的handler DecodeHandler--> HeaderExchangeHandler --> ExchangerHandlerAdapter（requestHandler) ExchangerHandlerAdapter是在DubboProtocol中实现的requestHandler
            //          内部receive方法会转换Invocation && 生成serviceKey从exporterMap中获取Exporter --> 获取Invoker -->调用Filter链 --> 调用Dubbo服务真正的实现类

            // 封装为ChannelEventRunnable：作用就是根据channel的state来选择调用handler的不同方法
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
        } catch (Throwable t) {
        	if(message instanceof Request && t instanceof RejectedExecutionException){
                sendFeedback(channel, (Request) message, t);
                return;
        	}
            throw new ExecutionException(message, channel, getClass() + " error when process received event .", t);
        }
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        ExecutorService executor = getExecutorService();
        try {
            executor.execute(new ChannelEventRunnable(channel, handler, ChannelState.CAUGHT, exception));
        } catch (Throwable t) {
            throw new ExecutionException("caught event", channel, getClass() + " error when process caught event .", t);
        }
    }
}
