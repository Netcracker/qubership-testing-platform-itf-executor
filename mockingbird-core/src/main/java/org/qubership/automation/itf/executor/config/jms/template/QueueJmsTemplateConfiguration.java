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

package org.qubership.automation.itf.executor.config.jms.template;

import org.qubership.atp.multitenancy.interceptor.jms.AtpJmsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class QueueJmsTemplateConfiguration {

    private final MessageConverter jacksonJmsMessageConverter;
    @Value("${message-broker.executor-stubs-outgoing-response.message-time-to-live}")
    private int outgoingResponseForStubsQueueMessagesTimeToLive;
    @Value("${message-broker.reports.message-time-to-live}")
    private int reportingQueueMessagesTimeToLive;

    @Bean
    public AtpJmsTemplate queueJmsTemplate(AtpJmsTemplate queueJmsTemplateInstance) {
        queueJmsTemplateInstance.setMessageConverter(jacksonJmsMessageConverter);
        queueJmsTemplateInstance.setPubSubDomain(false);
        ((JmsTemplate) queueJmsTemplateInstance).setExplicitQosEnabled(true);
        ((JmsTemplate) queueJmsTemplateInstance).setTimeToLive(outgoingResponseForStubsQueueMessagesTimeToLive);
        return queueJmsTemplateInstance;
    }

    @Bean
    public AtpJmsTemplate reportsQueueJmsTemplate(AtpJmsTemplate reportsQueueJmsTemplateInstance) {
        reportsQueueJmsTemplateInstance.setPubSubDomain(false);
        ((JmsTemplate) reportsQueueJmsTemplateInstance).setExplicitQosEnabled(true);
        ((JmsTemplate) reportsQueueJmsTemplateInstance).setTimeToLive(reportingQueueMessagesTimeToLive);
        return reportsQueueJmsTemplateInstance;
    }
}
