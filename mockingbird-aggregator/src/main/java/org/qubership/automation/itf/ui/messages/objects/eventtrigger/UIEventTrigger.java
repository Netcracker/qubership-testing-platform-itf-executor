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

package org.qubership.automation.itf.ui.messages.objects.eventtrigger;

import java.util.ArrayList;
import java.util.List;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.condition.ConditionsHelper;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerConstants;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UIEventTrigger extends UIObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIEventTrigger.class);
    private UIListen listen;
    private String type;
    private String event;
    private String state;
    private String on;
    private List<UICondition> condition;

    public UIEventTrigger() {
    }

    public UIEventTrigger(Storable eventTrigger) {
        this((EventTrigger) eventTrigger);
    }

    public UIEventTrigger(EventTrigger eventTrigger) {
        this(eventTrigger, true);
    }

    public UIEventTrigger(EventTrigger eventTrigger, boolean isFullWithParent) {
        super(eventTrigger, isFullWithParent);
        this.event = eventTrigger.getState().toString();
        this.state = eventTrigger.getState().toString();
        if (eventTrigger instanceof SituationEventTrigger) {
            if (((SituationEventTrigger) eventTrigger).getOn() == null) {
                ((SituationEventTrigger) eventTrigger).setOn("Finish");
            }
            this.on = ((SituationEventTrigger) eventTrigger).getOn().toString();
            if (((SituationEventTrigger) eventTrigger).getSituation() != null) {
                try {
                    this.listen = new UIListen(((SituationEventTrigger) eventTrigger).getSituation());
                } catch (Exception e) {
                    this.listen = new UIListen();
                }
            }
            this.type = ControllerConstants.SITUATION_EVENT_TRIGGER_TYPE.getStringValue();
        } else {
            this.type = ControllerConstants.OPERATION_EVENT_TRIGGER_TYPE.getStringValue();
        }
        if (eventTrigger.getConditionParameters() != null) {
            setCondition(UICondition.buildUiConditions(eventTrigger.getConditionParameters()));
        }
    }

    public void fillTrigger(EventTrigger trigger) {
        trigger.setName(getName());
        if (trigger instanceof SituationEventTrigger) {
            UIListen uiListen = getListen();
            if (getOn() != null) {
                ((SituationEventTrigger) trigger).setOn(getOn());
            }
            if (uiListen != null) {
                ((SituationEventTrigger) trigger).setSituation(
                        CoreObjectManager.getInstance().getManager(Situation.class).getById(uiListen.getId())
                );
            } else {
                ((SituationEventTrigger) trigger).setSituation(null);
            }
        }
        if (getCondition() != null) {
            List<ConditionParameter> conditionParameters = trigger.getConditionParameters() == null
                    ? new ArrayList<>()
                    : trigger.getConditionParameters();
            ConditionsHelper.fillConditionParameters(conditionParameters,
                    ControllerHelper.toConditionParameters(getCondition()));
            trigger.setConditionParameters(conditionParameters);
        }
    }

}
