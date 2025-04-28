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

package org.qubership.automation.itf.core.instance.step.impl;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.MESSAGE_PRETTY_FORMAT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.MESSAGE_PRETTY_FORMAT_DEFAULT_VALUE;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.annotation.AtpSpanTag;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.instance.chain.IncomingHelper;
import org.qubership.automation.itf.core.instance.step.impl.chain.TemplateProcessor;
import org.qubership.automation.itf.core.message.parser.ProducerMessageHelper;
import org.qubership.automation.itf.core.metric.MetricsAggregateService;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.message.TriggerExecutionMessage;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.interceptor.TransportInterceptor;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.core.util.exception.NoDeployedTransportException;
import org.qubership.automation.itf.core.util.format.Formatter;
import org.qubership.automation.itf.core.util.format.Formatters;
import org.qubership.automation.itf.core.util.helper.Comparators;
import org.qubership.automation.itf.core.util.helper.KeyHelper;
import org.qubership.automation.itf.core.util.holder.ActiveInterceptorHolder;
import org.qubership.automation.itf.core.util.loader.InterceptorClassLoader;
import org.qubership.automation.itf.core.util.logger.TimeLogger;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.core.util.transport.access.AccessTransport;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationStepHelper {

    private static final String CONTEXT_ID = "ContextId";
    private static final String TRANSPORT_ID = "transportId";
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final IncomingHelper incomingHelper;
    private final MetricsAggregateService metricsAggregateService;
    private final ProjectSettingsService projectSettingsService;

    public static boolean notLastValidationAttempt(StepInstance currentStepInstance, StepInstance nextStepInstance) {
        if (!currentStepInstance.isRetryStep()) {
            return false;
        } else {
            return currentStepInstance.getStep().equals(nextStepInstance.getStep());
        }
    }

    void sendRequest(StepInstance stepInstance) throws Exception {
        TimeLogger.LOGGER.debug("Start for method: IntegrationStepHelper.sendRequest");
        IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
        Operation integrationStepOperation = getAndCheckOperation(stepInstance, integrationStep);
        TransportConfiguration transportConfiguration = getAndCheckConfigurationFromDB(stepInstance,
                integrationStepOperation.getTransport());
        AccessTransport transport = getAndCheckRemoteTransport(stepInstance, transportConfiguration.getTypeName());
        Environment environment = getAndCheckEnvironment(stepInstance.getContext().tc());
        Message message = ProducerMessageHelper.getInstance().produceMessage(
                TemplateHelper.getById(integrationStep.returnStepTemplate().getID()),
                stepInstance.getContext(), integrationStepOperation, environment);
        computeConnectionProperties(message, environment, integrationStep, transportConfiguration, transport,
                stepInstance.getContext());
        processInterceptors(integrationStep, transportConfiguration, environment, message);
        stepInstance.setOutgoingMessage(message);
        Object sessionId = transport.send(stepInstance.getOutgoingMessage(), null,
                stepInstance.getParent().getContext().getProjectUuid());
        stepInstance.getContext().setSessionId(sessionId);
        stepInstance.getParent().getContext().setSessionId(sessionId);
        TimeLogger.LOGGER.debug("End for method: IntegrationStepHelper.sendRequest");
    }

    void sendReceiveSync(StepInstance stepInstance) throws Exception {
        TimeLogger.LOGGER.debug("Start for method: IntegrationStepHelper.sendReceiveSync");
        IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
        Operation integrationStepOperation = getAndCheckOperation(stepInstance, integrationStep);
        Situation situation = integrationStep.getParent();
        TransportConfiguration transportConfiguration = getAndCheckConfiguration(stepInstance,
                integrationStepOperation.getTransport());
        AccessTransport transport = getAndCheckRemoteTransport(stepInstance, transportConfiguration.getTypeName());
        Environment environment = getAndCheckEnvironment(stepInstance.getContext().tc());
        Message message = ProducerMessageHelper.getInstance().produceMessage(integrationStep.returnStepTemplate(),
                stepInstance.getContext(), integrationStepOperation, environment);
        computeConnectionProperties(message, environment, integrationStep, transportConfiguration, transport,
                stepInstance.getContext());
        stepInstance.setOutgoingMessage(message);
        BigInteger projectId = stepInstance.getContext().tc().getProjectId();

        Message response;
        boolean responseFromCache = false;
        String stringTtl = message.getConnectionProperties()
                .getOrDefault(PropertyConstants.Http.CACHE_RESPONSE_FOR_SECONDS, StringUtils.EMPTY).toString();
        if (StringUtils.isNumeric(stringTtl)) {
            String key = String.format("%s_%s%s", projectId,
                    message.getConnectionPropertiesParameter("baseUrl"),
                    message.getConnectionPropertiesParameter("endpoint"));
            response = CacheServices.getResponseCacheService().getByKey(key);
            if (Objects.isNull(response)) {
                response = transport.sendReceiveSync(message, projectId);
                CacheServices.getResponseCacheService().set(key, stringTtl, response);
            } else {
                responseFromCache = true;
            }
        } else {
            response = transport.sendReceiveSync(message, projectId);
        }

        if (!responseFromCache && Boolean.parseBoolean(projectSettingsService.get(projectId, MESSAGE_PRETTY_FORMAT,
                MESSAGE_PRETTY_FORMAT_DEFAULT_VALUE))) {
            formatBody(response, transportConfiguration.getTypeName());
        }
        stepInstance.setIncomingMessage(response);
        stepInstance.getContext().sp().put("endpointForRam",
                message.getConnectionProperties() != null && message.getConnectionProperties()
                        .containsKey("Resolved_Endpoint_URL")
                        ? message.getConnectionProperties().get("Resolved_Endpoint_URL")
                        : transportConfiguration.get("endpoint"));
        stepInstance.getContext().sp().put("incomingHeaders", response.getHeaders());
        stepInstance.getContext().sp().put("incomingConnectionProperties", response.getConnectionProperties());
        Map<String, MessageParameter> map = incomingHelper.processOutboundResponseMessage(
                transportConfiguration.getParent(), integrationStepOperation, situation, response,
                stepInstance.getContext(), stepInstance.getContext().tc().getProjectId());
        stepInstance.getContext().sp().putMessageParameters(map.values());
        ExecutionServices.getTCContextService().setMessageParameters(stepInstance.getContext().tc(), map);
        if (response.getFailedMessage() != null) {
            throw new RuntimeException(response.getFailedMessage());
        }
        TimeLogger.LOGGER.debug("End for method: IntegrationStepHelper.sendReceiveSync");
    }

    @AtpJaegerLog(spanTags = @AtpSpanTag(key = "send.response.step.instance.id", value = "#stepInstance.stepId" +
            ".toString()"))
    void sendSyncResponse(StepInstance stepInstance) {
        MdcUtils.put(MdcField.TRACE_ID.toString(), MDC.get(MdcField.STUB_TRACE_ID.toString()));
        TimeLogger.LOGGER.debug("Start for method: IntegrationStepHelper.sendSyncResponse");
        IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
        Operation operation = getAndCheckOperation(stepInstance, integrationStep);
        Message message = ProducerMessageHelper.getInstance()
                .produceMessage(integrationStep.returnStepTemplate(), stepInstance.getContext(), operation);
        message.setFailedMessage(stepInstance.getErrorMessage());
        if (stepInstance.getContext().getConnectionProperties() != null) {
            message.fillConnectionProperties(stepInstance.getContext().getConnectionProperties());
        }
        message.fillHeaders(message.getConnectionProperties(), "headers");
        stepInstance.setOutgoingMessage(message);
        String projectUuid = stepInstance.getContext().getProjectUuid().toString();
        executorToMessageBrokerSender.sendMessageToExecutorStubsOutgoingResponseQueue(
                new TriggerExecutionMessage(message,
                        (String) (stepInstance.getParent().getContext().getSessionId()),
                        (String) (stepInstance.getParent().getContext().getMessageBrokerSelectorValue())), projectUuid
        );
        collectDurationMetric(stepInstance.getContext().getTC(), stepInstance.getContext().getProjectUuid());
        TimeLogger.LOGGER.debug("End for method: IntegrationStepHelper.sendSyncResponse");
    }

    private void collectDurationMetric(TcContext tcContext, UUID projectUuid) {
        Object startTimeObject = tcContext.get("executor_start_time");
        if (startTimeObject instanceof String) {
            try {
                // Parse from default String representation of OffsetDateTime objects
                OffsetDateTime started = OffsetDateTime.parse((String) startTimeObject);
                OffsetDateTime finished = OffsetDateTime.now();
                tcContext.put("executor_finish_time", finished.toString()); // Default string representation
                Duration duration = Duration.between(started, finished);
                tcContext.put("executor_duration", String.valueOf(duration.toMillis()));
                Object endpointObject = tcContext.get("executor_endpoint");
                String endpoint = endpointObject == null ? "n/a" : endpointObject.toString();
                metricsAggregateService.recordIncomingRequestDuration(projectUuid, endpoint, duration);
            } catch (DateTimeParseException dtpe) {
                log.error("Error while collecting duration metrics", dtpe);
            }
        }
    }

    private void computeConnectionProperties(Message message,
                                             Environment environment,
                                             IntegrationStep integrationStep,
                                             TransportConfiguration transportConfiguration,
                                             AccessTransport transport,
                                             InstanceContext instanceContext) throws Exception {
        Server server = getAndCheckServer(environment, integrationStep.getReceiver());
        ConnectionProperties connectionProperties = server.calculate(integrationStep.getReceiver(),
                transportConfiguration, message, integrationStep.returnStepTemplate(), instanceContext);
        connectionProperties.put(CONTEXT_ID, instanceContext.getTC().getID().toString());
        connectionProperties.put(TRANSPORT_ID, transportConfiguration.getID().toString());
        message.fillConnectionProperties(connectionProperties);
        prepareLoadTemplateProperties(transport, message, instanceContext);
        processDynamicHeaders(transport, instanceContext, message);
        processDynamicProperties(transport, instanceContext, message);
        message.fillHeaders(message.getConnectionProperties(), "headers");
    }

    private void processDynamicHeaders(AccessTransport transport,
                                       InstanceContext context,
                                       Message message) throws RemoteException {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        transport.getProperties().forEach(property -> {
            String shortName = property.getShortName();
            Object obtain = connectionProperties.obtain(shortName);
            if (obtain instanceof String) {
                String propertyValue = (String) obtain;
                propertyValue = TemplateProcessor.getInstance()
                        .process(null, propertyValue, context, connectionProperties);
                connectionProperties.put(shortName, propertyValue);
            }
        });
        message.getConnectionProperties().putAll(connectionProperties);
    }

    private void processDynamicProperties(AccessTransport transport,
                                          InstanceContext context,
                                          Message message) throws RemoteException {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        transport.getProperties().forEach(property -> {
            String shortName = property.getShortName();
            Object obtain = connectionProperties.obtain(shortName);
            if (property.isDynamic()) {
                String propertyValue;
                if (obtain instanceof String) {
                    propertyValue = (String) obtain;
                    propertyValue = TemplateEngineFactory.get().process((Storable) null, propertyValue, context,
                            "Connection property '" + shortName + "'");
                    connectionProperties.put(shortName, propertyValue);
                } else if (obtain instanceof HashMap) {
                    HashMap<String, Object> properties = (HashMap) obtain;
                    HashMap<String, Object> updatedProperties = (HashMap) obtain;
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        if (entry.getValue() instanceof List) {
                            List<String> oldlist = (List<String>) entry.getValue();
                            List<String> newlist = new ArrayList<>();
                            for (String elem : oldlist) {
                                newlist.add(TemplateEngineFactory.get().process((Storable) null, elem, context,
                                        "Connection property '" + shortName + "'"));
                            }
                            updatedProperties.put(entry.getKey(), newlist);
                        } else {
                            propertyValue = TemplateEngineFactory.get()
                                    .process((Storable) null, (String) entry.getValue(), context,
                                            "Connection property '" + shortName + "'");
                            updatedProperties.put(entry.getKey(), propertyValue);
                        }
                    }
                    connectionProperties.put(shortName, updatedProperties);
                }
            }
        });
        message.getConnectionProperties().putAll(connectionProperties);
    }

    private void processInterceptors(IntegrationStep integrationStep,
                                     TransportConfiguration transportConfiguration,
                                     Environment environment,
                                     Message message) throws Exception {
        TimeLogger.LOGGER.debug("Start for method: IntegrationStepHelper.processInterceptors");
        Map<String, Interceptor> objectInterceptorMap = ActiveInterceptorHolder.getInstance().getActiveInterceptors()
                .get(integrationStep.returnStepTemplate().getID().toString());
        if (objectInterceptorMap == null || objectInterceptorMap.isEmpty()) {
            objectInterceptorMap = ActiveInterceptorHolder.getInstance().getActiveInterceptors()
                    .get(transportConfiguration.getID().toString());
        }
        if (objectInterceptorMap != null) {
            List<Interceptor> interceptors = new ArrayList<>(objectInterceptorMap.values());
            interceptors.sort(Comparators.INTERCEPTOR_COMPARATOR);
            for (Interceptor interceptor : interceptors) {
                if (interceptor.isActive() && interceptor.isApplicable(environment.getID().toString(),
                        integrationStep.getReceiver().getID().toString())) {
                    TransportInterceptor transportInterceptor = InterceptorClassLoader.getInstance()
                            .getInstanceClass(interceptor.getTypeName(), interceptor);
                    transportInterceptor.apply(message);
                    log.info("{} interceptor is applied", interceptor.getName());
                } else {
                    log.info("{} interceptor isn't applied, due to isn't applicable for current environment/receiver",
                            interceptor.getName());
                }
            }
        }
        TimeLogger.LOGGER.debug("End for method: IntegrationStepHelper.processInterceptors");
    }

    void processOutgoingContextKeys(StepInstance stepInstance) {
        TimeLogger.LOGGER.debug("Start for method: IntegrationStepHelper.processOutgoingContextKeys");
        IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
        String outgoingContextKeyDefinition = integrationStep.getOperation().getOutgoingContextKeyDefinition();
        Storable parent = integrationStep.getOperation();
        if (outgoingContextKeyDefinition == null) {
            outgoingContextKeyDefinition = integrationStep.getOperation().getParent().getOutgoingContextKeyDefinition();
            parent = integrationStep.getOperation().getParent();
        }
        if (outgoingContextKeyDefinition != null) {
            try {
                String contextKey = KeyHelper.defineKey(outgoingContextKeyDefinition, stepInstance.getContext(),
                        parent);
                if (!Strings.isNullOrEmpty(contextKey)) {
                    if (CacheServices.getTcBindingCacheService().bind(contextKey, stepInstance.getContext().tc())) {
                        log.debug("Context key for step instance {} is {}", stepInstance, contextKey);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing outgoing context definition", e);
            }
        }
        TimeLogger.LOGGER.debug("End  for method: IntegrationStepHelper.processOutgoingContextKeys");
    }

    private Operation getAndCheckOperation(StepInstance stepInstance, IntegrationStep integrationStep) {
        Operation integrationStepOperation = integrationStep.getOperation();
        if (integrationStepOperation == null) {
            throw new IllegalStateException(String.format("Operation is not set! Step: '%s', parent: '%s'",
                    stepInstance.getStep().getName(), stepInstance.getParent().getStepContainer().getName()));
        }
        return integrationStepOperation;
    }

    private TransportConfiguration getAndCheckConfigurationFromDB(StepInstance stepInstance,
                                                                  TransportConfiguration transportConfigurationLink) {
        if (transportConfigurationLink == null) {
            throw new IllegalStateException(String.format("Transport isn't set for operation! Step: '%s', parent: '%s'",
                    stepInstance.getStep().getName(), stepInstance.getParent().getStepContainer().getName()));
        }
        TransportConfiguration transportConfiguration = CoreObjectManager.managerFor(TransportConfiguration.class)
                .getById(transportConfigurationLink.getID());
        if (transportConfiguration == null) {
            throw new IllegalStateException(String.format("Transport isn't set for operation! Step: '%s', parent: '%s'",
                    stepInstance.getStep().getName(), stepInstance.getParent().getStepContainer().getName()));
        }
        return transportConfiguration;
    }

    private TransportConfiguration getAndCheckConfiguration(StepInstance stepInstance,
                                                            TransportConfiguration transportConfigurationLink) {
        if (transportConfigurationLink == null) {
            throw new IllegalStateException(String.format("Transport isn't set for operation! Step: '%s', parent: '%s'",
                    stepInstance.getStep().getName(), stepInstance.getParent().getStepContainer().getName()));
        }
        return transportConfigurationLink;
    }

    private AccessTransport getAndCheckRemoteTransport(StepInstance stepInstance, String transportTypeName)
            throws NoDeployedTransportException {
        AccessTransport transport = TransportRegistryManager.getInstance().find(transportTypeName);
        if (transport == null) {
            // Null transport is returned in case of NotBoundException while looking in TransportRegistry.
            // I think we should throw an exception here
            throw new IllegalStateException(String.format(
                    "Transport is not bound for operation! Step: '%s', parent: '%s', transport type: '%s'",
                    stepInstance.getStep().getName(), stepInstance.getParent().getStepContainer().getName(),
                    transportTypeName));
        }
        return transport;
    }

    private Environment getAndCheckEnvironment(TcContext tcContext) {
        if (tcContext.getEnvironmentId() == null) {
            throw new IllegalStateException(
                    String.format("Environment has not been set up for context '%s'", tcContext.getName()));
        }
        Environment environment = CoreObjectManager.managerFor(Environment.class).getById(tcContext.getEnvironmentId());
        if (environment == null) {
            throw new IllegalStateException(
                    String.format("Environment isn't found by id [%s] for context '%s'", tcContext.getEnvironmentId(),
                            tcContext.getName()));
        }
        return environment;
    }

    private Server getAndCheckServer(Environment environment, System receiverSystem) {
        Server server = environment.getOutbound().get(receiverSystem);
        if (server == null) {
            throw new IllegalStateException(
                    String.format("No associated server found for system '%s' in environment '%s'", receiverSystem,
                            environment));
        }
        return server;
    }

    private void prepareLoadTemplateProperties(AccessTransport transport, Message message, InstanceContext context) {
        try {
            for (PropertyDescriptor propertyDescriptor : transport.getProperties()) {
                FiledProcessorChain.getInstance().process(propertyDescriptor, message, context);
            }
        } catch (RemoteException e) {
            // Silently ignore
        }
    }

    public void checkErrors(StepInstance stepInstance) {
        if (stepInstance.getParent().getErrorMessage() != null) {
            Throwable error = stepInstance.getParent().getError();
            if (error != null) {
                boolean isValidException = error.getClass().isAssignableFrom(EngineIntegrationException.class);
                if (isValidException) {
                    if (((IntegrationStep) stepInstance.getStep()).getSender() == null) {
                        stepInstance.setError(new EngineIntegrationException(stepInstance.getParent().getError()));
                    }
                } else {
                    throw new IllegalArgumentException(stepInstance.getParent().getErrorMessage());
                }
            }
        }
    }

    private void formatBody(Message message, String transportType) {
        Formatter formatter = Formatters.getFormatterOrNull(transportType);
        if (formatter != null) {
            message.setText(formatter.format(message.getText()));
        }
    }
}
