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

package org.qubership.automation.itf.ui.controls.entities.template;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Triple;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.constants.Match;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.util.VelocityVariablesLexer;
import org.qubership.automation.itf.ui.util.VelocityVariablesParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

@RestController
@Transactional(readOnly = true)
public class TemplateDebugController extends ControllerHelper {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/exists", method = RequestMethod.POST)
    @AuditAction(auditAction = "Get Templates by part of name '{{#name}}' in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public ResponseEntity getByName(
            @RequestParam(value = "name") String name,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        return new ResponseEntity<>(TemplateHelper.getByPieceOfNameAndProject(name, projectId).isEmpty()
                ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/parameters", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Get parameters of the Template with id {{#id}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public String getTemplateParameters(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestBody String editRequest,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        Template template = TemplateHelper.getById(id);
        throwExceptionIfNull(template, "", id, Template.class, "get Template parameters");
        String templateContent =
                Strings.isNullOrEmpty(
                        editRequest.replace("{", "").replace("}", "")
                ) ? template.getText() : editRequest;
        JSONObject result = extractVelocityVariables(template, templateContent, projectId);
        return result.toJSONString();
    }

    private JSONObject extractVelocityVariables(Template template, String templateContent, BigInteger projectId) {
        VelocityVariablesLexer lexer = new VelocityVariablesLexer(new ANTLRInputStream(templateContent));
        VelocityVariablesParser parser = new VelocityVariablesParser(new CommonTokenStream(lexer));
        VelocityVariablesParser.VariablesContext templateContext = parser.variables();
        JSONObject result = new JSONObject();
        JSONArray variables = new JSONArray();
        Storable templateParent = template.getParent();
        Map<String, String> parentsMap = new HashMap<>();
        ObjectManager<? extends ParsingRule<? extends ParsingRuleProvider>> parsingRuleObjectManager;
        if (templateParent instanceof System) {
            parsingRuleObjectManager = CoreObjectManager.getInstance().getManager(SystemParsingRule.class);
            System parentSystem = (System) templateParent;
            parentsMap.put(parentSystem.getName(), "System");
            for (Operation operation : parentSystem.getOperations()) {
                if (operation.returnParsingRules().size() > 0) {
                    parentsMap.put(operation.getName(), "Operation");
                }
            }
        } else {
            parsingRuleObjectManager = CoreObjectManager.getInstance().getManager(OperationParsingRule.class);
            parentsMap.put(templateParent.getName(), "Operation");
            Storable parentSystem = templateParent.getParent();
            if (parentSystem instanceof System) {
                parentsMap.put(parentSystem.getName(), "System");
            }
        }
        for (final String variable : templateContext.variablesList) {
            String escapedName = variable.replace("$", "").replace("!", "").replace("{", "").replace("}", "");
            boolean isParsingRule = escapedName.startsWith("tc.saved.") || escapedName.startsWith("sp.");
            boolean isStepRule = false;
            escapedName = escapedName.replace("tc.saved.", "");
            if (escapedName.startsWith("sp.")) {
                escapedName = escapedName.substring(3);
                isStepRule = true;
            }
            String parentsStr = isParsingRule
                    ? getParsingRuleParents(projectId, escapedName, parentsMap, isStepRule, parsingRuleObjectManager)
                    : null;
            JSONObject variableJson = new JSONObject();
            variableJson.put("name", variable);
            variableJson.put("value", "");
            if (isParsingRule && !Strings.isNullOrEmpty(parentsStr)) {
                variableJson.put("source", "PARSING RULE");
                variableJson.put("parents", parentsStr);
            }
            variables.add(variableJson);
        }
        result.put("variables", variables);
        return result;
    }

    private String getParsingRuleParents(BigInteger projectId, String ruleName, Map<String, String> parentObjectNames,
                                         boolean isStepRule,
                                         ObjectManager<? extends ParsingRule<? extends ParsingRuleProvider>>
                                                 parsingRuleObjectManager) {   //Map: ParentName,ParentType
        Collection<? extends ParsingRule> parsingRules = parsingRuleObjectManager
                .getByProperties(projectId, Triple.of("name", Match.EQUALS, ruleName));
        if (parsingRules == null) {
            return null;
        }
        List<String> parentNames = new ArrayList<>();
        List<String> directParentNames = new ArrayList<>();
        for (ParsingRule parsingRule : parsingRules) {
            String ruleParentName = parsingRule.getParent().getName();
            if (isStepRule) {
                if (parentObjectNames.containsKey(ruleParentName)) {
                    directParentNames.add(String.format("%s: %s", parentObjectNames.get(ruleParentName),
                            ruleParentName));
                }
            } else {
                if (parentObjectNames.containsKey(ruleParentName)) {
                    directParentNames.add(String.format("%s: %s", parentObjectNames.get(ruleParentName),
                            ruleParentName));
                } else {
                    String parentType = parsingRule.getParent() instanceof System ? "System" : "Operation";
                    parentNames.add(String.format("%s: %s", parentType, ruleParentName));
                }
            }
        }
        if (parentNames.isEmpty() && directParentNames.isEmpty()) {
            return null;
        }
        return Joiner.on(",\n").join(directParentNames.isEmpty() ? parentNames : directParentNames);
    }
}
