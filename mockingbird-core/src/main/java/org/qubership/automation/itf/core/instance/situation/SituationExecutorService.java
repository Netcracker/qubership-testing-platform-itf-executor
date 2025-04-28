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

package org.qubership.automation.itf.core.instance.situation;

import static org.qubership.automation.itf.core.instance.situation.TCContextDiffCache.TC_CONTEXT_DIFF_CACHE;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.annotation.AtpSpanTag;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.instance.step.StepExecutorFactory;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.NextCallChainEventSubscriberHolder;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.SubscriberData;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.condition.ConditionsHelper;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.event.StepEvent;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Listener;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.regenerator.KeysRegenerator;
import org.qubership.automation.itf.core.report.ReportIntegration;
import org.qubership.automation.itf.core.util.FlatMapUtil;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants;
import org.qubership.automation.itf.core.util.constants.SituationLevelValidation;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.core.util.exception.IncomingValidationException;
import org.qubership.automation.itf.core.util.generator.id.UniqueIdGenerator;
import org.qubership.automation.itf.core.util.holder.EventTriggerHolder;
import org.qubership.automation.itf.core.util.iterator.StepIterator;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SituationExecutorService {

    private final TemplateEngine templateEngine;
    private final ReportLinkCollector reportLinkCollector;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final EventBusProvider eventBusProvider;
    private final StepExecutorFactory stepExecutorFactory;
    private final ProjectSettingsService projectSettingsService;

    public void executeInstance(final SituationInstance instance,
                                Storable source,
                                SpContext spContext,
                                NextCallChainEvent event) {
        executeInstance(instance, source, spContext, event, instance.getSituationById());
    }

    @AtpJaegerLog(spanTags = @AtpSpanTag(key = "execute.situation.instance.id",
            value = "#instance.situationId.toString()"))
    public void executeInstance(final SituationInstance instance,
                                Storable source,
                                SpContext spContext,
                                NextCallChainEvent event,
                                Situation situation) {
        MdcUtils.put(MdcField.TRACE_ID.toString(), MDC.get(MdcField.STUB_TRACE_ID.toString()));
        executeInstance(instance, source, spContext, event, situation, situation.getParent());
    }

    public void executeInstance(final SituationInstance instance,
                                Storable source,
                                SpContext spContext,
                                NextCallChainEvent event,
                                Situation situation,
                                Operation parentOperation) {
        fillParentsInfo(instance, situation, parentOperation);
        instance.setParentContext(prepareAndStartTcContextIfNeeded(instance));
        executeOnStartSituationTrigger(instance, situation);
        Map<String, Object> leftFlatMap = (StartedFrom.RAM2.equals(instance.getContext().getTC().getStartedFrom()))
                ? FlatMapUtil.flatten(instance.getContext().getTC())
                : new HashMap<>();
        instance.setStatus(Status.IN_PROGRESS);
        instance.setStartTime(new Date());
        instance.setName(TemplateEngineFactory.process(instance.getStepContainer(),
                situation.getName(), instance.getContext(), "Situation name"));
        SituationEvent.Start startSituationEvent = new SituationEvent.Start(instance);
        startSituationEvent.setSource(source);
        eventBusProvider.post(startSituationEvent);
        boolean needRetryIntegrationStep;
        boolean firstRun = true;
        while (true) {
            try {
                executeInstanceStep(instance, spContext, event, situation, firstRun, parentOperation);
                computeContextDiff(leftFlatMap, instance.getContext().getTC(), instance.getID());
                break;
            } catch (EngineIntegrationException e) {
                needRetryIntegrationStep = checkNeedRetryIntegrationStep(instance);
                retryPause(instance, !needRetryIntegrationStep);
                if (needRetryIntegrationStep) {
                    failureOfSituationWithoutAtpReport(instance, e);
                } else {
                    performPostCalculations(situation, instance,
                            instance.getStepInstances().get(instance.getStepInstances().size() - 1).getContext(), true);
                    computeContextDiff(leftFlatMap, instance.getContext().getTC(), instance.getID());
                    postStepEventFinishIfIsRetryStep(instance);
                    failureOfSituation(instance, e, "Failed message validation");
                }
                if (event != null) {
                    if (needRetryIntegrationStep) {
                        instance.setIterator(null);
                        firstRun = false;
                        // Will proceed to the next iteration, so step will be retried
                    } else if (situation.getValidateIncoming() == SituationLevelValidation.FAIL) {
                        postEventException(e.getShortMessage(), event);
                        break;
                    } else {
                        postResumeEventWithContinueTc(event);
                        break;
                    }
                } else {
                    failTCByException(instance, e);
                    break;
                }
            } catch (IncomingValidationException e) {
                computeContextDiff(leftFlatMap, instance.getContext().getTC(), instance.getID());
                if (situation.getValidateIncoming() == SituationLevelValidation.FAIL) {
                    failTCByException(instance, e);
                    if (instance.getContext().tc().getInitiator() instanceof SituationInstance) {
                        throw new RuntimeException("Exception while preparing a response for inbound message",
                                (e.getCause() instanceof EngineIntegrationException) ? e.getCause() : e);
                    }
                } else {
                    failureOfSituation(instance, e, e.getMessage());
                    throw new IncomingValidationException(e.getMessage());
                }
            } catch (Exception e) {
                computeContextDiff(leftFlatMap, instance.getContext().getTC(), instance.getID());
                failTCByException(instance, e);
                if (instance.getContext().tc().getInitiator() instanceof SituationInstance) {
                    throw new RuntimeException("Exception while preparing a response for inbound message",
                            (e.getCause() instanceof EngineIntegrationException) ? e.getCause() : e);
                }
                break;
            }
        }
    }

    private void executeOnStartSituationTrigger(SituationInstance instance, Situation situation) {
        List<Listener> listeners = EventTriggerHolder.getInstance().getOnStartSituationListeners(situation.getID());
        if (Objects.isNull(listeners) || listeners.isEmpty()) {
            return;
        }
        for (Listener listener : listeners) {
            log.debug("Get SituationEventTrigger from DB by id - started");
            executeByTrigger(CoreObjectManager.getInstance().getManager(SituationEventTrigger.class)
                    .getById(listener.getId()), instance);
        }
    }

    private void fillParentsInfo(SituationInstance instance, Situation situation, Operation parentOperation) {
        instance.setSituationId((BigInteger) situation.getID());
        if (parentOperation != null) {
            // default auto created situations don't have parents...
            instance.setOperationName(parentOperation.getName());
            instance.setOperationId((BigInteger) parentOperation.getID());
            instance.setSystemName(parentOperation.getParent().getName());
            instance.setSystemId((BigInteger) parentOperation.getParent().getID());
        }
    }

    private void performPostCalculations(Situation situation, SituationInstance instance,
                                         InstanceContext lastExecutedStepInstanceContext, boolean onlyLogException) {
        try {
            if (!StringUtils.isBlank(situation.getPostScript())) {
                templateEngine.process(situation, situation.getPostScript(),
                        lastExecutedStepInstanceContext, "post-script on the situation");
            }
            setParsedName(instance, situation.getName(), lastExecutedStepInstanceContext);
        } catch (Exception parsingException) {
            if (onlyLogException) {
                /* The most important here is that the situation will be failed due to validation,
                    so we don't need to process possible parsing exceptions carefully in that case.
                    So, we simply log an exception and proceed to a normal failure processing.
                 */
                log.warn("Instance {}: an exception occurred while post-script is executed.", instance,
                        parsingException);
            } else {
                throw new RuntimeException("An exception occurred while post-script is executed:", parsingException);
            }
        }
    }

    /*  The method computes TC-context difference between TC contexts at the start and end of situation execution.
     *   It should be computed here due to asynchronous manner of our reporting:
     *       - due to it, TC-context can be changed by other steps at the moment of the step reporting.
     *   Why flatten TC-contexts are compared instead of TC-contexts themselves?
     *       - Because TC-context groups are copied by reference, so difference is computed wrong.
     *   Why a difference is computed in case StartedFrom.RAM2 only?
     *       - Because (currently) we should compute and report a difference only to RAM2
     * */
    private void computeContextDiff(Map<String, Object> leftFlatMap, TcContext currentContext, Object id) {
        if (StartedFrom.RAM2.equals(currentContext.getStartedFrom())) {
            Map<String, Object> rightFlatMap = FlatMapUtil.flatten(currentContext);
            MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
            TC_CONTEXT_DIFF_CACHE.put(id.toString(), difference);
        }
    }

    /*  This preparation is necessary before posting event!!!
     *  Actions: 1. Get tcContext; 2. Set initiator, name; 3. Collect report links; 4. Start tcContext if not running
     */
    private TcContext prepareAndStartTcContextIfNeeded(final SituationInstance instance) {
        TcContext tcContext = instance.getContext().tc();
        if (tcContext.getInitiator() == null) {
            tcContext.setInitiator(instance);
            tcContext.setName(instance.getName());
            reportLinkCollector.collect(tcContext);
        }
        if (!tcContext.isRunning()) {
            // It's possible that the context found by key for inbound situation at the moment has already finished.
            // May be it's not a good decision but... silently start it again and continue processing
            if (tcContext.isRunnable()) {
                ExecutionServices.getTCContextService().start(tcContext);
            }
        }
        return tcContext;
    }

    /*
     *  The method returns true/false depending on MEP characteristics.
     *  Return value is almost the same as mep.isInbound(), with the only difference:
     *      - for INBOUND_RESPONSE_ASYNCHRONOUS mep.isInbound()=true, but the method returns false.
     *      One important point:
     *          - currently, there is no transport supported with such MEP.
     */
    private boolean isInboundRequestOnlyOrInboundSync(Mep mep) {
        return mep.isInbound() && ((mep.isRequest() && !mep.isResponse()) || mep.isSync());
    }

    /* The method returns:
     *    - false - if script is executed (so, it means 'the script is executed; there's NO need to execute it again')
     *    - firstRun - otherwise
     */
    private boolean executePreScriptAtFirstRun(Situation situation, InstanceContext instanceContext, boolean firstRun) {
        instanceContext.tc().putIfAbsent("saved", new JsonContext());
        if (firstRun && !StringUtils.isBlank(situation.getPreScript())) {
            templateEngine.process(situation, situation.getPreScript(), instanceContext, "pre-script on the situation");
            return false;
        }
        return firstRun;
    }

    private InstanceContext checkForNullContext(StepInstance lastExecutedStepInstance, SituationInstance instance) {
        return lastExecutedStepInstance == null
                ? instance.getContext()
                : lastExecutedStepInstance.getContext();
    }

    private void executeInstanceStep(final SituationInstance instance,
                                     SpContext spContext,
                                     NextCallChainEvent event,
                                     Situation situation,
                                     boolean firstRun,
                                     Operation parentOperation) throws Exception {
        TcContext tcContext = instance.getContext().tc();
        log.debug("regenerateKeys - started");
        KeysRegenerator.getInstance().regenerateKeys(instance.getContext(), situation.getKeysToRegenerate());
        log.debug("regenerateKeys - finished");
        StepInstance lastExecutedStepInstance = null;
        boolean needRunPreScript = firstRun;
        boolean inboundRequestOrSync = isInboundRequestOnlyOrInboundSync(parentOperation.getMep());
        if (inboundRequestOrSync) {
            log.info("Execution of inbound situation {}", situation);
            StepInstance stepInstance = createInbound(instance, spContext, parentOperation);
            spContext = stepInstance.getContext().sp();
            needRunPreScript = executePreScriptAtFirstRun(situation, stepInstance.getContext(), needRunPreScript);
            recalculateInboundContextName(instance, checkForNullContext(stepInstance, instance));
            String clientAddressContextVariable = projectSettingsService.get(tcContext.getProjectId(),
                    ProjectSettingsConstants.TC_CONTEXT_CLIENT_ADDRESS,
                    ProjectSettingsConstants.TC_CONTEXT_CLIENT_ADDRESS_DEFAULT_VALUE);
            if (!StringUtils.isBlank(clientAddressContextVariable)) {
                instance.getContext().tc().setClient(templateEngine.process(situation, clientAddressContextVariable,
                        stepInstance.getContext(), "'Client' context variable"));
            }
            try {
                validateIncomingMessage(instance, stepInstance.getContext(), situation);
            } catch (Exception ex) {
                if (situation.getValidateIncoming() != SituationLevelValidation.IGNORE) {
                    throw ex;
                }
            } finally {
                stepExecutorFactory.execute(stepInstance);
                lastExecutedStepInstance = stepInstance;
                if (parentOperation.getMep().isSync()) {
                    addHeadersInTC(spContext.getIncomingMessage().getHeaders(), stepInstance.getContext().getTC());
                }
                checkErrorsAndValidationLevel(instance, situation);
            }
        }
        //to refresh step iterator
        StepIterator iterator = instance.iterator();
        while (iterator.hasNext()) {
            StepInstance stepInstance = iterator.next();
            stepInstance.setID(UniqueIdGenerator.generate());
            if (spContext != null) {
                stepInstance.getContext().sp().putAll(spContext);
            }
            stepInstance.setIsRetryStep(isRetryOnFailStep(stepInstance));
            needRunPreScript = executePreScriptAtFirstRun(situation, stepInstance.getContext(), needRunPreScript);
            try {
                stepExecutorFactory.execute(stepInstance);
            } catch (Exception ex) {
                if (!situation.isIgnoreErrors()) {
                    throw ex;
                }
            } finally {
                recalculateInboundContextName(instance, checkForNullContext(stepInstance, instance));
                lastExecutedStepInstance = stepInstance;
                if (stepInstance.isRetryStep()) {
                    addCurrentValidStepAttemptValueAndStartTime(instance);
                }
            }
        }
        ExecutionServices.getTCContextService().updateLastAccess(tcContext);
        // Let's ensure that messageValidation method will be invoked not more than once
        if (!inboundRequestOrSync && tcContext.isStartValidation()) {
            /* If Initiator() instanceof  SituationInstance,
                it means that this situation is invoked via On-Start/On-Finish trigger from inbound situation triggered
                by stub.
                In that case we do NOT need to go away back to validation-retry loop in the parent method.
                Instead, like validation for inbound step (see above), we use 'validateIncomingMessage' method
            */
            if (tcContext.getInitiator() instanceof SituationInstance) {
                try {
                    validateIncomingMessage(instance, checkForNullContext(lastExecutedStepInstance, instance),
                            situation);
                } catch (Exception ex) {
                    if (situation.getValidateIncoming() != SituationLevelValidation.IGNORE) {
                        throw ex;
                    }
                } finally {
                    eventBusProvider.post(new StepEvent.Finish(lastExecutedStepInstance));
                    checkErrorsAndValidationLevel(instance, situation);
                }
            } else {
                messageValidation(instance, checkForNullContext(lastExecutedStepInstance, instance), situation);
            }
        }
        performPostCalculations(situation, instance, checkForNullContext(lastExecutedStepInstance, instance), false);
        recalculateInboundContextName(instance, checkForNullContext(lastExecutedStepInstance, instance));
        postStepEventFinishIfIsRetryStep(instance);
        instance.setStatus(Status.PASSED);
        instance.setEndTime(new Date());
        // The below block is needed only to report validation attempts count into stepInstance name.
        // May be it could be done easier?
        // Block to be reviewed - start
        List<StepInstance> listStepInstance = instance.getStepInstances();
        StepInstance stepInstance = listStepInstance.get(listStepInstance.size() - 1);
        if (isRetryOnFailStep(stepInstance)) {
            stepInstance.setName(reportValidationAttemptsCount(stepInstance));
        }
        // Block to be reviewed - end
        log.info("Situation {} executed", instance);
        eventBusProvider.post(new SituationEvent.Finish(instance));
        sendEndExceptionalSituationFinishEvent(tcContext, instance);
        if (event != null) {
            /* Merge tcContext changes made while step/situation execution into tcContext of CallChainInstance.
             * This is the tablet for - may be - not for root cause but only for its consequence
             * The problem description is:
             *  At this point instance.getContext().tc() contains changes of the context variables made during this step
             *  but event.getInstance().getContext().tc() - doesn't contain them!
             *  So, while the 'event' processing, inactual tcContext is reported. */
            TcContext eventInstanceContext = event.getInstance().getContext().tc();
            if (eventInstanceContext == null) {
                eventInstanceContext = new TcContext();
            }
            eventInstanceContext.merge(tcContext);
            eventBusProvider.post(event);
        }
        if (checkIsStub(situation)) {
            if (instance.getErrorMessage() != null) {
                if (situation.getValidateIncoming() == SituationLevelValidation.IGNORE) {
                    ExecutionServices.getTCContextService().finish(tcContext);
                } else {
                    ExecutionServices.getTCContextService().fail(tcContext);
                }
            } else {
                ExecutionServices.getTCContextService().finish(tcContext);
            }
        } else if (inboundRequestOrSync) {
            ExecutionServices.getTCContextService().updateInfo(tcContext);
        }
    }

    private void recalculateInboundContextName(SituationInstance instance, InstanceContext instanceContext) {
        TcContext tcContext = instance.getContext().tc();
        if (tcContext.getInitiator() instanceof SituationInstance && tcContext.getName().contains("$")) {
            try {
                String s = templateEngine.process(instance, tcContext.getName(), instanceContext, "TcContext name");
                tcContext.setName(s);
            } catch (Exception ex) {
                log.warn("TcContext name recalculation is failed for id={}, name='{}'", tcContext.getID(),
                        tcContext.getName());
            }
        }
    }

    private void setParsedName(SituationInstance instance,
                               String configuredName,
                               InstanceContext instanceContext) {
        String parsedName = TemplateEngineFactory.process(instance.getStepContainer(), configuredName, instanceContext,
                "Situation name");
        if (!StringUtils.isBlank(parsedName)) {
            instance.setName(parsedName);
        }
    }

    private void checkErrorsAndValidationLevel(SituationInstance instance, Situation situation) {
        SituationLevelValidation validationLevel = situation.getValidateIncoming();
        if (!validationLevel.equals(SituationLevelValidation.WARNING)
                && !validationLevel.equals(SituationLevelValidation.IGNORE)
                && instance.getError() != null) {
            throw new IncomingValidationException(instance.getErrorMessage());
        }
    }

    private void validateIncomingMessage(SituationInstance instance,
                                         InstanceContext instanceContext,
                                         Situation situation) throws Exception {
        try {
            messageValidation(instance, instanceContext, situation);
        } catch (EngineIntegrationException e) {
            SituationLevelValidation validationLevel = situation.getValidateIncoming();
            if (validationLevel.equals(SituationLevelValidation.WARNING)
                    || validationLevel.equals(SituationLevelValidation.IGNORE)
                    || validationLevel.equals(SituationLevelValidation.FAIL)) {
                instance.setError(new EngineIntegrationException(e.getMessage()));
            }
        }
    }

    private boolean checkIsStub(Situation situation) {
        String isStub;
        IntegrationStep integrationStep = situation.getIntegrationStep();
        if (integrationStep == null) {
            Operation operation = situation.getParent();
            // operation can be null for default auto created situations at 1st occurrence
            isStub = (operation == null) ? null : operation.getTransport().getConfiguration().get("isStub");
        } else {
            isStub = integrationStep.getOperation().getTransport().getConfiguration().get("isStub");
        }
        return (isStub != null && isStub.equals("Yes"));
    }

    private boolean checkNeedRetryIntegrationStep(final SituationInstance instance) {
        List<StepInstance> listStepInstance = instance.getStepInstances();
        StepInstance stepInstance = listStepInstance.get(listStepInstance.size() - 1);
        if (stepInstance.getStep() instanceof IntegrationStep) {
            IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
            if (integrationStep.isRetryOnFail()) {
                stepInstance.setEndTime(new Date());
                if (integrationStep.getValidationMaxTime() != 0) {
                    long timePassed = integrationStep.retrieveValidationUnitMaxTime()
                            .convert(countPassedTime(stepInstance), TimeUnit.MILLISECONDS);
                    if (isTimeExpired(integrationStep, timePassed)) {
                        stepInstance.setName(reportValidationAttemptsCount(stepInstance));
                        return false;
                    }
                } else if (areAttemptsExpired(stepInstance)) {
                    stepInstance.setName(reportValidationAttemptsCount(stepInstance));
                    return false;
                }
                return true; // "normal completion"
            } else {
                return false;
            }
        } else {
            // If we are here, it means that program logic is broken. Only IntegrationSteps are possible at the point.
            log.error("Trying to 'retryIntegrationStep' but it's not a IntegrationStep: {}", stepInstance);
            return false;
        }
    }

    private String reportValidationAttemptsCount(StepInstance stepInstance) {
        return stepInstance.getName() + " (attempts count: " + stepInstance.getCurrentValidAttemptValue() + ")";
    }

    /*  The method performs manipulations with validation retry steps only:
     *      1. Attempt number and start time are set
     *      2. Previous attempts are removed
     */
    private void addCurrentValidStepAttemptValueAndStartTime(SituationInstance instance) {
        List<StepInstance> listStepInstance = instance.getStepInstances();
        StepInstance lastStepInstance = listStepInstance.get(listStepInstance.size() - 1);
        if (listStepInstance.size() > 1) {
            StepInstance secondToLastStepInstance = listStepInstance.get(listStepInstance.size() - 2);
            lastStepInstance.setCurrentValidAttemptValue(secondToLastStepInstance.getCurrentValidAttemptValue() + 1);
            lastStepInstance.setStartTime(secondToLastStepInstance.getStartTime());
            listStepInstance.remove(0);
        } else {
            lastStepInstance.setCurrentValidAttemptValue(lastStepInstance.getCurrentValidAttemptValue() + 1);
        }
    }

    private boolean isRetryOnFailStep(StepInstance stepInstance) {
        boolean result = false;
        if (stepInstance.getStep() instanceof IntegrationStep) {
            IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
            if (integrationStep.isRetryOnFail()) {
                result = true;
            }
        }
        return result;
    }

    private void retryPause(SituationInstance instance, boolean isLastRetry) {
        StepIterator iterator = instance.iterator();
        StepInstance stepInstance = iterator.current();
        IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
        if (!integrationStep.isRetryOnFail()) {
            return;
        }
        long retryTimeout = integrationStep.getRetryTimeout();
        if (retryTimeout > 0L && !StringUtils.isBlank(integrationStep.getRetryTimeoutUnit())) {
            String timeUnit = integrationStep.getRetryTimeoutUnit().toLowerCase();
            if (isLastRetry) {
                Report.info(instance,
                        String.format("Pause %d %s after each [%s] (count: %d)", retryTimeout, timeUnit,
                                instance.getName(), stepInstance.getCurrentValidAttemptValue() - 1),
                        String.format("Waiting for [%d] %s", retryTimeout, timeUnit));
            } else {
                try {
                    log.info("{}: waiting for timeout '{}' {}...", instance, retryTimeout, timeUnit);
                    Thread.sleep(integrationStep.retrieveRetryTimeoutUnit().toMillis(retryTimeout));
                } catch (InterruptedException e) {
                    failureOfSituation(instance, e, "Interrupted Exception");
                }
            }
        }
    }

    private long countPassedTime(StepInstance stepInstance) {
        return (new Date()).getTime() - stepInstance.getStartTime().getTime();
    }

    private boolean isTimeExpired(IntegrationStep integrationStep, long timePassed) {
        return integrationStep.getValidationMaxTime() - timePassed < 0;
    }

    private boolean areAttemptsExpired(StepInstance stepInstance) {
        IntegrationStep integrationStep = (IntegrationStep) stepInstance.getStep();
        return (integrationStep.getValidationMaxAttempts() != 0)
                && (stepInstance.getCurrentValidAttemptValue()) >= integrationStep.getValidationMaxAttempts();
    }

    private void failTCByException(SituationInstance instance, Exception e) {
        failureOfSituation(instance, e, "Failed situation");
        if (instance.getContext().tc().getInitiator() != null) {
            failOfInitiator(instance.getContext().tc().getInitiator(), instance.getContext().tc(), e);
        }
        failureOfTC(instance);
    }

    private void failOfInitiator(AbstractContainerInstance initiator, TcContext tcContext, @Nonnull Exception e) {
        initiator.setStatus(Status.FAILED);
        initiator.setErrorName(e.getMessage());
        if (e.getCause() != null) {
            initiator.setErrorMessage(e.getCause().toString());
        }
        SubscriberData subscriberData = NextCallChainEventSubscriberHolder.getInstance()
                .getSubscriberData(tcContext.getID());
        /* We faced some cases when TcContext was removed from the NextCallChainEventSubscriberHolder cache
         *  earlier than this method was executed after the last attempt.
         *  So, we should at least avoid NPE if we don't have fixed the reason of such behaviour
         */
        if (subscriberData != null) {
            NextCallChainEvent.Exception exception = new NextCallChainEvent.Exception(
                    subscriberData.getParentSubscriberId(), null, (e.getCause() == null)
                    ? ExceptionUtils.getStackTrace(e)
                    : e.getCause().toString());
            exception.setID(subscriberData.getSubscriberId());
            eventBusProvider.post(exception);
        }
    }

    private void postEventException(String msg, NextCallChainEvent event) {
        NextCallChainEvent.Exception exception = new NextCallChainEvent.Exception(event.getParentId(),
                event.getInstance(), msg);
        exception.setID(event.getID());
        eventBusProvider.post(exception);
    }

    private void postResumeEventWithContinueTc(NextCallChainEvent event) {
        NextCallChainEvent.ResumeStepWithContinueTc resume = new NextCallChainEvent.ResumeStepWithContinueTc(
                event.getParentId(), event.getInstance());
        resume.setID(event.getID());
        eventBusProvider.post(resume);
    }

    private void failureOfTC(SituationInstance instance) {
        TcContext tcContext = instance.getContext().tc();
        ExecutionServices.getTCContextService().fail(tcContext);
    }

    private StepInstance getLastStepInstance(SituationInstance instance) {
        List<StepInstance> listStepInstance = instance.getStepInstances();
        return listStepInstance.get(listStepInstance.size() - 1);
    }

    private void postStepEventFinishIfIsRetryStep(SituationInstance instance) {
        StepInstance lastStepInstance = getLastStepInstance(instance);
        if (lastStepInstance.isRetryStep()) {
            eventBusProvider.post(new StepEvent.Finish(lastStepInstance));
        }
    }

    private void failureOfSituation(SituationInstance instance, Exception e, String message) {
        failureInstance(instance, e, message);
        eventBusProvider.post(new SituationEvent.Terminate(instance));
    }

    private void failureOfSituationWithoutAtpReport(SituationInstance instance, Exception e) {
        log.warn("Failed message validation at the {}, Error: {}", instance,
                (e instanceof EngineIntegrationException)
                        ? ((EngineIntegrationException) e).getShortMessage() : e.getMessage());
        instance.setStatus(Status.FAILED);
        instance.setEndTime(new Date());
    }

    private void failureInstance(SituationInstance instance, Exception e, String message) {
        log.error("{} at the {}, Error: {}", message, instance,
                (e instanceof EngineIntegrationException)
                        ? ((EngineIntegrationException) e).getShortMessage() : e.getMessage());
        instance.setError(e);
        instance.setStatus(Status.FAILED);
        instance.setEndTime(new Date());
    }

    private void messageValidation(SituationInstance instance,
                                   InstanceContext lastExecutedStepInstanceContext,
                                   Situation situation)
            throws Exception {
        if (situation.getBooleanValidateIncoming()) {
            if (!StringUtils.isBlank(situation.getPreValidationScript())) {
                templateEngine.process(situation, situation.getPreValidationScript(), lastExecutedStepInstanceContext,
                        "pre-validation script on the situation");
            }
            validationInIntegration(instance,
                    lastExecutedStepInstanceContext.sp(),
                    instance.getContext().getTC().getInitiator(),
                    situation);
        }
    }

    private void validationInIntegration(SituationInstance instance,
                                         SpContext spContext,
                                         AbstractContainerInstance initiator,
                                         Situation situation) throws Exception {
        if (initiator instanceof CallChainInstance || initiator instanceof SituationInstance) {
            if (!ReportIntegration.getInstance().runOnStepIntegrations(instance, spContext, initiator, situation)) {
                throw new EngineIntegrationException(initiator.getErrorName(), initiator.getErrorMessage());
            }
        }
    }

    /**
     * Preparing instance for situation to execute.
     *
     * @param situation situation to execute
     * @param context   execution context
     * @return SituationInstance that was prepared to execute
     * @throws Exception exception
     */
    public SituationInstance prepare(final Situation situation, final InstanceContext context) throws Exception {
        log.info("Preparing instance for situation {}...", situation);
        SituationInstance instance = new SituationInstance();
        log.debug("UniqueIdGenerator.generate - started");
        instance.setID(UniqueIdGenerator.generate());
        log.debug("UniqueIdGenerator.generate - finished");
        instance.setStepContainer(situation);
        instance.setName(situation.getName());
        instance.getContext().setTC(context.tc());
        instance.getContext().setProjectId(context.getProjectId());
        instance.getContext().setProjectUuid(context.getProjectUuid());
        if (Objects.nonNull(context.getSessionId())) {
            instance.getContext().setSessionId(context.getSessionId());
        }
        if (Objects.nonNull(context.getMessageBrokerSelectorValue())) {
            instance.getContext().setMessageBrokerSelectorValue(context.getMessageBrokerSelectorValue());
        }
        if (Objects.nonNull(context.getTransport())) {
            instance.getContext().setTransport(context.getTransport());
        }
        if (Objects.nonNull(context.getConnectionProperties())) {
            instance.getContext().setConnectionProperties(context.getConnectionProperties());
        }
        if (Objects.nonNull(context.sp())) {
            SpContext spContext = new SpContext();
            spContext.putAll(context.sp());
            instance.getContext().setSP(spContext);
        }
        return instance;
    }

    public void execute(Situation situation, InstanceContext context) throws Exception {
        execute(situation, context, null, null);
    }

    /**
     * Execute.
     *
     * @param situation situation to execute
     * @param context   execution context
     * @param source    where from situation executed. Nullable.
     *                  If you need specify source in report for example - propagate executor as source.
     *                  for example look to {@link SituationEvent}
     *                  and LoggerSubscriber#onSituationStart(SituationEvent.Start).
     * @param event     event for running next step in callchain
     * @throws Exception exception
     */
    public void execute(Situation situation, InstanceContext context,
                        @Nullable Storable source, NextCallChainEvent event) throws Exception {
        if (Objects.nonNull(situation)) {
            SituationInstance instance = prepare(situation, context);
            executeInstance(instance, source, context.sp(), event, situation);
        }
    }

    private StepInstance createInbound(final SituationInstance instance,
                                       final SpContext spContext,
                                       Operation parentOperation) {
        IntegrationStep step = new IntegrationStep();
        step.setOperation(parentOperation);
        step.setReceiver(parentOperation.getParent());
        step.setName(String.format("[%s] receives [%s]", step.getReceiver().getName(), step.getOperation().getName()));
        StepInstance stepInstance = new StepInstance();
        stepInstance.setParent(null);
        stepInstance.setID(UniqueIdGenerator.generate());
        stepInstance.init(step, instance.getContext(), spContext);
        stepInstance.getContext().setProjectId(stepInstance.getContext().tc().getProjectId());
        stepInstance.getContext().setProjectUuid(stepInstance.getContext().tc().getProjectUuid());
        stepInstance.setParent(instance);
        stepInstance.setName(step.getName());
        return stepInstance;
    }

    private void addHeadersInTC(Map<String, Object> headers, TcContext tcContext) {
        tcContext.put("headers", headers);
    }

    private void executeByTrigger(SituationEventTrigger trigger, SituationInstance situationInstance) {
        log.debug("Get SituationEventTrigger from DB by id - finished");
        Situation triggerParent = trigger.getParent();
        log.info("Event received by situation [{}], processing under {} {}...",
                situationInstance.getStepContainer(),
                triggerParent == null ? "" : triggerParent.getClass().getSimpleName(),
                triggerParent == null ? "" : triggerParent.getName());
        if (!situationInstance.getContext().tc().isFinished()) {
            List<ConditionParameter> conditionParameters = trigger.getConditionParameters();
            if (conditionParameters == null || conditionParameters.isEmpty()
                    || ConditionsHelper.isApplicable(situationInstance.getContext(), conditionParameters)) {
                if (conditionParameters == null || conditionParameters.isEmpty()) {
                    log.info("Condition is empty, applicable anyway");
                } else {
                    log.info("Conditions are applicable, handling under {} {}...",
                            triggerParent == null ? "" : triggerParent.getClass().getSimpleName(),
                            triggerParent == null ? "" : triggerParent.getName());
                }
                if (triggerParent != null) {
                    try {
                        execute(triggerParent, situationInstance.getContext());
                    } catch (Exception e) {
                        log.error("Error executing situation {}", triggerParent, e);
                    }
                } else {
                    log.warn("Run situation handler was called, but situation to execute is null");
                }
            } else {
                log.info("Conditions are not applicable, skip under {} {}",
                        triggerParent == null ? "" : triggerParent.getClass().getSimpleName(),
                        triggerParent == null ? "" : triggerParent.getName());
            }
        } else {
            log.info("Event was rejected due to context '{}' is closed",
                    situationInstance.getContext().tc().getName());
        }
    }

    private void sendEndExceptionalSituationFinishEvent(TcContext tcContext, SituationInstance situationInstance) {
        boolean isItEndSituationForSomeone = CacheServices.getAwaitingContextsCacheService()
                .containsKey(String.format("%s_%s", tcContext.getID(), situationInstance.getSituationId()));
        if (isItEndSituationForSomeone) {
            executorToMessageBrokerSender.sendEventToEndExceptionalSituationsTopic(
                    new SituationEvent.EndExceptionalSituationFinish(situationInstance),
                    situationInstance.getContext().getProjectUuid().toString());
        }
    }
}
