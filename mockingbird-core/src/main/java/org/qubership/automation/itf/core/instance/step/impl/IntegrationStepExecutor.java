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

import java.util.Date;
import java.util.Objects;

import org.qubership.automation.itf.core.instance.step.StepExecutor;
import org.qubership.automation.itf.core.model.event.StepEvent;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IntegrationStepExecutor implements StepExecutor {

    public static final Logger LOGGER = LoggerFactory.getLogger(IntegrationStepExecutor.class);

    private final EventBusProvider eventBusProvider;
    private final IntegrationStepHelper integrationStepHelper;

    @Autowired
    public IntegrationStepExecutor(EventBusProvider eventBusProvider,
                                   IntegrationStepHelper integrationStepHelper) {
        this.eventBusProvider = eventBusProvider;
        this.integrationStepHelper = integrationStepHelper;
    }

    public void execute(AbstractInstance abstractInstance) throws Exception {
        final StepInstance stepInstance = (StepInstance) abstractInstance;
        IntegrationStep step = (IntegrationStep) stepInstance.getStep();
        String stepIdentity = String
                .format("Step '%s' in '%s'", stepInstance.getStep().getName(), stepInstance.getParent());
        if (!step.isEnabled()) {
            eventBusProvider.post(new StepEvent.Skip(stepInstance));
            LOGGER.info("{} - disabled ==> skipped", stepIdentity);
            return;
        }
        LOGGER.info("{}: execution is started", stepIdentity);
        if (step.getUnit() != null && step.getDelay() > 0) {
            String timeUnit = step.getUnit().toLowerCase();
            LOGGER.info("{}: waiting for timeout '{}' {}...", stepIdentity, step.getDelay(), step.getUnit());
            Report.info(stepInstance.getParent(), String.format("Timeout for [%s]", step.getName()),
                    String.format("Waiting for [%d] %s", step.getDelay(), timeUnit));
            Thread.sleep(step.retrieveUnit().toMillis(step.getDelay()));
            LOGGER.info("{}: timeout '{}' {} is elapsed", stepIdentity, step.getDelay(), step.getUnit());
            Report.info(stepInstance.getParent(), String.format("Timeout for [%s]", step.getName()),
                    String.format("Timeout is elapsed: [%d] %s", step.getDelay(), timeUnit));
        }
        if (stepInstance.getContext().sp() == null) {
            stepInstance.getContext().setSP(new SpContext(stepInstance));
        }
        stepInstance.getParent().getStepInstances().add(stepInstance);
        stepInstance.setStatus(Status.IN_PROGRESS);
        stepInstance.setStartTime(new Date());
        /*
            Sending of StepEvent.Start is turned off, because is not processed at all (and not planned).
            To be deleted soon.
         */
        // eventBusProvider.post(new StepEvent.Start(stepInstance));
        try {
            Mep mep = step.getMep();
            switch (mep) {
                case OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS:
                    integrationStepHelper.sendReceiveSync(stepInstance);
                    break;
                case OUTBOUND_REQUEST_ASYNCHRONOUS:
                case INBOUND_RESPONSE_ASYNCHRONOUS:
                    integrationStepHelper.sendRequest(stepInstance);
                    break;
                case OUTBOUND_RESPONSE_ASYNCHRONOUS:
                case INBOUND_REQUEST_ASYNCHRONOUS:
                case INBOUND_REQUEST_SYNCHRONOUS:
                    break;
                case INBOUND_REQUEST_RESPONSE_SYNCHRONOUS:
                    integrationStepHelper.checkErrors(stepInstance);
                    if (((IntegrationStep) stepInstance.getStep()).returnStepTemplate() != null) {
                        integrationStepHelper.sendSyncResponse(stepInstance);
                    }
                    break;
                case INBOUND_RESPONSE_SYNCHRONOUS:
                    integrationStepHelper.sendSyncResponse(stepInstance);
                    break;
            }
            integrationStepHelper.processOutgoingContextKeys(stepInstance);
            stepInstance.setStatus(Objects.nonNull(stepInstance.getError()) ? Status.FAILED : Status.PASSED);
            stepInstance.setEndTime(new Date());
            LOGGER.info("{}: executed successfully", stepIdentity);
            if (!stepInstance.isRetryStep()) {
                eventBusProvider.post(new StepEvent.Finish(stepInstance));
            }
        } catch (Exception e) {
            stepInstance.setError(e);
            stepInstance.setStatus(Status.FAILED);
            stepInstance.setEndTime(new Date());
            eventBusProvider.post(new StepEvent.Terminate(stepInstance));
            throw e;
        }
    }

    @Override
    public void execute(AbstractInstance step, JsonContext customDataset) throws Exception {
        execute(step);
    }
}
