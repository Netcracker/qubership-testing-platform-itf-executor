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

package org.qubership.automation.itf.core.instance.testcase.execution.subscriber;

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.INFINITE_LOOP_PROTECTION_BARRIER;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.instance.step.StepExecutorFactory;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.DefferedSituationInstanceHolder;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.NextCallChainEventSubscriberHolder;
import org.qubership.automation.itf.core.model.condition.ConditionsHelper;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.event.CallChainEvent;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.NextEmbeddedStepEvent;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.AbstractCallChainStep;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.regenerator.KeysRegenerator;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.iterator.CallChainStepIterator;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusServiceProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;

@SuppressWarnings("UnstableApiUsage")
public class NextCallChainSubscriber extends AbstractChainSubscriber<NextCallChainEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextCallChainSubscriber.class);
    // Temporary? It's a defense against infinite loops due to incorrect conditions configuration
    private static final int infiniteLoopProtectionBarrier = Integer.parseInt(
            Config.getConfig().getString(INFINITE_LOOP_PROTECTION_BARRIER));
    private final CallChainStepIterator iterator;
    @Getter
    private final CallChainInstance instance;
    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private boolean resumed = false;

    public NextCallChainSubscriber(NextCallChainEvent event) {
        super(event.getID(), event.getParentId());
        instance = event.getInstance();
        iterator = instance.iterator();
    }

    public NextCallChainSubscriber(NextCallChainEvent event, CallChainStepIterator stepIterator) {
        super(event.getID(), event.getParentId());
        instance = event.getInstance();
        iterator = stepIterator;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handle(NextCallChainEvent event) {
        if (!(event instanceof NextCallChainEvent.Exception)) {
            handleEvent(event);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void pause(NextCallChainEvent.Pause event) {
        LOGGER.info("NextCallChain event: {} is pausing", event.getInstance());
        waiting.set(true);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void exception(NextCallChainEvent.Exception exception) {
        handleEvent(exception);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void resume(NextCallChainEvent.Resume event) {
        LOGGER.info("NextCallChain event: {} is resuming", event.getInstance());
        waiting.set(false);
        synchronized (waiting) {
            waiting.notify();
        }
    }

    public void registerSubscriberInHolder() {
        NextCallChainEventSubscriberHolder.getInstance()
                .add(instance.getContext().getTC().getID(), this.getId(), this.getParentId(), true);
    }

    @Override
    protected void onEvent(NextCallChainEvent event) {
        TcContext thisInstanceTcContext = instance.getContext().tc();
        TenantContext.setTenantInfo(thisInstanceTcContext.getProjectUuid().toString());
        try {
            ExecutionServices.getTCContextService().mergePendingContextsIfAny(thisInstanceTcContext);
            if (event instanceof NextCallChainEvent.Pause) {
                thisInstanceTcContext.setStatus(Status.PAUSED);
            } else if (event instanceof NextCallChainEvent.UpdateContext) {
                thisInstanceTcContext.merge(event.getInstance().getContext().tc());
            } else if (event instanceof NextCallChainEvent.ResumeWithoutContinue) {
                thisInstanceTcContext.merge(event.getInstance().getContext().tc());
                thisInstanceTcContext.setStatus(Status.IN_PROGRESS);
            } else if (event instanceof NextCallChainEvent.Exception) {
                if (this.getParentId() != null) {
                    sendFailEventToTheSubscriber(this.getParentId(),
                            new Exception(((NextCallChainEvent.Exception) event).getExceptionMessage()));
                }
                throw new RuntimeException(((NextCallChainEvent.Exception) event).getExceptionMessage());
            } else if (event instanceof NextCallChainEvent.Fail) {
                failInstance(instance, ((NextCallChainEvent.Fail) event).getException());
                if (this.getParentId() != null) {
                    sendFailEventToTheSubscriber(this.getParentId(), ((NextCallChainEvent.Fail) event).getException());
                } else {
                    ExecutionServices.getExecutionProcessManagerService().fail(thisInstanceTcContext);
                }
                destroy();
            } else if (event instanceof NextCallChainEvent.FailByTimeout) {
                failInstance(instance, ((NextCallChainEvent.FailByTimeout) event).getException());
                if (this.getParentId() != null) {
                    sendFailEventToTheSubscriber(this.getParentId(),
                            ((NextCallChainEvent.FailByTimeout) event).getException());
                } else {
                    ExecutionServices.getExecutionProcessManagerService().failByTimeout(thisInstanceTcContext);
                }
                destroy();
            } else {
                if (event instanceof NextCallChainEvent.Resume) {
                    thisInstanceTcContext.merge(event.getInstance().getContext().tc());
                    thisInstanceTcContext.setStatus(Status.IN_PROGRESS);
                    if (thisInstanceTcContext.isRunStepByStep()
                            && !event.getInstance().getContext().tc().isRunStepByStep()) {
                        thisInstanceTcContext.setRunStepByStep(event.getInstance().getContext().tc().isRunStepByStep());
                    }
                    resumed = true;
                } else if (event instanceof NextCallChainEvent.ResumeStepWithContinueTc) {
                    thisInstanceTcContext.setValidationFailed(true);
                    resumed = true;
                }
                executeNext(false);
            }
        } catch (Exception e) {
            LOGGER.error("TcContext id {}, Project [{}, {}], CallChain instance [{}] '{}', Step '{}': {}",
                    thisInstanceTcContext.getID(),
                    thisInstanceTcContext.getProjectId(),
                    thisInstanceTcContext.getProjectUuid(),
                    instance.getID(),
                    instance.getName(),
                    iterator.current().getName(), ExceptionUtils.getStackTrace(e));
            failInstance(instance, e);
            ExecutionServices.getExecutionProcessManagerService().fail(thisInstanceTcContext);
            destroy();
        }
    }

    private void finishInstance() {
        if (getParentId() == null) { //the root executor has no next step, then finish it
            ExecutionServices.getCallChainExecutorService().refreshExtensionLinks(instance);
            finishContext();
        } else {
            finishInstanceAsPassed(); //need to finish current callchain instance before running the next
            postNextCallChain();
        }
        destroy();
    }

    private void processDelay(TimeUnit timeUnit, long delay) throws InterruptedException {
        if (delay > 0) {
            if (timeUnit == null) {
                timeUnit = TimeUnit.SECONDS;
            }
            timeUnit.sleep(delay);
        }
    }

    private void postSituationStep(StepInstance stepInstance) {
        try {
            SituationStep situationStep = (SituationStep) stepInstance.getStep();
            Set<Situation> endSituations = situationStep.getEndSituations();
            Set<Situation> exceptionalSituations = situationStep.getExceptionalSituations();
            if (exceptionalSituations != null && !exceptionalSituations.isEmpty()) {
                registerSubscriberAndExceptionalSituations(stepInstance, exceptionalSituations);
            }
            if (endSituations != null && !endSituations.isEmpty()) {
                registerSubscriberAndEndSituations(stepInstance, endSituations, situationStep);
                StepExecutorFactory.executeStatic(stepInstance);
                return;
            }
            prepareAndExecuteSituationStep(stepInstance, situationStep);
        } catch (Exception e) {
            sendFailEventToTheSubscriber(this.getParentId() != null
                    ? this.getParentId()
                    : this.getId(), e);
        }
    }

    private void prepareAndExecuteSituationStep(StepInstance stepInstance,
                                                SituationStep situationStep) throws Exception {
        TemplateEngineFactory.get().process(situationStep, situationStep.getPreScript(),
                stepInstance.getContext(), "pre-script of CallChain Step");
        KeysRegenerator.getInstance()
                .regenerateKeys(stepInstance.getContext(), situationStep.getKeysToRegenerate());
        NextCallChainEvent callChainEvent = new NextCallChainEvent(this.getParentId(), instance);
        callChainEvent.setID(this.getId());
        try {
            IntegrationStep integrationStep = situationStep.getSituation().getIntegrationStep();
            addValidationRetryParametersToIntegrationStep(situationStep, integrationStep);
        } catch (Exception e) {
            LOGGER.info("Retry parameters weren't set to Integration Step for {} ", situationStep.getName());
        }
        ExecutionServices.getSituationExecutorService()
                .execute(situationStep.getSituation(), stepInstance.getContext(), situationStep.getSituation(),
                        callChainEvent);
    }

    private void registerSubscriberAndEndSituations(StepInstance stepInstance, Set<Situation> endSituations,
                                                    SituationStep situationStep) {
        StepEndSituationSubscriber subscriber = new StepEndSituationSubscriber(stepInstance.getContext().tc(),
                endSituations, this.getId(), situationStep.getWaitAllEndSituations());
        EventBusServiceProvider.getStaticReference().register(subscriber);
        String tcId = String.valueOf(stepInstance.getContext().tc().getID());
        for (Situation endSit : endSituations) {
            CacheServices.getAwaitingContextsCacheService()
                    .putIfAbsent(String.format("%s_%s", tcId, endSit.getID()), stepInstance.getStepId());
        }
    }

    private void registerSubscriberAndExceptionalSituations(StepInstance stepInstance,
                                                            Set<Situation> exceptionalSituations) {
        StepExceptionalSituationSubscriber subscriber = new StepExceptionalSituationSubscriber(
                stepInstance.getContext().tc(), exceptionalSituations, this.getId());
        EventBusServiceProvider.getStaticReference().register(subscriber);
        String tcId = String.valueOf(stepInstance.getContext().tc().getID());
        for (Situation exceptionalSituation : exceptionalSituations) {
            CacheServices.getAwaitingContextsCacheService()
                    .putIfAbsent(String.format("%s_%s", tcId, exceptionalSituation.getID()),
                            stepInstance.getStepId());
        }
    }

    private void executeNext(boolean forced) throws Exception {
        StepInstance stepInstance;
        boolean next;
        if (forced || iterator.current() == null) {
            if (iterator.hasNext()) {
                next = true;
            } else {
                finishInstance();
                return;
            }
        } else {
            // Check may be retry conditions are evaluated to false
            if (isAttemptsOrTimeAreOver(iterator.current())) {
                if (iterator.hasNext()) {
                    next = true;
                } else {
                    finishInstance();
                    return;
                }
            } else {
                next = false;
            }
        }
        stepInstance = (next)
                ? iterator.next()
                : iterator.current();
        if (validateStepCondition(stepInstance)) {
            if (next) {
                stepInstance.setStartTime(new Date());
            } else {
                AbstractCallChainStep callChainStep = (AbstractCallChainStep) stepInstance.getStep();
                LOGGER.info("Execute step again [iteration #{} of {}] due to retry conditions."
                                + "\nConditions: {}."
                                + "\nStepInstance: {}",
                        stepInstance.getCurrentCondAttemptValue() + 1,
                        callChainStep.getConditionMaxAttempts(),
                        callChainStep.getConditionParameters(),
                        stepInstance);
            }
            stepInstance.setCurrentCondAttemptValue(stepInstance.getCurrentCondAttemptValue() + 1);
            if (resumed) {
                reGetSituation(stepInstance);
            }
            executeStep(stepInstance);
        } else {
            executeNext(true);
        }
    }

    private void reGetSituation(StepInstance stepInstance) {
        if (stepInstance.getStep() instanceof SituationStep) {
            SituationStep step = (SituationStep) stepInstance.getStep();
            Situation situation = step.getSituation();
            if (situation != null && situation.getID() != null) {
                Situation newSituation = CoreObjectManager.getInstance().getManager(Situation.class)
                        .getById(situation.getID());
                step.setSituation(newSituation);
            }
        }
    }

    /*  Returns:
     *       true - if attempts or time are over (so we must proceed to the next step),
     *       false - otherwise (so we must retry current step)
     * */
    private boolean isAttemptsOrTimeAreOver(StepInstance stepInstance) {
        AbstractCallChainStep step = (AbstractCallChainStep) stepInstance.getStep();

        /* Variants are:
            1. max_attempts == 0 AND max_time == 0 AND no conditions ==> no retries at all
            2. max_attempts == 0 AND max_time == 0 AND some conditions ==> no 'explicit' retries but possible rerty
            by conditions
            3. max_attempts != 0 OR max_time != 0 ==> 'explicit' retries and possible rerties by conditions
         */
        if (!step.isConditionRetry()) {
            // Conditional retry functionality is turned off. Attempt #0 must be allowed only
            return (stepInstance.getCurrentCondAttemptValue() > 0);
        }
        if (step.getConditionMaxAttempts() == 0 && step.getConditionMaxTime() == 0) {
            if (step.getConditionParameters() == null || step.getConditionParameters().isEmpty()) {
                return true;
            } else {
                // Infinite loop protection (Temporary?); conditions will be evaluated later
                return infiniteLoopProtectionBarrier > 0 && stepInstance
                        .getCurrentCondAttemptValue() >= infiniteLoopProtectionBarrier;
            }
        } else {
            if (step.getConditionMaxAttempts() > 0 && stepInstance
                    .getCurrentCondAttemptValue() >= step.getConditionMaxAttempts()) {
                return true; // attempts are over
            }
            // return true if time is over; otherwise return false
            return step.getConditionMaxTime() > 1 && step.getConditionMaxTime() <= step.retrieveConditionUnitMaxTime()
                    .convert(countPassedTime(stepInstance), TimeUnit.MILLISECONDS);
        }
    }

    private void executeStep(StepInstance stepInstance) throws Exception {
        TcContext tc = instance.getContext().tc();
        if (stepInstance.getStep().isManual() || Status.PAUSED.equals(tc.getStatus()) || tc.isRunStepByStep()) {
            if (Status.IN_PROGRESS.equals(tc.getStatus())) {
                ExecutionServices.getTCContextService().pause(tc);
            }
            if (stepInstance.getStep() instanceof EmbeddedStep) {
                postNextEmbeddedStep(stepInstance);
            } else if (stepInstance.getStep() instanceof SituationStep) {
                addSituationStepToDeferredExecution(stepInstance);
            }
        } else {
            processDelay(stepInstance.getStep().retrieveUnit(), stepInstance.getStep().getDelay());
            if (stepInstance.getStep() instanceof EmbeddedStep) {
                postNextEmbeddedStep(stepInstance);
            } else if (stepInstance.getStep() instanceof SituationStep) {
                postSituationStep(stepInstance);
            }
        }
    }

    private boolean validateStepCondition(StepInstance stepInstance) {
        AbstractCallChainStep step = (AbstractCallChainStep) stepInstance.getStep();
        List<ConditionParameter> conditionParameters = step.getConditionParameters();
        return !step.isConditionRetry() || conditionParameters == null || conditionParameters.isEmpty()
                || ConditionsHelper.isApplicable(stepInstance.getContext(), conditionParameters);
    }

    private void postNextEmbeddedStep(StepInstance stepInstance) {
        NextEmbeddedStepEvent stepEvent = new NextEmbeddedStepEvent(this.getId(), stepInstance);
        stepEvent.setDataSetName(instance.getDatasetName());
        NextEmbeddedStepSubscriber stepSubscriber = new NextEmbeddedStepSubscriber(stepEvent.getID(), this.getId());
        subscribeAndPostEvent(stepSubscriber, stepEvent);
    }

    private void postNextCallChain() {
        NextCallChainEvent callChainEvent = new NextCallChainEvent(this.getParentId(), instance);
        callChainEvent.setID(this.getParentId());
        EventBusServiceProvider.getStaticReference().post(callChainEvent);
    }

    private void finishInstanceAsPassed() {
        instance.setStatus(Status.PASSED);
        instance.setEndTime(new Date());
        LOGGER.info("Call chain {} executed", instance);
        EventBusServiceProvider.getStaticReference().post(new CallChainEvent.Finish(instance));
    }

    private void finishInstanceAsFailed() {
        instance.setStatus(Status.FAILED);
        instance.setEndTime(new Date());
        LOGGER.error("Call chain {} failed", instance);
        EventBusServiceProvider.getStaticReference().post(new CallChainEvent.Terminate(instance));
    }

    private void finishContext() {
        TcContext tc = instance.getContext().tc();
        if (tc == null) {
            finishInstanceAsPassed();
        } else {
            if (tc.getStatus().equals(Status.FAILED)) {
                ExecutionServices.getExecutionProcessManagerService().fail(tc);
                finishInstanceAsFailed();
            } else {
                ExecutionServices.getExecutionProcessManagerService().finish(tc);
                finishInstanceAsPassed();
            }
        }
    }

    private void addSituationStepToDeferredExecution(StepInstance stepInstance) throws Exception {
        SituationStep situationStep = (SituationStep) stepInstance.getStep();
        SituationInstance situationInstance = ExecutionServices.getSituationExecutorService()
                .prepare(situationStep.getSituation(), stepInstance.getContext());
        situationInstance.getSituationById().fillKeysToRegenerate(situationStep.getKeysToRegenerate());
        DefferedSituationInstanceHolder.getInstance()
                .add(situationInstance.getContext().tc().getID(), situationInstance);
        Set<Situation> endSituations = situationStep.getEndSituations();
        if (endSituations != null && !endSituations.isEmpty()) {
            StepEndSituationSubscriber subscriber = new StepEndSituationSubscriber(stepInstance.getContext().tc(),
                    endSituations, this.getId(), situationStep.getWaitAllEndSituations());
            EventBusServiceProvider.getStaticReference().register(subscriber);
            NextCallChainEventSubscriberHolder.getInstance()
                    .add(stepInstance.getContext().getTC().getID(), this.getId(), this.getParentId(), false);
        } else {
            NextCallChainEventSubscriberHolder.getInstance()
                    .add(stepInstance.getContext().getTC().getID(), this.getId(), this.getParentId(), true);
        }
        LOGGER.warn(String.format("Situation %s is paused", situationInstance));
    }

    private void failInstance(AbstractContainerInstance instance, Exception e) {
        ExecutionServices.getCallChainExecutorService().refreshExtensionLinks((CallChainInstance) instance);
        instance.setError(e);
        instance.setStatus(Status.FAILED);
        instance.setEndTime(new Date());
        EventBusServiceProvider.getStaticReference().post(new CallChainEvent.Terminate((CallChainInstance) instance));
    }

    private long countPassedTime(StepInstance stepInstance) {
        return (new Date()).getTime() - stepInstance.getStartTime().getTime();
    }

    private void addValidationRetryParametersToIntegrationStep(SituationStep situationStep,
                                                               IntegrationStep integrationStep) {
        integrationStep.setRetryOnFail(situationStep.isRetryOnFail());
        integrationStep.setRetryTimeout(situationStep.getRetryTimeout());
        integrationStep.setRetryTimeoutUnit(situationStep.getRetryTimeoutUnit());
        integrationStep.setValidationMaxAttempts(situationStep.getValidationMaxAttempts());
        integrationStep.setValidationMaxTime(situationStep.getValidationMaxTime());
        integrationStep.setValidationUnitMaxTime(situationStep.getValidationUnitMaxTime());
    }

    protected void unregisterIfExpired() {
        TcContext tcContext = this.instance.getContext().tc();
        long lastAccess = tcContext.getLastUpdateTime();
        long failTimeout = tcContext.getTimeToLive();
        if (lastAccess > 0 && (lastAccess + failTimeout) <= System.currentTimeMillis()) {
            LOGGER.error(
                    "Subscriber {} [instanceId={}, tcContextId={}] is expired and it will be unregistered (the "
                            + "latest event was at {})", getClass().getSimpleName(), this.instance.getID(),
                    tcContext.getID(), new Date(lastAccess));
            destroy();
        }
    }

    @NotNull
    @Override
    protected String getTenantId(NextCallChainEvent event) {
        return event.getInstance().getContext().getProjectUuid().toString();
    }
}
