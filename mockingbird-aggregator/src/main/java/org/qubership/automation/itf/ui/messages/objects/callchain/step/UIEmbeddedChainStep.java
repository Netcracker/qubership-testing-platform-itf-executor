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

package org.qubership.automation.itf.ui.messages.objects.callchain.step;

import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.step.AbstractCallChainStep;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;

public class UIEmbeddedChainStep extends UIAbstractCallChainStep {

    private UIObject embeddedChain;

    public UIEmbeddedChainStep() {
        setType("embeddedChainStep");
    }

    public UIEmbeddedChainStep(EmbeddedStep step) {
        super(step, true);
        fillUiEmbeddedChainStep(step, true);
    }

    public UIEmbeddedChainStep(EmbeddedStep step, boolean isFullWithParent) {
        super(step, isFullWithParent);
        fillUiEmbeddedChainStep(step, isFullWithParent);
    }

    private void fillUiEmbeddedChainStep(EmbeddedStep step, boolean isFullWithParent) {
        setType("embeddedChainStep");
        if (step.getChain() != null) {
            setEmbeddedChain(new UIObject(step.getChain(), isFullWithParent));
        }
        fillConditionsFromStep(step);
        // Set conditional retry properties
        setConditionRetry(step.isConditionRetry());
        setConditionMaxAttempts(step.getConditionMaxAttempts());
        setConditionMaxTime(step.getConditionMaxTime());
        setConditionUnitMaxTime(step.getConditionUnitMaxTime());
        //Set Pre-Script
        setPreScript(step.getPreScript());
    }

    @Override
    public void _updateStep(AbstractCallChainStep step) {
        EmbeddedStep embeddedStep = (EmbeddedStep) step;
        if (embeddedChain != null) {
            CallChain callChain =
                    CoreObjectManager.getInstance().getManager(CallChain.class).getById(embeddedChain.getId());
            ControllerHelper.throwExceptionIfNull(callChain, embeddedChain.getName(), embeddedChain.getId(),
                    CallChain.class, "get CallChain by id");
            embeddedStep.setChain(callChain);
        } else {
            embeddedStep.setChain(null);
        }
        // Set conditional retry properties
        embeddedStep.setConditionRetry(isConditionRetry());
        embeddedStep.setConditionMaxAttempts(getConditionMaxAttempts());
        embeddedStep.setConditionMaxTime(getConditionMaxTime());
        embeddedStep.setConditionUnitMaxTime(getConditionUnitMaxTime());
        // Process conditions array
        processConditions(embeddedStep);
    }

    public UIObject getEmbeddedChain() {
        return embeddedChain;
    }

    public void setEmbeddedChain(UIObject embeddedChain) {
        this.embeddedChain = embeddedChain;
    }
}
