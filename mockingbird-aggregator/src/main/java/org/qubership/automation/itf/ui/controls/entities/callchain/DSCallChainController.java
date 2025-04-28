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

package org.qubership.automation.itf.ui.controls.entities.callchain;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSet;
import org.qubership.automation.itf.core.model.common.Named;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetLabel;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetList;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UITypedObject;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@Transactional(readOnly = true)
@RestController
public class DSCallChainController extends ControllerHelper {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/callchain/setDefaultDataset", method = RequestMethod.GET)
    @AuditAction(auditAction = "Dataset {{#datasetName}} with id {{#datasetId}} set as Default for CallChain id "
            + "{{#callchainId}} in the project {{#projectUuid}}")
    public void setDefaultDataset(
            @RequestParam(value = "id", defaultValue = "0") String callchainId,
            @RequestParam(value = "datasetName", required = false) String datasetName,
            @RequestParam(value = "datasetId", required = false) String datasetId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain callChain = getManager(CallChain.class).getById(callchainId);
        callChain.setDatasetId((datasetId == null || datasetId.isEmpty())
                ? datasetName
                : datasetId);
        callChain.store();
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/getDefaultDataset", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Default Dataset for CallChain id {{#id}} in the project {{#projectUuid}}")
    public String getDefaultDataset(@RequestParam(value = "id", defaultValue = "0") String id,
                                    @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain callChain = getManager(CallChain.class).getById(id);
        throwExceptionIfNull(callChain, null, id, CallChain.class, "get default DataSet");
        JSONObject result = new JSONObject();
        result.put("datasetId", callChain.getDatasetId());
        return result.toJSONString();
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/datasetlists", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Dataset Lists for CallChain id {{#id}} in the project {{#projectUuid}}")
    public UIWrapper<List<UIDataSetList>> getDataSetLists(@RequestParam(value = "id", defaultValue = "0") String id,
                                                          @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain callChain = getManager(CallChain.class).getById(id);
        throwExceptionIfNull(callChain, null, id, CallChain.class, "get DataSet lists");
        Set<DataSetList> compatibleDataSetLists = callChain.getCompatibleDataSetLists(projectUuid);
        List<UIDataSetList> result = Lists.newArrayListWithCapacity(compatibleDataSetLists.size());
        compatibleDataSetLists.stream().sorted(Comparator.comparing(Named::getName))
                .forEach(item -> result.add(new UIDataSetList(item)));
        return new UIWrapper<>(result);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/datasets", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Datasets for CallChain id {{#id}} in the project {{#projectUuid}}")
    public List<UIObject> getDataSetList(
            @RequestParam(value = "parent", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain chain = getManager(CallChain.class).getById(id);
        throwExceptionIfNull(chain, "", id, CallChain.class, "get DataSets");
        Set<DataSetList> dataSetLists = chain.getCompatibleDataSetLists(projectUuid);
        if (dataSetLists != null) {
            List<UIObject> response = Lists.newLinkedList();
            for (DataSetList dataSetList : dataSetLists) {
                Set<IDataSet> dataSets;
                try {
                    dataSets = dataSetList.getDataSets(projectUuid);
                } catch (Exception e) {
                    /* In case any error faced while getting datasets of one DSL,
                        we should NOT stop processing. We should retrieve all AVAILABLE datasets instead.
                        Possible errors are:
                            - One of Excel datasets files is missed,
                            - One of DSLs (served by Datasets service) is missed (~deleted, ~moved),
                            - The entire Datasets service is unavailable due to maintenance.
                     */
                    continue;
                }
                for (IDataSet dataSet : dataSets) {
                    dataSet.addModifiedToName(false);
                    UITypedObject uiTypedObject = new UITypedObject();
                    uiTypedObject.setName(dataSet.getName());
                    uiTypedObject.setType(dataSetList.getName());
                    uiTypedObject.setClassName(dataSet.getClass().getName());
                    if (dataSet instanceof RemoteDataSet) {
                        uiTypedObject.setId(dataSet.getIdDs());
                    }
                    response.add(uiTypedObject);
                }
            }
            return response;
        } else {
            return Collections.emptyList();
        }
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/getDatasetsLabels", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Datasets' Labels for CallChain id {{#id}} in the project {{#projectUuid}}")
    public List<UIDataSetLabel> getDataSetLabels(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain chain = CoreObjectManager.getInstance().getManager(CallChain.class).getById(id);
        throwExceptionIfNull(chain, "", id, CallChain.class, "get DataSet labels");
        Set<DataSetList> dataSetLists = chain.getCompatibleDataSetLists(projectUuid);
        if (dataSetLists != null) {
            Set<String> uniqueListOfDataSetLabels = new HashSet<>();
            List<UIDataSetLabel> result = Lists.newLinkedList();
            for (DataSetList dataSetList : dataSetLists) {
                Set<IDataSet> dataSets;
                try {
                    dataSets = dataSetList.getDataSets(projectUuid);
                } catch (Exception e) {
                    continue;
                }
                for (IDataSet dataSet : dataSets) {
                    if (dataSet instanceof RemoteDataSet) {
                        uniqueListOfDataSetLabels.addAll(dataSet.getLabels());
                    }
                }
            }
            for (String label : uniqueListOfDataSetLabels) {
                UIDataSetLabel uiDataSetLabel = new UIDataSetLabel();
                uiDataSetLabel.setName(label);
                result.add(uiDataSetLabel);
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }
}
