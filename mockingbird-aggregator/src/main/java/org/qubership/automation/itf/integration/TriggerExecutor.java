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

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.execution.ExecutorServiceProviderFactory;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvironmentManager;
import org.qubership.automation.itf.core.instance.chain.IncomingHelper;
import org.qubership.automation.itf.core.message.parser.Parser;
import org.qubership.automation.itf.core.model.communication.message.CommonTriggerExecutionMessage;
import org.qubership.automation.itf.core.model.communication.message.DiameterTriggerExecutionMessage;
import org.qubership.automation.itf.core.model.communication.message.TriggerExecutionMessage;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.interceptor.TransportInterceptor;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.TriggerConfiguration;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.IDiameterEventProducer;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.converter.PropertiesConverter;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.descriptor.StorableDescriptor;
import org.qubership.automation.itf.core.util.exception.DetectEnvironmentException;
import org.qubership.automation.itf.core.util.exception.IncomingValidationException;
import org.qubership.automation.itf.core.util.exception.SituationDefinitionException;
import org.qubership.automation.itf.core.util.helper.Comparators;
import org.qubership.automation.itf.core.util.loader.InterceptorClassLoader;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.MonitorManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.config.jms.ExecutorIntegrationConfig;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TriggerExecutor implements IDiameterEventProducer {

    private static final String TECH_SITUATION_STEP_FORMAT = "Step was passed with Technical Situation because the "
            + "suitable situation for incoming message was NOT found on system: '%s', operation: '%s'";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private IncomingHelper incomingHelper;

    @Autowired
    public void setExecutorStubsMessageSender(ExecutorToMessageBrokerSender executorToMessageBrokerSender,
                                              IncomingHelper incomingHelper) {
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
        this.incomingHelper = incomingHelper;
    }

    /**
     * This method using for processing messages from inbound triggers.
     *
     * @param triggerExecutionMessage itf serialized object (message) that contains :
     *                                String typeName, {@link Message}, {@link StorableDescriptor},
     *                                triggerConfigurationDescriptor, String sessionId
     */
    public void produceEvent(CommonTriggerExecutionMessage triggerExecutionMessage, OffsetDateTime started) {
        if (StringUtils.isEmpty(triggerExecutionMessage.getBrokerMessageSelectorValue())) {
            log.error("SessionId: {}, Message is rejected, due to Broker Message Selector Value is EMPTY!",
                    triggerExecutionMessage.getSessionId());
            return;
        }
        //we should use threading here! ==> But threading is implemented via concurrency at JmsListener
        String oldThreadName = Thread.currentThread().getName();
        try {
            final String threadName = oldThreadName + '/' + triggerExecutionMessage.getSessionId();
            Thread.currentThread().setName(threadName);
            TxExecutor.executeUnchecked((Callable<Void>) () -> {
                ObjectManager<TriggerConfiguration> manager = CoreObjectManager.managerFor(TriggerConfiguration.class);
                TriggerConfiguration triggerConfiguration = manager.getById(
                        triggerExecutionMessage.getTriggerConfigurationDescriptor().getId());
                TransportConfiguration transport = triggerConfiguration.getParent().getReferencedConfiguration();
                produceEvent(triggerConfiguration, transport,
                        triggerExecutionMessage.getTriggerConfigurationDescriptor(),
                        triggerExecutionMessage.getMessage(), triggerExecutionMessage.getSessionId(), threadName,
                        triggerExecutionMessage.getBrokerMessageSelectorValue(), started);
                /*
                    In case normal completion, response is sent into stubs via topic just after prepared,
                    from IntegrationStepHelper#sendSyncResponse method.
                */
                return null;
            }, ExecutorIntegrationConfig.STUB_TRANSACTION_DEFINITION);
        } catch (Throwable t) {
            log.error("SessionId {}: Error while processing incoming message",
                    triggerExecutionMessage.getSessionId(), t);
            String projectUuid = triggerExecutionMessage.getTriggerConfigurationDescriptor()
                    .getProjectUuid().toString();
            executorToMessageBrokerSender.sendMessageToExecutorStubsOutgoingResponseQueue(
                    new TriggerExecutionMessage(
                            new Message(String.format("SessionId %s: Error while processing incoming message: %s",
                                    triggerExecutionMessage.getSessionId(), t)),
                            triggerExecutionMessage.getSessionId(),
                            triggerExecutionMessage.getBrokerMessageSelectorValue()), projectUuid);
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    private void produceEvent(TriggerConfiguration triggerConfiguration, TransportConfiguration transport,
                              StorableDescriptor triggerDescriptor, Message message, String sessionId,
                              String threadName, String brokerMessageSelectorValue, OffsetDateTime started) {
        UUID projectUuid = triggerDescriptor.getProjectUuid();
        BigInteger projectId = triggerDescriptor.getProjectId();
        final InstanceContext instanceContext = initInstanceContext(transport, message, triggerDescriptor.getName(),
                projectId, projectUuid);
        try {
            boolean isConfiguredSituation = doCallChain(instanceContext, message, triggerConfiguration, transport,
                    sessionId, brokerMessageSelectorValue, projectId, projectUuid, started);
            if (!isConfiguredSituation) {
                throw new SituationDefinitionException("No suitable configured situation is found! Please check"
                        + " configuration and/or incoming message.");
            }
        } catch (final Exception ex) {
            String triggerTypeName = triggerConfiguration.getTypeName();
            String errorMessage = createErrorMessage(ex, triggerDescriptor, message, sessionId, triggerTypeName);
            sendFailedMessageToBroker(sessionId, brokerMessageSelectorValue, errorMessage, projectUuid);
            Exception exception = prepareException(errorMessage, ex);
            doCallChainCrash(exception, instanceContext, calculateContextName(message, triggerTypeName),
                    !(ex instanceof IncomingValidationException || ex instanceof SituationDefinitionException),
                    projectId, projectUuid);
        } finally {
            Thread.currentThread().setName(threadName);
        }
    }

    private void sendFailedMessageToBroker(String sessionId, String brokerMessageSelectorValue, String description,
                                           UUID projectUuid) {
        Message response = new Message();
        response.setFailedMessage(description);
        executorToMessageBrokerSender.sendMessageToExecutorStubsOutgoingResponseQueue(
                new TriggerExecutionMessage(response, sessionId, brokerMessageSelectorValue),
                projectUuid.toString());
    }

    /**
     * The only usage is in the Diameter Outbound transport: Class: ExternalInterceptor, method: produceMessageToItf
     * Please do NOT combine this method with other 'produceEvent' methods!
     * This method call special "doCallChain" method for diameter transport.
     *
     * @param executionMessage special itf diameter object (message) that contains:
     *                         {@link Message} , Object transportId, Object tcContextId, String sessionId.
     */
    public void produceEventDiameter(DiameterTriggerExecutionMessage executionMessage) {
        ExecutorServiceProviderFactory.get().requestForInboundProcessing().execute(() -> {
            final String threadName = Thread.currentThread().getName();
            Object tcContextId = executionMessage.getTcContextId();
            Object transportId = executionMessage.getTransportId();
            Message message = executionMessage.getMessage();
            String sessionId = executionMessage.getSessionId();
            /*
             *   Generally speaking, when Diameter interceptor receives a message,
             *   tc-context could be already evicted from cache, due to finish or terminate_by_timeout.
             *   We should check it. Currently, we simply log it and return.
             *   Further investigations of the root cause will follow.
             * */
            TcContext tcContext = CacheServices.getTcContextCacheService().getById(tcContextId);
            if (tcContext == null) {
                log.error("TcContext isn't found by id {}. Diameter message processing is impossible "
                        + "(TransportId: {})  SessionId: {}", tcContextId, transportId, sessionId);
                return;
            }
            BigInteger projectId = tcContext.getProjectId();
            UUID projectUuid = tcContext.getProjectUuid();
            TxExecutor.executeUnchecked(() -> {
                /*
                 *   Generally speaking, when Diameter interceptor receives a message,
                 *   some configuration changes could be already made, so, for example, Transport could be deleted.
                 *   We should check it. Currently, we simply log it and return.
                 * */
                TransportConfiguration transport = CoreObjectManager.managerFor(TransportConfiguration.class)
                        .getById(transportId);
                if (transport == null) {
                    throw new RuntimeException("Transport isn't found by id " + transportId + "Diameter message "
                            + "processing is impossible. tcContextId: " + tcContextId + " sessionId: " + sessionId);
                }
                InstanceContext instanceContext = initInstanceContext(transport, message, transport.getName(),
                        projectId, projectUuid);
                try {
                    boolean isConfiguredSituation = doCallChain(tcContext, instanceContext, message,
                            sessionId, transport);
                    if (!isConfiguredSituation) {
                        throw new SituationDefinitionException("No suitable configured situation is found! "
                                + "Please check configuration and/or incoming message.");
                    }
                    CacheServices.getPendingDataContextsCacheService().addContext(tcContext, sessionId);
                } catch (final IncomingValidationException | SituationDefinitionException e) {
                    doCallChainCrash(e, instanceContext, "", false, projectId, projectUuid);
                } catch (final Exception e) {
                    doCallChainCrash(e, instanceContext, "", true, projectId, projectUuid);
                } finally {
                    Thread.currentThread().setName(threadName);
                }
                return null; //for correct finishing transaction
            }, TxExecutor.readOnlyTransaction());
        });
    }

    /*
    This method is used to send a response from "Inbound Diameter transport" (WA).
    Inbound Diameter Transport wasn't implemented yet in ITF4.
    But we come to this method when Diameter Interceptors (RAR, SNR etc.) receive messages.
    Please note: This method is a specific implementation for this transport only.
    DO NOT change the implementation of this method if you fix or refactor another implementation of "doCallChain"
    method
    for other transports. You can change this method if your changes apply only to Diameter Transport.
    */
    private boolean doCallChain(TcContext tcContext, InstanceContext instanceContext, Message message, String sessionId,
                                TransportConfiguration transport) throws Exception {
        instanceContext.setTransport(transport);
        System system = transport.getParent();
        MdcUtils.put(MdcField.PROJECT_ID.toString(), tcContext.getProjectUuid().toString());
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), tcContext.getID().toString());
        log.info("Parsing message, system is {}...", system.getName());
        Operation operation = incomingHelper.processIncomingMessage(message, instanceContext, transport,
                tcContext.getProjectId(), tcContext.getProjectUuid());
        transport = operation.getTransport();
        instanceContext.setTransport(transport); // Contrary to our common behavior, transport from operation is used
        Thread.currentThread().setName(Thread.currentThread().getName() + "/" + tcContext.getID());
        Object locObject = MonitorManager.getInstance().get("$tcid=" + tcContext.getID());
        synchronized (locObject) {
            return prepareAndExecuteSituation(instanceContext, tcContext, instanceContext.getSP(), message, operation,
                    system, sessionId, null, transport.getName());
        }
    }

    private boolean doCallChain(InstanceContext instanceContext,
                                Message message,
                                TriggerConfiguration triggerConfiguration,
                                TransportConfiguration transport,
                                String sessionId,
                                String brokerMessageSelectorValue,
                                BigInteger projectId,
                                UUID projectUuid,
                                OffsetDateTime started) throws Exception {
        if (transport == null) {
            throw new IllegalStateException(
                    String.format("No matching transport found for %s", triggerConfiguration.getName()));
        }
        instanceContext.setTransport(transport);
        Server server = triggerConfiguration.getParent().getParent();
        System system = transport.getParent();
        instanceContext.setConnectionProperties(PropertiesConverter.convert(
                triggerConfiguration.getParent().getTypeName(),
                triggerConfiguration.getParent().getReferencedConfiguration(),
                triggerConfiguration.getParent(),
                triggerConfiguration));
        log.info("Parsing message, system is {}...", system.getName());
        Operation operation = incomingHelper.processIncomingMessage(message, instanceContext, transport, projectId,
                projectUuid);
        log.info("Detecting context, operation is {}...", operation.getName());
        String contextKey = incomingHelper.getContextKey(instanceContext, system, operation, false);
        TcContext tcContext = incomingHelper.findOrCreateTcContextByKeys(contextKey, transport.get("isStub"),
                projectId, projectUuid);
        fillTcContextParams(instanceContext, message, triggerConfiguration, started, tcContext);
        MdcUtils.put(MdcField.CONTEXT_ID.toString(), tcContext.getID().toString());
        Thread.currentThread().setName(Thread.currentThread().getName() + "/" + tcContext.getID());
        Object locObject = MonitorManager.getInstance().get("$tcid=" + tcContext.getID());
        synchronized (locObject) {
            prepareEnv(tcContext, system, server);
            return prepareAndExecuteSituation(instanceContext, tcContext, instanceContext.getSP(), message, operation,
                    system, sessionId, brokerMessageSelectorValue, server.getName());
        }
    }

    private String computeEndpointTagValue(Object uriParams, String typeName, Object triggerId) {
        return uriParams == null ? typeName + " / trigger " + triggerId : uriParams.toString();
    }

    private Exception prepareException(String errorDescription, Exception e) {
        log.error(errorDescription);
        return processException(e, errorDescription);
    }

    private Exception processException(Exception exception, String description) {
        Class<? extends Exception> exceptionClass = exception.getClass();
        return exceptionClass.isAssignableFrom(IncomingValidationException.class)
                ? exception
                : new Exception(description);
    }

    private String calculateContextName(Message message, String typename) {
        // Begin - transport specific part.
        // TODO: May be this code should be moved to transport modules
        if (typename.endsWith(".RESTInboundTransport") || typename.endsWith(".SOAPOverHTTPInboundTransport")) {
            // Using headers, let's compose the context name as "From: <remote-address> To: <configured-endpoint>"
            Object obj = message.getHeaders().get("x-forwarded-for");
            String from = null;
            String to = null;
            if (obj != null) {
                from = obj.toString();
            }
            if (StringUtils.isBlank(from)) {
                obj = message.getHeaders().get("remoteAddr");
                if (obj != null) {
                    from = obj.toString();
                }
            }
            obj = message.getHeaders().get("CamelHttpUri");
            if (obj != null) {
                to = obj.toString();
            }
            return "From: " + from + " To: " + to;
        }
        // End - transport specific part.
        return null;
    }

    private String createErrorMessage(Exception ex, StorableDescriptor triggerConfigurationDescriptor, Message message,
                                      String sessionId, String typename) {
        boolean isRest = typename.endsWith(".RESTInboundTransport");
        boolean isSoap = !isRest && typename.endsWith(".SOAPOverHTTPInboundTransport");
        StringBuilder builder = new StringBuilder((isSoap)
                ? "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<soap:Body>\n<soap:Fault>\n<faultcode>soap:Server</faultcode>\n<faultstring>"
                : "");
        builder.append("Exception while processing incoming message received by the trigger: ");
        builder.append(triggerConfigurationDescriptor.getName()).append("\nSession id: ").append(sessionId);
        // Begin - transport specific part.
        // TODO: May be this code should be moved to transport modules
        if (isRest || isSoap) {
            builder.append("\nSome important message headers: ");
            appendHeader(builder, message, "CamelHttpUrl");
            appendHeader(builder, message, "CamelHttpQuery");
            appendHeader(builder, message, "remoteHost");
            appendHeader(builder, message, "remotePort");
            appendHeader(builder, message, "remoteAddr");
            appendHeader(builder, message, "x-forwarded-for");
            appendHeader(builder, message, "user-agent");
        }
        // End - transport specific part.
        builder.append("\nException: ").append(ex.getMessage());
        if (ex.getCause() != null) {
            builder.append("\nCaused by: ").append(isSoap
                    ? StringEscapeUtils.escapeXml10(ex.getCause().toString()) : ex.getCause());
        }
        if (isSoap) {
            builder.append("\n</faultstring>\n</soap:Fault>\n</soap:Body>\n</soap:Envelope>");
        }
        return builder.toString();
    }

    private void appendHeader(StringBuilder builder, Message message, String headerName) {
        Object headerValue = message.getHeaders().get(headerName);
        if (headerValue != null) {
            builder.append("\n").append(headerName).append(": ").append(headerValue);
        }
    }

    private InstanceContext initInstanceContext(TransportConfiguration transport,
                                                Message message,
                                                String triggerName,
                                                BigInteger projectId,
                                                UUID projectUuid) {
        log.info("Event received for trigger {}", triggerName);
        final InstanceContext context = new InstanceContext();
        final SpContext spContext = new SpContext();
        context.setSP(spContext);
        try {
            doIntercept(transport, message);
            context.setProjectId(projectId);
            context.setProjectUuid(projectUuid);
            prepareMessage(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        spContext.setIncomingMessage(message);
        spContext.put("incomingHeaders", message.getHeaders()); // NITP-4708
        spContext.put("incomingConnectionProperties", message.getConnectionProperties());
        return context;
    }

    private Map<String, Object> getEnvironmentInfo(System system, Server server) throws Exception {
        List<Object[]> environmentInfo = CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, EnvironmentManager.class)
                .getByServerAndSystemIdPair((BigInteger) system.getID(), (BigInteger) server.getID());
        if (environmentInfo.isEmpty()) {
            throw new DetectEnvironmentException(
                    String.format("No environment with inbound system %s and server %s found", system, server));
        } else if (environmentInfo.size() > 1) {
            log.warn("More than one environment with inbound system {} and server {} found", system, server);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", environmentInfo.get(0)[0]);
        map.put("name", environmentInfo.get(0)[1]);
        return map;
    }

    private void prepareEnv(TcContext tcContext, System system, Server server) throws Exception {
        if (tcContext.getEnvironmentId() == null) {
            Map<String, Object> environmentInfo = getEnvironmentInfo(system, server);
            tcContext.setEnvironmentId((BigInteger) environmentInfo.get("id"));
            tcContext.setEnvironmentName(String.valueOf(environmentInfo.get("name")));
        }
    }

    private void doCallChainCrash(Exception e, InstanceContext instanceContext, String contextName,
                                  boolean executeUnexpectedSituation, BigInteger projectId, UUID projectUuid) {
        log.error("Error while processing incoming message", e);
        if (instanceContext.tc() == null) {
            instanceContext.setTC(CacheServices.getTcBindingCacheService().findByKey(
                    String.format("Unexpected instanceContext at %s", LocalDateTime.now().format(dateTimeFormatter)),
                    projectId, projectUuid));
        }
        TcContext tcContext = instanceContext.tc();
        if (!StringUtils.isBlank(contextName)) {
            tcContext.setName(contextName);
        }
        tcContext.setPartNum(TCContextService.getCurrentPartitionNumberByProject(tcContext.getProjectUuid()));
        if (executeUnexpectedSituation) {
            /*  In case EngineIntegrationException, tcContext has already failed (SituationExecutorService has failed
            situationInstance and the instanceContext)
                So, we don't need to fail the instanceContext again and to execute UnexpectedSituation
             */
            if (!(tcContext.getStatus().equals(Status.FAILED))) {
                incomingHelper.executeUnexpectedSituation(instanceContext, e);
            }
        } else {
            ExecutionServices.getTCContextService().fail(tcContext);
        }
    }

    private void prepareMessage(Message message)
            throws IOException {
        if (message.getFile() != null) {
            message.setText(FileUtils.readFileToString(message.getFile()));
        }
    }

    /*
     *   Method:
     *       - determines a situation,
     *       - applies parsing rules from the situation (if any),
     *       - prepares a situationInstance for the situation,
     *       - executes the situationInstance.
     *   There is one important use case:
     *       - due to some reasons (misconfiguration and/or invalid incoming message) no situation is determined.
     *   In that case, the method:
     *       - creates a virtual situation, so called 'Default Inbound Situation' (the name starts with '[ ***
     * DEFAULT (autocreated) *** ]'),
     *       - logs and reports the fact that no suitable configured situation is found,
     *       - prepares a situationInstance for the situation,
     *       - executes the situationInstance.
     *
     *   Return value:
     *       - true/false; true means suitable configured situation is found.
     * */
    private boolean prepareAndExecuteSituation(InstanceContext context, TcContext tcContext, SpContext spContext,
                                               Message message, Operation operation, System system, String sessionId,
                                               String brokerMessageSelectorValue, String serverName) throws Exception {
        ExecutionServices.getTCContextService().setMessageParameters(tcContext, spContext.getMessageParameters());
        context.setTC(tcContext);
        context.setSessionId(sessionId);
        if (brokerMessageSelectorValue != null) {
            context.setMessageBrokerSelectorValue(brokerMessageSelectorValue);
        }
        log.info("Detecting situation, operation is {}...", operation.getName());
        incomingHelper.tryToGetRequestMethod(message, context);
        addParamsToSpContext(context, tcContext, message, serverName);
        Situation situation = incomingHelper.detectSituationFromOperation(operation, context);
        boolean isDefaultInboundSituation = (situation == operation.getDefaultInboundSituation());
        context.nilsp(); // This cleanup was added by Roman Aksenenko 28/03/2017 (Rev.#3943), without any comments
        if (!situation.getParsingRules().isEmpty()) {
            Parser parser = new Parser();
            log.debug("situationMessageParameters parsing - is started");
            Map<String, MessageParameter> situationMessageParameters = parser.parse(tcContext.getProjectId(), message,
                    context, situation.getParsingRules());
            log.debug("situationMessageParameters parsing - is finished");
            spContext.putMessageParameters(situationMessageParameters.values());
            ExecutionServices.getTCContextService().setMessageParameters(context.tc(), situationMessageParameters);
        }
        SituationInstance instance = ExecutionServices.getSituationExecutorService().prepare(situation, context);
        if (isDefaultInboundSituation) {
            instance.setSituationId((BigInteger) situation.getID());
            instance.setOperationName(operation.getName());
            instance.setOperationId((BigInteger) operation.getID());
            instance.setSystemName(system.getName());
            instance.setSystemId((BigInteger) system.getID());

            /*  Situation instance status is PASSED !!!
                This is (historically from the very beginning, for DT project) because it is reported to ATP as
                Warning (see below).
                Confirmed as correct behavior by Nikolay Durasov at 12/05/2021
             */
            String msg = String.format(TECH_SITUATION_STEP_FORMAT, system.getName(), operation.getName());
            instance.setErrorName("No suitable configured situation is found");
            instance.setErrorMessage(
                    "No suitable configured situation is found! Please check configuration and/or incoming message."
                            + "\nOperation: " + operation.getName() + "\nSystem: " + system.getName() + " ["
                            + system.getID() + "]");
            log.warn(msg);
            Report.warn(instance, "Misconfigured situation is found", msg); // Report Warning into ATP (NITP-4077)
            ExecutionServices.getSituationExecutorService().executeInstance(
                    instance, null, spContext, null, situation, operation);
        } else {
            if (message.getFailedMessage() != null) {
                instance.setErrorMessage(message.getFailedMessage());
            }
            ExecutionServices.getSituationExecutorService().executeInstance(
                    instance, null, spContext, null, situation);
        }
        return !isDefaultInboundSituation;
    }

    private void addParamsToSpContext(InstanceContext context, TcContext tcContext, Message message, String server) {
        context.put("sp.providerUrl", message.getConnectionProperties().get("providerUrl")); // NITP-4247
        context.put("sp.environment", tcContext.getEnvironmentName());
        context.put("sp.server", (server != null) ? server : "");  //NITP 5010
    }

    private void doIntercept(TransportConfiguration transport, Message message) throws Exception {
        if (transport.getInterceptors() == null || transport.getInterceptors().isEmpty()) {
            return;
        }
        List<Interceptor> interceptors = new ArrayList<>(transport.getInterceptors());
        interceptors.sort(Comparators.INTERCEPTOR_COMPARATOR);
        for (Interceptor interceptor : interceptors) {
            applyIfActive(interceptor, message);
        }
    }

    private void applyIfActive(Interceptor interceptor, Message message) throws Exception {
        if (interceptor.isActive()) {
            TransportInterceptor transportInterceptor = InterceptorClassLoader.getInstance()
                    .getInstanceClass(interceptor.getTypeName(), interceptor);
            transportInterceptor.apply(message);
            log.info("{} interceptor is applied", interceptor.getName());
        }
    }

    private void fillTcContextParams(InstanceContext context, Message message,
                                     TriggerConfiguration triggerConfiguration,
                                     OffsetDateTime started, TcContext tcContext) {
        tcContext.setPartNum(TCContextService.getCurrentPartitionNumberByProject(context.getProjectUuid()));
        if (tcContext.getInitiator() == null) {
            // Just created context, set validation on step enabled... (NITP-6043, TASUP-11537)
            tcContext.setStartValidation(true);
            tcContext.setStartedFrom(StartedFrom.ITF_STUB);
        }
        if (tcContext.getTimeToLive() < 1) {
            tcContext.setTimeToLive();
        }
        tcContext.setAndCalculateNeedToReportToItf();
        tcContext.put("executor_start_time", started.toString()); // Default string representation
        tcContext.put("executor_endpoint", computeEndpointTagValue(message.getConnectionProperties().get("uriParams"),
                triggerConfiguration.getParent().getTypeName(), triggerConfiguration.getID()));
    }
}
