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

package org.qubership.automation.itf.ui.controls.entities.parsingrule;

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.operation.OperationException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.OperationParsingRuleObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemParsingRuleObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIObjectsToDelete;
import org.qubership.automation.itf.ui.messages.objects.UIParsingRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@RestController
public class ParsingRuleController extends AbstractController<UIParsingRule, ParsingRule> {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).PARSING_RULE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/parsingrule/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all ParsingRules by parent id {{#parentId}} in the project {{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "parent", defaultValue = "0") String parentId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        ParsingRuleProvider parent = _getParent(parentId);
        return getManagerByParent(parent).getAllByParentId(parentId).stream().map(this::_newInstanceTClass).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).PARSING_RULE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/parsingrule", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get ParsingRule by id {{#id}} in the project {{#projectUuid}}")
    public UIParsingRule getById(@RequestParam(value = "id", defaultValue = "0") String id,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).PARSING_RULE.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/parsingrule", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create ParsingRule under parent id {{#id}} of type {{#type}} in the project " +
            "{{#projectUuid}}")
    public UIParsingRule create(@RequestParam(value = "parent", defaultValue = "0") String parentId,
                                @RequestParam(value = "projectUuid") UUID projectUuid,
                                @RequestParam(value = "type") Class<? extends Storable> type) {
        ParsingRuleProvider parent =
                (ParsingRuleProvider) CoreObjectManager.getInstance().getManager(type).getById(parentId);
        return new UIParsingRule(getManagerByParent(parent).create(parent));
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).PARSING_RULE.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/parsingrule", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update ParsingRule by id {{#parsingRule.id}} in the project {{#projectUuid}}")
    public UIParsingRule update(@RequestBody UIParsingRule parsingRule,
                                @RequestParam(value = "projectUuid") UUID projectUuid) {
        ObjectManager<? extends ParsingRule<? extends ParsingRuleProvider>>
                objectManager = SystemParsingRule.class.getName().equals(parsingRule.getClassName())
                ? ControllerHelper.getManager(SystemParsingRule.class)
                : ControllerHelper.getManager(OperationParsingRule.class);
        return updateUIObject(objectManager.getById(parsingRule.getId()), parsingRule);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).PARSING_RULE.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/parsingrule", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete ParsingRules from project {{#projectUuid}}")
    public List<UIObject> delete(@RequestBody UIObjectsToDelete uiObjectsToDelete,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        List<ParsingRule<? extends ParsingRuleProvider>> parsingRules = Lists.newArrayListWithExpectedSize(
                uiObjectsToDelete.getUiDeleteObjectReq().length);
        Map<ParsingRule<? extends ParsingRuleProvider>, List<String>> usages = new HashMap<>();
        Set<Storable> parents = new HashSet<>();
        for (String id : uiObjectsToDelete.getUiDeleteObjectReq()) {
            ParsingRule<? extends ParsingRuleProvider> parsingRule = getParsingRule(new BigInteger(id),
                    uiObjectsToDelete.getParentClassName());
            if (parsingRule != null) {
                parsingRules.add(parsingRule);
                parents.add(parsingRule.getParent());
                if (parsingRule instanceof OperationParsingRule) {
                    findSituationsUsingParsingRule(parsingRule, usages);
                }
            }
        }
        if (usages.isEmpty()) {
            List<UIObject> deletedParsingRules = Lists.newArrayListWithExpectedSize(parsingRules.size());
            for (ParsingRule<? extends ParsingRuleProvider> parsingRule : parsingRules) {
                deletedParsingRules.add(new UIObject(parsingRule));
                parsingRule.getParent().removeParsingRule(parsingRule);
                parsingRule.remove();
            }
            return deletedParsingRules;
        } else {
            throw new OperationException("Delete", makeErrorMessage(usages));
        }

    }

    private void findSituationsUsingParsingRule(ParsingRule<? extends ParsingRuleProvider> checkedParsingRule,
                                                Map<ParsingRule<? extends ParsingRuleProvider>, List<String>> usages) {
        List<String> names = new ArrayList<>();
        for (Situation situation : ((Operation) checkedParsingRule.getParent()).getSituations()) {
            for (ParsingRule parsingRule : situation.getParsingRules()) {
                if (parsingRule.getID().equals(checkedParsingRule.getID())) {
                    names.add(situation.getName());
                    break;
                }
            }
        }
        if (!names.isEmpty()) {
            usages.put(checkedParsingRule, names);
        }
    }

    private String makeErrorMessage(Map<ParsingRule<? extends ParsingRuleProvider>, List<String>> usages) {
        StringBuilder errorMessageBuilder = new StringBuilder("Some of parsing rules to be deleted are selected in " +
                "situations,\n please unselect them first:");
        usages.forEach((key, value) -> {
            errorMessageBuilder.append("\nParsingRule: '").append(key.getParamName()).append("' is used in: ");
            boolean started = false;
            for (String str : value) {
                if (started) {
                    errorMessageBuilder.append(", ");
                } else {
                    started = true;
                }
                errorMessageBuilder.append('\'').append(str).append('\'');
            }
        });
        return errorMessageBuilder.toString();
    }

    @Override
    protected ParsingRule _beforeUpdate(UIParsingRule uiParsingRule, ParsingRule parsingRule) {
        ControllerHelper.validateParsingRule(uiParsingRule, parsingRule);
        parsingRule.fillProperties(uiParsingRule.getParamName(), uiParsingRule.getType(),
                uiParsingRule.getExpression(), uiParsingRule.getMultiple(), uiParsingRule.getAutosave());
        return super._beforeUpdate(uiParsingRule, parsingRule);
    }

    @Override
    protected Class<ParsingRule> _getGenericUClass() {
        return ParsingRule.class;
    }

    @Override
    protected UIParsingRule _newInstanceTClass(ParsingRule object) {
        return new UIParsingRule(object);
    }

    @Override
    protected ParsingRuleProvider _getParent(String parentId) {
        ParsingRuleProvider parent = getManager(System.class).getById(parentId);
        if (Objects.isNull(parent)) {
            parent = getManager(Operation.class).getById(parentId);
        }
        ControllerHelper.throwExceptionIfNull(
                parent,
                null,
                parentId,
                ParsingRuleProvider.class,
                "find parent System or Operation to create parsing rule under it");
        return parent;
    }

    private ObjectManager<? extends ParsingRule<? extends ParsingRuleProvider>> getManagerByParent(ParsingRuleProvider parent) {
        return parent instanceof System
                ? ControllerHelper.getManager(SystemParsingRule.class)
                : ControllerHelper.getManager(OperationParsingRule.class);
    }

    private ParsingRule<? extends ParsingRuleProvider> getParsingRule(BigInteger id, String parentShortName) {
        if (parentShortName.equals("System")) {
            return CoreObjectManager.getInstance()
                    .getSpecialManager(SystemParsingRule.class, SystemParsingRuleObjectManager.class)
                    .getByIdOnly(id);
        } else {
            return CoreObjectManager.getInstance()
                    .getSpecialManager(OperationParsingRule.class, OperationParsingRuleObjectManager.class)
                    .getByIdOnly(id);
        }
    }
}
