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

package org.qubership.automation.itf.ui.controls;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.IDataSetListManager;
import org.qubership.automation.itf.core.model.common.Named;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.testcase.AbstractTestCase;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIList;
import org.qubership.automation.itf.ui.messages.objects.UIDataSet;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetList;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParameter;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParametersGroup;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.util.UIHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatasetController extends UIHelper {

    private static final Function<DataSetList, UIDataSetList> UI_DS_LIST_FUNC = UIDataSetList::new;
    private static ConcurrentHashMap<String, Set<DataSetList>> dataSetListsHolder = new ConcurrentHashMap<>();

    /**
     * Get DSL from atp-datasets and Excel file.
     *
     * @param projectUuid ITF project UUID
     * @return collection with object {@code {@link UIList<UIObject>}}
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/datasetlists", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Dataset Lists for project {{#projectUuid}}")
    public UIList<UIObject> getLists(@RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<? extends DataSetListsSource> dataSetLists =
                CoreObjectManager.getInstance().getSpecialManager(DataSetList.class, IDataSetListManager.class)
                        .getAllSources(projectUuid);
        if (Objects.isNull(dataSetLists)) {
            return null;
        }
        return getObjectList(dataSetLists.stream().sorted(Comparator.comparing(Named::getName))
                .collect(Collectors.toList()));
    }

    /**
     * Get DS from atp-datasets and Excel file.
     *
     * @param id          source ID
     * @param projectUuid ITF project UUID
     * @return collection with object {@code {@link UIList<UIDataSetList>}}.
     * @throws Exception may occur when receiving data
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/datasetlist", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Dataset Lists by VA id {{#id}} in the project {{#projectUuid}}")
    public UIList<UIDataSetList> getList(@RequestParam(value = "source", required = false) String id,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        IDataSetListManager man = CoreObjectManager.getInstance().getSpecialManager(DataSetList.class,
                IDataSetListManager.class);
        DataSetListsSource lists = man.getSourceById(id, projectUuid);
        if (lists == null) {
            return null;
        }
        return getUIList(lists.getDataSetLists()
                        .stream().sorted(Comparator.comparing(Named::getName)).collect(Collectors.toList()),
                UI_DS_LIST_FUNC::apply);
    }

    /**
     * Get all datasets.
     *
     * @param sources     source ID
     * @param projectUuid ITF project UUID
     * @return string in format Json.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/dataset/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Datasets under Sources '{{#sources}}' in the project {{#projectUuid}}")
    public String getAllDatasets(@RequestParam(value = "sources", defaultValue = "") String sources,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        JSONObject result = new JSONObject();
        JSONArray datasets = new JSONArray();
        DataSetListsSource dataSetListsSources = ((IDataSetListManager) CoreObjectManager.getInstance()
                .getManager(DataSetList.class)).getSourceById(sources, projectUuid);
        Set<DataSetList> dataSetLists = dataSetListsSources.getDataSetLists();
        for (DataSetList dataSetList : dataSetLists) {
            for (IDataSet dataSet : dataSetList.getDataSets(projectUuid)) {
                final String groupName = dataSetList.getName();
                final String datasetName = dataSet.getName();
                final String displayName =
                        String.format("[%s] %s", groupName, datasetName);
                datasets.add(new JSONObject() {
                    {
                    put("groupName", groupName);
                    put("datasetName", datasetName);
                    put("name", displayName);
                }
                });
            }
        }
        dataSetListsHolder.put("dataSetLists", dataSetLists);
        result.put("datasets", datasets);
        return result.toJSONString();
    }

    /**
     * Get all dataset sources.
     *
     * @param projectUuid ITF project UUID
     * @return string in format Json.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/datasetsources/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all DSLs and Datasets in the project {{#projectUuid}}")
    public String getAllDatalists(@RequestParam(value = "projectUuid") UUID projectUuid) {
        JSONObject result = new JSONObject();
        JSONArray datasets = new JSONArray();
        Collection<? extends DataSetListsSource> dataSetListsSources =
                CoreObjectManager.getInstance().getSpecialManager(DataSetList.class, IDataSetListManager.class)
                        .getAllSources(projectUuid);
        dataSetListsSources.forEach(dataSetListsSource -> {
            datasets.add(new JSONObject() {
                {
                put("naturalId", dataSetListsSource.getNaturalId().toString());
                put("name", dataSetListsSource.getName());
                }
            });
        });
        result.put("datasets", datasets);
        return result.toJSONString();
    }

    /**
     * Read dataset and get data for debugger.
     *
     * @param datasetName dataset name
     * @param projectUuid ITF project UUID
     * @return object with type {@link JsonContext}.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/dataset/read/debug", method = RequestMethod.GET)
    @AuditAction(auditAction = "Read dataset with name {{#datasetName}} for debug, project {{#projectUuid}}")
    public JsonContext readDatasetForDebugger(
            @RequestParam(value = "name", defaultValue = "") String datasetName,
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam(value = "projectId") BigInteger projectId) {
        JsonContext result = null;
        Set<DataSetList> dataSetLists = dataSetListsHolder.get("dataSetLists");
        if (CollectionUtils.isNotEmpty(dataSetLists)) {
            for (DataSetList dataSetList : dataSetLists) {
                IDataSet dataSet = dataSetList.getDataSet(datasetName, projectId);
                if (dataSet != null) {
                    result = dataSet.read(projectId);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Read dataset and get data.
     *
     * @param datasetName dataset name
     * @param entityId    entity id
     * @param entityType  entity type
     * @param projectUuid ITF project UUID
     * @return object with type {@link UIDataSet}.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/dataset/read", method = RequestMethod.GET)
    @AuditAction(auditAction = "Read dataset with name {{#datasetName}}, for Entity id {{#entityId}} and type "
            + "{{#entityType}}, project {{#projectUuid}}")
    public UIDataSet readDataset(
            @RequestParam(value = "name", defaultValue = "") String datasetName,
            @RequestParam(value = "entity", defaultValue = "") String entityId,
            @RequestParam(value = "type", defaultValue = "") String entityType,
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam(value = "projectId") BigInteger projectId) {
        if (datasetName != null) {
            AbstractTestCase entity = CoreObjectManager.getInstance().getManager(CallChain.class).getById(entityId);
            ControllerHelper.throwExceptionIfNull(entity, null, entityId, CallChain.class,
                    "get callchain by id");
            for (DataSetList dataSetList : entity.getCompatibleDataSetLists(projectUuid)) {
                IDataSet dataSet = dataSetList.getDataSet(datasetName, projectId);
                if (dataSet != null) {
                    JsonContext datasetContent = dataSet.read(projectId);
                    if (datasetContent != null) {
                        UIDataSet uiDataSet = new UIDataSet();
                        Set<UIDataSetParametersGroup> dataSetParametersGroup = new HashSet<>();
                        Set<UIDataSetParameter> ungroupedDatasetParameters = new LinkedHashSet<>();
                        for (Object o : datasetContent.entrySet()) {
                            Map.Entry entry = (Map.Entry) o;
                            UIDataSetParametersGroup parametersGroup = new UIDataSetParametersGroup();
                            /* Remote dataSet can be (partially) without groups.
                                But here we need groups for UI purposes...
                                So we should check entry.getValue():
                                    - if this is Map<String, Object> - then this is parametersGroup,
                                    - otherwise - this is parameter without groups.
                             */
                            if (!(entry.getValue() instanceof Map)) {
                                processParameter(entry.getValue(), (String) entry.getKey(), ungroupedDatasetParameters);
                                continue;
                            }
                            parametersGroup.setName((String) entry.getKey());
                            Set<UIDataSetParameter> datasetParameters = new LinkedHashSet<>();
                            datasetParameters.add(new UIDataSetParameter("", ""));
                            for (Map.Entry<String, Object> parameter :
                                    ((Map<String, Object>) entry.getValue()).entrySet()) {
                                processParameter(parameter.getValue(), parameter.getKey(), datasetParameters);
                            }
                            parametersGroup.setDataSetParameter(datasetParameters);
                            dataSetParametersGroup.add(parametersGroup);
                        }
                        if (!ungroupedDatasetParameters.isEmpty()) {
                            /* In order UI to work properly.
                                Earlier we assumed that datasets always have groups,
                                but RemoteDataSets can contain some parameters without groups (or groups hierarchy).
                             */
                            UIDataSetParametersGroup ungroupedParametersGroup = new UIDataSetParametersGroup();
                            ungroupedParametersGroup.setName("Autogenerated_No_Group");
                            ungroupedParametersGroup.setDataSetParameter(ungroupedDatasetParameters);
                            dataSetParametersGroup.add(ungroupedParametersGroup);
                        }
                        uiDataSet.setDataSetParametersGroup(dataSetParametersGroup);
                        return uiDataSet;
                    }
                }
            }
        }
        return null;
    }

    private void processParameter(Object value, String key, Set<UIDataSetParameter> datasetParameters) {
        if (value instanceof JSONArray) {
            int indx = 0;
            for (Object arrayValue : (JSONArray) value) {
                datasetParameters.add(
                        new UIDataSetParameter(key + "[" + (indx++) + "]", (String) arrayValue));
            }
        } else {
            datasetParameters.add(new UIDataSetParameter(key, (String) value));
        }
    }
}
