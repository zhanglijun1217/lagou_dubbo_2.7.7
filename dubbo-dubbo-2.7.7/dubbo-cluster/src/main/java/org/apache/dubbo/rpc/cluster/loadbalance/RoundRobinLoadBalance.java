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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Round robin load balance.
 * 加权轮询负载均衡算法
 * 轮询是一种无状态负载均衡算法，实现简单，适用于集群中所有 Provider 节点性能相近的场景。
 * 但是如果Provider节点性能、负载有差异，那么需要加权轮询来负载均衡
 *
 * 加权之后，分配给每个 Provider 节点的流量比会接近或等于它们的权重比。
 * 例如，Provider 节点 A、B、C 权重比为 5:1:1，
 * 那么在 7 次请求中，节点 A 将收到 5 次请求，节点 B 会收到 1 次请求，节点 C 则会收到 1 次请求。
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private static final int RECYCLE_PERIOD = 60000;

    protected static class WeightedRoundRobin {
        // 配置的权重
        private int weight;
        // 每次负载均衡算法执行 变化的权重
        private AtomicLong current = new AtomicLong(0);
        private long lastUpdate;

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);
        }

        public long increaseCurrent() {
            return current.addAndGet(weight);
        }

        public void sel(int total) {
            current.addAndGet(-1 * total);
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }

    // 和一致性hash套路一样 缓存serviceKey_methodName 作为key value也是map key是Invoker对象 value是WeightedRoundRobin对象（记录了Invoker节点的配置权重和在轮询过程中的动态当前权重）
    private ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> methodWeightMap = new ConcurrentHashMap<String, ConcurrentMap<String, WeightedRoundRobin>>();

    /**
     * get invoker addr list cached for specified invocation
     * <p>
     * <b>for unit test only</b>
     *
     * @param invokers
     * @param invocation
     * @return
     */
    protected <T> Collection<String> getInvokerAddrList(List<Invoker<T>> invokers, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        Map<String, WeightedRoundRobin> map = methodWeightMap.get(key);
        if (map != null) {
            return map.keySet();
        }
        return null;
    }

    /**
     * 新的请求进来时，RoundRobinLoadBalance会遍历Invoker列表，并用对应的currentWeight加上其配置的权重
     * 遍历的过程中会找到currentWeight最大的Invoker 将其减去权重总和 返回相应的Invoker对象。
     * 当下个请求再到来的时候，会再次重复上述过程，为Invoker的currentWeight权重加上配置的权重 挑选currentWeight权重最大的Invoker 之后再次减去总的权重方便下一轮选举
     * @param invokers
     * @param url
     * @param invocation
     * @param <T>
     * @return
     */
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        // 为当前服务方法在缓存中初始化 Invoker -> WeightedRoundRobin 映射 存在直接获取
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        int totalWeight = 0;
        // 最大的current值
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        Invoker<T> selectedInvoker = null;
        WeightedRoundRobin selectedWRR = null;
        for (Invoker<T> invoker : invokers) {
            // 获取invoker的身份String 作为map的key
            String identifyString = invoker.getUrl().toIdentityString();
            // 获取权重 （配置值）
            int weight = getWeight(invoker, invocation);
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(identifyString, k -> {
                //  检测当前Invoker是否有相应的WeightedRoundRobin对象，没有则进行创建
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });

            if (weight != weightedRoundRobin.getWeight()) {
                //weight changed 配置的权重发生变化也会重新设置
                weightedRoundRobin.setWeight(weight);
            }
            // current+配置的权重
            long cur = weightedRoundRobin.increaseCurrent();
            //  设置lastUpdate字段
            weightedRoundRobin.setLastUpdate(now);

            // 找具有最大currentWeight的Invoker，以及Invoker对应的WeightedRoundRobin
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedInvoker = invoker;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight; // 计算权重总和
        }
        if (invokers.size() != map.size()) {
            map.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
        }
        if (selectedInvoker != null) {
            // 用选中的最大权重的Invoker的currentWeight减去totalWeight
            selectedWRR.sel(totalWeight);
            // 返回选中的Invoker对象
            return selectedInvoker;
        }
        // should not happen here
        // 一个兜底 应该不会走到这
        return invokers.get(0);
    }

}
