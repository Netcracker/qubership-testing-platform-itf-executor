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

package org.qubership.automation.itf.executor.config.jms;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@Configuration
@EnableJms
public class ExecutorIntegrationConfig {

    public static final TransactionDefinition STUB_TRANSACTION_DEFINITION = initTransactionDefinition();
    @Value("${message-broker.url}")
    private String brokerUrl;
    @Value("${message-broker.useCompression}")
    private String useCompression;
    @Value("${message-broker.useAsyncSend}")
    private String useAsyncSend;
    @Value("${message-broker.queuePrefetch}")
    private int queuePrefetch;
    @Value("${message-broker.reports.useCompression}")
    private String reportsUseCompression;
    @Value("${message-broker.reports.useAsyncSend}")
    private String reportsUseAsyncSend;
    @Value("${message-broker.reports.maxThreadPoolSize}")
    private int maxThreadPoolSize;
    @Value("${message-broker.reports.connectionsPoolSize}")
    private int connectionsPoolSize;

    private static TransactionDefinition initTransactionDefinition() {
        int timeoutSeconds = 0;
        String stubsProcessingDurationTimeMax = System.getProperty("stubs.processing.duration.time.max");
        if (StringUtils.isNotEmpty(stubsProcessingDurationTimeMax)) {
            try {
                timeoutSeconds = Integer.parseInt(stubsProcessingDurationTimeMax);
            } catch (NumberFormatException ex) {
                timeoutSeconds = 0;
            }
        }
        if (timeoutSeconds <= 0) {
            // No transaction duration control is configured (= old default behavior)
            return TxExecutor.readOnlyTransaction();
        } else {
            DefaultTransactionDefinition readonlyWithTimeout =
                    new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
            readonlyWithTimeout.setReadOnly(true);
            readonlyWithTimeout.setTimeout(timeoutSeconds);
            return readonlyWithTimeout;
        }
    }

    /**
     * {@link ActiveMQConnectionFactory} defaultActiveMqConnectionFactory creating and configuration.
     * Will use for atp-itf-executor jms listeners and senders.
     *
     * @return {@link ActiveMQConnectionFactory} defaultActiveMqConnectionFactory.
     */
    @Bean
    public ActiveMQConnectionFactory defaultActiveMqConnectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
        activeMQConnectionFactory.setBrokerURL(brokerUrl);
        activeMQConnectionFactory.setUseCompression(Boolean.parseBoolean(useCompression));
        activeMQConnectionFactory.setUseAsyncSend(Boolean.parseBoolean(useAsyncSend));
        activeMQConnectionFactory.setAlwaysSyncSend(!activeMQConnectionFactory.isUseAsyncSend());
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(queuePrefetch);
        activeMQConnectionFactory.setPrefetchPolicy(prefetchPolicy);
        return activeMQConnectionFactory;
    }

    /**
     * {@link ActiveMQConnectionFactory} reportsActiveMQConnectionFactory creating and configuration.
     * Will use for atp-itf-executor senders.
     *
     * @return {@link ActiveMQConnectionFactory} reportsActiveMQConnectionFactory.
     */
    @Bean
    public ActiveMQConnectionFactory reportsActiveMQConnectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
        activeMQConnectionFactory.setBrokerURL(brokerUrl);
        activeMQConnectionFactory.setMaxThreadPoolSize(maxThreadPoolSize);
        activeMQConnectionFactory.setUseCompression(Boolean.parseBoolean(reportsUseCompression));
        activeMQConnectionFactory.setUseAsyncSend(Boolean.parseBoolean(reportsUseAsyncSend));
        activeMQConnectionFactory.setAlwaysSyncSend(!activeMQConnectionFactory.isUseAsyncSend());
        return activeMQConnectionFactory;
    }

    @Bean
    public ConnectionFactory reportsPooledConnectionFactory(
            ActiveMQConnectionFactory reportsActiveMQConnectionFactory) {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(reportsActiveMQConnectionFactory);
        pooledConnectionFactory.setMaxConnections(connectionsPoolSize);
        pooledConnectionFactory.setCreateConnectionOnStartup(true);
        return pooledConnectionFactory;
    }

    @Bean
    public MessageConverter simpleMessageConverter() {
        //additional logic in future might be there
        return new SimpleMessageConverter();
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(executorIntegrationObjectMapper());
        return converter;
    }

    @Bean
    public MessageConverter endSituationsJacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(endExceptionalSituationsMapper());
        return converter;
    }

    @Bean
    public ObjectMapper executorIntegrationObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.setFailOnUnknownId(false);
        filterProvider.addFilter("reportWorkerFilter_SituationInstance", SimpleBeanPropertyFilter.serializeAllExcept(
                "prefix", "description", "running", "finished", "transportConfiguration", "version", "iterator"
        ));
        objectMapper.setFilterProvider(filterProvider);
        return objectMapper;
    }

    @Bean
    public ObjectMapper endExceptionalSituationsMapper() {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.setFailOnUnknownId(false);
        filterProvider.addFilter("reportWorkerFilter_SituationInstance", SimpleBeanPropertyFilter.serializeAllExcept(
                "prefix", "description", "running", "finished", "transportConfiguration", "version", "iterator",
                "stepContainer"
        ));
        filterProvider.addFilter("reportWorkerFilter_InstanceContext",
                SimpleBeanPropertyFilter.serializeAllExcept(
                        "transport", "version", "history", "collectHistory", "prefix", "description", "empty"));
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setFilterProvider(filterProvider);
        return objectMapper;
    }
}
