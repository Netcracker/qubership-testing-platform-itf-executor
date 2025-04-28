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
import java.util.List;

import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UICondition;

public class UITriggerRelation extends UIObject {

    private List<UICondition> conditions = new ArrayList<>();

    public UITriggerRelation(EventTrigger eventTrigger, List<ConditionParameter> conditionParameters) {
        super(eventTrigger);
        conditions.addAll(UICondition.buildUiConditions(conditionParameters));
    }

    public List<UICondition> getConditions() {
        return conditions;
    }

}
