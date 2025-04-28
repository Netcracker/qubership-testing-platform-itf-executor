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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitTimeMonitoringThreadPoolExecutor extends ThreadPoolExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitTimeMonitoringThreadPoolExecutor.class);

    public WaitTimeMonitoringThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                                TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public WaitTimeMonitoringThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                                TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                                ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public WaitTimeMonitoringThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                                TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                                RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public WaitTimeMonitoringThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                                TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                                ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        final long startTime = System.currentTimeMillis();
        return super.submit(() -> {
                    warnLongDuration(startTime);
                    return task.call();
                }
        );
    }

    @Override
    public Future<?> submit(Runnable task) {
        final long startTime = System.currentTimeMillis();
        return super.submit(() -> {
                    warnLongDuration(startTime);
                    task.run();
                }
        );
    }

    private void warnLongDuration(long startTime) {
        final long queueDuration = System.currentTimeMillis() - startTime;
        if (queueDuration >= 1000L) {
            LOGGER.warn("Task awaited in the queue: {} (msecs)", queueDuration);
        }
    }
}
