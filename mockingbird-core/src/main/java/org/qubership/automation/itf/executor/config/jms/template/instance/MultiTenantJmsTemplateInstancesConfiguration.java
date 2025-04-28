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

package org.qubership.automation.itf.executor.config.jms.template.instance;

import javax.jms.ConnectionFactory;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.multitenancy.interceptor.jms.MultiTenantJmsTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnProperty(name = "atp.multi-tenancy.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MultiTenantJmsTemplateInstancesConfiguration {

    private final ConnectionFactory defaultActiveMqConnectionFactory;
    private final ConnectionFactory reportsActiveMQConnectionFactory;
    private final ConnectionFactory reportsPooledConnectionFactory;

    @Bean
    public MultiTenantJmsTemplate topicJmsTemplateInstance() {
        return initMultiTenantJmsTemplate(defaultActiveMqConnectionFactory);
    }

    @Bean
    public MultiTenantJmsTemplate queueJmsTemplateInstance() {
        return initMultiTenantJmsTemplate(defaultActiveMqConnectionFactory);
    }

    @Bean
    public MultiTenantJmsTemplate endSituationsTopicJmsTemplateInstance() {
        return initMultiTenantJmsTemplate(defaultActiveMqConnectionFactory);
    }

    @Bean
    public MultiTenantJmsTemplate reportsQueueJmsTemplateInstance() {
        return initMultiTenantJmsTemplate(reportsPooledConnectionFactory);
    }

    @NotNull
    private MultiTenantJmsTemplate initMultiTenantJmsTemplate(ConnectionFactory connectionFactory) {
        MultiTenantJmsTemplate multiTenantJmsTemplate = new MultiTenantJmsTemplate();
        multiTenantJmsTemplate.setConnectionFactory(connectionFactory);
        return multiTenantJmsTemplate;
    }
}
