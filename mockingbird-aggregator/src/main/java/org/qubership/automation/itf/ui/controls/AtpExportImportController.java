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

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByProjectIdManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemObjectManager;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.objects.ei.SimpleItfEntity;
import org.qubership.automation.itf.executor.service.ExportImportService;
import org.qubership.automation.itf.ui.util.AtpExportImportHelper;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@AllArgsConstructor
@Slf4j
public class AtpExportImportController {

    private final ExportImportService exportImportService;

    /**
     * Get call chain root folder by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntity with call chain root folder (id, name, parentId).
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/folders/root/callchain/project/{projectUuid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get root CallChain Folder for Atp Export, project {{#projectUuid}}")
    public SimpleItfEntity getRootCallchainFolderByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        //noinspection unchecked
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        return AtpExportImportHelper.createSimpleItfEntity(project.getCallchains());
    }

    /**
     * Get call chain folders by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with call chain folders.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/folders/sub/callchain/project/{projectUuid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get all CallChain Folders for Atp Export, project {{#projectUuid}}")
    public List<SimpleItfEntity> getCallchainSubFoldersByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        //noinspection unchecked
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        AtpExportImportHelper.fillSubFolders(project.getCallchains(), result);
        return result;
    }

    /**
     * Get call chains by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with call chains.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/callchains/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get all CallChains for Atp Export, project {{#projectUuid}}")
    public List<SimpleItfEntity> getCallchainsByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        //noinspection unchecked
        BigInteger projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class, SearchManager.class)
                .getEntityInternalIdByUuid(projectUuid);
        //noinspection unchecked
        Collection<? extends CallChain> callChains = CoreObjectManager.getInstance()
                .getSpecialManager(CallChain.class, SearchByProjectIdManager.class).getByProjectId(projectId);
        for (CallChain callChain : callChains) {
            result.add(AtpExportImportHelper.createSimpleItfEntity(callChain));
        }
        return result;
    }

    /**
     * Get system root folder by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntity with system root folder (id, name).
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/folders/root/system/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get root System Folder for Atp Export, project {{#projectUuid}}")
    public SimpleItfEntity getRootSystemFolderByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        //noinspection unchecked
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        return AtpExportImportHelper.createSimpleItfEntity(project.getSystems());
    }

    /**
     * Get system folders by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with sysnem folders.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/folders/sub/system/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get all System Folders for Atp Export, project {{#projectUuid}}")
    public List<SimpleItfEntity> getSystemSubFoldersByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        //noinspection unchecked
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        AtpExportImportHelper.fillSubFolders(project.getSystems(), result);
        return result;
    }

    /**
     * Get systems by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with systems.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/systems/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get all Systems for Atp Export, project {{#projectUuid}}")
    public List<SimpleItfEntity> getSystemsByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        //noinspection unchecked
        BigInteger projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class, SearchManager.class)
                .getEntityInternalIdByUuid(projectUuid);
        Collection<? extends System> systems = CoreObjectManager.getInstance()
                .getSpecialManager(System.class, SystemObjectManager.class).getByProjectId(projectId);
        for (System system : systems) {
            result.add(AtpExportImportHelper.createSimpleItfEntity(system));
        }
        return result;
    }

    /**
     * Get environment root folder by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntity with environment root folder (id, name).
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/folders/root/environment/project/{projectUuid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get root Environment Folder for Atp Export, project {{#projectUuid}}")
    public SimpleItfEntity getRootEnvironmentFolderByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        //noinspection unchecked
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        return AtpExportImportHelper.createSimpleItfEntity(project.getEnvironments());
    }

    /**
     * Get environment folders by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with environment folders.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/folders/sub/environment/project/{projectUuid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get all Environment Folders for Atp Export, project {{#projectUuid}}")
    public List<SimpleItfEntity> getEnvironmentFoldersByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        //noinspection unchecked
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        AtpExportImportHelper.fillSubFolders(project.getEnvironments(), result);
        return result;
    }

    /**
     * Get environments by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with environments.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/environments/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get all Environments for Atp Export, project {{#projectUuid}}")
    public List<SimpleItfEntity> getEnvironmentsByAtpExport(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        //noinspection unchecked
        BigInteger projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class, SearchManager.class)
                .getEntityInternalIdByUuid(projectUuid);
        //noinspection unchecked
        Collection<? extends Environment> environments = CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, SearchByProjectIdManager.class).getByProjectId(projectId);
        for (Environment environment : environments) {
            result.add(AtpExportImportHelper.createSimpleItfEntity(environment));
        }
        return result;
    }

    /**
     * Get integration configs by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with IntegrationConfig.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/integrationconfigs/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get Integration Configs from project {{#projectUuid}}")
    public List<SimpleItfEntity> getIntegrationConfigsByProjectId(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        if (!Objects.isNull(project)) {
            Collection<? extends IntegrationConfig> integrationConfigs = CoreObjectManager.getInstance()
                    .getManager(IntegrationConfig.class).getAllByParentId(project.getID());
            result = integrationConfigs.stream().map(AtpExportImportHelper::createSimpleItfEntity)
                    .collect(Collectors.toList());
        } else {
            log.error("Can't find project by UUID {}, empty SimpleItfEntity list will be returned.", projectUuid);
        }
        return result;
    }

    /**
     * Get project settings by project UUID.
     *
     * @param projectUuid ATP project UUID
     * @return SimpleItfEntities list with StubProject.
     */
    @Transactional(readOnly = true)
    @GetMapping(value = "/api/settings/project/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get Project Settings from project {{#projectUuid}}")
    public List<SimpleItfEntity> getProjectSettingsByProjectId(@PathVariable("projectUuid") UUID projectUuid) {
        List<SimpleItfEntity> result = Lists.newArrayList();
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        if (!Objects.isNull(project)) {
            result.add(AtpExportImportHelper.createSimpleItfEntity(project));
        } else {
            log.error("Can't find project by UUID {}, empty SimpleItfEntity will be returned.", projectUuid);
            result.add(new SimpleItfEntity());
        }
        return result;
    }

    @Transactional(readOnly = true)
    @PostMapping(value = "/api/bvTcByChains", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Atp Export: Collecting BulkValidator TestCases by CallChains Ids")
    public List<UUID> getBvTcByItfChains(@RequestBody Set<String> itfCallChains) {
        log.info("Collecting BulkValidator TestCases by CallChainsIds...");
        return exportImportService.collectBvTcByChainsIds(itfCallChains);
    }

    @Transactional(readOnly = true)
    @PostMapping(value = "/api/bvTcBySystems", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Atp Export: Collecting BulkValidator TestCases by Systems Ids")
    public List<UUID> getBvTcByItfSystems(@RequestBody Set<String> itfSystems) {
        log.info("Collecting BulkValidator TestCases by SystemsIds...");
        return exportImportService.collectBvTcBySystemsIds(itfSystems);
    }

    @Transactional(readOnly = true)
    @PostMapping(value = "/api/bvTcByEnvs", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Atp Export: Collecting BulkValidator TestCases by Environments Ids")
    public List<UUID> getBvTcByItfEnvs(@RequestBody Set<String> itfEnvironments) {
        log.info("Collecting BulkValidator TestCases by EnvironmentsIds...");
        return exportImportService.collectBvTcByEnvironmentsIds(itfEnvironments);
    }

    @Transactional(readOnly = true)
    @PostMapping(value = "/api/dslByChains", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Atp Export: Collecting DSLs by CallChains Ids")
    public List<UUID> getDslByItfChains(@RequestBody Set<String> itfCallChains) {
        log.info("Collecting DSL by CallChainsIds...");
        return exportImportService.collectDslByChainsIds(itfCallChains);
    }
}
