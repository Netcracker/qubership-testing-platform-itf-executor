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

package org.qubership.automation.itf.ui.messages.objects;

import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.ui.messages.objects.template.UITemplate;

public class UIStep extends UIObject {

    private UISystem sender;
    private UISystem receiver;
    private UIOperation operation;
    private UITemplate template;
    private long delay;
    private String unit;
    private String enabled;
    private String manual;
    private String mep;

    public UIStep() {
        this.sender = new UISystem();
        this.receiver = new UISystem();
        this.template = new UITemplate();
        this.operation = new UIOperation();
    }

    public UIStep(Step step) {
        super();
    }

    public UISystem getSender() {
        return sender;
    }

    public void setSender(UISystem sender) {
        this.sender = sender;
    }

    public void defineSender(System sender) {
        this.receiver = new UISystem(sender);
    }

    public UISystem getReceiver() {
        return receiver;
    }

    public void setReceiver(UISystem receiver) {
        this.receiver = receiver;
    }

    public void defineReceiver(System receiver) {
        this.receiver = new UISystem(receiver);
    }

    public UIOperation getOperation() {
        return operation;
    }

    public void setOperation(UIOperation operation) {
        this.operation = operation;
    }

    public void defineOperation(Operation operation) {
        this.operation = new UIOperation(operation);
    }

    public UITemplate getTemplate() {
        return template;
    }

    public void setTemplate(UITemplate template) {
        this.template = template;
    }

    public void defineTemplate(Template template) {
        this.template = new UITemplate(template);
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

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getManual() {
        return manual;
    }

    public void setManual(String manual) {
        this.manual = manual;
    }

    public String getMep() {
        return mep;
    }

    public void setMep(String mep) {
        this.mep = mep;
    }

    private void defineStep(Step step) {
        this.defineObjectParam(step);
        if (step.getMep() != null) {
            this.setMep(step.getMep().toString());
        }
        IntegrationStep integrationStep = (IntegrationStep) step;
        if (integrationStep.getOperation() != null) {
            this.getOperation().setId(integrationStep.getOperation().getID().toString());
            this.getOperation().setName(integrationStep.getOperation().getName());
            this.getOperation().setParent(new UIObject(integrationStep.getOperation().getParent()));
        } else {
            this.setOperation(null);
        }
        if (integrationStep.returnStepTemplate() != null) {
            this.getTemplate().setId(integrationStep.returnStepTemplate().getID().toString());
            this.getTemplate().setName(integrationStep.returnStepTemplate().getName());
        } else {
            this.setTemplate(null);
        }
        if (integrationStep.getSender() != null) {
            this.getSender().setId(integrationStep.getSender().getID().toString());
            this.getSender().setName(integrationStep.getSender().getName());
        } else {
            this.setSender(null);
        }
        if (integrationStep.getReceiver() != null) {
            this.getReceiver().setId(integrationStep.getReceiver().getID().toString());
            this.getReceiver().setName(integrationStep.getReceiver().getName());
        } else {
            this.setReceiver(null);
        }
        this.setDelay(integrationStep.getDelay());
        this.setUnit(integrationStep.getUnit());
        this.setEnabled(integrationStep.isEnabled() ? "Yes" : "No");
        this.setManual(integrationStep.isManual() ? "Yes" : "No");
    }
}
