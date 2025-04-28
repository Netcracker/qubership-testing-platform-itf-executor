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

package org.qubership.automation.itf.core.execution;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.qubership.automation.itf.core.instance.testcase.execution.subscriber.NextCallChainSubscriber;
import org.qubership.automation.itf.core.model.event.CallChainEvent;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.iterator.CallChainStepIterator;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

@Service
public class DebugExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugExecutor.class);
    private EventBusProvider eventBusProvider;

    @Autowired
    public DebugExecutor(EventBusProvider eventBusProvider) {
        this.eventBusProvider = eventBusProvider;
    }

    public TcContext executeCallChainBeginStep(TcContext tcContext, Step step) {
        return executeCallChainBeginStep(tcContext, CallChainInstance.class.cast(tcContext.getInitiator()),
                SituationStep.class.cast(step));
    }

    private TcContext executeCallChainBeginStep(TcContext tcContext, CallChainInstance instance,
                                                SituationStep beginnerSituationStep) {
        ExecutorServiceProviderFactory.get().requestForRegular()
                .submit(() -> execute(tcContext, instance, beginnerSituationStep));
        return instance.getContext().tc();
    }

    private void execute(TcContext tcContext, CallChainInstance instance, SituationStep beginnerSituationStep) {
        String threadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName("[" + instance.getContext().tc().getID() + "] " + instance.getName());
            TxExecutor.execute((Callable<Void>) () -> {
                _execute(tcContext, beginnerSituationStep);
                return null;
            });
        } catch (Exception t) {
            LOGGER.error("Error executing in separate thread {}", instance, t);
        } finally {
            Thread.currentThread().setName(threadName);
        }
    }

    private void _execute(TcContext tcContext, SituationStep beginnerSituationStep) {
        CallChainInstance instance = reviveContextAndGetInstance(tcContext);
        LOGGER.info("Executing Call Chain {}...", instance);
        reviveAndPostInstance(instance, beginnerSituationStep);
    }

    private CallChainInstance reviveContextAndGetInstance(TcContext tcContext) {
        CacheServices.getTcBindingCacheService().bind(tcContext);
        CallChainInstance instance = (CallChainInstance) tcContext.getInitiator();
        if (tcContext.getInitiator().getContext() == null) {
            tcContext.getInitiator().setContext(new InstanceContext());
        }
        tcContext.getInitiator().getContext().setTC(tcContext);
        tcContext.setStartedFrom(StartedFrom.ITF_UI);
        instance.setStatus(Status.IN_PROGRESS);
        instance.setStartTime(new Date());
        ExecutionServices.getTCContextService().prolong(instance.getContext().tc());
        return instance;
    }

    private void reviveAndPostInstance(CallChainInstance instance, SituationStep beginnerSituationStep) {
        ExecutionServices.getCallChainExecutorService().runBeforeIntegrations(instance);
        eventBusProvider.post(new CallChainEvent.Start(instance));
        List<Step> steps = Lists.newArrayList();
        NextCallChainEvent nextCallChainEvent = new NextCallChainEvent(null, instance);
        CallChain callChain = CoreObjectManager.getInstance().getManager(CallChain.class)
                .getById(instance.getTestCaseId());
        searchingPointOfEntrySteps(callChain, beginnerSituationStep, steps);
        CallChainStepIterator stepIterator = new CallChainStepIterator(callChain, instance, steps);
        NextCallChainSubscriber subscriber = new NextCallChainSubscriber(nextCallChainEvent, stepIterator);
        eventBusProvider.register(subscriber, EventBusProvider.Priority.HIGH);
        eventBusProvider.post(nextCallChainEvent);
    }

    private List<Step> searchingPointOfEntrySteps(CallChain parent, SituationStep situationStep, List<Step> steps) {
        boolean currentStepFound = false;
        for (Step step : parent.getSteps()) {
            if (step instanceof SituationStep) {
                if (situationStep.equals(step)) {
                    currentStepFound = true;
                }
                if (currentStepFound) {
                    steps.add(step);
                }
            } else {
                if (step instanceof EmbeddedStep) {
                    if (((EmbeddedStep) step).getChain() != null) { //This is a crutch. Chain can't be empty.
                        searchingPointOfEntrySteps(((EmbeddedStep) step).getChain(), situationStep, steps);
                    }
                }
            }
        }
        return steps;
    }
}
