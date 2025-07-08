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

package org.qubership.automation.itf.ui.controls.entities.system;

import static org.qubership.automation.itf.ui.controls.entities.util.KeyDefinitionControllerHelper.editContextKeyDefinition;
import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemObjectManager;
import org.qubership.automation.itf.core.model.IdNamePair;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.message.delete.DeleteEntityResultMessage;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.constants.SystemMode;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIECIObject;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.qubership.automation.itf.ui.messages.objects.UISystem;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironment;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SystemController extends AbstractController<UISystem, System> {

    /**
     * Get all systems by project id.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Systems for project {{#projectId}}/{{#projectUuid}}")
    public List<? extends UIECIObject> getAll(@RequestParam(value = "projectUuid") UUID projectUuid,
                                              @RequestParam BigInteger projectId) {
        Collection<System> systems = CoreObjectManager.getInstance()
                .getSpecialManager(System.class, SystemObjectManager.class).getByProjectId(projectId);
        List<UIECIObject> uiEciObjects = new ArrayList<>();
        for (System system : systems) {
            uiEciObjects.add(new UIECIObject(system));
        }
        return uiEciObjects;
    }

    /**
     * Get all simple Systems (id, name only) by project id.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/allSimple", method = RequestMethod.GET)
    public List<IdNamePair> getAllSimple(@RequestParam(value = "projectUuid") UUID projectUuid,
                                         @RequestParam BigInteger projectId) {
        return CoreObjectManager.getInstance().getSpecialManager(System.class, SystemObjectManager.class)
                .getSimpleListByProject(projectId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/allbyparent", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Systems by parent id {{#parentId}} for project {{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "parentId", defaultValue = "0") String parentId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        return getUiObjectsBySystems(parentId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get System by id {{#id}} for project {{#projectUuid}}")
    public UISystem getById(@RequestParam(value = "id", defaultValue = "0") final String id,
                            @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    /*
        This request was added because the page formation on the monitoring tab goes
        through feign client (see MonitoringController)
     */
    @Transactional(readOnly = true)
    @RequestMapping(value = "/system/{id}", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get System by id {{#id}} using feign")
    public UISystem feignGetById(@PathVariable(value = "id") String id) {
        return super.getById(id);
    }

    /**
     * Get all systems by the project UUID as json array of {name, id, className} objects.
     * Currently, it is used from Itf-Lite to support 'Export to ITF' UI.
     *
     * @return array of {name, id, className} objects.
     */
    @Transactional(readOnly = true)
    @RequestMapping(value = "/systems", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Systems for project {{#projectUuid}}")
    public Collection<UIIdentifiedObject> getSystemsByProjectId(@RequestParam UUID projectId) {
        BigInteger internalProjectId = CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getEntityInternalIdByUuid(projectId);
        if (Objects.isNull(internalProjectId)) {
            throw new ObjectNotFoundException("Project", projectId.toString(), null, "get Systems under project");
        }
        Collection<System> systems = CoreObjectManager.getInstance()
                .getSpecialManager(System.class, SystemObjectManager.class).getByProjectId(internalProjectId);
        String className = System.class.getName();
        return systems.stream().map(system -> {
            UIIdentifiedObject uiIdentifiedObject = new UIIdentifiedObject();
            uiIdentifiedObject.setId(system.getID().toString());
            uiIdentifiedObject.setName(system.getName());
            uiIdentifiedObject.setClassName(className);
            return uiIdentifiedObject;
        }).collect(Collectors.toList());
    }

    /**
     * Get all outbound operations by the system ID as json array of {name, id, className} objects.
     * Currently, it is used from Itf-Lite to support 'Export to ITF' UI.
     *
     * @return array of {name, id, className} objects.
     */
    @Transactional(readOnly = true)
    @RequestMapping(value = "/system/{id}/operations", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Outbound Operations from System with id {{#id}}")
    public Collection<UIIdentifiedObject> getOutOperationsBySystem(@PathVariable BigInteger id) {
        System system = CoreObjectManager.getInstance().getManager(System.class).getById(id);
        ControllerHelper.throwExceptionIfNull(system, null, String.valueOf(id), System.class,
                "get Operations under System");
        return getOperationsBySystemAndDirection(system, true);
    }

    /**
     * TODO Add JavaDoc.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/system", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create System under parent id {{#parentId}} in the project {{#projectUuid}}")
    public UIObject create(@RequestParam(value = "id", defaultValue = "0") String parentId,
                           @RequestParam(value = "projectUuid") UUID projectUuid,
                           @RequestBody UIObject requestBody) {
        if (requestBody == null) {
            return super.create(parentId);
        } else {
            return super.create(parentId, requestBody.getName(), "system", requestBody.getDescription(),
                    requestBody.getLabels());
        }
    }

    /**
     * TODO Add JavaDoc.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/system", method = RequestMethod.DELETE, produces = "application/json")
    @AuditAction(auditAction = "Delete Systems from project {{#projectId}}/{{#projectUuid}}")
    public DeleteEntityResultMessage<String, UIObject> delete(
            @RequestParam(value = "ignoreUsages", defaultValue = "false") Boolean ignoreUsages,
            @RequestBody final String deleteObjectsReq,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, List<UIObject>> systemsWithTriggers = new HashMap<>();
        Map<String, String> usingSystems = new HashMap<>();
        List<String> idsOfSystemsToDelete = new ArrayList<>();
        List<String> idsOfFoldersToDelete = new ArrayList<>();
        List<System> systemsWithoutTriggers = new ArrayList<>();
        JsonArray deleteObjects = ControllerHelper.GSON.fromJson(deleteObjectsReq, JsonObject.class)
                .getAsJsonArray("entitiesForDelete");
        for (int i = 0; i < deleteObjects.size(); i++) {
            JsonObject entity = deleteObjects.get(i).getAsJsonObject();
            if (entity.get("isParent").getAsBoolean()) {
                idsOfFoldersToDelete.add(entity.get("id").getAsString());
            } else {
                idsOfSystemsToDelete.add(entity.get("id").getAsString());
            }
        }
        for (String id : idsOfSystemsToDelete) {
            System system = getManager(System.class).getById(id);
            List<UIObject> triggers = new ArrayList<>();
            Map<String, List<BigInteger>> blockingTriggers = getManager(System.class).findImportantChildren(system);
            if (blockingTriggers != null && !blockingTriggers.isEmpty()) {
                /*
                    Currently in UI triggers list is not used, even in error message.
                    So, results from .findImportantChildren() are simply wrapped into UIObjects.
                    May be, further changes would be made: return count of objects instead of objects list.
                 */
                // Wrap Event Triggers
                if (blockingTriggers.containsKey("SituationEventTriggers")) {
                    for (BigInteger triggerId : blockingTriggers.get("SituationEventTriggers")) {
                        UIObject uiObject = new UIObject();
                        uiObject.setId(triggerId.toString());
                        uiObject.setClassName("SituationEventTrigger");
                        triggers.add(uiObject);
                    }
                }
                //Wrap Transport Triggers
                if (blockingTriggers.containsKey("TransportTriggers")) {
                    for (BigInteger triggerId : blockingTriggers.get("TransportTriggers")) {
                        UIObject uiObject = new UIObject();
                        uiObject.setId(triggerId.toString());
                        uiObject.setClassName("TransportTrigger");
                        triggers.add(uiObject);
                    }
                }
            }
            if (!triggers.isEmpty()) {
                systemsWithTriggers.put(system.getID().toString(), triggers);
            } else {
                systemsWithoutTriggers.add(system);
            }
        }
        for (System system : systemsWithoutTriggers) {
            Collection<UsageInfo> usageInfo = getManager(System.class).remove(system, ignoreUsages);
            if (usageInfo != null) {
                usingSystems.put(system.getID().toString(), usageInfoListAsString(usageInfo));
            } else {
                getManager(SystemFolder.class).getById(system.getParent().getID()).getObjects().remove(system);
            }
        }
        for (String id : idsOfFoldersToDelete) {
            SystemFolder folder = getManager(SystemFolder.class).getById(id);
            if (Objects.nonNull(folder) && folder.getObjects().isEmpty() && folder.getSubFolders().isEmpty()) {
                getManager(SystemFolder.class).remove(folder, ignoreUsages);
            } else {
                usingSystems.put(folder.getID().toString(), "Folder '" + folder.getName()
                        + "' contains subfolders and/or systems.");
            }
        }
        return new DeleteEntityResultMessage<>(systemsWithTriggers, usingSystems);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/system", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update System with id {{#uiSystem.id}} in the project {{#projectUuid}}")
    public UISystem update(@RequestBody final UISystem uiSystem,
                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.update(uiSystem);
    }

    @Override
    public Class<System> _getGenericUClass() {
        return System.class;
    }

    @Override
    public UISystem _newInstanceTClass(System object) {
        return new UISystem(object);
    }

    @Override
    public Storable _getParent(String parentId) {
        return getManager(Folder.class).getById(parentId);
    }

    @Override
    public System _beforeUpdate(UISystem uiObject, System object) {
        SystemMode mode = SystemMode.fromString(uiObject.getMode());
        object.setMode(mode == null ? SystemMode.STUB : mode);
        if (uiObject.getOperationDefinition() != null && uiObject.getOperationDefinition().isLoaded()) {
            object.setOperationKeyDefinition(uiObject.getOperationDefinition().getData());
        }
        try {
            editContextKeyDefinition(object, uiObject.getIncoming(), uiObject.getOutgoing());
        } catch (Exception e) {
            log.error("Edit ContextKeyDefinition is failed", e);
        }
        return object;
    }

    @Override
    public void _deleteSubObjects(System object) {
        /*  Really?! Delete all templates and all operations under the system without at least confirmation?!
                Done - Confirmation message informs a user that templates and operations will be deleted too
            If this is correct behaviour (I don't think so), why we do NOT delete parsing rules under the system?
         */
        Map<String, Map<String, String>> result = Maps.newHashMap();
        boolean isSafe = haveUsages(object, result, false);
        Set<Template> templates = object.returnTemplates();
        for (Template temp : templates) {
            temp.remove();
        }
        Set<Operation> operations = object.getOperations();
        for (Operation op : operations) {
            for (Template temp : op.returnTemplates()) {
                temp.remove();
            }
        }
        /* Not subObjects but must be deleted too:
            1. Outbound transport configurations under the system
            2. Inbound transport configurations (including triggers) under the system

            This is done on the database side via "on delete cascade" option in two constraints:
                alter table mb_configuration add constraint fk_i8uw39bedn18j0ryhtp79hw4g
                FOREIGN KEY (system_id) REFERENCES mb_systems(id) on delete cascade;
                alter table mb_configurations_param add CONSTRAINT fk_jqaoxtqjckwwj07pms9933w56
                FOREIGN KEY (configuration_id) REFERENCES mb_configuration(id) on delete cascade;
         */
    }

    /**
     * TODO Add JavaDoc.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/usages", method = RequestMethod.GET, produces = "application/json")
    @AuditAction(auditAction = "Get usages of the System with id {{#id}} in the project {{#projectUuid}}")
    public Map<String, Object> getUsages(@RequestParam(value = "id") String id,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        ObjectManager<System> systemManager = getManager(System.class);
        System system = systemManager.getById(id);
        Collection<UsageInfo> usageInfo = systemManager.findUsages(system);
        Map<String, Object> usages = new HashMap<>();
        List<Object> usage = new ArrayList<>();
        if (usageInfo != null) {
            for (UsageInfo item : usageInfo) {
                /* Two types of referers are currently checked:
                    - IntegrationSteps (by Sender and by Receiver) ==> parent is Situation,
                    - Environments (environment.outbound or environment.inbound under System)
                 */
                if (item.getReferer() instanceof Environment) {
                    Environment env = (Environment) item.getReferer();
                    usage.add(new UIEnvironment(env));
                } else if (item.getReferer().getParent() instanceof Situation) {
                    Situation situation = (Situation) item.getReferer().getParent();
                    usage.add(new UISituation(situation));
                } else {
                    usage.add(new UIObject(item.getReferer()));
                }
            }
            usages.put("usages", usage);
        }
        return usages;
    }

    private List<? extends UIObject> getUiObjectsBySystems(String parentId) {
        super.setSimple(true);
        List<? extends UIObject> list;
        list = (Strings.isNullOrEmpty(parentId)) ? super.getAll() : super.getAll(parentId);
        super.setSimple(false);
        return list;
    }

    private Collection<UIIdentifiedObject> getOperationsBySystemAndDirection(System system, boolean isOutbound) {
        Collection<UIIdentifiedObject> toReturn = new ArrayList<>();
        String className = Operation.class.getName();
        for (Operation operation : system.getOperations()) {
            TransportConfiguration transport = operation.getTransport();
            if (transport != null && (
                    (isOutbound && transport.getMep().isOutbound())
                            || (!isOutbound && transport.getMep().isInbound()))) {
                UIIdentifiedObject uiIdentifiedObject = new UIIdentifiedObject();
                uiIdentifiedObject.setId(operation.getID().toString());
                uiIdentifiedObject.setName(operation.getName());
                uiIdentifiedObject.setClassName(className);
                toReturn.add(uiIdentifiedObject);
            }
        }
        return toReturn;
    }

    @Override
    protected void checkVersion(System object, UISystem uiObject) {
        //skip version checking for System
    }
}
