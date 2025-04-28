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

package org.qubership.automation.itf.ui.config;

import org.aspectj.lang.annotation.Pointcut;
import org.qubership.automation.itf.core.execution.DefaultExecutorServiceProvider;
import org.qubership.automation.itf.core.execution.ExecutorServiceProvider;
import org.qubership.automation.itf.core.execution.ExecutorServiceProviderFactory;
import org.qubership.automation.itf.core.template.velocity.VelocityTemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.generator.id.CounterIdGenerator;
import org.qubership.automation.itf.core.util.generator.id.IdGenerator;
import org.qubership.automation.itf.core.util.generator.id.IdGeneratorInterface;
import org.qubership.automation.itf.core.util.generator.prefix.IPrefixFactory;
import org.qubership.automation.itf.core.util.generator.prefix.PrefixGenerator;
import org.qubership.automation.itf.core.util.generator.prefix.StringPrefixFactory;
import org.qubership.automation.itf.ui.aspects.ErrorHandler;
import org.qubership.automation.itf.ui.aspects.TransactionAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configuration
public class RootConfiguration {

    @Value("${executor.thread.pool.size}")
    private int executorThreadPoolSize;
    @Value("${background.executor.thread.pool.size}")
    private int backgroundExecutorThreadPoolSize;
    @Value("${executor.thread.pool.core.size}")
    private int executorThreadPoolCoreSize;

    @Bean
    public CounterIdGenerator idGenerator() {
        return new CounterIdGenerator();
    }

    @Bean(name = "IdGenerator")
    public IdGeneratorInterface getIDGenerator() {
        IdGenerator.init(idGenerator());
        return IdGenerator.get();
    }

    @Bean
    public StringPrefixFactory prefixFactory() {
        return new StringPrefixFactory();
    }

    @Bean(name = "PrefixGenerator")
    public IPrefixFactory getPrefixGenerator() {
        PrefixGenerator.init(prefixFactory());
        return PrefixGenerator.get();
    }

    @Bean
    public VelocityTemplateEngine templateEngine() {
        return new VelocityTemplateEngine();
    }

    @Bean(name = "TemplateEngineFactory")
    public TemplateEngine getTemplateEngineFactory() {
        TemplateEngineFactory.init(templateEngine());
        return TemplateEngineFactory.get();
    }

    @Bean
    public DefaultExecutorServiceProvider executionProvider(@Value("${executor.thread.pool.core.size}") int executorThreadPoolCoreSize,
                                                            @Value("${executor.thread.pool.size}") int executorThreadPoolSize,
                                                            @Value("${background.executor.thread.pool.size}") int backgroundExecutorThreadPoolSize) {
        return new DefaultExecutorServiceProvider(executorThreadPoolCoreSize, executorThreadPoolSize,
                backgroundExecutorThreadPoolSize);
    }

    @Bean(name = "ExecutorServiceProviderFactory")
    public ExecutorServiceProvider getExecutorServiceProviderFactory() {
        ExecutorServiceProviderFactory.init(executionProvider(executorThreadPoolCoreSize, executorThreadPoolSize,
                backgroundExecutorThreadPoolSize));
        return ExecutorServiceProviderFactory.get();
    }

    @Bean
    public TransactionAspect transactionAspect() {
        return new TransactionAspect();
    }

    @Bean
    @Pointcut("within(org.qubership.automation.itf.ui..*)")
    public ErrorHandler errorHandler() {
        return new ErrorHandler();
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/");
        viewResolver.setSuffix(".html");
        return viewResolver;
    }
}
