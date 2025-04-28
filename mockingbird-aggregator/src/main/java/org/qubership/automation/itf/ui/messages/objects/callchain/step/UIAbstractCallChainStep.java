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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qubership.automation.itf.core.model.condition.ConditionsHelper;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.jpa.step.AbstractCallChainStep;
import org.qubership.automation.itf.core.util.Pair;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UITypedObject;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UICondition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Lists;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = UIEmbeddedChainStep.class, name = "embeddedChainStep"), @JsonSubTypes.Type(
        value = UISituationStep.class,
        name = "situationStep")})
public abstract class UIAbstractCallChainStep extends UITypedObject {

    private long delay;
    private String unit;
    private boolean enabled;
    private boolean manual;
    private List<Pair<String, String>> keysToRegenerate = Lists.newArrayList();
    private String stepName;
    private int conditionMaxAttempts;
    private long conditionMaxTime;
    private String conditionUnitMaxTime;
    private boolean conditionRetry;
    private List<UICondition> conditions;
    private String preScript;
    private int order;

    public UIAbstractCallChainStep() {
    }

    public UIAbstractCallChainStep(AbstractCallChainStep step) {
        super(step, true);
        fillUiAbstractCallChainStep(step, true);
    }

    public UIAbstractCallChainStep(AbstractCallChainStep step, boolean isFullWithParent) {
        super(step, isFullWithParent);
        fillUiAbstractCallChainStep(step, isFullWithParent);
    }

    private void fillUiAbstractCallChainStep(AbstractCallChainStep step, boolean isFullWithParent) {
        setOrder(step.getOrder());
        setDelay(step.getDelay());
        setUnit(step.getUnit());
        setEnabled(step.isEnabled());
        setManual(step.isManual());
        for (Map.Entry<String, String> entry : step.getKeysToRegenerate().entrySet()) {
            this.keysToRegenerate.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public void updateObject(AbstractCallChainStep step) {
        super.updateObject(step);
        step.setEnabled(getEnabled());
        step.setManual(getManual());
        step.setDelay(getDelay());
        step.setUnit(getUnit());
        step.getKeysToRegenerate().clear();
        if (this.keysToRegenerate != null) {
            for (Pair<String, String> regenerationKey : this.keysToRegenerate) {
                step.addKeyToRegenerate(regenerationKey.getKey(), regenerationKey.getValue());
            }
        }
        step.setPreScript(getPreScript());
        _updateStep(step);
    }

    public List<Pair<String, String>> getKeysToRegenerate() {
        return keysToRegenerate;
    }

    public void setKeysToRegenerate(List<Pair<String, String>> keysToRegenerate) {
        this.keysToRegenerate = keysToRegenerate;
    }

    public abstract void _updateStep(AbstractCallChainStep step);

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public int getConditionMaxAttempts() {
        return conditionMaxAttempts;
    }

    public void setConditionMaxAttempts(int conditionMaxAttempts) {
        this.conditionMaxAttempts = conditionMaxAttempts;
    }

    public long getConditionMaxTime() {
        return conditionMaxTime;
    }

    public void setConditionMaxTime(long conditionMaxTime) {
        this.conditionMaxTime = conditionMaxTime;
    }

    public boolean isConditionRetry() {
        return conditionRetry;
    }

    public void setConditionRetry(boolean conditionRetry) {
        this.conditionRetry = conditionRetry;
    }

    public String getConditionUnitMaxTime() {
        return conditionUnitMaxTime;
    }

    public void setConditionUnitMaxTime(String conditionUnitMaxTime) {
        this.conditionUnitMaxTime = conditionUnitMaxTime;
    }

    public List<UICondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<UICondition> conditions) {
        this.conditions = conditions;
    }

    public String getPreScript() {
        return preScript;
    }

    public void setPreScript(String preScript) {
        this.preScript = preScript;
    }

    protected void processConditions(AbstractCallChainStep step) {
        if (getConditions() != null) {
            List<ConditionParameter> conditionParameters = (step.getConditionParameters() == null)
                    ? new ArrayList<>() : step.getConditionParameters();
            ConditionsHelper.fillConditionParameters(conditionParameters,
                    ControllerHelper.toConditionParameters(getConditions()));
            step.setConditionParameters(conditionParameters);

        } else {
            if (step.getConditionParameters() != null) {
                step.setConditionParameters(new ArrayList<>());
            }
        }
    }

    protected void fillConditionsFromStep(AbstractCallChainStep step) {
        List<ConditionParameter> conditionParameters = step.getConditionParameters();
        if (conditionParameters != null && !conditionParameters.isEmpty()) {
            List<UICondition> uiConditions = conditionParameters.stream().map(UICondition::new)
                    .sorted((c1, c2) -> c1.getOrderId() > c2.getOrderId() ? 1 : -1).collect(Collectors.toList());
            setConditions(uiConditions);
        }
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
