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

import java.util.regex.Matcher;

import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfoBuilder;

public class RunCallchainsByLabelForAllDatasetsAtpAction extends RunCallchainsByLabel {
    public RunCallchainsByLabelForAllDatasetsAtpAction() {
        super(ATPActionConstants.RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_NAME.stringValue(),
                ATPActionConstants.RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_DESCRIPTION.stringValue(),
                ATPActionConstants.RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_MASK.stringValue(),
                ATPActionConstants.RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_TEMPLATE.stringValue());
    }

    @Override
    public void setTestRunInfoParams(TestRunInfo testRunInfo, Matcher matcher) {
        TestRunInfoBuilder.setCallchainsWithAllDatasetsByLabel(testRunInfo, matcher);
    }
}
