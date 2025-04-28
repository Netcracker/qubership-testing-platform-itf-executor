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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.step.AbstractCallChainStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.regenerator.KeysRegeneratable;
import org.qubership.automation.itf.core.util.Pair;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.ParsingRuleProvider;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataRegeneratorController extends ControllerHelper {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "regenerator/key/delete", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Keys on Situation id {{#situationId}} / SituationStep id {{#situationStepId}} "
            + "in the project {{#projectUuid}}")
    public void deleteKeys(@RequestParam(defaultValue = StringUtils.EMPTY) String situationId,
                           @RequestParam(defaultValue = StringUtils.EMPTY) String situationStepId,
                           @RequestBody String[] keys,
                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        //deprecated
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "regenerator/key/check", method = RequestMethod.POST)
    @AuditAction(auditAction = "Check Keys on Situation id {{#situationId}} / SituationStep id {{#situationStepId}} "
            + "in the project {{#projectId}}/{{#projectUuid}}")
    public List<Pair<String, String>> checkKeysBulk(
            @RequestParam(defaultValue = StringUtils.EMPTY) String situationId,
            @RequestParam(defaultValue = StringUtils.EMPTY) String situationStepId,
            @RequestBody List<String> keys,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam("projectUuid") UUID projectUuid) {
        KeysRegeneratable regeneratable = getKeysRegeneratable(situationId, situationStepId);
        ArrayList<Pair<String, String>> result = new ArrayList<>();
        keys.forEach(k -> {
            if (isKeyValid(k)) {
                result.add(new Pair<>(k, ""));
            } else {
                result.add(new Pair<>(k, "ERROR. Key '" + k + "' can't be used, it must start with 'sp.' or 'tc.'"));
            }
        });
        if (regeneratable instanceof AbstractCallChainStep) {
            prepareCheckAgainstDatasets(result);
            areKeysDefinedInTc(regeneratable, result, projectUuid);
        } else if (regeneratable instanceof Situation) {
            prepareCheckAgainstParsingRules(result);
            Operation operation = (Operation) regeneratable.getParent();
            checkInParsingRuleProviderAndItsParent(result, operation);
        }
        return result;
    }

    private boolean isKeyValid(String key) {
        return key.matches("^(sp\\.|tc\\.)(\\S+)");
    }

    private void prepareCheckAgainstDatasets(List<Pair<String, String>> keys) {
        keys.forEach(k -> {
            if (k.getValue().isEmpty()) {
                // Remove starting "tc."; mark keys starting with "sp." in order not to check them
                if (k.getKey().startsWith("sp.")) {
                    k.setValue("WARNING. 'sp.'-members are not in the dataset");
                } else {
                    k.setKey(k.getKey().substring(3));
                }
            }
        });
    }

    private void prepareCheckAgainstParsingRules(List<Pair<String, String>> keys) {
        keys.forEach(k -> {
            if (k.getValue().isEmpty()) {
                // Remove starting "tc.saved" or "sp."; mark keys starting with "tc." (not "tc.saved.") in order not
                // to check them
                if (k.getKey().startsWith("sp.")) {
                    k.setKey(k.getKey().substring(3));
                } else if (k.getKey().startsWith("tc.saved.")) {
                    k.setKey(k.getKey().substring(9));
                } else {
                    k.setValue("WARNING. May be, it's better to change 'tc.' (not 'tc.saved.') variables on a "
                            + "callchain step?");
                }
            }
        });
    }

    private void areKeysDefinedInTc(KeysRegeneratable regeneratable, List<Pair<String, String>> keys,
                                    Object projectUuid) {
        if (allProcessed(keys)) {
            return;
        }
        Set<DataSetList> compatibleDataSetLists =
                ((CallChain) regeneratable.getParent()).getCompatibleDataSetLists(projectUuid);
        if (compatibleDataSetLists == null || compatibleDataSetLists.isEmpty()) {
            keys.forEach(key -> key.setValue("ERROR. " + regeneratable.getParent().getName() + " has no defined "
                    + "dataset"));
            return;
        }
        for (DataSetList dataSetList : compatibleDataSetLists) {
            Set<String> bufferedDataSetListVariables = dataSetList.getVariables();
            keys.forEach(key -> {
                if (bufferedDataSetListVariables.contains(key.getKey())) {
                    key.setValue(dataSetList.getName());
                }
            });
            if (allProcessed(keys)) {
                break; // All keys are processed; there is no need to get other DSL variables and recheck
            }
        }
        keys.forEach(key -> {
            if (key.getValue().isEmpty()) {
                key.setValue("ERROR. Datasets don't contain the key");
            }
        });
    }

    private boolean allProcessed(List<Pair<String, String>> keys) {
        AtomicBoolean found = new AtomicBoolean(true);
        keys.forEach(key -> {
            // The value is NOT empty means the key is processed
            if (key.getValue().isEmpty()) {
                found.set(false);
            }
        });
        return found.get();
    }

    private void checkInParsingRuleProviderAndItsParent(List<Pair<String, String>> keys, ParsingRuleProvider provider) {
        keys.forEach(key -> {
            // The value is NOT empty means the key is invalid
            if (key.getValue().isEmpty()) {
                boolean found;
                //for operation
                found = checkInParsingRuleProvider(key, provider);
                //for system
                if (!found) {
                    found = checkInParsingRuleProvider(key, (System) provider.getParent());
                }
                if (!found) {
                    key.setValue("Parsing rules wont't be overridden.");
                }
            }
        });
    }

    private boolean checkInParsingRuleProvider(Pair<String, String> key, ParsingRuleProvider provider) {
        String providerClass = provider.getClass().getSimpleName().toLowerCase();
        for (ParsingRule parsingRule : provider.returnParsingRules()) {
            if (parsingRule.getParamName() != null && parsingRule.getParamName().equals(key.getKey())) {
                key.setValue("WARNING. Parsing rule '" + parsingRule.getParamName() + "' in " + providerClass + " '"
                        + provider.getName() + "' will be overridden!");
                return true; // checked
            }
        }
        return false;
    }

    private KeysRegeneratable getKeysRegeneratable(String situationId, String callChainStep) {
        KeysRegeneratable keysRegeneratable;
        boolean isSituation;
        if (StringUtils.isNotBlank(situationId)) {
            isSituation = true;
            keysRegeneratable = CoreObjectManager.getInstance().getManager(Situation.class).getById(situationId);
        } else if (StringUtils.isNoneBlank(callChainStep)) {
            isSituation = false;
            keysRegeneratable = (AbstractCallChainStep) CoreObjectManager.getInstance().getManager(Step.class)
                    .getById(callChainStep);
        } else {
            throw new IllegalArgumentException("Situation or CallChain Step id should be not blank.");
        }
        throwExceptionIfNull(keysRegeneratable, "",
                isSituation ? situationId : callChainStep, KeysRegeneratable.class,
                isSituation ? "get Situation by id" : "get CallChain Step by id");
        return keysRegeneratable;
    }
}
