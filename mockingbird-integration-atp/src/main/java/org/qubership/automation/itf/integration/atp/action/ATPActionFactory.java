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

package org.qubership.automation.itf.integration.atp.action;

import java.util.Collection;
import java.util.Map;

import org.qubership.automation.itf.integration.atp.action.model.impl.AbstractAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunAndValidateCallChainWithAtpDatasetAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunAndValidateCallChainWithDatasetAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunAndValidateCallChainWithDefaultDatasetAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunAndValidateCallchainAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunAndValidateCallchainWithDatasetLabelAndDataSetListPath;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallChainWithDatasetAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallChainWithDefaultDatasetAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallchainAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallchainWithDatasetAndOptions;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallchainWithDatasetLabelAndDataSetListPath;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallchainsByLabelForAllDatasetsAtpAction;
import org.qubership.automation.itf.integration.atp.action.model.impl.RunCallchainsByLabelWithDefaultDatasetsAtpAction;

import com.google.common.collect.Maps;

public class ATPActionFactory {
    private static final Map<String, AbstractAtpAction> atpActionMapping;

    static {
        atpActionMapping = Maps.newHashMap();
        atpActionMapping.put(ATPActionConstants.RUN_ITF_CASE_BY_NAME_NAME.stringValue(), new RunCallchainAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME.stringValue(),
                new RunCallChainWithDatasetAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_NAME.stringValue(),
                new RunAndValidateCallChainWithDatasetAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_NAME.stringValue(),
                new RunCallChainWithDefaultDatasetAtpAction());
        atpActionMapping.put(
                ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_NAME.stringValue(),
                new RunAndValidateCallChainWithDefaultDatasetAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_ATP_DATASET_NAME.stringValue(),
                new RunAndValidateCallChainWithAtpDatasetAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_NAME.stringValue(),
                new RunCallchainWithDatasetAndOptions());
        atpActionMapping.put(
                ATPActionConstants.RUN_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_NAME.stringValue(),
                new RunCallchainWithDatasetLabelAndDataSetListPath());
        atpActionMapping.put(
                ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_NAME
                        .stringValue(),
                new RunAndValidateCallchainWithDatasetLabelAndDataSetListPath());
        atpActionMapping.put(ATPActionConstants.RUN_ITF_CASES_BY_LABEL_WITH_DEFAULT_DATASET_NAME.stringValue(),
                new RunCallchainsByLabelWithDefaultDatasetsAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_NAME.stringValue(),
                new RunCallchainsByLabelForAllDatasetsAtpAction());
        atpActionMapping.put(ATPActionConstants.RUN_AND_VALIDATE_ITF_CASE_NAME.stringValue(),
                new RunAndValidateCallchainAtpAction());
    }

    public static Collection<AbstractAtpAction> getActions() {
        return atpActionMapping.values();
    }
}
