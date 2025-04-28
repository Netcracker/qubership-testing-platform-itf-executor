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

package org.qubership.automation.itf.core.instance.chain;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.OperationEventTriggerObjectManager;
import org.qubership.automation.itf.core.message.parser.Parser;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.condition.ConditionsHelper;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.event.StepEvent;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.exception.KeyDefinitionException;
import org.qubership.automation.itf.core.util.exception.OperationDefinitionException;
import org.qubership.automation.itf.core.util.generator.id.UniqueIdGenerator;
import org.qubership.automation.itf.core.util.helper.KeyHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.cache.service.impl.BoundContextsCacheService;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncomingHelper {

    public static final Step UNEXPECTED_STEP = new IntegrationStep() {
        {
            setName("Undefined step");
        }

        @Override
        public System getReceiver() {
            return null;
        }

        @Override
        public System getSender() {
            return null;
        }

        @Override
        public Operation getOperation() {
            return null;
        }

        @Override
        public Template returnStepTemplate() {
            return null;
        }

        @Override
        public Mep getMep() {
            return Mep.INBOUND_REQUEST_ASYNCHRONOUS;
        }

        @Override
        public Situation getParent() {
            return null;
        }
    };
    public static final Situation UNEXPECTED_SITUATION = new Situation() {
        @Override
        public List<Step> getSteps() {
            return Collections.singletonList(UNEXPECTED_STEP);
        }

        @Override
        public String getName() {
            return "Unexpected situation";
        }
    };
    private final EventBusProvider eventBusProvider;

    /**
     * TODO Add JavaDoc.
     */
    public String getContextKey(InstanceContext context,
                                @Nonnull System system,
                                @Nullable Operation operation,
                                boolean throwKeyDefinitionException) throws KeyDefinitionException {
        log.debug("Define context key");
        Storable parent = null;
        String contextDefinition = null;
        if (operation != null) {
            contextDefinition = operation.getIncomingContextKeyDefinition();
            parent = operation;
        }
        if (Strings.isNullOrEmpty(contextDefinition)) {
            contextDefinition = system.getIncomingContextKeyDefinition();
            parent = system;
        }
        if (Strings.isNullOrEmpty(contextDefinition)) {
            if (throwKeyDefinitionException) {
                throw new KeyDefinitionException(system, operation);
            } else {
                return null;
            }
        }
        String contextKey = KeyHelper.defineKey(contextDefinition, context, parent);
        log.debug("Context key is: " + contextKey);
        return contextKey;
    }

    /*  Depending on isStub.equals("Yes") or not, the method performs:
     *      - if isStub.equals("Yes"), a new tcContext is created and put into bindingCache by the key,
     *      - otherwise, we primarily try to get existing tcContext from bindingCache by the key;
     *          - if no such key in the bindingCache, a new tcContext is created and put into bindingCache by the key.
     *
     *  How it works if contextKey is actually multiple keys?
     *  The algorithm is basically the same, with additions:
     *      - if a context is created, we put it to bindingCache by each key value,
     *      - if a context is searched in the bindingCache, we perform search by all keys.
     *
     *  Extra behavior change:
     *      - If contextKey is empty, we not operate with bindingCache at all. Instead, we simply create a new context.
     *  One more extra behavior change:
     *      - Empty keys (after splitting) are ignored
     */
    public TcContext findOrCreateTcContextByKeys(String contextKey, String isStub, BigInteger projectId,
                                                 UUID projectUuid) {
        if (StringUtils.isBlank(contextKey)) {
            return ExecutionServices.getTCContextService().createInMemory(projectId, projectUuid);
        }
        String[] contextKeys = contextKey.split("\\(\\+key\\)\\n");
        BoundContextsCacheService boundContextsCacheService = CacheServices.getTcBindingCacheService();
        if (isStub != null && isStub.equals("Yes")) {
            return (contextKeys.length == 1)
                    ? boundContextsCacheService.createByKey(contextKey, true, projectId, projectUuid)
                    : boundContextsCacheService.createByKeys(contextKeys, true, projectId, projectUuid);
        } else {
            return (contextKeys.length == 1)
                    ? boundContextsCacheService.findByKey(contextKey, projectId, projectUuid)
                    : boundContextsCacheService.findByKeys(contextKeys, projectId, projectUuid);
        }
    }

    /**
     * TODO Add JavaDoc.
     */
    public Operation processIncomingMessage(Message message,
                                            InstanceContext context,
                                            TransportConfiguration transport,
                                            BigInteger projectId, UUID projectUuid) throws Exception {
        System system = transport.getParent();
        Parser parser = new Parser();
        log.debug("Parse general message parameters to define operation");
        Map<String, MessageParameter> systemMessageParameters = parser.parse(projectId, message, context, system);
        context.sp().putMessageParameters(systemMessageParameters.values());
        log.debug("Define operation using this parsed parameters");
        try {
            Operation operation = system.defineOperation(context);
            log.info("Operation is: " + operation.getName());
            Set<ParsingRule> parsingRuleWithoutSituationParsingRules = getParsingRulesWithoutSituationParsingRules(
                    operation);
            log.debug("Parse concrete message parameters for specific operation");
            Map<String, MessageParameter> operationMessageParameters = parser
                    .parse(projectId, message, context, parsingRuleWithoutSituationParsingRules);
            context.sp().putMessageParameters(operationMessageParameters.values());
            return operation;
        } catch (OperationDefinitionException e) {
            TcContext tcContext;
            try {
                String contextKey = getContextKey(context, system, null, true);
                tcContext = findOrCreateTcContextByKeys(contextKey, "No", projectId, projectUuid);
            } catch (KeyDefinitionException e1) {
                log.error("Cannot define context to log error, automatic context for exception is created", e);
                tcContext = findOrCreateTcContextByKeys("Error Context key", "No", projectId, projectUuid);
                tcContext.setName("Error Context");
            }
            tcContext.setProjectId(projectId);
            tcContext.setProjectUuid(context.getProjectUuid());
            tcContext.setStartedFrom(StartedFrom.ITF_STUB);
            tcContext.setAndCalculateNeedToReportToItf();
            context.setTC(tcContext);
            throw e;
        }
    }

    /**
     * TODO Add JavaDoc.
     */
    public Map<String, MessageParameter> processOutboundResponseMessage(System system,
                                                                        Operation operation,
                                                                        Situation situation,
                                                                        Message message,
                                                                        InstanceContext instanceContext,
                                                                        BigInteger projectId) {
        Parser parser = new Parser();
        Map<String, MessageParameter> messageParameters = parser
                .parse(projectId, message, instanceContext, system.returnParsingRules());
        if (situation.getParsingRules().isEmpty()) {
            messageParameters.putAll(parser.parse(projectId, message, instanceContext, operation.returnParsingRules()));
        } else {
            messageParameters.putAll(parser.parse(projectId, message, instanceContext, situation.getParsingRules()));
        }
        return messageParameters;
    }

    private StepInstance createUnexpectedStepInstance(InstanceContext context,
                                                      SituationInstance unexpectedSituation,
                                                      Throwable error) {
        StepInstance stepInstance = new StepInstance();
        stepInstance.setID(UniqueIdGenerator.generate());
        stepInstance.init(UNEXPECTED_STEP);
        stepInstance.getContext().putAll(context);
        stepInstance.setParent(unexpectedSituation);
        stepInstance.setError(error);
        stepInstance.setStartTime(context.getTC().getStartTime());
        stepInstance.setEndTime(new Date());
        stepInstance.setStatus(Status.FAILED);
        stepInstance.getContext().setProjectUuid(context.getProjectUuid());
        stepInstance.getContext().setProjectId(context.getProjectId());
        unexpectedSituation.getStepInstances().add(stepInstance);
        return stepInstance;
    }

    private SituationInstance createUnexpectedSituationInstance(InstanceContext context, Throwable error) {
        SituationInstance unexpectedSituation = new SituationInstance();
        unexpectedSituation.setID(UniqueIdGenerator.generate());
        unexpectedSituation.setParentContext(context.getTC());
        unexpectedSituation.getContext().putAll(context);
        unexpectedSituation.setStepContainer(UNEXPECTED_SITUATION);
        unexpectedSituation.setError(error);
        unexpectedSituation.setStartTime(context.getTC().getStartTime());
        unexpectedSituation.setEndTime(new Date());
        unexpectedSituation.setStatus(Status.FAILED);
        unexpectedSituation.getContext().setProjectUuid(context.getProjectUuid());
        unexpectedSituation.getContext().setProjectId(context.getProjectId());
        unexpectedSituation.setName("*** Runtime exception occurred ***");
        return unexpectedSituation;
    }

    /**
     * TODO Add JavaDoc.
     */
    public void executeUnexpectedSituation(InstanceContext context, Exception e) {
        SituationInstance unexpectedSituation = createUnexpectedSituationInstance(context, e);
        StepInstance unexpectedStepInstance = createUnexpectedStepInstance(context, unexpectedSituation, e);
        context.tc().setInitiator(unexpectedSituation);
        ExecutionServices.getTCContextService().fail(context.tc());
        eventBusProvider.post(new SituationEvent.Start(unexpectedSituation));
        eventBusProvider.post(new StepEvent.Start(unexpectedStepInstance));
        eventBusProvider.post(new StepEvent.Terminate(unexpectedStepInstance));
        eventBusProvider.post(new SituationEvent.Terminate(unexpectedSituation));
    }

    public Situation detectSituationFromOperation(Operation operation, JsonContext context) {
        log.debug("getAllActive OperationEventTrigger request to DB - started");
        List<? extends EventTrigger> operationEventTriggers = CoreObjectManager.getInstance()
                .getSpecialManager(OperationEventTrigger.class, OperationEventTriggerObjectManager.class)
                .getAllActive(operation);
        log.debug("getAllActive OperationEventTrigger request to DB - finished");
        for (EventTrigger eventTrigger : operationEventTriggers) {
            List<ConditionParameter> conditionParameters = eventTrigger.getConditionParameters();
            if (conditionParameters == null || conditionParameters.isEmpty()) {
                log.info("Situation {}: conditions are empty; situation is to be executed",
                        eventTrigger.getParent());
                return (Situation) eventTrigger.getParent(); // this is default situation
            }
            if (ConditionsHelper.isApplicable(context, conditionParameters)) {
                return (Situation) eventTrigger.getParent();
            }
        }
        return operation.getDefaultIfInbound();
    }

    /**
     * TODO Add JavaDoc.
     */
    public void tryToGetRequestMethod(Message message, InstanceContext context) {
        Object method = message.getConnectionProperties().get("method");
        if (method != null) {
            context.sp().put("method", method);
        }
    }

    /**
     * TODO Add JavaDoc.
     */
    public Set<ParsingRule> getParsingRulesWithoutSituationParsingRules(Operation operation) {
        Set<ParsingRule> result = Sets.newHashSet();
        Set<ParsingRule> operationParsingRules = operation.returnParsingRules();
        if (!operationParsingRules.isEmpty()) {
            Set<ParsingRule> situationsParsingRules = Sets.newHashSet();
            for (Situation situation : operation.getSituations()) {
                situationsParsingRules.addAll(situation.getParsingRules());
            }
            for (ParsingRule operationParsingRule : operationParsingRules) {
                if (!situationsParsingRules.contains(operationParsingRule)) {
                    result.add(operationParsingRule);
                }
            }
        }
        return result;
    }
}
