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

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_CAPTURING_FILTER_DEFAULT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;

import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.model.ArgumentValue;
import org.qubership.automation.itf.integration.atp.util.DefinitionBuilder;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfoBuilder;

import com.google.common.collect.Lists;

public class RunCallchainWithDatasetAndOptions extends AbstractAtpAction {

    public RunCallchainWithDatasetAndOptions() {
        super(ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_NAME.stringValue(),
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_DESCRIPTION.stringValue(),
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_MASK.stringValue(),
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_TEMPLATE.stringValue());
    }

    @Override
    public List<ArgumentValue> getAvailableValues(BigInteger projectId) {
        return Lists.newArrayList(DefinitionBuilder.getAvailableCallchainsList(projectId),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.DATASET_INDEX.intValue(),
                        new String[]{ATPActionConstants.DEFAULT.stringValue()}),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.TCPDUMP_ENABLED_INDEX.intValue(),
                        new String[]{ATPActionConstants.TRUE.stringValue()}),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.TCPDUMP_FILTER_INDEX.intValue(),
                        new String[]{projectId == null ? "50" :
                                CoreServices.getProjectSettingsService().get(projectId,
                                        TCPDUMP_CAPTURING_FILTER_DEFAULT, "50")}),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.TCPDUMP_PACKET_COUNT_INDEX.intValue(),
                        new String[]{projectId == null ? "" : CoreServices.getProjectSettingsService().get(projectId,
                                TCPDUMP_PACKET_COUNT_DEFAULT, "")}),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.BV_ENABLED_INDEX.intValue(),
                        new String[]{ATPActionConstants.TRUE.stringValue()}),
                DefinitionBuilder.getAvailableValuesList(ATPActionConstants.BV_ACTION_INDEX.intValue(), new String[]{
                        "CreateNewTestRun", "ReadCompare"})
        );
    }

    @Override
    public void setTestRunInfoParams(TestRunInfo testRunInfo, Matcher matcher) {
        TestRunInfoBuilder.setCallchain(testRunInfo, matcher);
        TestRunInfoBuilder.setDataset(testRunInfo, matcher);
        TestRunInfoBuilder.setTcpDumpOption(testRunInfo, matcher);
        TestRunInfoBuilder.setBVOption(testRunInfo, matcher);
    }
}
