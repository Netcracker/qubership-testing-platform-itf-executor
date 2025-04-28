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

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION_DEFAULT_VALUE;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;

public class RunAndValidateCallChainWithDatasetAtpAction extends RunCallChainWithDatasetAtpAction {

    public RunAndValidateCallChainWithDatasetAtpAction() {
        super(ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_NAME.stringValue(),
                ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_DESCRIPTION.stringValue(),
                ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_MASK.stringValue(),
                ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_TEMPLATE.stringValue());
    }

    public void setTestRunInfoParams(TestRunInfo testRunInfo, Matcher matcher) {
        super.setTestRunInfoParams(testRunInfo, matcher);
        String bvAction = CoreServices.getProjectSettingsService()
                .get(testRunInfo.getProjectId(), BV_DEFAULT_ACTION, BV_DEFAULT_ACTION_DEFAULT_VALUE);
        testRunInfo.setBvAction(bvAction);
        testRunInfo.setValidateMessageOnStep(StringUtils.isNotEmpty(bvAction));
    }
}
