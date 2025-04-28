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

import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonThreadPoolFactory {

    private static final DaemonThreadPoolFactory INSTANCE = new DaemonThreadPoolFactory();
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonThreadPoolFactory.class);
    private static final Thread.UncaughtExceptionHandler HANDLER = (thread, exception) -> LOGGER.error("Uncaught "
            + "exception in thread {}", thread.getName(), exception);
    private WeakHashMap<ExecutorService, Object> weakHashMap = new WeakHashMap<>();

    private DaemonThreadPoolFactory() {
    }

    public static DaemonThreadPoolFactory getInstance() {
        return INSTANCE;
    }

    public static ExecutorService fixedThreadPool(int poolSize, String prefix) {
        return getInstance()._fixedThreadPool(poolSize, prefix);
    }

    public static ExecutorService singleThreadExecutor(String prefix) {
        return Executors.newSingleThreadExecutor(new DaemonThreadFactory(prefix));
    }

    public static ExecutorService cachedThreadPool(int poolSize, String prefix) {
        return getInstance()._cachedThreadPool(poolSize, prefix);
    }

    public void shutdown() {
        Iterator<ExecutorService> iterator = weakHashMap.keySet().iterator();
        iterator.forEachRemaining(ExecutorService::shutdownNow);
    }

    private ExecutorService _fixedThreadPool(int poolSize, String prefix) {
        ExecutorService service = Executors.newFixedThreadPool(poolSize, new DaemonThreadFactory(prefix));
        weakHashMap.put(service, null);
        return service;
    }

    private ExecutorService _cachedThreadPool(int poolSize, String prefix) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, poolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), new DaemonThreadFactory(prefix));
        weakHashMap.put(executor, null);
        return executor;
    }

    private static class DaemonThreadFactory implements ThreadFactory {

        private AtomicInteger threadCounter = new AtomicInteger(0);
        private String namePrefix;

        public DaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(@Nonnull Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(namePrefix + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(HANDLER);
            return thread;
        }
    }
}
