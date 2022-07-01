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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.Transporter;

/**
 * Default extension of {@link Transporter} using netty4.x.
 */
public class NettyTransporter implements Transporter {

    public static final String NAME = "netty";

    // 在dubbo导出暴露服务的过程中 会调用bind方法
    @Override
    public RemotingServer bind(URL url, ChannelHandler handler) throws RemotingException {
        // 返回一个NettyServer
        return new NettyServer(url, handler);
    }

    // 在dubbo引用过程中 会调用connect方法来引用一个dubbo服务
    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        // 返回一个NettyClient
        return new NettyClient(url, handler);
    }

}
