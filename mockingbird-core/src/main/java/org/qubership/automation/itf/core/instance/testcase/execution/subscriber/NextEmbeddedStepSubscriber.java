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

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.model.event.CallChainEvent;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.NextEmbeddedStepEvent;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.regenerator.KeysRegenerator;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.provider.EventBusServiceProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class NextEmbeddedStepSubscriber extends AbstractChainSubscriber<NextEmbeddedStepEvent> {

    public NextEmbeddedStepSubscriber(String id, String parentId) {
        super(id, parentId);
    }

    @Override
    protected void unregisterIfExpired() {
        //method is empty and should not be executed
        //NextEmbeddedStepSubscriber is destroyed in onEvent() method
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handle(NextEmbeddedStepEvent event) {
        handleEvent(event);
    }

    @Override
    protected void onEvent(NextEmbeddedStepEvent event) throws Exception {
        TenantContext.setTenantInfo(getTenantId(event));
        StepInstance instance = event.getInstance();
        EmbeddedStep step = (EmbeddedStep) instance.getStep();
        CallChain callChain = CoreObjectManager.getInstance().getManager(CallChain.class)
                .getById(step.getChain().getID());
        TcContext tcContext = instance.getContext().tc();
        try {
            modifyContext(instance.getContext(), step);
            CallChainInstance chainInstance = ExecutionServices.getCallChainExecutorService()
                    .prepare(tcContext.getProjectId(), tcContext.getProjectUuid(), callChain, tcContext,
                            tcContext.getEnvironmentById(), null, null);
            chainInstance.setDatasetName(event.getDataSetName());
            chainInstance.setStartTime(new Date());
            NextCallChainEvent chainEvent = new NextCallChainEvent(getParentId(), chainInstance);
            NextCallChainSubscriber subscriber = new NextCallChainSubscriber(chainEvent);
            subscriber.registerSubscriberInHolder();
            EventBusServiceProvider.getStaticReference().register(subscriber);
            EventBusServiceProvider.getStaticReference().post(new CallChainEvent.Start(chainInstance));
            EventBusServiceProvider.getStaticReference().post(chainEvent);
        } catch (Exception exc) {
            EventBusServiceProvider.getStaticReference().post(new NextCallChainEvent.Fail(this.getParentId(), exc));
        } finally {
            destroy();
        }
    }

    @NotNull
    @Override
    protected String getTenantId(NextEmbeddedStepEvent event) {
        return event.getInstance().getContext().getProjectUuid().toString();
    }

    private void modifyContext(final InstanceContext context, final EmbeddedStep step) {
        TxExecutor.executeUnchecked(() -> {
            TemplateEngineFactory.get().process(step, step.getPreScript(), context,
                    "pre-script of CallChain Step '" + step.getName() + "'");
            KeysRegenerator.getInstance().regenerateKeys(context,
                    CoreObjectManager.getInstance().getManager(EmbeddedStep.class).getById(step.getID())
                            .getKeysToRegenerate());
            return null;
        });
    }
}
