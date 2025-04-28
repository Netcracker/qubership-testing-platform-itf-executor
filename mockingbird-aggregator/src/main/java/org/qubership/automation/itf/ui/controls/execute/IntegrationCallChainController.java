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

package org.qubership.automation.itf.ui.controls.execute;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION_DEFAULT_VALUE;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION_OPTIONS_VALUE;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang.IllegalClassException;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.integration.IntegrationException;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.BvCaseContainingObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.util.Maps2;
import org.qubership.automation.itf.core.util.engine.EngineControlIntegration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.ui.controls.util.Collector;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional(readOnly = true)
@RestController
@RequiredArgsConstructor
@Slf4j
public class IntegrationCallChainController extends ExecutorControllerHelper {

    private final ProjectSettingsService projectSettingsService;

    private static List<Storable> initObjects(Collection<UIObject> sources) {
        List<Storable> result = new ArrayList<>();
        CoreObjectManagerService coreObjectManager = CoreObjectManager.getInstance();
        for (UIIdentifiedObject source : sources) {
            String sourceId = source.getId();
            String sourceClassName = source.getClassName();
            if (!CallChain.class.getCanonicalName().equals(sourceClassName)) {
                throw new IllegalClassException("Unexpected class: " + sourceClassName
                        + ". Class is not supported by reference regenerator.");
            }
            result.add(coreObjectManager.getManager(CallChain.class).getById(sourceId));
        }
        return result;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/callchain/integration/bv", method = RequestMethod.DELETE)
    @Operation(summary = "DeleteOrUnlinkBvCase",
            description = "Delete or unlink bv case",
            tags = {SwaggerConstants.BULK_INTEGRATION_API})
    @AuditAction(auditAction = "Delete or unlink bv case with selected DataSet name {{#dsName}} and CallChain id "
            + "{{#callChainId}} in the project {{#projectId}}/{{#projectUuid}}")
    public void deleteOrUnlinkBvCase(
            @RequestParam(value = "dsName") String dsName,
            @RequestParam(value = "chainId") String callChainId,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "isDeleting") Boolean isDeleting,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws IOException {
        CallChain callChain = CoreObjectManager.getInstance().getManager(CallChain.class).getById(callChainId);
        throwExceptionIfNull(callChain, null, callChainId, CallChain.class, "get callchain by id");
        if (isDeleting && CoreObjectManager.getInstance().getSpecialManager(CallChain.class,
                BvCaseContainingObjectManager.class).countBvCaseUsages(callChain.getBvCases().get(dsName)) == 1) {
            IntegrationConfig bvConfig = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
            if (bvConfig != null) {
                EngineControlIntegration engine = (EngineControlIntegration) EngineIntegrationRegistry.getInstance()
                        .find(BULK_VALIDATOR_INTEGRATION);
                engine.delete(callChain, bvConfig, Maps2.map("dataset.name", dsName).build(), projectId);
            }
        }
        callChain.getBvCases().remove(dsName);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/callchain/integration/bv/read", method = RequestMethod.POST)
    @Operation(summary = "ReadBVCase", description = "Read BV Case", tags = {SwaggerConstants.BULK_INTEGRATION_API})
    @AuditAction(auditAction = "Read BV case with selected DataSet name {{#dsName}} and CallChain id {{#chainId}} in "
            + "the project {{#projectId}}/{{#projectUuid}}")
    public void readBvCase(
            @RequestParam(value = "dsName") String dsName,
            @RequestParam(value = "chainId") String chainId,
            @RequestBody Properties properties,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        EngineControlIntegration engine =
                (EngineControlIntegration) EngineIntegrationRegistry.getInstance().find(BULK_VALIDATOR_INTEGRATION);
        if (engine != null) {
            IntegrationConfig integrationConf = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
            if (integrationConf != null) {
                CallChain callChain = CoreObjectManager.getInstance().getManager(CallChain.class).getById(chainId);
                engine.configure(callChain, integrationConf, Maps2.map("dataset.name", dsName).val("islLink",
                        (String) properties.get("islLink")).build(), projectId);
            } else {
                throw new IntegrationException("Bulk Validator Integration config isn't found for the project!");
            }
        } else {
            throw new IntegrationException("Bulk Validator Integration Engine isn't found in the registry!");
        }
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/integration/bv/get/bvconfig", method = RequestMethod.GET)
    @Operation(summary = "GetBvAction",
            description = "Retrieve BV Case config",
            tags = {SwaggerConstants.BULK_INTEGRATION_API})
    @AuditAction(auditAction = "Get BV action config for project {{#projectId}}/{{#projectUuid}}")
    public Map<String, Object> getBvAction(@RequestParam(value = "projectId") BigInteger projectId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, Object> result = new HashMap<>();
        String defaultAction = projectSettingsService.get(projectId, BV_DEFAULT_ACTION,
                BV_DEFAULT_ACTION_DEFAULT_VALUE);
        result.put("actions", BV_DEFAULT_ACTION_OPTIONS_VALUE);
        result.put("defaultAction", defaultAction);
        return result;
    }

    @Transactional
    @RequestMapping(value = "/callchain/integration/bv", method = RequestMethod.GET, produces = "text/plain")
    @Operation(summary = "CreateBvCase", description = "Create BV Case", tags = {SwaggerConstants.BULK_INTEGRATION_API})
    @AuditAction(auditAction = "Create BV case using BV link {{#bvTcId}} with selected DataSet name {{#dsName}} and "
            + "CallChain id {{#callChainId}} in the project {{#projectId}}/{{#projectUuid}}")
    public String createBvCase(
            @RequestParam(value = "dsName") String dsName,
            @RequestParam(value = "chainId") String callChainId,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "bvTcId", required = false) String bvTcId) throws IOException {
        EngineControlIntegration engine =
                (EngineControlIntegration) EngineIntegrationRegistry.getInstance().find(BULK_VALIDATOR_INTEGRATION);
        if (engine != null) {
            IntegrationConfig integrationConf = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
            if (integrationConf != null) {
                CallChain callChain = CoreObjectManager.getInstance().getManager(CallChain.class).getById(callChainId);
                if (bvTcId == null) {
                    engine.create(callChain, integrationConf, Maps2.map("dataset.name", dsName).build(), projectId);
                } else {
                    callChain.getBvCases().putAll(Maps2.map(dsName, bvTcId).build());
                    if (!engine.isExist(callChain, integrationConf, Maps2.map("dataset.name", dsName).build(),
                            projectId)) {
                        throw new IntegrationException("BV Test Case with ID '" + bvTcId + "' isn't found");
                    }
                }
                callChain.store();
                return callChain.getBvCases().get(dsName);
            } else {
                throw new IntegrationException("Bulk Validator Integration config isn't found for the project!");
            }
        } else {
            throw new IntegrationException("Bulk Validator Integration Engine isn't found in the registry!");
        }
    }

    @Transactional
    @RequestMapping(value = "/callchain/integration/bv/regenerate",
            method = RequestMethod.PUT,
            produces = "application/json")
    @Operation(summary = "RegenerateBvCases",
            description = "Copy BV testcases linked with callchain + dataset",
            tags = {SwaggerConstants.BULK_INTEGRATION_API})
    @AuditAction(auditAction = "Copy BV testcases linked with CallChain and Dataset in the project {{#projectId}}")
    public List<UIObject> regenerateBvCases(
            @RequestParam(value = "create_new") boolean createNew,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestBody List<UIObject> objects) throws Exception {
        EngineControlIntegration engine =
                (EngineControlIntegration) EngineIntegrationRegistry.getInstance().find(BULK_VALIDATOR_INTEGRATION);
        if (engine != null) {
            IntegrationConfig integrationConf = findIntegrationConfig(BULK_VALIDATOR_INTEGRATION, projectId);
            HashSet<CallChain> callChains =
                    new HashSet<>(Collector.collectCallChainsFromCallChainsAndFolders(initObjects(objects)));
            for (CallChain callChain : callChains) {
                CallChain callChainNatural = getManager(CallChain.class).getById(callChain.getNaturalId());
                if (callChainNatural == null) {
                    log.warn("No natural parent found for callchain with id {}, so "
                            + "skipping BulkValidator links regeneration", callChain.getID());
                    continue;
                }
                if (createNew) {
                    for (Map.Entry bvCase : callChainNatural.getBvCases().entrySet()) {
                        String newTcId = engine.copyWithName(integrationConf, "", bvCase.getValue().toString(),
                                projectId);
                        callChain.getBvCases().put(bvCase.getKey().toString(), newTcId);
                    }
                } else {
                    callChain.setBvCases(new HashMap(callChainNatural.getBvCases()));
                }
                callChain.store();
            }
        } else {
            throw new IntegrationException("Bulk Validator Integration Engine isn't found in the registry!");
        }
        return objects;
    }
}
