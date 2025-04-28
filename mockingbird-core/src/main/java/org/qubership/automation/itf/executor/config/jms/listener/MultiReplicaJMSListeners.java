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

package org.qubership.automation.itf.executor.config.jms.listener;

import java.math.BigInteger;
import java.util.Objects;

import javax.jms.JMSException;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.EnumUtils;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.diameter.config.ConfigReader;
import org.qubership.automation.diameter.dictionary.DictionaryConfig;
import org.qubership.automation.itf.core.execution.ExecutorServiceProviderFactory;
import org.qubership.automation.itf.core.message.DictionaryReloadMessage;
import org.qubership.automation.itf.core.message.TcContextOperationMessage;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MultiReplicaJMSListeners {

    private final ObjectMapper executorIntegrationObjectMapper;
    private final TCContextService tcContextService;

    @JmsListener(destination = "${message-broker.executor-sync-reload-dictionary.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    public void onSyncReloadDictionaryMessage(ActiveMQTextMessage activeMQTextMessage) {
        try {
            DictionaryReloadMessage message = executorIntegrationObjectMapper.readValue(activeMQTextMessage.getText(),
                    DictionaryReloadMessage.class);
            ConfigReader.read(
                    new DictionaryConfig(message.getDictionaryPath(), message.getParserClass(), message.getProject()),
                    true);
            log.info("Diameter dictionary is successfully reloaded on pod {}", Config.getConfig().getRunningHostname());
        } catch (JMSException | JsonProcessingException e) {
            log.error("Error while message processing: {}", e.getMessage());
        } catch (Exception ee) {
            log.error("Diameter dictionary reloading is failed: {}", ee.getMessage());
        }
    }

    @Transactional
    @JmsListener(destination = "${message-broker.executor-tccontext-operations.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    public void onTcContextOperationsMessage(ActiveMQTextMessage activeMQTextMessage) {
        try {
            TcContextOperationMessage message = executorIntegrationObjectMapper.readValue(activeMQTextMessage.getText(),
                    TcContextOperationMessage.class);
            BigInteger contextId = message.getId();
            if (contextId == null) {
                throw new IllegalArgumentException("Parameter 'contextId' is null or missed");
            }
            MdcUtils.put(MdcField.CONTEXT_ID.toString(), contextId.toString());
            String newState = message.getStatus();
            if (newState == null) {
                throw new IllegalArgumentException(
                        String.format("Context ID=%s: Parameter 'state' (Context state) is null or missed", contextId));
            } else if (!EnumUtils.isValidEnum(Status.class, newState)) {
                throw new IllegalArgumentException(
                        String.format("Context ID=%s: Parameter 'state' (Context state) value is incorrect: '%s'",
                                contextId, newState));
            }
            TcContext tcContext = CacheServices.getTcContextCacheService().getById(contextId);
            if (Objects.isNull(tcContext)) {
                throw new IllegalArgumentException(
                        String.format("Context ID=%s: Context isn't found by id", contextId));
            }
            if (!tcContext.getPodName().equals(Config.getConfig().getRunningHostname())) {
                return;
            }
            ExecutorServiceProviderFactory.get().requestForRegular().submit(
                    () -> progressingTcContext(tcContext, Status.valueOf(newState), contextId));
        } catch (JMSException | JsonProcessingException e) {
            log.error("Error while 'TcContext Operations' message processing: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @JmsListener(destination = "${message-broker.executor-disabling-stepbystep.topic}",
            containerFactory = "defaultJmsListenerTopicContainerFactory")
    public void onTcContextDisablingStepByStep(ActiveMQTextMessage activeMQTextMessage) {
        try {
            String contextId = activeMQTextMessage.getText();
            if (contextId == null) {
                throw new IllegalArgumentException("Parameter 'contextId' is null or missed");
            }
            MdcUtils.put(MdcField.CONTEXT_ID.toString(), contextId);
            tcContextService.disableStepByStepOnCurrentServiceInstance(new BigInteger(contextId));
        } catch (JMSException e) {
            log.error("Error while 'Disable step-by-step mode' message processing: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    private void progressingTcContext(TcContext tcContext, Status newStatus, BigInteger contextId) {
        /*  There may be attempts to perform incorrect state changes - via pressing Pause/Resume buttons
            in the in actual context popups.
            Currently we check the correctness of the transfer from the currentState to newState here.
            May be we must check it in the methods invoked.
        */
        switch (newStatus) {
            case PAUSED:
                if (tcContext.getInitiator() == null || !(tcContext.getInitiator() instanceof CallChainInstance)) {
                    throw new IllegalArgumentException(
                            String.format("Context ID=%s: initiator is NOT a callchain;  Pausing is cancelled",
                                    contextId));
                } else if (Status.IN_PROGRESS.equals(tcContext.getStatus()) || tcContext.isRunStepByStep()) {
                    try {
                        tcContextService.pauseOnCurrentServiceInstance(tcContext);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                String.format("Context ID=%s: Pausing is failed", contextId), e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("Context ID=%s is in the %s state; Pausing is cancelled", contextId,
                                    tcContext.getStatus()));
                }
                break;
            case STOPPED:
                if (Status.IN_PROGRESS.equals(tcContext.getStatus()) || Status.PAUSED.equals(tcContext.getStatus())
                        || Status.NOT_STARTED.equals(tcContext.getStatus())) {
                    try {
                        tcContextService.stopOnCurrentServiceInstance(tcContext);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                String.format("Context ID=%s: Stopping is failed", contextId), e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("Context ID=%s is in the %s state; Stopping is cancelled", contextId,
                                    tcContext.getStatus()));
                }
                break;
            case IN_PROGRESS:
                if (tcContext.getInitiator() == null || !(tcContext.getInitiator() instanceof CallChainInstance)) {
                    throw new IllegalArgumentException(
                            String.format("Context ID=%s: initiator is NOT a callchain;  Resuming is cancelled",
                                    contextId));
                } else if (Status.PAUSED.equals(tcContext.getStatus())) {
                    try {
                        tcContextService.resumeOnCurrentServiceInstance(tcContext);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                String.format("Context ID=%s: Resuming is failed", contextId), e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("Context ID=%s is in the %s state; Resuming is cancelled", contextId,
                                    tcContext.getStatus()));
                }
                break;
            default:
                log.error("Context ID {}: Unexpected state [{}] for context", contextId, newStatus);
        }
    }
}
