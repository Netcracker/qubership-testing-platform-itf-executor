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

package org.qubership.automation.itf.executor.config.jms.listener.factory;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
public class JmsListenersContainerFactoriesConfiguration {

    @Value("${message-broker.stubs-executor.listenerContainerFactory.concurrency}")
    private String stubsExecutorListenerConcurrency;

    @Value("${message-broker.stubs-executor.listenerContainerFactory.maxMessagesPerTask}")
    private String stubsExecutorListenerMaxMessagesPerTask;

    /**
     * Configuration for DefaultJmsListenerContainerFactory instance for stubs jms listeners.
     *
     * @param defaultActiveMqConnectionFactory connection factory that using by listeners.
     * @param jmsQueueListenerContainerFactory instance of DefaultJmsListenerContainerFactory.
     * @return configured stubDefaultJmsListenerQueueContainerFactory.
     */
    @Bean
    public DefaultJmsListenerContainerFactory stubDefaultJmsListenerQueueContainerFactory(
            ConnectionFactory defaultActiveMqConnectionFactory,
            DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory) {
        jmsQueueListenerContainerFactory.setConnectionFactory(defaultActiveMqConnectionFactory);
        jmsQueueListenerContainerFactory.setConcurrency(stubsExecutorListenerConcurrency);
        if (stubsExecutorListenerMaxMessagesPerTask != null && !stubsExecutorListenerMaxMessagesPerTask.isEmpty()) {
            int maxMessagesPerTask = Integer.parseInt(stubsExecutorListenerMaxMessagesPerTask);
            if (maxMessagesPerTask == -1 || maxMessagesPerTask > 0) {
                jmsQueueListenerContainerFactory.setMaxMessagesPerTask(maxMessagesPerTask);
            }
        }
        jmsQueueListenerContainerFactory.setPubSubDomain(false);
        jmsQueueListenerContainerFactory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
        jmsQueueListenerContainerFactory.setSessionAcknowledgeMode(ActiveMQSession.INDIVIDUAL_ACKNOWLEDGE);
        jmsQueueListenerContainerFactory.setAutoStartup(false);
        return jmsQueueListenerContainerFactory;
    }

    /**
     * DefaultJmsListenerContainerFactory instance configuration for default jms listeners.
     *
     * @param defaultActiveMqConnectionFactory connection factory that using by listeners.
     * @param jmsTopicListenerContainerFactory instance of DefaultJmsListenerContainerFactory.
     * @return configured defaultJmsListenerTopicContainerFactory.
     */
    @Bean
    public DefaultJmsListenerContainerFactory defaultJmsListenerTopicContainerFactory(
            ConnectionFactory defaultActiveMqConnectionFactory,
            DefaultJmsListenerContainerFactory jmsTopicListenerContainerFactory) {
        jmsTopicListenerContainerFactory.setConnectionFactory(defaultActiveMqConnectionFactory);
        jmsTopicListenerContainerFactory.setPubSubDomain(true);
        jmsTopicListenerContainerFactory.setAutoStartup(false);
        return jmsTopicListenerContainerFactory;
    }
}
