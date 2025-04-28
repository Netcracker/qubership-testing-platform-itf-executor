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

import java.util.Collection;

import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.jpa.step.AbstractCallChainStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;

import com.google.common.collect.Sets;

public class UISituationStep extends UIAbstractCallChainStep {

    private UIObject situation;
    private Collection<UIObject> endSituations = Sets.newHashSetWithExpectedSize(2);
    private Collection<UIObject> exceptionalSituation = Sets.newHashSetWithExpectedSize(5);
    private boolean waitAllEndSituations;

    private boolean retryOnFail;
    private long retryTimeout;
    private String retryTimeoutUnit;
    private int validationMaxAttempts;
    private long validationMaxTime;
    private String validationUnitMaxTime;

    public UISituationStep() {
        setType("situationStep");
    }

    public UISituationStep(SituationStep step) {
        super(step, true);
        fillUiSituationStep(step, true);
    }

    public UISituationStep(SituationStep step, boolean isFullWithParent) {
        super(step, isFullWithParent);
        fillUiSituationStep(step, isFullWithParent);
    }

    public UISituationStep(SituationStep step, String stepName) {
        super(step, true);
        fillUiSituationStep(step, true);
        setStepName(stepName);
    }

    public UISituationStep(SituationStep step, String stepName, boolean isFullWithParent) {
        super(step, isFullWithParent);
        fillUiSituationStep(step, isFullWithParent);
        setStepName(stepName);

    }

    private void fillUiSituationStep(SituationStep step, boolean isFullWithParent) {
        setType("situationStep");
        if (step.getSituation() != null) {
            setSituation(new UIObject(step.getSituation(), isFullWithParent));
        }
        if (!step.getEndSituations().isEmpty()) {
            for (Situation situation : step.getEndSituations()) {
                endSituations.add(new UIObject(situation, isFullWithParent));
            }
        }
        waitAllEndSituations = step.getWaitAllEndSituations();
        if (!step.getExceptionalSituations().isEmpty()) {
            for (Situation situation : step.getExceptionalSituations()) {
                exceptionalSituation.add(new UIObject(situation, isFullWithParent));
            }
        }
        fillConditionsFromStep(step);
        fillConditionRetryPropsFromStep(step);
        fillValidationRetryPropsFromStep(step);
        setPreScript(step.getPreScript());
    }

    private void fillConditionRetryPropsFromStep(SituationStep step) {
        // Set conditional retry properties
        setConditionRetry(step.isConditionRetry());
        setConditionMaxAttempts(step.getConditionMaxAttempts());
        setConditionMaxTime(step.getConditionMaxTime());
        setConditionUnitMaxTime(step.getConditionUnitMaxTime());
    }

    private void fillValidationRetryPropsFromStep(SituationStep step) {
        // Set validation retry properties
        setRetryOnFail(step.isRetryOnFail());
        setRetryTimeout(step.getRetryTimeout());
        setRetryTimeoutUnit(step.getRetryTimeoutUnit());
        setValidationMaxAttempts(step.getValidationMaxAttempts());
        setValidationMaxTime(step.getValidationMaxTime());
        setValidationUnitMaxTime(step.getValidationUnitMaxTime());
    }

    @Override
    public void _updateStep(AbstractCallChainStep step) {
        SituationStep situationStep = (SituationStep) step;
        ObjectManager<Situation> manager = CoreObjectManager.getInstance().getManager(Situation.class);
        // Process situation on which the step is based
        if (situation != null && situation.getId() != null) {
            Situation situation = manager.getById(this.situation.getId());
            ControllerHelper.throwExceptionIfNull(situation, this.situation.getName(), this.situation.getId(),
                    Situation.class, "get Situation by id");
            situationStep.setSituation(situation);
        } else {
            situationStep.setSituation(null);
        }
        // Process end-Situations array
        situationStep.getEndSituations().clear();
        if (endSituations != null && !(endSituations.isEmpty())) {
            for (UIObject uiSituation : endSituations) {
                // Do NOT add null/empty situation to array
                if (uiSituation == null || uiSituation.getId() == null) {
                    continue;
                }
                Situation thisSituation = manager.getById(uiSituation.getId());
                ControllerHelper.throwExceptionIfNull(thisSituation, uiSituation.getName(), uiSituation.getId(),
                        Situation.class, "get End Situation by id");
                situationStep.getEndSituations().add(thisSituation);
            }
            situationStep.setWaitAllEndSituations(this.getWaitAllEndSituations());
        }
        // Process exceptional-Situations array
        situationStep.getExceptionalSituations().clear();
        if (exceptionalSituation != null && !(exceptionalSituation.isEmpty())) {
            for (UIObject uiSituation : exceptionalSituation) {
                // Do NOT add null/empty situation to array
                if (uiSituation == null || uiSituation.getId() == null) {
                    continue;
                }
                Situation thisSituation = manager.getById(uiSituation.getId());
                ControllerHelper.throwExceptionIfNull(thisSituation, uiSituation.getName(), uiSituation.getId(),
                        Situation.class, "get Exceptional Situation by id");
                situationStep.getExceptionalSituations().add(thisSituation);
            }
        }
        // Set conditional retry properties
        situationStep.setConditionRetry(isConditionRetry());
        situationStep.setConditionMaxAttempts(getConditionMaxAttempts());
        situationStep.setConditionMaxTime(getConditionMaxTime());
        situationStep.setConditionUnitMaxTime(getConditionUnitMaxTime());
        // Set validation retry properties
        situationStep.setRetryOnFail(isRetryOnFail());
        situationStep.setRetryTimeout(getRetryTimeout());
        situationStep.setRetryTimeoutUnit(getRetryTimeoutUnit());
        situationStep.setValidationMaxAttempts(getValidationMaxAttempts());
        situationStep.setValidationMaxTime(getValidationMaxTime());
        situationStep.setValidationUnitMaxTime(getValidationUnitMaxTime());
        // Process conditions array
        processConditions(situationStep);
    }

    public UIObject getSituation() {
        return situation;
    }

    public void setSituation(UIObject situation) {
        this.situation = situation;
    }

    public Collection<UIObject> getEndSituations() {
        return endSituations;
    }

    public void setEndSituations(Collection<UIObject> endSituations) {
        this.endSituations = endSituations;
    }

    public Collection<UIObject> getExceptionalSituation() {
        return exceptionalSituation;
    }

    public void setExceptionalSituation(Collection<UIObject> exceptionalSituation) {
        this.exceptionalSituation = exceptionalSituation;
    }

    public boolean getWaitAllEndSituations() {
        return waitAllEndSituations;
    }

    public void setWaitAllEndSituations(boolean waitAllEndSituations) {
        this.waitAllEndSituations = waitAllEndSituations;
    }

    public boolean isRetryOnFail() {
        return retryOnFail;
    }

    public void setRetryOnFail(boolean retryOnFail) {
        this.retryOnFail = retryOnFail;
    }

    public long getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(long retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

    public String getRetryTimeoutUnit() {
        return retryTimeoutUnit;
    }

    public void setRetryTimeoutUnit(String retryTimeoutUnit) {
        this.retryTimeoutUnit = retryTimeoutUnit;
    }

    public int getValidationMaxAttempts() {
        return validationMaxAttempts;
    }

    public void setValidationMaxAttempts(int validationMaxAttempts) {
        this.validationMaxAttempts = validationMaxAttempts;
    }

    public long getValidationMaxTime() {
        return validationMaxTime;
    }

    public void setValidationMaxTime(long validationMaxTime) {
        this.validationMaxTime = validationMaxTime;
    }

    public String getValidationUnitMaxTime() {
        return validationUnitMaxTime;
    }

    public void setValidationUnitMaxTime(String validationUnitMaxTime) {
        this.validationUnitMaxTime = validationUnitMaxTime;
    }
}
