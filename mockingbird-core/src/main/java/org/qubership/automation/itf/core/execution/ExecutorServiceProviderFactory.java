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

import javax.inject.Inject;
import javax.inject.Named;

import org.qubership.automation.itf.core.util.config.Config;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Factory with an ability to pass executionProvider thru constructor using DI.
 * If executionProvider is not set before 'get' method invoked, the DEFAULT strategy is used
 * You can {@link #init(Supplier)} singleton manually
 */
public class ExecutorServiceProviderFactory {

    private static final int EXECUTOR_THREAD_POOL_SIZE = Integer.parseInt(Config.getConfig().getString(
            "executor.thread.pool.size"));
    private static final int EXECUTOR_THREAD_POOL_CORE_SIZE = Integer.parseInt(Config.getConfig().getString(
            "executor.thread.pool.core.size"));
    private static final int BACKGROUND_EXECUTOR_THREAD_POOL_SIZE = Integer.parseInt(Config.getConfig().getString(
            "background.executor.thread.pool.size"));

    private static final Supplier<ExecutorServiceProvider> DEFAULT =
            () -> new DefaultExecutorServiceProvider(
                    EXECUTOR_THREAD_POOL_CORE_SIZE,
                    EXECUTOR_THREAD_POOL_SIZE,
                    BACKGROUND_EXECUTOR_THREAD_POOL_SIZE);

    private static volatile ExecutorServiceProvider INSTANCE;

    @Inject
    protected ExecutorServiceProviderFactory(@Named("executionProvider") ExecutorServiceProvider provider) {
        init(provider);
    }

    public static ExecutorServiceProvider get() {
        init(DEFAULT);
        return INSTANCE;
    }

    public static void init(Supplier<ExecutorServiceProvider> instance) {
        if (INSTANCE == null) {
            synchronized (ExecutorServiceProviderFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = instance.get();
                }
            }
        }
    }

    public static void init(ExecutorServiceProvider instance) {
        init(Suppliers.ofInstance(instance));
    }
}
