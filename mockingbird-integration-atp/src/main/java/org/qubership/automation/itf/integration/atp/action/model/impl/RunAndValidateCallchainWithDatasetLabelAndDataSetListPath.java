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

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.model.ArgumentValue;
import org.qubership.automation.itf.integration.atp.util.DefinitionBuilder;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfoBuilder;

import com.google.common.collect.Lists;

public class RunAndValidateCallchainWithDatasetLabelAndDataSetListPath extends AbstractAtpAction {

    public RunAndValidateCallchainWithDatasetLabelAndDataSetListPath() {
        super(ATPActionConstants
                        .RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_NAME
                        .stringValue(),
                ATPActionConstants
                        .RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_DESCRIPTION
                        .stringValue(),
                ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_MASK
                        .stringValue(),
                ATPActionConstants
                        .RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_TEMPLATE
                        .stringValue());
    }

    @Override
    public List<ArgumentValue> getAvailableValues(BigInteger projectId) {
        return Lists.newArrayList(DefinitionBuilder.getAvailableCallchainsList(projectId),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.DATASET_LABEL_INDEX.intValue(),
                        new String[]{ATPActionConstants.DEFAULT.stringValue()}),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.DATASETLIST_PATH_INDEX.intValue(),
                        new String[]{ATPActionConstants.DEFAULT.stringValue()}));
    }

    @Override
    public void setTestRunInfoParams(TestRunInfo testRunInfo, Matcher matcher) {
        TestRunInfoBuilder.setCallchain(testRunInfo, matcher);
        TestRunInfoBuilder.setDatasetsWithLabel(testRunInfo, matcher);
        String bvAction = CoreServices.getProjectSettingsService()
                .get(testRunInfo.getProjectId(), BV_DEFAULT_ACTION, BV_DEFAULT_ACTION_DEFAULT_VALUE);
        testRunInfo.setBvAction(bvAction);
        testRunInfo.setValidateMessageOnStep(StringUtils.isNotEmpty(bvAction));
    }
}
