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

package org.qubership.automation.itf.integration.atp2.kafka.service.impl;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.DATA_SET_SERVICE_DS_FORMAT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.DATA_SET_SERVICE_DS_FORMAT_DEFAULT_VALUE;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.descriptor.ProjectSettingsDescriptor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ProjectEvent;
import org.qubership.automation.itf.integration.atp2.kafka.service.ProjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("projectService")
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectSettingsDescriptor projectSettingsDescriptor;
    private final ProjectSettingsService projectSettingsService;
    private final CoreObjectManagerService coreObjectManagerService;
    @Value("${atp.multi-tenancy.enabled}")
    private boolean multiTenancyEnabled;

    @Transactional
    @Override
    public void create(String event, ProjectEvent projectEvent) {
        log.info("'Create Project' Event is received: {}", event);
        UUID projectUuid = projectEvent.getProjectId();
        //noinspection unchecked
        BigInteger projectId = coreObjectManagerService.getSpecialManager(StubProject.class, SearchManager.class)
                .getEntityInternalIdByUuid(projectUuid);
        if (projectId != null) {
            log.error("Such project already exists: {}, {}", projectUuid, projectId);
        } else {
            createProject(projectEvent);
        }
    }

    @Override
    public void update(String event, ProjectEvent projectEvent) {
        log.info("'Update Project' Event is received: {}", event);
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(projectEvent.getProjectId().toString());
            update(projectEvent);
            TenantContext.setDefaultTenantInfo();
        } else {
            update(projectEvent);
        }
    }

    @Transactional
    public void update(ProjectEvent projectEvent) {
        UUID projectUuid = projectEvent.getProjectId();
        //noinspection unchecked
        BigInteger projectId = coreObjectManagerService.getSpecialManager(StubProject.class, SearchManager.class)
                .getEntityInternalIdByUuid(projectUuid);
        synchronized (projectUuid) {
            if (projectId == null) {
                log.info("Create non-existent project {}, {}", projectUuid, projectEvent.getProjectName());
                createProject(projectEvent);
            } else {
                log.info("Event Project ID {} is ours; project name and settings are to be updated", projectUuid);
                StubProject project = coreObjectManagerService.getManager(StubProject.class).getById(projectId);
                boolean projectNameChanged = changeProjectName(project, projectEvent.getProjectName());
                Map<String, String> projectSettings = projectSettingsService.getAll(projectId);
                boolean dataSetFormatChanged = changeDataSetFormat(projectSettings, projectEvent);
                if (projectNameChanged || dataSetFormatChanged) {
                    if (dataSetFormatChanged) {
                        project.setStorableProp(projectSettings);
                        projectSettingsService.update(projectId, DATA_SET_SERVICE_DS_FORMAT,
                                projectSettings.get(DATA_SET_SERVICE_DS_FORMAT), false);
                    }
                    project.store();
                }
            }
        }
    }

    @Override
    public void delete(String event, ProjectEvent projectEvent) {
        log.info("'Delete Project' Event is received: {}; ignored.", event);
    }

    private void createProject(ProjectEvent projectEvent) {
        Map<String, String> projectSettings = projectSettingsDescriptor.asMapWithDefaultValues();
        changeDataSetFormat(projectSettings, projectEvent);
        StubProject project = coreObjectManagerService.getManager(StubProject.class)
                .create(null, StubProject.class.getName(), projectSettings);
        project.setName(projectEvent.getProjectName());
        project.setUuid(projectEvent.getProjectId());
        project.store();
        projectSettingsService.fillCache(project, projectSettings);
    }

    private boolean changeDataSetFormat(Map<String, String> projectSettings, ProjectEvent projectEvent) {
        String newDatasetFormat = projectEvent.getDatasetFormat().toString();
        if (newDatasetFormat.equals(projectSettings.getOrDefault(DATA_SET_SERVICE_DS_FORMAT,
                DATA_SET_SERVICE_DS_FORMAT_DEFAULT_VALUE))) {
            log.info("Old and New DatasetFormat settings are the same: {}", newDatasetFormat);
            return false;
        }
        projectSettings.put(DATA_SET_SERVICE_DS_FORMAT, newDatasetFormat);
        log.info("DatasetFormat setting is changed to {}", newDatasetFormat);
        return true;
    }

    private boolean changeProjectName(StubProject project, String newName) {
        if (StringUtils.isBlank(newName) || newName.equals(project.getName())) {
            log.info("The new project name is blank or the same with old: {}, the name is unchanged", newName);
            return false;
        }
        project.setName(newName.trim());
        log.info("Project name is changed to {}", newName);
        return true;
    }
}
