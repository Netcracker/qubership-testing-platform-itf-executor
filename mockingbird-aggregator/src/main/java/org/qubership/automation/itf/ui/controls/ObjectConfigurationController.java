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

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ByProject;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.container.StepContainer;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.ui.messages.objects.ExistenceChecker;
import org.qubership.automation.itf.ui.messages.objects.StarterSituationExistanceChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ObjectConfigurationController {

    public final Logger LOGGER = LoggerFactory.getLogger(ObjectConfigurationController.class);

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/exists", method = RequestMethod.POST)
    @AuditAction(auditAction = "Check if Situation exists in the project {{#projectId}}/{{#projectUuid}}")
    public ResponseEntity isSituationExists(
            @RequestBody StarterSituationExistanceChecker checker,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        String situationName = findSituation(checker, projectId);
        return situationName == null
                ? getResponseEntity(HttpStatus.NO_CONTENT) : getResponseEntity(HttpStatus.OK, situationName);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/entity/exists", method = RequestMethod.POST)
    @AuditAction(auditAction = "Check if Entity exists in the project {{#projectId}}/{{#projectUuid}}")
    public ResponseEntity isEntityExists(@RequestBody ExistenceChecker checker,
                                         @RequestParam(value = "projectId") BigInteger projectId,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        Storable storable = checkEntityExisting(checker, projectId);
        if (storable != null) {
            String returnedData = storable instanceof Operation ?
                    storable.getName() + "|" + ((Operation) storable).getTransport().getName() : storable.getName();
            return getResponseEntity(HttpStatus.OK, returnedData);
        }
        return getResponseEntity(HttpStatus.NO_CONTENT);
    }

    private Storable checkEntityExisting(ExistenceChecker checker, BigInteger projectId) {
        if (checker != null && checker.getEntityName() != null) {
            Storable storable = null;
            if ("Template".equals(checker.getEntityClass())) {
                Collection<Template<? extends TemplateProvider>> templates = TemplateHelper
                        .getByParentNameAndProject(checker.getEntityParent(), projectId);
                for (Template<? extends TemplateProvider> template : templates) {
                    if (checker.getEntityName().equals(template.getName())) {
                        storable = template;
                    }
                }
            } else if ("Operation".equals(checker.getEntityClass())) {
                Collection<? extends Operation> operations =
                        CoreObjectManager.getInstance().getSpecialManager(Operation.class, ByProject.class)
                                .getByParentNameAndProject(checker.getEntityParent(), projectId);
                if (operations != null) {
                    for (Operation operation : operations) {
                        Map.Entry<String, String> entry = checker.getParams().entrySet().iterator().next();
                        try {
                            Field field = operation.getClass().getDeclaredField(entry.getKey());
                            field.setAccessible(true);
                            String fieldValue = (String) field.get(operation);
                            if (entry.getValue().equals(fieldValue)) {
                                storable = operation;
                                break;
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            LOGGER.error(String.format("Error while getting the value of %s for %s", entry.getKey(),
                                    operation.getName()));
                        }
                    }
                }
            }
            return storable;
        }
        return null;
    }

    private String findSituation(StarterSituationExistanceChecker checker, BigInteger projectId) {
        //TODO avoid double checking of all situations
        ExistenceChecker parentChecker = checker.getParentChecker();
        if (parentChecker != null) {
            Operation parentOperation = (Operation) checkEntityExisting(parentChecker, projectId);
            if (parentOperation != null) {
                for (Situation situation : parentOperation.getSituations()) {
                    if (isStepExists(checker.getSenderName(), checker.getReceiverName(),
                            checker.getOperationChecker(), checker.getTemplateName(), situation, projectId)) {
                        return situation.getName();
                    }
                }
            }
        }
        return null;
    }

    private boolean isStepExists(String senderName, String receiverName, ExistenceChecker operationChecker,
                                 String templateName, StepContainer stepContainer, BigInteger projectId) {
        for (Step step : stepContainer.getSteps()) {
            IntegrationStep integrationStep = (IntegrationStep) step;
            if (isNamesEquals(senderName, integrationStep.getSender()) && isNamesEquals(receiverName,
                    integrationStep.getReceiver()) && (operationChecker == null
                    || checkEntityExisting(operationChecker, projectId) != null)
                    && isNamesEquals(templateName, integrationStep.returnStepTemplate())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNamesEquals(String objectName, Storable storable) {
        if (storable == null && StringUtils.isBlank(objectName)) {
            return true;
        }
        return storable != null && storable.getName().equalsIgnoreCase(objectName);
    }

    private ResponseEntity<String> getResponseEntity(HttpStatus statusCode, String... response) {
        if (ArrayUtils.isEmpty(response)) {
            return new ResponseEntity<>(statusCode);
        } else {
            return new ResponseEntity<>(response[0], statusCode);
        }
    }
}
