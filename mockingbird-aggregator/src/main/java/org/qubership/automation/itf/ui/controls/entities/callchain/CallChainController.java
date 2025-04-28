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

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.IDataSetListManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByProjectIdManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.step.AbstractCallChainStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.messages.objects.BulkUpdateRequest;
import org.qubership.automation.itf.ui.messages.objects.UIChainObjectsList;
import org.qubership.automation.itf.ui.messages.objects.UIDataSet;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetList;
import org.qubership.automation.itf.ui.messages.objects.UIKey;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.callchain.UICallChain;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIAbstractCallChainStep;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Transactional(readOnly = true)
@RestController
public class CallChainController extends AbstractController<UICallChain, CallChain> {

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all CallChains for project {{#projectId}}/{{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "projectId") BigInteger projectId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<? extends CallChain> all = CoreObjectManager.getInstance().getSpecialManager(CallChain.class,
                SearchByProjectIdManager.class).getByProjectId(projectId);
        return asListUIObject(all, false, false);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/allbyparent", method = RequestMethod.GET)
    public List<? extends UIObject> getAll(@RequestParam(value = "parentId", defaultValue = "0") String parentId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getAll(parentId);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/allIdsAndNames", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all CallChains (id, name) for project {{#projectId}}/{{#projectUuid}}")
    public UIChainObjectsList getAllIdsAndNames(@RequestParam(value = "projectId") BigInteger projectId,
                                                @RequestParam(value = "projectUuid") UUID projectUuid) {
        return new UIChainObjectsList(projectId);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get CallChain by id {{#id}} in the project {{#projectUuid}}")
    public UICallChain getById(@RequestParam(value = "id", defaultValue = "0") String id,
                               @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/callchain", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create CallChain under parent id {{#parentId}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public UICallChain create(
            @RequestParam(value = "id", defaultValue = "0") String parentId,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.create(parentId, projectId);
    }

    @Nonnull
    private Folder<CallChain> getFolderByIdOrRootIfNull(@Nullable String id, BigInteger projectId) {
        Folder<CallChain> folder;
        if (StringUtils.isNotBlank(id)) {
            folder = CoreObjectManager.getInstance().getManager(Folder.class).getById(id);
        } else {
            folder = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId).getCallchains();
        }
        return folder;
    }

    private void checkFoldersForBulkUpdate(BulkUpdateRequest bulkObject, String id, BigInteger projectId) {
        bulkUpdateObject(bulkObject, CoreObjectManager.getInstance().getManager(Folder.class).getById(id));
        Folder<CallChain> folder = getFolderByIdOrRootIfNull(id, projectId);
        for (int i = 0; i < folder.getObjects().size(); i++) {
            bulkUpdateObject(bulkObject,
                    getManager(CallChain.class).getById(folder.getObjects().get(i).getID().toString()));
        }
        if (bulkObject.isChildrenFolders()) {
            for (int i = 0; i < folder.getSubFolders().size(); i++) {
                checkFoldersForBulkUpdate(bulkObject, folder.getSubFolders().get(i).getID().toString(), projectId);
            }
        }
    }

    private void rename_performAdd(BulkUpdateRequest bulkObject, Storable updatingObject) {
        if (bulkObject.getPosition().equals("~start")) {
            updatingObject.setName(bulkObject.getTextData() + updatingObject.getName());
        } else if (bulkObject.getPosition().equals("~end")) {
            updatingObject.setName(updatingObject.getName() + bulkObject.getTextData());
        } else if (bulkObject.getIntPosition() != -1) {
            if (updatingObject.getName().length() > bulkObject.getIntPosition()) {
                StringBuilder stringBuffer = new StringBuilder(updatingObject.getName());
                stringBuffer.insert(bulkObject.getIntPosition(), bulkObject.getTextData());
                updatingObject.setName(stringBuffer.toString());
            }
        } else {
            if (updatingObject.getName().contains(bulkObject.getPosition())) {
                int indexPosition = updatingObject.getName().indexOf(bulkObject.getPosition());
                indexPosition += bulkObject.getPosition().length();
                StringBuilder stringBuffer = new StringBuilder(updatingObject.getName());
                stringBuffer.insert(indexPosition, bulkObject.getTextData());
                updatingObject.setName(stringBuffer.toString());
            }
        }
    }

    private void rename_performChange(BulkUpdateRequest bulkObject, Storable updatingObject) {
        if (bulkObject.getPosition().equals("~start")) {
            String notDeletedSubString = updatingObject.getName().substring(bulkObject.getTextData().length());
            updatingObject.setName(bulkObject.getTextData() + notDeletedSubString);
        } else if (bulkObject.getPosition().equals("~end")) {
            String notDeletedSubString = updatingObject.getName().substring(0,
                    updatingObject.getName().length() - bulkObject.getTextData().length());
            updatingObject.setName(notDeletedSubString + bulkObject.getTextData());
        } else if (bulkObject.getIntPosition() != -1) {
            if (updatingObject.getName().length() > bulkObject.getIntPosition()) {
                String firstPartSubString = updatingObject.getName().substring(0, bulkObject.getIntPosition());
                String secondPartSubString =
                        updatingObject.getName().substring(
                                bulkObject.getIntPosition() + bulkObject.getTextData().length());
                updatingObject.setName(firstPartSubString + bulkObject.getTextData() + secondPartSubString);
            }
        } else {
            if (updatingObject.getName().contains(bulkObject.getPosition())) {
                int indexPosition = updatingObject.getName().indexOf(bulkObject.getPosition());
                String firstPartSubString = updatingObject.getName().substring(0, indexPosition);
                String secondPartSubString =
                        updatingObject.getName().substring(indexPosition + bulkObject.getPosition().length());
                updatingObject.setName(firstPartSubString + bulkObject.getTextData() + secondPartSubString);
            }
        }
    }

    private void rename_performDelete(BulkUpdateRequest bulkObject, Storable updatingObject) {
        if (bulkObject.getPosition().equals("~start")) {
            if (Integer.parseInt(bulkObject.getTextData()) < updatingObject.getName().length()) {
                updatingObject.setName(updatingObject.getName().substring(Integer.parseInt(bulkObject.getTextData())));
            }
        } else if (bulkObject.getPosition().equals("~end")) {
            if (Integer.parseInt(bulkObject.getTextData()) < updatingObject.getName().length()) {
                updatingObject.setName(updatingObject.getName().substring(0,
                        updatingObject.getName().length() - Integer.parseInt(bulkObject.getTextData())));
            }
        } else if (bulkObject.getIntPosition() != -1) {
            if (updatingObject.getName().length() > bulkObject.getIntPosition()) {
                String firstPartSubString = updatingObject.getName().substring(0, bulkObject.getIntPosition());
                String secondPartSubString =
                        updatingObject.getName().substring(
                                firstPartSubString.length() + Integer.parseInt(bulkObject.getTextData()));
                updatingObject.setName(firstPartSubString + secondPartSubString);
            }
        } else {
            if (updatingObject.getName().contains(bulkObject.getPosition())) {
                int indexPosition = updatingObject.getName().indexOf(bulkObject.getPosition());
                String firstPartSubString = updatingObject.getName().substring(0, indexPosition);
                String secondPartSubString =
                        updatingObject.getName().substring(indexPosition + bulkObject.getPosition().length());
                updatingObject.setName(firstPartSubString + secondPartSubString);
            }
        }
    }

    private void bulkUpdateObject(BulkUpdateRequest bulkObject, Storable updatingObject) {
        try {
            switch (bulkObject.getSelectedOperation()) {
                case "add":
                    rename_performAdd(bulkObject, updatingObject);
                    break;
                case "change":
                    rename_performChange(bulkObject, updatingObject);
                    break;
                case "delete":
                    rename_performDelete(bulkObject, updatingObject);
                    break;
                default:
            }
        } catch (Exception ex) {
            // Silently ignore now; should be processed more accurately. May be, should be collected into string
            // buffer in caller method
        }
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/callchain/bulk", method = RequestMethod.POST)
    @AuditAction(auditAction = "Bulk update CallChains in the project {{#projectId}}")
    public UIResult bulkUpdate(@RequestParam(value = "projectId") BigInteger projectId,
                               @RequestParam(value = "projectUuid") UUID projectUuid,
                               @RequestBody BulkUpdateRequest bulkObject) {
        for (int i = 0; i < bulkObject.getCheckedObjectsIdList().size(); i++) {
            if (bulkObject.getCheckedObjectsClassNameList().get(i).equals("ChainFolder")) {
                checkFoldersForBulkUpdate(bulkObject, bulkObject.getCheckedObjectsIdList().get(i), projectId);
            } else {
                bulkUpdateObject(bulkObject,
                        getManager(CallChain.class).getById(bulkObject.getCheckedObjectsIdList().get(i)));
            }
        }
        return new UIResult(true, "success");
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/callchain", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update CallChain by id {{#callchain.id}} in the project {{#projectUuid}}")
    public UICallChain update(@RequestBody UICallChain callChain,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.update(callChain);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/callchain", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete CallChains from project {{#projectUuid}}")
    public Map<String, List<UIObject>> delete(@RequestBody Collection<UICallChain> objectsToDelete,
                                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, List<UIObject>> allObjects = isHaveUsages(objectsToDelete, projectUuid);
        super.delete(objectsToDelete);
        return allObjects;
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/fullPath", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get full-path-to-CallChain by id {{#id}}")
    public UIWrapper getFullPath(@RequestParam(value = "id", defaultValue = "0") String id,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        String path = "";
        Storable folder = CoreObjectManager.getInstance().getManager(Folder.class).getById(id);
        while (folder != null) {
            path = ("/" + folder.getName()) + path;
            folder = folder.getParent();
        }
        return new UIWrapper(path);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/usages", method = RequestMethod.GET, produces = "application/json")
    @AuditAction(auditAction = "Get usages of CallChain by id {{#id}}")
    public Map<String, Object> getUsages(@RequestParam(value = "id") String id,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        ObjectManager<CallChain> callChainManager = getManager(CallChain.class);
        CallChain callChain = callChainManager.getById(id);
        Collection<UsageInfo> usageInfo = callChainManager.findUsages(callChain);
        Map<String, Object> usages = new HashMap<>();
        List<Object> usage = new ArrayList<>();
        if (usageInfo != null) {
            for (UsageInfo item : usageInfo) {
                usage.add(new UIObject(item.getReferer()));
            }
            usages.put("usages", usage);
        }
        return usages;
    }

    @Override
    protected Class<CallChain> _getGenericUClass() {
        return CallChain.class;
    }

    @Override
    protected UICallChain _newInstanceTClass(CallChain object) {
        return new UICallChain(object);
    }

    @Override
    protected Folder _getParent(String parentId) {
        return getManager(Folder.class).getById(parentId);
    }

    @Override
    protected CallChain _beforeUpdate(UICallChain uiObject, CallChain object) {
        updateSteps(uiObject.getSteps(), object);
        updateKeys(uiObject.getKeys(), object);
        updateDataSetLists(uiObject.getDataSetLists(), object);
        return super._beforeUpdate(uiObject, object);
    }

    private void updateSteps(UIWrapper<List<UIAbstractCallChainStep>> steps, CallChain callChain) {
        if (steps != null && steps.isLoaded()) {
            List<Step> newSteps = Lists.newArrayListWithExpectedSize(steps.getData().size());
            for (UIAbstractCallChainStep uiStep : steps.getData()) {
                Step step = getManager(Step.class).getById(uiStep.getId());
                uiStep.updateObject((AbstractCallChainStep) step);
                step.setOrder(uiStep.getOrder());
                newSteps.add(step);
            }
            callChain.fillSteps(newSteps);
        }
    }

    private void updateKeys(UIWrapper<List<UIKey>> keys, CallChain callChain) {
        if (keys != null && keys.isLoaded()) {
            callChain.getKeys().clear();
            for (UIKey key : keys.getData()) {
                callChain.getKeys().add(key.getKey());
            }
        }
    }

    private void updateDataSetLists(UIWrapper<List<UIDataSetList>> dataSetLists, CallChain object) {
        if (dataSetLists != null && dataSetLists.isLoaded()) {
            IDataSetListManager om = CoreObjectManager.getInstance().getSpecialManager(DataSetList.class,
                    IDataSetListManager.class);
            Set<DataSetList> actual = Sets.newHashSet();
            for (UIDataSetList uiDataSetList : dataSetLists.getData()) {
                DataSetList dataSetList = om.getById(uiDataSetList.getId(),
                        CoreObjectManager.getInstance()
                                .getManager(StubProject.class).getById(object.getProjectId()).getUuid());
                if (dataSetList != null) {
                    actual.add(dataSetList);
                    if (uiDataSetList.getList() != null && uiDataSetList.getList().isLoaded()) {
                        for (UIDataSet dataSet : uiDataSetList.getList().getData()) {
                            object.getBvCases().put(dataSet.getName(), dataSet.getBvCaseId());
                        }
                    }
                }
            }
            object.fillCompatibleDataSetLists(actual);
        }
    }

    @Override
    protected void checkVersion(CallChain object, UICallChain uiObject) {
        //skip version checking for CallChain
    }

    private Map<String, List<UIObject>> isHaveUsages(Collection<UICallChain> objectsToDelete, UUID projectUuid) {
        Map<String, List<UIObject>> allObjects = new HashMap<>();
        List<UIObject> deletedUiObjects = new ArrayList<>();
        List<UIObject> usedUiObjects = new ArrayList<>();

        for (UIObject uiObject : objectsToDelete) {
            Map<String, Object> callchainUsages = getUsages(uiObject.getId(), projectUuid);
            if (!((ArrayList) callchainUsages.get("usages")).isEmpty()) {
                usedUiObjects.add(uiObject);
            } else {
                deletedUiObjects.add(uiObject);
            }
        }
        allObjects.put("usages", usedUiObjects);
        allObjects.put("deleted", deletedUiObjects);
        objectsToDelete.removeAll(usedUiObjects);
        return allObjects;
    }
}
