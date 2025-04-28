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

package org.qubership.automation.itf.ui.controls.entities.situation;

import static org.qubership.automation.itf.ui.util.UIHelper.getObjectList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.configuration.ConfigurationException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.qubership.automation.itf.ui.messages.objects.UIOperation;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

@Deprecated
@RestController
public class SituationUtilController {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/usages", method = RequestMethod.POST)
    @AuditAction(auditAction = "Get all usages of selected Situations in the project {{#projectUuid}}")
    public Map<String, Object> getUsages(@RequestBody UIIds uiSituation,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        ObjectManager<Situation> situationManager = CoreObjectManager.getInstance().getManager(Situation.class);
        Map<String, Object> usages = new HashMap<>();
        List<Object> usage = new ArrayList<>();
        for (String situationId : uiSituation.getIds()) {
            Situation situation = situationManager.getById(situationId);
            ControllerHelper.throwExceptionIfNull(situation, "", situationId, Situation.class,
                    "get Situation by id");
            Collection<UsageInfo> usageInfo = situationManager.findUsages(situation);
            Map<String, Object> situationProperties = new HashMap<>();
            Map<String, Object> usageProperties = new HashMap<>();
            List<Object> usagesObjectsList = new ArrayList<>();
            if (usageInfo == null) {
                throw new Exception("Usage info for " + uiSituation + " not found");
            }
            situationProperties.put("situationId", situation.getID().toString());
            situationProperties.put("situationName", situation.getName());
            usageProperties.put("situation", situationProperties);
            for (UsageInfo item : usageInfo) {
                usagesObjectsList.add(getUsagesObjectProperties(item, situation));
            }
            usageProperties.put("usagesObjects", usagesObjectsList);
            usage.add(usageProperties);
        }
        usages.put("usages", usage);
        return usages;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/byparent", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Situations under Operation id {{#id}} in the project {{#projectUuid}}")
    public UIObjectList getSituationsByParent(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<? extends Situation> situations =
                CoreObjectManager.getInstance().getManager(Situation.class).getAllByParentId(id);
        return getObjectList(situations);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/outbound", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Outbound Situations in the project {{#projectId}}/{{#projectUuid}}")
    public UIObjectList getSituationsOutbound(@RequestParam(value = "projectId") BigInteger projectId,
                                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<? extends Situation> situations =
                CoreObjectManager.getInstance().getManager(Situation.class).getAll();
        return getObjectList(situations); // TODO: method name contradicts its real action. Should be revised.
    }

    // This method is deprecated, use SituationController.create(...) method instead of it
    @Deprecated
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/situation/simple", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Situation under Operation with id {{#uiOperation.id}} in the project "
            + "{{#projectUuid}}")
    public UISituation add(@RequestBody UIOperation uiOperation,
                           @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        Operation operation = updateTransportOnOperation(uiOperation);
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).create(operation);
        situation.setName("New Situation");
        CoreObjectManager.getInstance().getManager(IntegrationStep.class).create(situation, IntegrationStep.TYPE);
        if (situation.getMep().isInboundRequest()) {
            OperationEventTrigger eventTrigger = CoreObjectManager.getInstance().getManager(OperationEventTrigger.class)
                    .create(situation, OperationEventTrigger.TYPE);
            situation.fillOperationEventTriggers(Sets.newHashSet(eventTrigger));
        }
        situation.store();
        situation.flush();
        return new UISituation(situation);
    }

    private Map<String, Object> getUsagesObjectProperties(UsageInfo item, Situation situation) {
        Map<String, Object> objectProperties = new HashMap<>();
        objectProperties.put("usagesId", item.getReferer().getParent().getID().toString());
        objectProperties.put("usagesName", item.getReferer().getParent().getName());
        if (item.getReferer() instanceof SituationStep) {
            SituationStep situationStep = (SituationStep) item.getReferer();
            objectProperties.put("stepPosition", situationStep.getOrder() + 1);
            objectProperties.put("objectType", "situationStep");
            objectProperties.put("stepId", situationStep.getID().toString());
            if (situationStep.getSituation() != null && situationStep.getSituation().getID() == situation.getID()) {
                objectProperties.put("isSituation", "true");
            }
            if (!situationStep.getEndSituations().isEmpty()) {
                for (Situation endSituation : situationStep.getEndSituations()) {
                    if (endSituation.getID() == situation.getID()) {
                        objectProperties.put("isEndSituation", "true");
                        break;
                    }
                }
            }
            if (!situationStep.getExceptionalSituations().isEmpty()) {
                for (Situation exSituation : situationStep.getExceptionalSituations()) {
                    if (exSituation.getID() == situation.getID()) {
                        objectProperties.put("isExceptionalSituation", "true");
                        break;
                    }
                }
            }
        } else if (item.getReferer() instanceof SituationEventTrigger) {
            SituationEventTrigger situationInCondition = (SituationEventTrigger) item.getReferer();
            objectProperties.put("situationPosition", situationInCondition.getOn());
            objectProperties.put("operationIdForLink", situationInCondition.getParent().getParent().getID().toString());
            objectProperties.put("objectType", "situation");
        }
        return objectProperties;
    }

    private Operation updateTransportOnOperation(UIOperation uiOperation) {
        Operation operation = CoreObjectManager.getInstance().getManager(Operation.class).getById(uiOperation.getId());
        ControllerHelper.throwExceptionIfNull(operation, uiOperation.getName(), uiOperation.getId(), Operation.class,
                "update Transport on Operation");
        UIWrapper<UITransport> transport = uiOperation.getTransport();
        if (transport == null) {
            throw new ConfigurationException("Transport isn't specified on operation. Please set transport first.");
        }
        TransportConfiguration transportConfiguration =
                CoreObjectManager.getInstance().getManager(TransportConfiguration.class)
                        .getById(transport.getData().getId());
        ControllerHelper.throwExceptionIfNull(transportConfiguration, transport.getData().getName(),
                transport.getData().getId(),
                TransportConfiguration.class, "update Transport on Operation");
        if (!transportConfiguration.equals(operation.getTransport())) {
            operation.setTransport(transportConfiguration);
            operation.store();
        }
        return operation;
    }
}
