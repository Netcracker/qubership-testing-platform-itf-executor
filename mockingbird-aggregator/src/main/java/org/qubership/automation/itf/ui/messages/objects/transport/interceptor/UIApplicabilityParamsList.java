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

package org.qubership.automation.itf.ui.messages.objects.transport.interceptor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.ui.messages.objects.UIResult;

import com.google.common.collect.Lists;

public class UIApplicabilityParamsList {

    private List<UIApplicabilityParams> values = Lists.newArrayList();

    public UIApplicabilityParamsList() {
    }

    public List<UIApplicabilityParams> getValues() {
        return values;
    }

    public void setValues(List<UIApplicabilityParams> values) {
        this.values = values;
    }

    public UIResult validate() {
        UIResult result = fieldsAreNotEmpty();
        if (result.isSuccess()) {
            result = applicableParametersAreNotDuplicated();
        }
        return result;
    }

    private UIResult fieldsAreNotEmpty() {
        for (UIApplicabilityParams applicabilityParams : values) {
            if (applicabilityParams.getEnvironment() == null) {
                return new UIResult(false, "Environment can not be empty. Please, fill the parameters.");
            }
        }
        return new UIResult();
    }

    private UIResult applicableParametersAreNotDuplicated() {
        for (int i = 0; i < values.size(); i++) {
            for (int g = i + 1; g < values.size(); g++) {
                String sourceEnvId = values.get(i).getEnvironment().getId();
                String comparingEnvId = values.get(g).getEnvironment().getId();
                String sourceSystemId = values.get(i).getSystem() != null ? values.get(i).getSystem().getId() :
                        StringUtils.EMPTY;
                String comparingSystemId = values.get(g).getSystem() != null ? values.get(g).getSystem().getId() :
                        StringUtils.EMPTY;
                if (sourceEnvId.equals(comparingEnvId) && sourceSystemId.equals(comparingSystemId)) {
                    return new UIResult(false, String.format("Applicability Parameters(Environment = %s, System = %s)"
                                    + " already exist in interceptor.", values.get(i).getEnvironment().getName(),
                            values.get(i).getSystem() != null ? values.get(i).getSystem().getName() : "empty"));
                }
            }
        }
        return new UIResult();
    }
}
