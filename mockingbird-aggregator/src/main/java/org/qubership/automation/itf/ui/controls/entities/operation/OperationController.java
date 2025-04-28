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

package org.qubership.automation.itf.ui.controls.entities.operation;

import static org.qubership.automation.itf.ui.controls.entities.util.KeyDefinitionControllerHelper.editContextKeyDefinition;
import static org.qubership.automation.itf.ui.controls.entities.util.SituationControllerHelper.synchronizeSituations;
import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;
import org.qubership.automation.itf.core.model.communication.message.delete.DeleteEntityResultMessage;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.Pair;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIOperation;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UIEventTriggerSyncActivationRequest;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationController extends AbstractController<UIOperation, Operation> {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).OPERATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/operation/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get {{#direction}} Operations under System id {{#id}} in the project {{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "system", required = false) String id,
                                           @RequestParam(value = "displayType", defaultValue = "selectList") String displayType,
                                           @RequestParam(value = "direction", defaultValue = "all") String direction,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        if ("selectList".equals(displayType)) {
            super.setSimple(true);
            List<? extends UIObject> list = (StringUtils.isNotBlank(direction) && !direction.equals("all"))
                    ? super.getAllSuitable(id, direction) : super.getAll(id);
            super.setSimple(false);
            return list;
        } else {
            List<UIOperation> list = new ArrayList<>();
            boolean onlyInbound = StringUtils.isNotBlank(direction) && "inbound".equals(direction);
            boolean onlyOutbound = StringUtils.isNotBlank(direction) && "outbound".equals(direction);
            Collection<? extends Operation> operations = getManager(Operation.class).getAllByParentId(id);
            for (Operation operation : operations) {
                if ((onlyInbound && !operation.getMep().isInboundRequest())
                        || (onlyOutbound && !operation.getMep().isOutboundRequest())) {
                    continue;
                }
                UIOperation uiOperation = new UIOperation();
                uiOperation.fillForQuickDisplay(operation);
                list.add(uiOperation);
            }
            return list;
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).OPERATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/operation", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Operation by id {{#id}} in the project {{#projectUuid}}")
    public UIOperation getById(@RequestParam(value = "id", defaultValue = "0") String id,
                               @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    /*
    ATPII-30543: this request was added because the page formation on the monitoring tab goes through feign client
    row - 508, MonitoringController.class
     */
    @Transactional(readOnly = true)
    @RequestMapping(value = "/operation/{id}", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Operation by id {{#id}} via feign")
    public UIOperation feignGetById(@PathVariable(value = "id") String id) {
        return super.getById(id);
    }


    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).OPERATION.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/operation", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Operation under System id {{#id}} in the project {{#projectUuid}}")
    public UIOperation create(@RequestParam(value = "system", defaultValue = "0") String id,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.create(id);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).OPERATION.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/operation", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Operation in the project {{#projectUuid}}")
    public Pair<UIOperation, UIEventTriggerSyncActivationRequest> update(@RequestBody UIOperation uiOperation,
                                                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        Operation object = manager().getById(uiOperation.getId());
        beforeStoreUpdated(object, uiOperation);
        EventTriggerSyncActivationRequest request = beforeUpdate(uiOperation, object);
        UIOperation updatedUiOperation = storeUpdated(object, uiOperation);
        return new Pair<>(updatedUiOperation, new UIEventTriggerSyncActivationRequest(request));
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).OPERATION.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/operation", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Operations from system id {{#id}} in the project {{#projectUuid}}")
    public Map<String, Object> delete(
            @RequestParam(value = "ignoreUsages", defaultValue = "false") Boolean ignoreUsages,
            @RequestParam(value = "system", defaultValue = "0") String id,
            @RequestBody UIIds deleteRequest,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        System system = getManager(System.class).getById(id);
        ControllerHelper.throwExceptionIfNull(system, "", id, System.class, "delete operation");
        List<Operation> filteredOperations = system.getOperations().stream().filter(
                operation -> Arrays.asList(deleteRequest.getIds()).contains(operation.getID().toString())
        ).collect(Collectors.toList());
        Map<String, List<UIObject>> operationsWithTriggers = new HashMap<>();
        Map<String, String> usingOperations = new HashMap<>();
        List<Operation> operationsWithoutTriggers = new ArrayList<>();

        for (Operation operation : filteredOperations) {
            List<UIObject> eventTriggers = new ArrayList<>();
            Map<String, List<BigInteger>> blockingTriggers = getManager(Operation.class)
                    .findImportantChildren(operation);
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
                        eventTriggers.add(uiObject);
                    }
                }
            }
            if (!eventTriggers.isEmpty()) {
                operationsWithTriggers.put(operation.getID().toString(), eventTriggers);
            } else {
                operationsWithoutTriggers.add(operation);
            }
        }
        Iterator<Operation> iter = operationsWithoutTriggers.iterator();
        while (iter.hasNext()) {
            Operation operation = iter.next();
            Collection<UsageInfo> usageInfo = getManager(Operation.class).remove(operation, ignoreUsages);
            if (usageInfo != null) {
                usingOperations.put(operation.getID().toString(), usageInfoListAsString(usageInfo));
            }
            iter.remove();
        }
        system.store();
        system.flush();
        Map<String, Object> result = new HashMap<>();
        result.put("parentVersion", system.getVersion());
        result.put("result", new DeleteEntityResultMessage<>(operationsWithTriggers, usingOperations));
        return result;
    }

    protected EventTriggerSyncActivationRequest beforeUpdate(UIOperation uiOperation, Operation operation) {
        updateTransport(uiOperation, operation);
        updateDefinitionKey(uiOperation, operation);
        editContextKeyDefinition(operation, uiOperation.getIncoming(), uiOperation.getOutgoing());
        return synchronizeSituations(uiOperation, operation);
    }

    @Override
    protected boolean _isObjectSuitable(Operation object, String... param) {
        if (Objects.nonNull(param[0])) {
            return ("outbound".equals(param[0]) && object.getMep().isOutboundRequest()) ||
                    ("inbound".equals(param[0]) && object.getMep().isInboundRequest()) ||
                    ("all".equals(param[0]));
        }
        throw new RuntimeException("Param [0] for Suitable can't be null");
    }

    @Override
    protected Class<Operation> _getGenericUClass() {
        return Operation.class;
    }

    @Override
    protected UIOperation _newInstanceTClass(Operation object) {
        return new UIOperation(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        System system = getManager(System.class).getById(parentId);
        ControllerHelper.throwExceptionIfNull(system, "", parentId, System.class, "get System by id");
        return system;
    }

    private void updateTransport(UIOperation uiOperation, Operation operation) {
        if (uiOperation.getTransport() != null) {
            UITransport uiTransport = uiOperation.getTransport().getData();
            if (uiTransport != null) {
                TransportConfiguration transport =
                        getManager(TransportConfiguration.class).getById(uiTransport.getId());
                if (transport != null) {
                    operation.setTransport(transport);
                }
            }
        }
    }

    private void updateDefinitionKey(UIOperation uiOperation, Operation operation) {
        if (uiOperation.getDefinitionKey() == null) {
            operation.setOperationDefinitionKey(null);
        } else {
            operation.setOperationDefinitionKey(uiOperation.getDefinitionKey().getData().getKey());
        }
    }

    @Override
    protected void checkVersion(Operation object, UIOperation uiObject) {
        //skip version checking for Operation
    }
}
