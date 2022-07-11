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
import org.apache.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * LeastActiveLoadBalance
 * <p>
 * Filter the number of invokers with the least number of active calls and count the weights and quantities of these invokers.
 * If there is only one invoker, use the invoker directly;
 * if there are multiple invokers and the weights are not the same, then random according to the total weight;
 * if there are multiple invokers and the same weight, then randomly called.
 *
 * 最小活跃数负载均衡算法  依赖 {@see ActiveLimitFilter中对 活跃调用次数的统计}
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // Number of invokers
        int length = invokers.size();

        // The least active value of all invokers
        // 最小的活跃请求数
        int leastActive = -1;

        // The number of invokers having the same least active value (leastActive)
        // 记录活跃请求数最小的Invoker集合的个数
        int leastCount = 0;

        // The index of invokers having the same least active value (leastActive)
        // 记录活跃请求数最小的Invoker在invokers数组中的下标位置
        int[] leastIndexes = new int[length];

        // the weight of every invokers
        // 记录活跃请求数最小的Invoker集合中，每个Invoker的权重值
        int[] weights = new int[length];

        // The sum of the warmup weights of all the least active invokers
        int totalWeight = 0; // 总权重

        // The weight of the first least active invoker
        int firstWeight = 0;

        // Every least active invoker has the same weight value?
        boolean sameWeight = true; // 是否每个最小活跃数的Invoker有相同的权重


        // Filter out all the least active invokers
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            // Get the active number of the invoker 获取活跃数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive();

            // Get the weight of the invoker's configuration. The default value is 100.
            // 获取当前Invoker节点的权重
            int afterWarmup = getWeight(invoker, invocation);

            // save for later use
            weights[i] = afterWarmup;

            // If it is the first invoker or the active number of the invoker is less than the current least active number
            if (leastActive == -1 || active < leastActive) {
                // 当前的Invoker是第一个活跃请求数最小的Invoker，则记录如下信息

                // Reset the active number of the current invoker to the least active number
                // 重新记录最小活跃数
                leastActive = active;

                // Reset the number of least active invokers
                // 重新记录活跃请求数最小的Invoker集合个数
                leastCount = 1;

                // Put the first least active invoker first in leastIndexes
                // 重新记录最小活跃数Invoker索引下标
                leastIndexes[0] = i;

                // Reset totalWeight 重新记录总权重
                totalWeight = afterWarmup;
                // Record the weight the first least active invoker 记录第一个最小活跃数的权重
                firstWeight = afterWarmup;

                // Each invoke has the same weight (only one invoker here)
                sameWeight = true;

                // If current invoker's active value equals with leaseActive, then accumulating.
            } else if (active == leastActive) {
                // 又找到一个最小活跃请求数的Invoker

                // Record the index of the least active invoker in leastIndexes order 记录该Invoker的下标
                leastIndexes[leastCount++] = i;
                // Accumulate the total weight of the least active invoker 更新总权重
                totalWeight += afterWarmup;
                // If every invoker has the same weight?
                if (sameWeight && afterWarmup != firstWeight) {
                    //  更新权重值是否相等
                    sameWeight = false;
                }
            }
        }
        // Choose an invoker from all the least active invokers
        if (leastCount == 1) {
            // 只有一个最小活跃数 直接返回
            // If we got exactly one invoker having the least active value, return this invoker directly.
            return invokers.get(leastIndexes[0]);
        }
        if (!sameWeight && totalWeight > 0) {
            // 下面按照RandomLoadBalance的逻辑，从活跃请求数最小的Invoker集合中，随机选择一个Invoker对象返回
            // 即最小活跃数Invoker集合中如果权重不一致 那么按照加权随机算法去选出一个Invoker 具体@see RandomLoadBalance
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on 
            // totalWeight.
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
            // Return a invoker based on the random value.
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexes[i];
                offsetWeight -= weights[leastIndex];
                if (offsetWeight < 0) {
                    return invokers.get(leastIndex);
                }
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        // 和加权随机一样 如果都有一样的权重 则从最小随机数的Invoker列表中随机选一个
        return invokers.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }
}
