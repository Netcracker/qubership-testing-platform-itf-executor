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

package org.qubership.automation.itf.core.metric;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.execution.ExecutorServiceProviderFactory;
import org.qubership.automation.itf.core.execution.WaitTimeMonitoringThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.NonNull;

@Service
public class MetricsAggregateService {

    private static MeterRegistry meterRegistry;
    private Counter.Builder executorCallChainCounter;
    private Counter.Builder executorContextSizeCounter;
    private static Counter.Builder executorContextSizeInHazelcastCounter;
    @Value("#{${exclude.registry.metrics.tags}}")
    private Map<String, List<String>> excludeRegistryMetricsTags;
    @Value("${message-broker.stubs-executor-incoming-request.queue}")
    private String destinationQueueName;
    private ApplicationContext applicationContext;
    private DefaultMessageListenerContainer defaultMessageListenerContainer;
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    @Autowired
    public MetricsAggregateService(ApplicationContext applicationContext,
                                   JmsListenerEndpointRegistry jmsListenerEndpointRegistry,
                                   MeterRegistry meterRegistry) {
        this.applicationContext = applicationContext;
        this.jmsListenerEndpointRegistry = jmsListenerEndpointRegistry;
        this.meterRegistry = meterRegistry;
        this.executorCallChainCounter = Counter
                .builder(Metric.ATP_ITF_EXECUTOR_CALLCHAIN_COUNT_BY_PROJECT.getValue())
                .tags(MetricTag.PROJECT.getValue(), "", MetricTag.CALLCHAIN_NAME.getValue(), "")
                .description("total number of running call chains");
        this.executorContextSizeCounter = Counter
                .builder(Metric.ATP_ITF_EXECUTOR_CONTEXT_SIZE_BY_PROJECT.getValue())
                .tags(MetricTag.PROJECT.getValue(), "", MetricTag.CALLCHAIN_NAME.getValue(), "")
                .description("total size of testcase contexts");
        this.executorContextSizeInHazelcastCounter = Counter
                .builder(Metric.ATP_ITF_EXECUTOR_HAZELCAST_CONTEXT_SIZE_BY_PROJECT.getValue())
                .tags(MetricTag.PROJECT.getValue(), "", MetricTag.CONTEXT_ID.getValue(), "")
                .description("total size of testcase contexts in Hazelcast");
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (excludeRegistryMetricsTags.containsKey(id.getName())) {
                    List<String> excludeTags = excludeRegistryMetricsTags.get(id.getName());
                    List<Tag> tags = stream(id.getTagsAsIterable().spliterator(), false)
                            .filter(t -> !excludeTags.contains(t.getKey()))
                            .collect(toList());
                    return id.replaceTags(tags);
                }
                return id;
            }
        });
    }

    @EventListener
    public void init(ContextRefreshedEvent event) {
        if (event.getSource().equals(applicationContext)) {
            contextInitialized();
            fillJmsListenerStatsMetric();
            fillExecutorPoolStatsMetric();
        }
    }

    public void incrementCallChainCountToProject(@NonNull UUID projectUuid, @NonNull String callChainName) {
        executorCallChainCounter
                .tags(MetricTag.PROJECT.getValue(), projectUuid.toString(), MetricTag.CALLCHAIN_NAME.getValue(),
                        callChainName)
                .register(meterRegistry)
                .increment();
    }

    public void incrementContextSizeCountToProject(@NonNull UUID projectUuid, @NonNull String callChainName, int size) {
        if (StringUtils.isNotEmpty(callChainName)) {
            executorContextSizeCounter
                    .tags(MetricTag.PROJECT.getValue(), projectUuid.toString(), MetricTag.CALLCHAIN_NAME.getValue(),
                            callChainName)
                    .register(meterRegistry)
                    .increment(size);
        }
    }

    public static void summaryHazelcastContextSizeCountToProject(@NonNull UUID projectUuid, @NonNull Object contextId,
                                                                   int size) {
        DistributionSummary summary = DistributionSummary
                .builder(Metric.ATP_ITF_EXECUTOR_HAZELCAST_CONTEXT_SIZE_BY_PROJECT.getValue())
                .description("total size of testcase contexts in Hazelcast")
                .baseUnit("bytes")
                .tags(MetricTag.PROJECT.getValue(), projectUuid.toString(), MetricTag.CONTEXT_ID.getValue(),
                        contextId.toString())
                .register(meterRegistry);
        summary.record(size);
    }

    public static void removeHazelcastContextSizeCountToProject(@NonNull UUID projectUuid, @NonNull Object contextId) {
        meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(
                        Metric.ATP_ITF_EXECUTOR_HAZELCAST_CONTEXT_SIZE_BY_PROJECT.getValue()))
                .filter(m -> projectUuid.toString().equals(m.getId().getTag(MetricTag.PROJECT.getValue()))
                        && contextId.equals(m.getId().getTag(MetricTag.CONTEXT_ID.getValue())))
                .forEach(meterRegistry::remove);
    }

    public void recordExecuteCallchainDuration(@NonNull UUID projectUuid, @NonNull String callChainName,
                                               @NonNull Duration duration) {
        meterRegistry.timer(Metric.ATP_ITF_EXECUTOR_CALLCHAIN_SECONDS_BY_PROJECT.getValue(),
                        MetricTag.PROJECT.getValue(), projectUuid.toString(),
                        MetricTag.CALLCHAIN_NAME.getValue(), callChainName)
                .record(duration);
    }

    public void recordIncomingRequestDuration(@NonNull UUID projectUuid, @NonNull String endPoint,
                                              @NonNull Duration duration) {
        meterRegistry.timer(Metric.ATP_ITF_EXECUTOR_STUB_REQUEST_SECONDS_BY_PROJECT.getValue(),
                        MetricTag.PROJECT.getValue(), projectUuid.toString(),
                        MetricTag.ENDPOINT.getValue(), endPoint)
                .record(duration);
    }

    private void contextInitialized() {
        for (MessageListenerContainer messageListenerContainer : jmsListenerEndpointRegistry.getListenerContainers()) {
            if (destinationQueueName.equals(((DefaultMessageListenerContainer) messageListenerContainer).getDestinationName())) {
                defaultMessageListenerContainer = (DefaultMessageListenerContainer) messageListenerContainer;
                break;
            }
        }
    }

    private void fillJmsListenerStatsMetric() {
        initializeGauges(Metric.ATP_ITF_EXECUTOR_JMS_LISTENER_THREAD_POOL_ACTIVE_SIZE,
                () -> defaultMessageListenerContainer.getActiveConsumerCount());
        initializeGauges(Metric.ATP_ITF_EXECUTOR_JMS_LISTENER_THREAD_POOL_MAX_SIZE,
                () -> defaultMessageListenerContainer.getMaxConcurrentConsumers());
    }

    private void fillExecutorPoolStatsMetric() {
        initializeGauges(Metric.ATP_ITF_EXECUTOR_REGULAR_POOL_ACTIVE_SIZE,
                () -> ((WaitTimeMonitoringThreadPoolExecutor) ExecutorServiceProviderFactory.get().requestForRegular()).getActiveCount());
        initializeGauges(Metric.ATP_ITF_EXECUTOR_REGULAR_POOL_MAX_SIZE,
                () -> ((WaitTimeMonitoringThreadPoolExecutor) ExecutorServiceProviderFactory.get().requestForRegular()).getMaximumPoolSize());
        initializeGauges(Metric.ATP_ITF_EXECUTOR_INBOUND_POOL_ACTIVE_SIZE,
                () -> ((ThreadPoolExecutor) ExecutorServiceProviderFactory.get().requestForInboundProcessing()).getActiveCount());
        initializeGauges(Metric.ATP_ITF_EXECUTOR_INBOUND_POOL_MAX_SIZE,
                () -> ((ThreadPoolExecutor) ExecutorServiceProviderFactory.get().requestForInboundProcessing()).getLargestPoolSize());
    }

    private void initializeGauges(Metric metric, Supplier<Number> function) {
        Gauge.builder(metric.getValue(), function).register(meterRegistry);
    }

    private void initializeGauges(UUID projectUuid, Metric metric, Map<UUID, AtomicInteger> metricsMap) {
        if (!metricsMap.containsKey(projectUuid)) {
            metricsMap.put(projectUuid, new AtomicInteger());
            Gauge.builder(metric.getValue(), () -> metricsMap.get(projectUuid).get())
                    .tag(MetricTag.PROJECT.getValue(), projectUuid.toString())
                    .register(meterRegistry);
        }
    }
}
