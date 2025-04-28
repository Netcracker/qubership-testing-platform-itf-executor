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

package org.qubership.automation.itf.integration.atp.action.model.impl;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;

import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.model.ArgumentValue;
import org.qubership.automation.itf.integration.atp.util.DefinitionBuilder;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfoBuilder;

import com.google.common.collect.Lists;

public class RunCallChainWithDefaultDatasetAtpAction extends RunCallChainWithDatasetAtpAction {

    public RunCallChainWithDefaultDatasetAtpAction() {
        super(ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_NAME.stringValue(),
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_DESCRIPTION.stringValue(),
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_MASK.stringValue(),
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_TEMPLATE.stringValue());
    }

    RunCallChainWithDefaultDatasetAtpAction(String name, String description, String mask, String template) {
        super(name, description, mask, template);
    }

    public List<ArgumentValue> getAvailableValues(BigInteger projectId) {
        return Lists.newArrayList(DefinitionBuilder.getAvailableCallchainsList(projectId));
    }

    public void setTestRunInfoParams(TestRunInfo testRunInfo, Matcher matcher) {
        TestRunInfoBuilder.setCallchain(testRunInfo, matcher);
        TestRunInfoBuilder.setDefaultDataset(testRunInfo, matcher);
    }
}
