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

package org.qubership.automation.itf.executor.cache.hazelcast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HazelcastAsyncExecutor {
    private static final Thread.UncaughtExceptionHandler HANDLER = (t, e) ->
            log.error("Uncaught exception in thread {}", t.getName(), e);

    @Getter
    private final ExecutorService asyncTasksPool;
    private final int asyncProcessingPoolCoreSize;
    private final int asyncProcessingPoolMaxSize;

    /**
     * Constructor.
     *
     * @param asyncProcessingPoolCoreSize - core size of asyncTasksPool,
     * @param asyncProcessingPoolMaxSize - max size of asyncTasksPool.
     */
    @Autowired
    public HazelcastAsyncExecutor(@Value("${hazelcast.async.pool.core.size}") int asyncProcessingPoolCoreSize,
                                  @Value("${hazelcast.async.pool.max.size}") int asyncProcessingPoolMaxSize) {
        this.asyncProcessingPoolCoreSize = asyncProcessingPoolCoreSize;
        this.asyncProcessingPoolMaxSize = asyncProcessingPoolMaxSize;
        this.asyncTasksPool = initAsyncPool();
    }

    private ExecutorService initAsyncPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("hz-AsyncTasksPool-%d")
                .setDaemon(false)
                .setPriority(Thread.NORM_PRIORITY)
                .setUncaughtExceptionHandler(HANDLER)
                .build();
        return new ThreadPoolExecutor(
                asyncProcessingPoolCoreSize,
                asyncProcessingPoolMaxSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
    }
}
