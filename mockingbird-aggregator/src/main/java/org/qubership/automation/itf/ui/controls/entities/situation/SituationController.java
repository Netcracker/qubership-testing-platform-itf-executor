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

import static org.qubership.automation.itf.ui.controls.entities.util.SituationControllerHelper.fillSituation;
import static org.qubership.automation.itf.ui.controls.execute.ExecutorControllerHelper.BULK_VALIDATOR_INTEGRATION;
import static org.qubership.automation.itf.ui.controls.execute.ExecutorControllerHelper.findIntegrationConfig;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.configuration.ConfigurationException;
import org.qubership.automation.itf.core.exceptions.operation.OperationException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.BvCaseContainingObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.LabeledObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SituationObjectManager;
import org.qubership.automation.itf.core.model.IdNamePair;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.engine.EngineControlIntegration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.UIListImpl;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIParsingRule;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.qubership.automation.itf.ui.messages.objects.UISituationExtended;
import org.qubership.automation.itf.ui.util.UIHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SituationController extends AbstractController<UISituation, Situation> {

    private SituationController self;

    @Autowired
    public void setSelf(SituationController self) {
        this.self = self;
    }

    /**
     * Get List of Situations.
     *
     * @param parentId    parent id
     * @param isFull      logical operator used for filling full structure or simple
     * @param projectId   inner project id
     * @param projectUuid project UUID
     * @return {@link UIListImpl} If parentId = 0 - All situations inside project (by projectId)
     *     Otherwise - All situations under parent operation (by parentId)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Situations under Operation by id {{#parentId}} in the project {{#projectUuid}}")
    public UIListImpl getSituations(@RequestParam(value = "parent", defaultValue = "0") String parentId,
                                    @RequestParam(value = "isFull", defaultValue = "true") boolean isFull,
                                    @RequestParam(value = "projectId") BigInteger projectId,
                                    @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        if ("0".equals(parentId)) {
            Collection<IdNamePair> idNamePairCollection =
                    CoreObjectManager.getInstance().getSpecialManager(Situation.class, SituationObjectManager.class)
                            .findAllByProjectIdOfNameAndId(projectId);
            UIListImpl<UISituation> uiSituationUIList = new UIListImpl<>();
            List<UISituation> uiSituations = Lists.newLinkedList();
            for (IdNamePair entry : idNamePairCollection) {
                uiSituations.add(new UISituation(entry.getId(), entry.getName()));
            }
            uiSituationUIList.setObjects(uiSituations);
            return uiSituationUIList;
        }
        Operation operation = CoreObjectManager.getInstance().getManager(Operation.class).getById(parentId);
        ControllerHelper.throwExceptionIfNull(operation, StringUtils.EMPTY, parentId, Operation.class,
                "get Situations under Operation");
        Collection<? extends Situation> situations = operation.getSituations();
        if (!isFull) {
            return UIHelper.getObjectList(situations);
        } else {
            UIListImpl<UISituation> uiSituationUIList = new UIListImpl<>();
            List<UISituation> uiSituations = Lists.newLinkedList();
            for (Situation entry : situations) {
                uiSituations.add(new UISituation(entry, false));
            }
            uiSituationUIList.setObjects(uiSituations);
            return uiSituationUIList;
        }
    }

    /**
     * Get Situation by id.
     *
     * @param id          object id
     * @param projectUuid project UUID
     * @return {@link UISituation} for the situation found
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Situation by id {{#id}} in the project {{#projectUuid}}")
    public UISituation getById(@RequestParam(value = "id", defaultValue = "0") String id,
                               @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    /**
     * This request was added because the page formation on the monitoring tab
     * goes through feign client (see MonitoringController).
     *
     * @param id situation id
     * @return {@link UISituation} for the situation found
     */
    @Transactional(readOnly = true)
    @RequestMapping(value = "/situation/{id}", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Situation by id {{#id}} via feign")
    public UISituation feignGetById(@PathVariable(value = "id") String id) {
        return super.getById(id);
    }

    /**
     * Creates situation under parent operation (identified by parentId).
     *
     * @param parentId    parent id
     * @param projectUuid project UUID
     * @return {@link UISituation} for the situation created
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/situation", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Situation under Operation with id {{#parentId}} in the project {{#projectUuid}}")
    public UISituation create(@RequestParam(value = "operation", defaultValue = "0") String parentId,
                              @RequestParam(value = "projectUuid") UUID projectUuid,
                              @RequestBody UISituation uiAddSituationReq) throws Exception {
        Operation operation = ControllerHelper.getManager(Operation.class).getById(parentId);
        ControllerHelper.throwExceptionIfNull(operation, "", parentId, Operation.class,
                "create Situation under Operation");
        UISituation emptyUISituation = super.create(parentId);
        if (uiAddSituationReq == null || StringUtils.isBlank(uiAddSituationReq.getName())) {
            return emptyUISituation;
        }
        Situation situation = ControllerHelper.getManager(Situation.class).getById(emptyUISituation.getId());
        situation.setName(uiAddSituationReq.getName());
        fillSituation(uiAddSituationReq, situation, operation);
        situation.store();
        return new UISituation(situation);
    }

    /**
     * Creates situation under parent operation (identified by id).
     * Creates a new Template, fills its fields.
     * Fills situation.
     *
     * @param id                operation id
     * @param uiAddSituationReq {@link UISituationExtended}
     * @param projectUuid       project UUID
     * @return {@link UISituation} for the situation created
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'CREATE') and @entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/createTemplateAndSituation", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Template and Situation under Operation with id {{#id}} in the project "
            + "{{#projectUuid}}")
    public UISituation createTemplateAndSituation(@RequestParam(value = "operation", defaultValue = "0") String id,
                                                  @RequestBody UISituationExtended uiAddSituationReq,
                                                  @RequestParam(value = "projectUuid") UUID projectUuid) {
        Operation operation = ControllerHelper.getManager(Operation.class).getById(id);
        ControllerHelper.throwExceptionIfNull(operation, "", id, Operation.class,
                "create Template and Situation under Operation");
        Situation situation = ControllerHelper.getManager(Situation.class).create(operation);
        situation.setName(uiAddSituationReq.getUiSituation().getName());
        UIObject uiObject = uiAddSituationReq.getUiSituation().getTemplate();
        if (uiObject.getId() == null) {
            OperationTemplate template =
                    CoreObjectManager.getInstance().getManager(OperationTemplate.class).create(operation);
            template.setName(uiAddSituationReq.getUiSituation().getTemplate().getName());
            template.setText(uiAddSituationReq.getContent());
            uiObject.setId(template.getID().toString());
        }
        fillSituation(uiAddSituationReq.getUiSituation(), situation, operation);
        situation.store();
        return new UISituation(situation);
    }

    /**
     * Update Situation based on uiSituation object received.
     *
     * @param uiSituation {@link UISituation} ui situation
     * @param projectUuid project UUID
     * @return {@link UISituation} new object constructed from updated Situation
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/situation", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Situation with id {{#uiSituation.id}} in the project {{#projectUuid}}")
    public UISituation update(@RequestBody UISituation uiSituation,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        self._update(uiSituation);
        return self.refresh(uiSituation);
    }

    @Transactional
    public void _update(UISituation uiSituation) {
        super.update(uiSituation);
    }

    @Transactional(readOnly = true)
    public UISituation refresh(UISituation uiSituation) {
        Situation updatedSituation =
                CoreObjectManager.getInstance().getManager(Situation.class).getById(uiSituation.getId());
        return new UISituation(updatedSituation);
    }

    /**
     * Get situation scripts.
     *
     * @param id          situation id
     * @param projectUuid project UUID
     * @return String[] array of scripts
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/scripts", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Scripts of the Situation with id {{#id}} in the project {{#projectUuid}}")
    public String[] getScripts(@RequestParam(value = "id", defaultValue = "0") String id,
                               @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (StringUtils.isBlank(id)) {
            throw new ConfigurationException("Empty situation id is given. Situation must be stored first!");
        }
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(id);
        ControllerHelper.throwExceptionIfNull(situation, "", id, Situation.class,
                "get Scripts under Situation");
        String[] result = new String[3];
        result[0] = situation.getPreScript();
        result[1] = situation.getPostScript();
        result[2] = situation.getPreValidationScript();
        return result;
    }

    /**
     * Delete situation(s) by ids.
     * Usages checking is performed. If at least one situation has usages, deletion is cancelled,
     * and IllegalArgumentException is thrown.
     *
     * @param uiDeleteObjectReq {@link UIIds} ids object to delete
     * @param projectId         inner project id
     * @param projectUuid       project UUID
     * @return List of {@link UIObject} for deleted objects
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/situation", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Situations from Project {{#projectId}}/{{#projectUuid}}")
    public List<UIObject> delete(@RequestBody UIIds uiDeleteObjectReq,
                                 @RequestParam(value = "projectId") BigInteger projectId,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        ObjectManager<Situation> situationManager = CoreObjectManager.getInstance().getManager(Situation.class);
        List<Situation> situations = new ArrayList<>();
        for (String situationId : uiDeleteObjectReq.getIds()) {
            Situation situation = situationManager.getById(situationId);
            if (situation == null) {
                continue; // Situation is already deleted by someone, or id is wrong
            }
            Collection<UsageInfo> usages = situationManager.findUsages(situation);
            interruptIfSituationHasUsages(situation, usages);
            situations.add(situation);
        }
        List<UIObject> uiDeletedObjects = new ArrayList<>();
        for (Situation situation : situations) {
            uiDeletedObjects.add(new UIObject(situation));
            situation.getParent().getSituations().remove(situation);
            super.delete(situation);
            log.info("Situation is deleted: {}", situation);
        }
        return uiDeletedObjects;
    }

    /**
     * Create BV testcase linked with the situation (identified by situationId),
     * or link existing BV testcase with the situation - in case bvLink is provided.
     *
     * @param situationId situation id
     * @param bvTcId      bv testcase uuid
     * @param projectId   inner project id
     * @param projectUuid project UUID
     * @return BV testcase link
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/situation/integration/bv", method = RequestMethod.GET, produces = "text/plain")
    @AuditAction(auditAction = "Create BVCase with bvLink {{#bvTcId}} on Situation with id {{#situationId}} in the "
            + "project {{#projectId}}")
    public String createBvCaseOnSituation(@RequestParam(value = "situationId") String situationId,
                                          @RequestParam(value = "projectId") BigInteger projectId,
                                          @RequestParam(value = "bvTcId", required = false) String bvTcId,
                                          @RequestParam(value = "projectUuid") UUID projectUuid) throws IOException {
        if (StringUtils.isBlank(situationId)) {
            throw new ConfigurationException("Empty situation id is given. Situation must be stored first!");
        }
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(situationId);
        ControllerHelper.throwExceptionIfNull(situation, "", situationId, Situation.class,
                "get BV Test Case");
        EngineControlIntegration engine = (EngineControlIntegration) EngineIntegrationRegistry.getInstance()
                .find(BULK_VALIDATOR_INTEGRATION);
        if (engine != null) {
            IntegrationConfig integrationConf = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
            if (integrationConf != null) {
                if (bvTcId == null) {
                    engine.create(situation, integrationConf, projectId);
                } else {
                    situation.setBvTestcase(bvTcId);
                    if (!engine.isExist(situation, integrationConf, null, projectId)) {
                        throw new ConfigurationException("BV Test Case with ID '\" + bvTcId + \"' isn't found");
                    }
                }
                return situation.getBvTestcase();
            } else {
                throw new ConfigurationException("Bulk Validator Integration config isn't found for the project!"
                        + "\nPlease check 'Integration Configurations' and configure BV integration.");

            }
        } else {
            throw new ConfigurationException("Bulk Validator Integration Engine isn't found in the registry!");
        }
    }

    /**
     * Delete or unlink BV testcase from the situation (identified by situationId).
     *
     * @param situationId situation id
     * @param isDeleting  logical operator used for deleting integration
     * @param projectId   inner project id
     * @param projectUuid project UUID
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/situation/integration/bv", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete or unlink BVCase on Situation with id {{#situationId}} in the project "
            + "{{#projectId}} / {{#projectUuid}}")
    public void deleteOrUnlinkBvCaseOnSituation(@RequestParam(value = "situationId") String situationId,
                                                @RequestParam(value = "projectId") BigInteger projectId,
                                                @RequestParam(value = "isDeleting") Boolean isDeleting,
                                                @RequestParam(value = "projectUuid") UUID projectUuid)
            throws IOException {
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(situationId);
        ControllerHelper.throwExceptionIfNull(situation, "", situationId, Situation.class,
                "get situation by id");
        if (isDeleting && (
                CoreObjectManager.getInstance().getSpecialManager(Situation.class, BvCaseContainingObjectManager.class)
                        .countBvCaseUsages(situation.getBvTestcase()) == 1)) {
            IntegrationConfig bvConfig = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
            if (bvConfig != null) {
                EngineControlIntegration engine = (EngineControlIntegration) EngineIntegrationRegistry.getInstance()
                        .find(BULK_VALIDATOR_INTEGRATION);
                engine.delete(situation, bvConfig, projectId);
            }
        }
        situation.setBvTestcase("");
    }

    /**
     * Get situations (with parent operation and system info) using BV testcases.
     *
     * @param projectId   inner project id
     * @param projectUuid project UUID
     * @return list with situations
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/getSituationsWithBvLinks", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Situations with BvLinks in the project {{#projectId}}/{{#projectUuid}}")
    public List<Object[]> getSituationsWithBvLinks(@RequestParam(value = "projectId") BigInteger projectId,
                                                   @RequestParam(value = "projectUuid") UUID projectUuid) {
        return CoreObjectManager.getInstance().getSpecialManager(Situation.class, BvCaseContainingObjectManager.class)
                .getObjectsWithBvLinks(projectId);
    }

    /**
     * Get situation's parsing rules.
     *
     * @param id          situation id
     * @param projectUuid project UUID
     * @return set {@link UIParsingRule} parsing rule
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/parsingRules", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get ParsingRules for Situation by id {{#id}} in the project {{#projectUuid}}")
    public Set<UIParsingRule> getParsingRules(@RequestParam(value = "id", defaultValue = "0") String id,
                                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        Set<UIParsingRule> result = Sets.newHashSet();
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(id);
        ControllerHelper.throwExceptionIfNull(situation, "", id, Situation.class,
                "get Parsing Rules under Situation");
        Set<ParsingRule> parsingRules = situation.getParsingRules();
        if (parsingRules != null) {
            for (ParsingRule parsingRule : parsingRules) {
                UIParsingRule uiParsingRule = new UIParsingRule(parsingRule);
                result.add(uiParsingRule);
            }
        }
        return result;
    }

    /**
     * Get all 'situation'-labels by project id.
     *
     * @param projectId   inner project id
     * @param projectUuid project UUID
     * @return set with labels
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/label", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Situation Labels in the project {{#projectId}}/{{#projectUuid}}")
    public Set<String> getLabels(@RequestParam(value = "projectId") BigInteger projectId,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        return CoreObjectManager.getInstance().getSpecialManager(Situation.class,
                LabeledObjectManager.class).getAllLabels(projectId);
    }

    @Override
    protected Situation _beforeUpdate(UISituation uiSituation, Situation situation) {
        IntegrationStep integrationStep = situation.getIntegrationStep();
        if (integrationStep == null) {
            integrationStep = ControllerHelper.getManager(IntegrationStep.class)
                    .create(situation, IntegrationStep.TYPE);
        }
        if (stepParamsAreDifferent(uiSituation.getReceiver(), integrationStep.getReceiver())) {
            integrationStep.setReceiver(uiSituation.getReceiver() != null
                    ? ControllerHelper.getManager(System.class).getById(uiSituation.getReceiver().getId())
                    : null);
        }
        if (stepParamsAreDifferent(uiSituation.getOperation(), integrationStep.getOperation())) {
            integrationStep.setOperation(uiSituation.getOperation() != null
                    ? ControllerHelper.getManager(Operation.class).getById(uiSituation.getOperation().getId())
                    : null);
        }
        if (stepParamsAreDifferent(uiSituation.getTemplate(), integrationStep.returnStepTemplate())) {
            integrationStep.setTemplate(uiSituation.getTemplate() != null
                    ? TemplateHelper.getById(uiSituation.getTemplate().getId())
                    : null);
        }
        integrationStep.setUnit(uiSituation.getUnit());
        integrationStep.setDelay(uiSituation.getDelay());
        if (uiSituation.getParsingRules() != null && uiSituation.getParsingRules().isLoaded()) {
            Set<OperationParsingRule> operationParsingRulesSet = Sets.newHashSetWithExpectedSize(100);
            for (UIParsingRule uiParsingRule : uiSituation.getParsingRules().getData()) {
                OperationParsingRule parsingRule = CoreObjectManager.getInstance()
                        .getManager(OperationParsingRule.class).getById(uiParsingRule.getId());
                if (parsingRule != null) {
                    ControllerHelper.validateParsingRule(uiParsingRule, parsingRule);
                    parsingRule.setName(uiParsingRule.getName());
                    parsingRule.fillProperties(uiParsingRule.getParamName(), uiParsingRule.getType(),
                            uiParsingRule.getExpression(), uiParsingRule.getMultiple(), uiParsingRule.getAutosave());
                    operationParsingRulesSet.add(parsingRule);
                }
            }
            situation.setParsingRules(operationParsingRulesSet);
        }
        if (uiSituation.getPreScript() != null && uiSituation.getPreScript().isLoaded()) {
            situation.setPreScript(uiSituation.getPreScript().getData());
        }
        if (uiSituation.getPostScript() != null && uiSituation.getPostScript().isLoaded()) {
            situation.setPostScript(uiSituation.getPostScript().getData());
        }
        if (uiSituation.getPreValidationScript() != null && uiSituation.getPreValidationScript().isLoaded()) {
            situation.setPreValidationScript(uiSituation.getPreValidationScript().getData());
        }
        return super._beforeUpdate(uiSituation, situation);
    }

    @Override
    protected Class<Situation> _getGenericUClass() {
        return Situation.class;
    }

    @Override
    protected UISituation _newInstanceTClass(Situation object) {
        return new UISituation(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        return ControllerHelper.getManager(Operation.class).getById(parentId);
    }

    private boolean stepParamsAreDifferent(UIObject newParamValue, Storable oldParamValue) {
        if (newParamValue == null) {
            return (oldParamValue != null);
        } else if (oldParamValue == null) {
            return true;
        } else {
            return !newParamValue.getId().equals(oldParamValue.getID().toString());
        }
    }

    private void interruptIfSituationHasUsages(Situation situation, Collection<UsageInfo> usages) {
        if (Objects.isNull(usages) || usages.isEmpty()) {
            return;
        }
        throw new OperationException("Delete Situation", String.format(
                "Situation [name: '%s', id: '%s'] is used on callChain step(s) and/or on situation event trigger(s).\n"
                        + " Please find usages of this situation (use UI button 'Usages' on situation)"
                        + " and remove trigger(s) and(or) callChain step(s) first",
                situation.getName(), situation.getID()));
    }

    @Override
    protected void checkVersion(Situation object, UISituation uiObject) {
        //skip version checking for situation
    }
}
