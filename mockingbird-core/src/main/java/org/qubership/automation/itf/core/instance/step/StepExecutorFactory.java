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

package org.qubership.automation.itf.core.instance.step;

import java.util.Map;

import org.qubership.automation.itf.core.instance.step.impl.IntegrationStepExecutor;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

@Component
public class StepExecutorFactory {

    private static Map<Class<? extends Step>, StepExecutor> executorBindings = Maps.newHashMapWithExpectedSize(10);
    private IntegrationStepExecutor integrationStepExecutor;
    private SituationStepExecutor situationStepExecutor;

    @Autowired
    public StepExecutorFactory(IntegrationStepExecutor integrationStepExecutor,
                               SituationStepExecutor situationStepExecutor) {
        this.integrationStepExecutor = integrationStepExecutor;
        this.situationStepExecutor = situationStepExecutor;

        executorBindings.put(IntegrationStep.class, integrationStepExecutor);
        executorBindings.put(SituationStep.class, situationStepExecutor);
    }

    public static void executeStatic(StepInstance stepInstance) throws Exception {
        getExecutor(stepInstance).execute(stepInstance);
    }

    private static StepExecutor getExecutor(StepInstance stepInstance) throws Exception {
        for (Map.Entry<Class<? extends Step>, StepExecutor> entry : executorBindings.entrySet()) {
            if (entry.getKey().isAssignableFrom(stepInstance.getStep().getClass())) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException(String.format("No executor found for stepInstance [%s], type [%s]",
                stepInstance.getStep(), stepInstance.getStep().getClass().getSimpleName()));
    }

    public void execute(StepInstance stepInstance) throws Exception {
        getExecutor(stepInstance).execute(stepInstance);
    }
}
