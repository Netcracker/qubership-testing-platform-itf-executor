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
public class TopicJmsTemplateConfiguration {

    private final MessageConverter endSituationsJacksonJmsMessageConverter;
    private final MessageConverter jacksonJmsMessageConverter;
    @Value("${message-broker.other-topics.message-time-to-live}")
    private int otherTopicsMessagesTimeToLive;
    @Value("${message-broker.end-exceptional-situations-events.message-time-to-live}")
    private int situationEventsTopicMessagesTimeToLive;

    /**
     * {@link AtpJmsTemplate} topicJmsTemplate configuration.
     *
     * @param topicJmsTemplateInstance {@link AtpJmsTemplate} instance.
     * @return configured topicJmsTemplate.
     */
    @Bean
    public AtpJmsTemplate topicJmsTemplate(AtpJmsTemplate topicJmsTemplateInstance) {
        topicJmsTemplateInstance.setMessageConverter(jacksonJmsMessageConverter);
        topicJmsTemplateInstance.setPubSubDomain(true);
        ((JmsTemplate) topicJmsTemplateInstance).setExplicitQosEnabled(true);
        ((JmsTemplate) topicJmsTemplateInstance).setTimeToLive(otherTopicsMessagesTimeToLive);
        return topicJmsTemplateInstance;
    }

    /**
     * {@link AtpJmsTemplate} endSituationsTopicJmsTemplate configuration.
     *
     * @param endSituationsTopicJmsTemplateInstance {@link AtpJmsTemplate} instance.
     * @return configured endSituationsTopicJmsTemplate.
     */
    @Bean
    public AtpJmsTemplate endSituationsTopicJmsTemplate(AtpJmsTemplate endSituationsTopicJmsTemplateInstance) {
        endSituationsTopicJmsTemplateInstance.setMessageConverter(endSituationsJacksonJmsMessageConverter);
        endSituationsTopicJmsTemplateInstance.setPubSubDomain(true);
        ((JmsTemplate) endSituationsTopicJmsTemplateInstance).setExplicitQosEnabled(true);
        ((JmsTemplate) endSituationsTopicJmsTemplateInstance).setTimeToLive(situationEventsTopicMessagesTimeToLive);
        return endSituationsTopicJmsTemplateInstance;
    }
}
