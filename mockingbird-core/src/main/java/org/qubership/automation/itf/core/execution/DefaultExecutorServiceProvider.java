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

package org.qubership.automation.itf.core.execution;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class DefaultExecutorServiceProvider implements ExecutorServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutorServiceProvider.class);
    private static final Thread.UncaughtExceptionHandler HANDLER = (t, e) -> LOGGER.error("Uncaught exception in "
            + "thread {}", t.getName(), e);

    /*
         In order to have direct access to messages queue (in order to monitor processing)
     */
    private final BlockingQueue<Runnable> regularQueue;
    private final BlockingQueue<Runnable> backgroundQueue;
    private final SynchronousQueue<Runnable> inboundQueue;

    private final ExecutorService backgroundPool;
    private final ExecutorService regularPool;
    private final ExecutorService inboundPool;

    public DefaultExecutorServiceProvider(int executorThreadPoolCoreSize,
                                          int executorThreadPoolSize,
                                          int backgroundExecutorThreadPoolSize) {
        ThreadFactory regularThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("rpt-%d")
                .setDaemon(false)
                .setPriority(Thread.NORM_PRIORITY)
                .setUncaughtExceptionHandler(HANDLER)
                .build();
        if (executorThreadPoolCoreSize < executorThreadPoolSize) {
            int coreSize = (executorThreadPoolCoreSize < 1) ? 5 : executorThreadPoolCoreSize;
            regularQueue = new SynchronousQueue<>();
            regularPool = new WaitTimeMonitoringThreadPoolExecutor(
                    coreSize,
                    executorThreadPoolSize,
                    60L,
                    TimeUnit.SECONDS,
                    regularQueue,
                    regularThreadFactory
            );
        } else {
            regularQueue = new LinkedBlockingQueue<>();
            regularPool = new WaitTimeMonitoringThreadPoolExecutor(
                    executorThreadPoolSize,
                    executorThreadPoolSize,
                    60L,
                    TimeUnit.SECONDS,
                    regularQueue,
                    regularThreadFactory
            );
        }
        backgroundQueue = new LinkedBlockingQueue<>();
        backgroundPool = new WaitTimeMonitoringThreadPoolExecutor(backgroundExecutorThreadPoolSize,
                backgroundExecutorThreadPoolSize,
                120L,
                TimeUnit.SECONDS,
                backgroundQueue,
                new ThreadFactoryBuilder()
                        .setNameFormat("bpt-%d")
                        .setDaemon(false)
                        .setPriority(Thread.NORM_PRIORITY)
                        .setUncaughtExceptionHandler(HANDLER)
                        .build()
        );
        inboundQueue = new SynchronousQueue<>();
        inboundPool = new ThreadPoolExecutor(0,
                Integer.MAX_VALUE,
                20L,
                TimeUnit.SECONDS,
                inboundQueue,
                new ThreadFactoryBuilder()
                        .setNameFormat("ipt-%d")
                        .setDaemon(false)
                        .setPriority(6)
                        .setUncaughtExceptionHandler(HANDLER)
                        .build()
        );
    }

    public ExecutorService requestForBackgroundJob() {
        return backgroundPool;
    }

    public ExecutorService requestForRegular() {
        return regularPool;
    }

    public ExecutorService requestForInboundProcessing() {
        return inboundPool;
    }

    public BlockingQueue<Runnable> getRegularQueue() {
        return regularQueue;
    }

    public BlockingQueue<Runnable> getBackgroundQueue() {
        return backgroundQueue;
    }

    public SynchronousQueue<Runnable> getInboundQueue() {
        return inboundQueue;
    }

    @Override
    public void shutdown() {
        backgroundPool.shutdownNow();
        regularPool.shutdownNow();
        inboundPool.shutdownNow();
    }
}
