/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.executor.cache.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service
public class CallchainSubscriberCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallchainSubscriberCacheService.class);
    private static final LoadingCache<Object, List<NextCallChainSubscriber>> TC_CONTEXT_SUBSCRIBERS_CACHE
            = CacheBuilder.newBuilder().build(new CacheLoader<Object, List<NextCallChainSubscriber>>() {
        @Override
        public List<NextCallChainSubscriber> load(@Nonnull Object id) {
            return new ArrayList<>();
        }
    });

    public void cleanUp() {
        TC_CONTEXT_SUBSCRIBERS_CACHE.cleanUp();
    }

    public void registerSubscriber(Object subscriber) {
        if (subscriber instanceof NextCallChainSubscriber) {
            Object tcId = ((NextCallChainSubscriber) subscriber).getInstance().getContext().tc().getID();
            synchronized (tcId) {
                try {
                    TC_CONTEXT_SUBSCRIBERS_CACHE.get(tcId).add((NextCallChainSubscriber) subscriber);
                } catch (ExecutionException e) {
                    LOGGER.error("Exception adding {} for tcId {}", subscriber, tcId, e.getMessage());
                }
            }
        }
    }

    public void unregisterSubscriber(Object subscriber) {
        if (subscriber instanceof NextCallChainSubscriber) {
            Object tcId = ((NextCallChainSubscriber) subscriber).getInstance().getContext().tc().getID();
            synchronized (tcId) {
                List<NextCallChainSubscriber> list = TC_CONTEXT_SUBSCRIBERS_CACHE.getIfPresent(tcId);
                if (list != null) {
                    list.remove((NextCallChainSubscriber) subscriber);
                    if (list.isEmpty()) {
                        TC_CONTEXT_SUBSCRIBERS_CACHE.invalidate(tcId);
                    }
                }
            }
        }
    }

    public List<NextCallChainSubscriber> unregisterAllSubscribers(Object tcId) {
        synchronized (tcId) {
            List<NextCallChainSubscriber> list = TC_CONTEXT_SUBSCRIBERS_CACHE.getIfPresent(tcId);
            TC_CONTEXT_SUBSCRIBERS_CACHE.invalidate(tcId);
            return list;
        }
    }
}
