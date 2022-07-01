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
package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.Transporters;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.transport.DecodeHandler;

/**
 * DefaultMessenger
 *
 *
 */
public class HeaderExchanger implements Exchanger {

    public static final String NAME = "header";

    // 在服务引用过程中 进行connect
    @Override
    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        // 逻辑和bind其实是相同的 为handler绑定外层handler 再调用Transporter去初始化和启动NettyClient 用于发送请求
        // 返回了包装的NettyClient
        return new HeaderExchangeClient(Transporters.connect(url, new DecodeHandler(new HeaderExchangeHandler(handler))), true);
    }

    //在暴露服务的过程中 绑定handler给一个RemotingServer 返回一个ExchangeServer
    @Override
    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        // 传入的handler是 requestHandler 类型是ExchangeHandlerAdapter

        // Transporters.bind 会在内部启动NettyServer 返回的也是RemotingServer
        // 对handler封装了很多其他的Handler 每个Handler都有自己处理的逻辑 比如DecodeHandler就是解码

        // 返回一个HeaderExchangeServer 包装RemotingServer
        return new HeaderExchangeServer(
                // Transporters门面类 构建RemotingServer （默认返回一个NettyServer）
                Transporters.bind(url,
                    // 对handler包装 HeaderExchangeHandler、DecodeHandler
                    new DecodeHandler(
                            new HeaderExchangeHandler(handler)
                    )
                )
        );
    }

}
