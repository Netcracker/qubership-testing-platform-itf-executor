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

package org.qubership.automation.itf.ui.controls.execute;

import java.math.BigInteger;
import java.util.Set;

import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;

public class ExecutorControllerHelper extends ControllerHelper {

    public static final String BULK_VALIDATOR_INTEGRATION = "Bulk Validator Integration";
    public static final String DATASET_SERVICE_INTEGRATION = "Remote Dataset Integration";
    static final String EXECUTED_STATUS = "EXECUTED";
    static final String CATALOGUE_LINK_PATTERN_FOR_DS_TOOL = "project/%s/data-sets/dsl/%s";
    private static final String BASE_FORMAT = "Call Chain '%s'";
    static final String REQUESTED_FORMAT = BASE_FORMAT + " requested for execution.";
    static final String NOT_EXECUTED_FORMAT = BASE_FORMAT + " was not executed";

    public static IntegrationConfig findIntegrationConfig(String toolName, BigInteger projectId) {
        Set<IntegrationConfig> intConfs =
                CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId).getIntegrationConfs();
        for (IntegrationConfig intConf : intConfs) {
            if (intConf.getToolName().equals(toolName)) {
                return intConf;
            }
        }
        return null;
    }

    /*  To support executor actions like
     *        Run and validate starters chain "[Regression][Iter6][Core] Check UUID for all entities"
     *           with dataset "Telefonica Germany|CIM Customer Personal Data|Personal Data"
     *    Must be revised, it's definitely not so good decision because:
     *     - 3 ids are received from executor: (VA id, DSL id, DS id) or (Excel file name, Sheet name, DS name),
     *     - but callChain.findDataSetByName searches DS in ALL DSLs compatible with the callchain
     */
    public static IDataSet findDataSetByName(String datasetName, CallChain callChain, BigInteger projectId) {
        String[] names = datasetName.split("\\|");
        if (names.length == 3) {
            return callChain.findDataSetByName(names[2], projectId);
        } else {
            return callChain.findDataSetByName(datasetName, projectId);
        }
    }
}
