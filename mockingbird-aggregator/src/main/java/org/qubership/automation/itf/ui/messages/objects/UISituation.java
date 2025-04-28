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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.Pair;
import org.qubership.automation.itf.core.util.constants.SituationLevelValidation;
import org.qubership.automation.itf.ui.controls.util.ControllerConstants;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UIEventTrigger;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.qubership.automation.itf.ui.util.UIHelper;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class UISituation extends UITypedObject {

    private static final String TYPE = "situation";
    private static final Function<EventTrigger, UIEventTrigger> TO_UI_TRIGGER_FULL
            = input -> new UIEventTrigger(input);
    private static final Function<EventTrigger, UIEventTrigger> TO_UI_TRIGGER_SHORT
            = input -> new UIEventTrigger(input, false);
    private String mep;
    private ImmutableList<UIEventTrigger> triggers;
    private UIWrapper<Set<UIParsingRule>> parsingRules;
    private UIObject receiver;
    private UIObject operation;
    private UIObject template;
    private long delay;
    private String unit;
    private int priority;
    private SituationLevelValidation validateIncoming;
    private String bvTestcase;
    private List<Pair<String, String>> keysToRegenerate = Lists.newArrayList();
    private UIWrapper<String> preScript;
    private UIWrapper<String> postScript;
    private UIWrapper<String> preValidationScript;
    private boolean ignoreErrors;

    public UISituation() {
    }

    public UISituation(String id, String name) {
        this.setId(id);
        this.setName(name);
    }

    public UISituation(Storable situation) {
        this((Situation) situation);
    }

    public UISituation(Situation situation) {
        super(situation);
        IntegrationStep step = situation.getIntegrationStep();
        if (step != null) {
            if (step.getReceiver() != null) {
                receiver = new UIObject(step.getReceiver());
            }
            if (step.getOperation() != null) {
                operation = new UIObject(step.getOperation());
            }
            if (step.returnStepTemplate() != null) {
                template = new UIObject(step.returnStepTemplate());
            }
            delay = step.getDelay();
            unit = step.getUnit();
        }
        commonInit(situation, true);
    }

    public UISituation(Situation situation, boolean isFullWithParent) {
        super(situation, isFullWithParent);
        IntegrationStep step = situation.getIntegrationStep();
        if (step != null) {
            if (step.getReceiver() != null) {
                receiver = new UIObject(step.getReceiver(), isFullWithParent);
            }
            if (step.getOperation() != null) {
                operation = new UIObject(step.getOperation(), isFullWithParent);
            }
            if (step.returnStepTemplate() != null) {
                template = new UIObject(step.returnStepTemplate(), isFullWithParent);
            }
            delay = step.getDelay();
            unit = step.getUnit();
        }
        commonInit(situation, isFullWithParent);
    }

    private void commonInit(Situation situation, boolean isFullWithParent) {
        setType(TYPE);
        defineParsingRules(situation.getParsingRules(), isFullWithParent);
        validateIncoming = situation.getValidateIncoming();
        bvTestcase = situation.getBvTestcase();
        mep = situation.getMep().toString();
        setPreScript(new UIWrapper<>(situation.getPreScript()));
        setPostScript(new UIWrapper<>(situation.getPostScript()));
        setPreValidationScript(new UIWrapper<>(situation.getPreValidationScript()));
        setLabels(situation.getLabels());
        setIgnoreErrors(situation.isIgnoreErrors());
        if (situation.getOperationEventTriggers().isEmpty()
                && situation.getSituationEventTriggers().isEmpty()) {
            if (situation.getParent().getTransport() != null && situation.getMep().isInboundRequest()) {
                UIEventTrigger uiEventTrigger = new UIEventTrigger();
                uiEventTrigger.setType(ControllerConstants.OPERATION_EVENT_TRIGGER_TYPE.getStringValue());
                uiEventTrigger.setName(uiEventTrigger.getType());
                setTriggers(Collections.singletonList(uiEventTrigger));
                ControllerHelper.addEventTriggers(getTriggers(), situation);
                List<UIEventTrigger> uiTriggers = new ArrayList<>();
                for (EventTrigger eventTrigger : situation.getSituationEventTriggers()) {
                    uiTriggers.add((new UIEventTrigger(eventTrigger)));
                }
                setTriggers(uiTriggers);
            }
        } else {
            setTriggers(Collections2.transform(situation.getAllEventTriggers(),
                    isFullWithParent ? TO_UI_TRIGGER_FULL : TO_UI_TRIGGER_SHORT));
        }
        for (OperationEventTrigger eventTrigger : situation.getOperationEventTriggers()) {
            priority = eventTrigger.getPriority();
        }
        for (Map.Entry<String, String> entry : situation.getKeysToRegenerate().entrySet()) {
            keysToRegenerate.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
    }

    private void defineParsingRules(Set<ParsingRule> parsingRules, boolean isFullWithParent) {
        if (this.parsingRules == null) {
            this.parsingRules = new UIWrapper<>(Sets.newHashSetWithExpectedSize(parsingRules.size()));
        }
        if (parsingRules != null) {
            for (ParsingRule entry : parsingRules) {
                this.parsingRules.getData().add(new UIParsingRule(entry, isFullWithParent));
            }
        }
    }

    public ImmutableList<UIEventTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(Collection<UIEventTrigger> triggers) {
        this.triggers = UIHelper.isNotNullCopyOfImmutableList(triggers);
    }

    public UIWrapper<Set<UIParsingRule>> getParsingRules() {
        return parsingRules;
    }

    public void setParsingRules(UIWrapper<Set<UIParsingRule>> parsingRules) {
        this.parsingRules = parsingRules;
    }

    public UIObject getReceiver() {
        return receiver;
    }

    public void setReceiver(UIObject receiver) {
        this.receiver = receiver;
    }

    public UIObject getOperation() {
        return operation;
    }

    public void setOperation(UIObject operation) {
        this.operation = operation;
    }

    public UIObject getTemplate() {
        return template;
    }

    public void setTemplate(UIObject template) {
        this.template = template;
    }

    public String getMep() {
        return mep;
    }

    public void setMep(String mep) {
        this.mep = mep;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public SituationLevelValidation getValidateIncoming() {
        return validateIncoming;
    }

    public void setValidateIncoming(SituationLevelValidation validateIncoming) {
        this.validateIncoming = validateIncoming;
    }

    public String getBvTestcase() {
        return bvTestcase;
    }

    public void setBvTestcase(String bvTestcase) {
        this.bvTestcase = bvTestcase;
    }

    public List<Pair<String, String>> getKeysToRegenerate() {
        return keysToRegenerate;
    }

    public void setKeysToRegenerate(List<Pair<String, String>> keysToRegenerate) {
        this.keysToRegenerate = keysToRegenerate;
    }

    public UIWrapper<String> getPreScript() {
        return preScript;
    }

    public void setPreScript(UIWrapper<String> preScript) {
        this.preScript = preScript;
    }

    public UIWrapper<String> getPostScript() {
        return postScript;
    }

    public void setPostScript(UIWrapper<String> postScript) {
        this.postScript = postScript;
    }

    public UIWrapper<String> getPreValidationScript() {
        return preValidationScript;
    }

    public void setPreValidationScript(UIWrapper<String> preValidationScript) {
        this.preValidationScript = preValidationScript;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }
}
