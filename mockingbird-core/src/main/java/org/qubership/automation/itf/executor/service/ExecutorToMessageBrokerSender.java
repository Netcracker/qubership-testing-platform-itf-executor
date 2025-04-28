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

package org.qubership.automation.itf.executor.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.activemq.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.atp.multitenancy.interceptor.jms.AtpJmsTemplate;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerStateResponse;
import org.qubership.automation.itf.core.model.communication.message.TriggerExecutionMessage;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutorToMessageBrokerSender {

    private final AtpJmsTemplate topicJmsTemplate;
    private final AtpJmsTemplate endSituationsTopicJmsTemplate;
    private final AtpJmsTemplate queueJmsTemplate;
    private final AtpJmsTemplate reportsQueueJmsTemplate;
    private final Environment env;
    private final Map<String, AtpJmsTemplate> jmsTemplateMap = new HashMap<>();
    private final String HOSTNAME_MESSAGE_PROPERTY = "hostname";

    @Value("${message-broker.executor-stubs-sync.topic}")
    String executorStubsSyncTopic;
    @Value("${message-broker.executor-stubs-outgoing-response.queue}")
    String executorStubsOutgoingResponseQueue;
    @Value("${message-broker.executor-configurator-event-triggers.topic}")
    String executorConfiguratorEventTriggersTopic;
    @Value("${message-broker.eds-update.topic}")
    String externalDataStorageUpdateTopic;
    @Value("${message-broker.end-exceptional-situations-events.topic}")
    String endExceptionalSituationsEventsTopic;
    @Value("${message-broker.executor-tccontext-operations.topic}")
    String tcContextOperationsTopic;
    @Value("${message-broker.executor-disabling-stepbystep.topic}")
    String disablingStepByStepTopic;
    @Value("${message-broker.executor-sync-reload-dictionary.topic}")
    String syncReloadDictionaryTopic;
    @Value("${message-broker.reports.queue}")
    String reportsIntegrationQueue;

    @PostConstruct
    public void init() {
        jmsTemplateMap.put("topic", topicJmsTemplate);
        jmsTemplateMap.put("queue", queueJmsTemplate);
    }

    public void sendMessageToExecutorStubsSyncTopic(String message) {
        topicJmsTemplate.convertAndSend(executorStubsSyncTopic, message);
    }

    @AtpJaegerLog()
    public void sendMessageToExecutorStubsOutgoingResponseQueue(TriggerExecutionMessage triggerExecutionMessage,
                                                                String tenantId) {
        if (StringUtils.isEmpty(triggerExecutionMessage.getBrokerMessageSelectorValue())) {
            log.error("SessionId: {}, Response is NOT sent to stubs, due to Broker Message Selector Value is EMPTY!",
                    triggerExecutionMessage.getSessionId());
            return;
        }
        MdcUtils.put(MdcField.TRACE_ID.toString(), MDC.get(MdcField.STUB_TRACE_ID.toString()));
        Map<String, Object> properties = new HashMap<>();
        properties.put(CustomHeader.X_PROJECT_ID, tenantId);
        properties.put("traceId", MDC.get("traceId"));
        properties.put(HOSTNAME_MESSAGE_PROPERTY, triggerExecutionMessage.getBrokerMessageSelectorValue());
        queueJmsTemplate.convertAndSend(executorStubsOutgoingResponseQueue, triggerExecutionMessage, properties);
        log.info("SessionId: {}, Broker Message Selector Value: {}. Response is sent to stubs",
                triggerExecutionMessage.getSessionId(), triggerExecutionMessage.getBrokerMessageSelectorValue());
    }

    public void sendMessageExecutorConfiguratorEventTriggersTopic(EventTriggerStateResponse message, String tenantId) {
        topicJmsTemplate.convertAndSend(executorConfiguratorEventTriggersTopic, message,
                Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }

    public void sendMessageToExternalDataStorageUpdateTopic(Object message, String tenantId) {
        topicJmsTemplate.convertAndSend(externalDataStorageUpdateTopic, message,
                Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }

    public void sendEventToEndExceptionalSituationsTopic(Object event, String tenantId) {
        endSituationsTopicJmsTemplate.convertAndSend(endExceptionalSituationsEventsTopic, event,
                Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }

    public void sendMessageToTcContextOperationsTopic(Object message, String tenantId) {
        topicJmsTemplate.convertAndSend(tcContextOperationsTopic, message,
                Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }

    public void sendMessageToDisableStepByStepTopic(Object message, String tenantId) {
        topicJmsTemplate.convertAndSend(disablingStepByStepTopic, message,
                Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }

    public void sendMessageToSyncReloadDictionaryTopic(Object message, String tenantId) {
        topicJmsTemplate.convertAndSend(syncReloadDictionaryTopic, message,
                Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }

    public void sendMessageToReportingQueue(Object message) {
        reportsQueueJmsTemplate.convertAndSend(reportsIntegrationQueue, message);
    }

    public void sendMessage(Object message, String queueNameParameterName, String queueType,
                            String tenantId) throws Exception {
        String queueName = env.getProperty(queueNameParameterName);
        if (queueName == null) {
            throw new ConfigurationException(
                    String.format("Parameter \"%s\" not found in application.properties", queueNameParameterName));
        }
        AtpJmsTemplate jmsTemplate = jmsTemplateMap.get(queueType);
        if (jmsTemplate == null) {
            throw new ConfigurationException(
                    String.format("Type \"%s\" cannot be processed, supported types: \"topic\", \"queue\".",
                            queueType));
        }
        jmsTemplate.convertAndSend(queueName, message, Collections.singletonMap(CustomHeader.X_PROJECT_ID, tenantId));
    }
}
