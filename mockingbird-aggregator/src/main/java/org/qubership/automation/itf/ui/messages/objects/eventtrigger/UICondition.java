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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UICondition {

    private String name;
    private String condition;
    private String value;
    private String etc;
    private int orderId;

    public UICondition() {
    }

    public UICondition(ConditionParameter conditionParameter) {
        setName(conditionParameter.getName());
        setCondition(conditionParameter.getCondition() == null ? "" : conditionParameter.getCondition().toString());
        setValue(conditionParameter.getValue());
        setOrderId(conditionParameter.getOrderId());
        if (conditionParameter.getEtc() != null) {
            setEtc(conditionParameter.getEtc().toString());
        }
    }

    public static List<UICondition> buildUiConditions(@Nonnull List<ConditionParameter> conditionParameters) {
        return conditionParameters.stream().map(UICondition::new).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UICondition that = (UICondition) o;
        return Objects.equals(name, that.name)
                && Objects.equals(condition, that.condition)
                && Objects.equals(value, that.value)
                && Objects.equals(etc, that.etc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, condition, value, etc);
    }

    @Override
    public String toString() {
        return "Condition{"
                + "name='" + name + '\''
                + ", condition='" + condition + '\''
                + ", value='" + value + '\''
                + ", etc='" + etc + '\''
                + '}';
    }
}
