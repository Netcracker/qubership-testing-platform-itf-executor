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

package org.qubership.automation.itf.integration;

import java.time.OffsetDateTime;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.json.JSONObject;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.model.communication.message.CommonTriggerExecutionMessage;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerBulkActivationRequest;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSingleActivationRequest;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.util.descriptor.StorableDescriptor;
import org.qubership.automation.itf.core.util.eds.ExternalDataManagementService;
import org.qubership.automation.itf.core.util.eds.model.FileInfo;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.SecurityHelper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class StubJMSListeners {

    private final TriggerExecutor triggerExecutor;
    private final ObjectMapper executorIntegrationObjectMapper;
    private final EventTriggerActivationService eventTriggerActivationService;
    private final ExternalDataManagementService externalDataManagementService;
    private final Map<String, SseEmitter> sseEmitters;
    private final EventBusProvider eventBusProvider;
    private final ObjectMapper endExceptionalSituationsMapper;

    @Value("${atp-itf-executor.sse-reconnect-time}")
    private Long sseReconnectTime;

    /**
     * JMSListener to process messages from stubs-executor-incoming-request queue
     * and produce event to trigger executor.
     *
     * @param activeMqTextMessage - message from stubs which contains
     *       - String trigger typeName
     *       - {@link CommonTriggerExecutionMessage} message
     *       - {@link StorableDescriptor} triggerConfigurationDescriptor
     *       - String sessionId
     */
    @JmsListener(destination = "${message-broker.stubs-executor-incoming-request.queue}",
            containerFactory = "stubDefaultJmsListenerQueueContainerFactory")
    @AtpJaegerLog()
    public void onExecutorStubsSyncMessage(ActiveMQTextMessage activeMqTextMessage) {
        try {
            final OffsetDateTime started = OffsetDateTime.now();
            activeMqTextMessage.acknowledge();
            setThreadName("stub");
            CommonTriggerExecutionMessage message = executorIntegrationObjectMapper.readValue(
                    activeMqTextMessage.getText(), CommonTriggerExecutionMessage.class);
            MdcUtils.put(MdcField.PROJECT_ID.toString(), message.getTriggerConfigurationDescriptor().getProjectUuid());
            MdcUtils.put(MdcField.SESSION_ID.toString(), message.getSessionId());
            String currentTraceId = activeMqTextMessage.getStringProperty("traceId");
            MdcUtils.put(MdcField.TRACE_ID.toString(), currentTraceId);
            MdcUtils.put(MdcField.STUB_TRACE_ID.toString(), currentTraceId);
            log.info("Project: {}. SessionId: {}. Message for execution is received.",
                    message.getTriggerConfigurationDescriptor().getProjectUuid(), message.getSessionId());
            log.debug("Message for execution: {}", message.getMessage());
            triggerExecutor.produceEvent(message, started);
        } catch (JMSException | JsonProcessingException e) {
            log.error("Error while message processing: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @JmsListener(destination = "${message-broker.configurator-executor-event-triggers.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    public void onConfiguratorExecutorEventTriggerMessage(ActiveMQTextMessage activeMqTextMessage) {
        try {
            log.info("Message for event trigger activation received");
            log.debug("Message for execution: {}", activeMqTextMessage.getText());
            setThreadName("event");
            JsonNode jsonMessage = executorIntegrationObjectMapper.readTree(activeMqTextMessage.getText());
            String type = jsonMessage.get("type").asText();
            String sessionId = jsonMessage.get("sessionId").asText();
            MdcUtils.put(MdcField.SESSION_ID.toString(), sessionId);
            String tenantId = activeMqTextMessage.getStringProperty(CustomHeader.X_PROJECT_ID);
            SecurityHelper.propagateSecurityContext(jsonMessage);
            switch (type) {
                case "single":
                    eventTriggerActivationService.switchTriggerState(
                            executorIntegrationObjectMapper.readValue(activeMqTextMessage.getText(),
                                    EventTriggerSingleActivationRequest.class), tenantId, null, false);
                    break;
                case "bulk":
                    eventTriggerActivationService.switchTriggersState(
                            executorIntegrationObjectMapper.readValue(activeMqTextMessage.getText(),
                                    EventTriggerBulkActivationRequest.class), tenantId);
                    break;
                case "sync":
                    eventTriggerActivationService.syncTriggersState(
                            executorIntegrationObjectMapper.readValue(activeMqTextMessage.getText(),
                                    EventTriggerSyncActivationRequest.class), tenantId, true, false);
                    break;
                case "afterRestoreSituationEventTrigger":
                    eventTriggerActivationService.syncTriggersState(
                            executorIntegrationObjectMapper.readValue(activeMqTextMessage.getText(),
                                    EventTriggerSyncActivationRequest.class), tenantId, false, true);
                    break;
                case "afterRestoreOperationEventTrigger":
                    eventTriggerActivationService.switchTriggerState(
                            executorIntegrationObjectMapper.readValue(activeMqTextMessage.getText(),
                                    EventTriggerSingleActivationRequest.class), tenantId, true, true);
                    break;
                default:
                    log.error("Unexpected value from configurator_executor_event_triggers topic: {}", type);
            }
        } catch (JMSException | JsonProcessingException e) {
            log.error("Error while message processing: {}", e.getMessage());
        } finally {
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }

    @JmsListener(destination = "${message-broker.stubs-configurator.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    @JmsListener(destination = "${message-broker.executor-configurator-event-triggers.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    public void onSseToConfiguratorMessage(ActiveMQTextMessage activeMqTextMessage) {
        String sessionId = "";
        SseEmitter emitter = null;
        try {
            setThreadName("event");
            String activeMqMessage = activeMqTextMessage.getText();
            sessionId = executorIntegrationObjectMapper.readTree(activeMqMessage).get("sessionId").asText();
            MdcUtils.put(MdcField.SESSION_ID.toString(), sessionId);
            emitter = sseEmitters.get(sessionId);
            if (emitter != null) {
                SseEmitter.SseEventBuilder sseEvent = SseEmitter.event().id(sessionId).name("message")
                        .data(activeMqMessage).reconnectTime(sseReconnectTime);
                emitter.send(sseEvent);
                log.debug("Message for execution: {}", sseEvent);
            }
        } catch (Exception ex) {
            if (emitter != null) {
                log.error("Exception while sending a response thrown by emitter with sessionId: {}", sessionId, ex);
                String clientErrorMessage = "Failed to execute request. See logs for more details.";
                emitter.completeWithError(new EngineIntegrationException(clientErrorMessage));
            } else {
                log.error("Error while message processing: {}", activeMqTextMessage, ex);
            }
        } finally {
            if (emitter != null) {
                emitter.complete();
                log.info("Event emitter with sessionId: {} is completed.", sessionId);
                sseEmitters.remove(sessionId);
            }
            MDC.clear();
        }
    }

    @JmsListener(destination = "${message-broker.eds-update.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    public void onExternalDataStorageUpdateMessage(ActiveMQTextMessage activeMqTextMessage) {
        try {
            setThreadName("eds");
            FileInfo fileInfo = executorIntegrationObjectMapper.readValue(activeMqTextMessage.getText(),
                    FileInfo.class);
            MdcUtils.put(MdcField.PROJECT_ID.toString(), fileInfo.getProjectUuid());
            switch (fileInfo.getEventType()) {
                case UPLOAD: {
                    if (fileInfo.getObjectId() != null) {
                        externalDataManagementService.getFileManagementService()
                                .save(externalDataManagementService.getExternalStorageService()
                                        .getFileInfo(fileInfo.getObjectId()));
                    } else {
                        externalDataManagementService.getFileManagementService().save(fileInfo);
                    }
                    log.info("File '{}' is loaded into local storage successfully.", fileInfo.getFileName());
                    break;
                }
                case DELETE: {
                    externalDataManagementService.getFileManagementService().delete(fileInfo);
                    break;
                }
                default: {
                    throw new RuntimeException(String.format("Unknown file event type '%s' for topic "
                            + "'message-broker.eds-update.topic'.", fileInfo.getEventType()));
                }
            }
        } catch (Exception e) {
            log.error("Error while message processing: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @JmsListener(destination = "${message-broker.end-exceptional-situations-events.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    @Transactional
    public void onEndExceptionalSituationsFinishEvent(ActiveMQTextMessage activeMqTextMessage) {
        try {
            setThreadName("event");
            SituationEvent.EndExceptionalSituationFinish event = endExceptionalSituationsMapper
                    .readValue(activeMqTextMessage.getText(), SituationEvent.EndExceptionalSituationFinish.class);
            JSONObject json = new JSONObject(activeMqTextMessage.getText());
            String tcContextId = json.getJSONObject("situationInstance").getJSONObject("context").getString("tc");
            MdcUtils.put(MdcField.CONTEXT_ID.toString(), tcContextId);
            TcContext tcContext = CacheServices.getTcContextCacheService().getById(tcContextId);
            if (tcContext == null) {
                return;
            }
            event.getSituationInstance().getContext().setTC(tcContext);
            event.getSituationInstance().setParentContext(tcContext);
            eventBusProvider.post(event);
        } catch (Exception e) {
            log.error("Error while posting finish event to EventBus after getting it "
                    + "from 'end_exceptional_situations_events' topic: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    /**
     * Set current thread name according to method invoked (so, according to where the message is received from).
     * This renaming is to track thread name in the PostgreSQL pg_stat_activity view (application name column).
     * Without it thread names are truncated, so all valuable information is lost.
     *
     * @param prefix - String prefix for a new Thread name.
     */
    private void setThreadName(String prefix) {
        String currentName = Thread.currentThread().getName();
        int pos = currentName.indexOf('#');
        if (pos > -1) {
            Thread.currentThread().setName(prefix + currentName.substring(pos));
        } else {
            Thread.currentThread().setName(prefix);
        }
    }

}
