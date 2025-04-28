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

package org.qubership.automation.itf.ui.controls.entities;

import static org.qubership.automation.itf.ui.util.UIHelper.convertMapOfTypeToUITypeList;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.EventTriggerBriefInfo;
import org.qubership.automation.itf.core.model.communication.StubUser;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.helper.ClassResolver;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.TriggerProvider;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.UITypeList;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UIEventTrigger;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.qubership.automation.itf.ui.util.EventTriggerHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Collections2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Tags({
        @Tag(name = SwaggerConstants.TRIGGER_QUERY_API,
                description = SwaggerConstants.TRIGGER_QUERY_API_DESCR),
        @Tag(name = SwaggerConstants.TRIGGER_COMMAND_API,
                description = SwaggerConstants.TRIGGER_COMMAND_API_DESCR)
})
public class EventTriggerController extends AbstractController<UIEventTrigger, EventTrigger> {

    public static EventTriggerSyncActivationRequest synchronizeTriggers(
            final Collection<UIEventTrigger> uiEventTriggers, TriggerProvider parent) {
        List<EventTriggerBriefInfo> triggersToDeactivate = new ArrayList<>();
        List<EventTriggerBriefInfo> triggersToReactivate = new ArrayList<>();
        if (uiEventTriggers != null) {
            List<String> uiEventTriggersIds = uiEventTriggers.stream().map(UIEventTrigger::getId)
                    .collect(Collectors.toList());
            //first - delete all missed triggers under parent
            Collection<EventTriggerBriefInfo> triggersToDelete = parent.getAllEventTriggers().stream()
                    .filter(eventTrigger -> !uiEventTriggersIds.contains(String.valueOf(eventTrigger.getID())))
                    .map(eventTrigger ->
                            new EventTriggerBriefInfo((BigInteger) eventTrigger.getID(), eventTrigger.getType()))
                    .collect(Collectors.toList());
            triggersToDeactivate.addAll(triggersToDelete);
            //second - add triggers which have no ids in request
            Collection<UIEventTrigger> triggersToAdd = Collections2.filter(uiEventTriggers,
                    input -> StringUtils.isBlank(input.getId()));
            for (UIEventTrigger uiEventTrigger : triggersToAdd) {
                EventTrigger trigger = EventTriggerHelper.create((Storable) parent, uiEventTrigger.getType());
                uiEventTrigger.fillTrigger(trigger);
                if (trigger instanceof OperationEventTrigger) {
                    parent.getOperationEventTriggers().add((OperationEventTrigger) trigger);
                } else {
                    parent.getSituationEventTriggers().add((SituationEventTrigger) trigger);
                }
            }
            //and at last - update all others
            Collection<UIEventTrigger> triggersToModify = Collections2.filter(uiEventTriggers,
                    input -> StringUtils.isNotBlank(input.getId()));
            for (UIEventTrigger uiEventTrigger : triggersToModify) {
                String eventTriggerId = uiEventTrigger.getId();
                EventTrigger trigger = EventTriggerHelper.getByIdAndType(eventTriggerId, uiEventTrigger.getType());
                if (trigger != null) {
                    uiEventTrigger.fillTrigger(trigger);
                    triggersToReactivate.add(
                            new EventTriggerBriefInfo(new BigInteger(eventTriggerId), uiEventTrigger.getType()));
                } else {
                    log.warn("Event trigger not found by id {}; skipped", eventTriggerId);
                }
            }
        }
        return new EventTriggerSyncActivationRequest(triggersToDeactivate, triggersToReactivate, new StubUser(""), "");
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRIGGER.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/trigger", method = RequestMethod.GET)
    @Operation(summary = "GetTrigger", description = "Retrieve trigger by id", tags =
            {SwaggerConstants.TRIGGER_QUERY_API})
    @AuditAction(auditAction = "Get Event Trigger by id {{#id}} in the project {{#projectUuid}}")
    public UIEventTrigger get(@RequestParam(value = "id") String id,
                              @RequestParam(value = "type") String type,
                              @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        EventTrigger eventTrigger = StringUtils.isEmpty(type)
                ? EventTriggerHelper.getById(id) : EventTriggerHelper.getByIdAndType(id, type);
        ControllerHelper.throwExceptionIfNull(eventTrigger, "", id, EventTrigger.class, "get Event Trigger by id");
        return new UIEventTrigger(eventTrigger);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRIGGER.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/trigger/state", method = RequestMethod.GET)
    @Operation(summary = "GetTriggerState",
            description = "Retrieve trigger status by id",
            tags = {SwaggerConstants.TRIGGER_QUERY_API})
    @AuditAction(auditAction = "Get Event Trigger state by id {{#id}} in the project {{#projectUuid}}")
    public String getState(@RequestParam(value = "id") String id,
                           @RequestParam(value = "type") String type,
                           @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        EventTrigger eventTrigger = StringUtils.isEmpty(type)
                ? EventTriggerHelper.getById(id) : EventTriggerHelper.getByIdAndType(id, type);
        ControllerHelper.throwExceptionIfNull(eventTrigger, "", id, EventTrigger.class, "get Event Trigger by id");
        return "state: " + eventTrigger.getState();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRIGGER.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/trigger/types", method = RequestMethod.GET)
    @Operation(summary = "GetTriggerTypes",
            description = "Retrieve trigger types",
            tags = {SwaggerConstants.TRIGGER_QUERY_API})
    @AuditAction(auditAction = "Get Event Trigger types supported, project {{#projectUuid}}")
    public UITypeList getTypes(@RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, String> objectTriggersTypes = ClassResolver.getInstance().resolveByInterface(EventTrigger.class);
        return convertMapOfTypeToUITypeList(objectTriggersTypes);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRIGGER.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/trigger", method = RequestMethod.POST)
    @Operation(summary = "CreateTrigger",
            description = "Create trigger by specified situation ID",
            tags = {SwaggerConstants.TRIGGER_COMMAND_API})
    @AuditAction(auditAction = "Create Event Trigger under Situation id {{#parentId}} in the project {{#projectUuid}}")
    public UIEventTrigger create(
            @RequestParam(value = "parentId") String parentId,
            @RequestParam(value = "projectUuid") UUID projectUuid
    ) throws Exception {
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(parentId);
        ControllerHelper.throwExceptionIfNull(situation, "", parentId, Situation.class, "get Situation by id");
        SituationEventTrigger eventTrigger = CoreObjectManager.getInstance().getManager(SituationEventTrigger.class)
                .create(situation, SituationEventTrigger.TYPE);
        situation.getSituationEventTriggers().add(eventTrigger);
        return new UIEventTrigger(eventTrigger);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRIGGER.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/trigger", method = RequestMethod.PUT)
    @Operation(summary = "UpdateTrigger",
            description = "Update trigger by EditRequest",
            tags = {SwaggerConstants.TRIGGER_COMMAND_API})
    @AuditAction(auditAction = "Update Event Trigger by id {{#id}} in the project {{#projectUuid}}")
    public void put(
            @RequestParam(value = "id") String id,
            @RequestBody UIEventTrigger editRequest,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        EventTrigger eventTrigger = EventTriggerHelper.getByIdAndType(id, editRequest.getType());
        ControllerHelper.throwExceptionIfNull(eventTrigger, "", id, EventTrigger.class, "get Event Trigger by id");
        editRequest.fillTrigger(eventTrigger);
        eventTrigger.store();
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRIGGER.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/trigger", method = RequestMethod.DELETE)
    @Operation(summary = "DeleteTriggers",
            description = "Delete triggers by id. Currently disabled.",
            tags = {SwaggerConstants.TRIGGER_QUERY_API})
    @AuditAction(auditAction = "Delete Event Triggers in the project {{#projectUuid}}")
    @Deprecated
    public void delete(
            @RequestParam(value = "system", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestBody UIIds deleteRequest) throws IOException {
        //Not used
    }

    @Override
    protected Class<EventTrigger> _getGenericUClass() {
        return EventTrigger.class;
    }

    @Override
    protected UIEventTrigger _newInstanceTClass(EventTrigger object) {
        return new UIEventTrigger(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        return CoreObjectManager.getInstance().getManager(Situation.class).getById(parentId);
    }
}
