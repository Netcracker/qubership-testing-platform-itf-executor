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

package org.qubership.automation.itf.executor.event.trigger;

import java.util.List;

import org.qubership.automation.itf.core.model.condition.ConditionsHelper;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventTriggerService {

    public static final Logger LOGGER = LoggerFactory.getLogger(EventTriggerService.class);

    protected static void executeSituationBySituationEventTrigger(SituationEvent event,
                                                                  SituationEventTrigger situationEventTrigger) {
        Situation parent = situationEventTrigger.getParent();
        SituationInstance situationInstance = event.getSituationInstance();
        List<ConditionParameter> conditionParameters = situationEventTrigger.getConditionParameters();
        InstanceContext instanceContext = situationInstance.getContext();
        LOGGER.info("Event received by situation [{}], processing under {} {}...", situationInstance.getStepContainer(),
                parent == null ? "" : parent.getClass().getSimpleName(),
                parent == null ? "" : parent.getName());
        if (!instanceContext.tc().isFinished()) {
            if (conditionParameters == null || conditionParameters.isEmpty()
                    || ConditionsHelper.isApplicable(instanceContext, conditionParameters)) {
                if (conditionParameters == null || conditionParameters.isEmpty()) {
                    LOGGER.info("Condition is empty, applicable anyway");
                } else {
                    LOGGER.info("Conditions are applicable, handling under {} {}...",
                            parent == null ? "" : parent.getClass().getSimpleName(),
                            parent == null ? "" : parent.getName());
                }
                if (parent != null) {
                    try {
                        ExecutionServices.getSituationExecutorService().execute(parent, instanceContext);
                    } catch (Exception e) {
                        LOGGER.error(String.format("Error executing situation %s", parent), e);
                    }
                } else {
                    LOGGER.warn("Run situation handler was called, but situation to execute is null");
                }
            } else {
                LOGGER.info("Condition property is not applicable, skip under {} {}",
                        parent == null ? "" : parent.getClass().getSimpleName(),
                        parent == null ? "" : parent.getName());
            }
        } else {
            LOGGER.info("Event was rejected due to context '{}' is closed", instanceContext.tc().getName());
        }
    }
}
