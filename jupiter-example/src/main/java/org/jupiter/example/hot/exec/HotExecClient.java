/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.example.hot.exec;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Pair;
import org.jupiter.hot.exec.JavaClassExec;
import org.jupiter.hot.exec.JavaCompiler;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.error.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

import java.net.SocketAddress;

import static org.jupiter.common.util.JConstants.DEFAULT_VERSION;

/**
 * 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子.
 *
 * jupiter
 * org.jupiter.example.hot.exec
 *
 * @author jiachun.fjc
 */
public class HotExecClient {

    public static void main(String[] args) {
        Directory directory = new ServiceMetadata("exec", DEFAULT_VERSION, "JavaClassExec");

        NettyConnector connector = new JNettyTcpConnector();
        // 连接ConfigServer
        connector.initRegistryService("127.0.0.1", 20001);
        // 自动管理可用连接
        JConnector.ConnectionManager manager = connector.manageConnections(directory);
        // 等待连接可用
        if (!manager.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        JavaClassExec service = ProxyFactory
                .create()
                .connector(connector)
                .dispatchMode(DispatchMode.BROADCAST)
                .asyncMode(AsyncMode.ASYNC_CALLBACK)
                .interfaceClass(JavaClassExec.class)
                .listener(new JListener() {

                    @Override
                    public void complete(JRequest request, Pair<SocketAddress, Object> result) throws Exception {
                        System.out.println("complete=" + result);
                    }

                    @Override
                    public void failure(JRequest request, Throwable cause) {
                        System.out.println("failure=" + cause);
                    }
                })
                .newProxyInstance();

        try {
            byte[] classBytes = JavaCompiler.compile(
                    System.getProperty("user.dir") + "/jupiter-example/src/main/java/",
                    UserExecImpl.class.getName(),
                    Lists.newArrayList("-verbose", "-source", "1.7", "-target", "1.7"));

            service.exec(classBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}