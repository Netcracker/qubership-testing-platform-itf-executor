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

import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.regenerator.KeysRegenerator;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.springframework.stereotype.Component;

@Component
public class SituationStepExecutor implements StepExecutor {

    @Override
    public void execute(AbstractInstance step) throws Exception {
        SituationStep situationStep = (SituationStep) ((StepInstance) step).getStep();
        TemplateEngineFactory.get().process(situationStep, situationStep.getPreScript(), step.getContext(),
                "pre-script on the CallChain Step");
        KeysRegenerator.getInstance().regenerateKeys(step.getContext(), situationStep.getKeysToRegenerate());
        ExecutionServices.getSituationExecutorService()
                .execute(situationStep.getSituation(), step.getContext(), step.getSource(), null);
    }

    @Override
    public void execute(AbstractInstance step, JsonContext customDataset) throws Exception {
        execute(step);
    }
}
