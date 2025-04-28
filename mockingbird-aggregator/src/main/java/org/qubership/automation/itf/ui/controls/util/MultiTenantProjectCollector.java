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

package org.qubership.automation.itf.ui.controls.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.StubProjectObjectManager;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MultiTenantProjectCollector {

    public static final String DEFAULT_CLUSTER = "default";
    public static final String ADDITIONAL_CLUSTERS = "additional";
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    public Map<String, Collection<StubProject>> collectProjects() {
        Map<String, Collection<StubProject>> allProjects = new HashMap<>();
        Collection<StubProject> additionalClustersProjects = new ArrayList<>();
        if (multiTenancyEnabled) {
            StubProjectObjectManager manager = CoreObjectManager.getInstance()
                    .getSpecialManager(StubProject.class, StubProjectObjectManager.class);
            for (String tenantId : TenantContext.getTenantIds(false)) {
                if (StringUtils.isEmpty(tenantId)) {
                    log.warn("TenantId is null or empty, skipped.");
                    continue;
                }
                TenantContext.setTenantInfo(tenantId);
                try {
                    UUID projectUuid = UUID.fromString(tenantId);
                    BigInteger projectId = getInternalProjectIdByUuid(manager, projectUuid);
                    if (projectId != null) {
                        StubProject project = getProject(manager, projectId);
                        if (project != null) {
                            additionalClustersProjects.add(project);
                        } else {
                            log.warn("Can't find the project by id = {}", projectId);
                        }
                    } else {
                        log.warn("Can't find the project internal id by UUID = {}", tenantId);
                    }
                } catch (IllegalArgumentException ex) {
                    log.error("Illegal value of tenantId: {}", tenantId, ex);
                } catch (Exception ex) {
                    log.error("Exception while processing of tenantId: {}", tenantId, ex);
                }
            }
            if (additionalClustersProjects.isEmpty()) {
                log.warn("Please note: no projects in additional cluster(s) are found!");
            }
            TenantContext.setDefaultTenantInfo();
        }
        allProjects.put(DEFAULT_CLUSTER, getProjectsFromDefault(additionalClustersProjects));
        allProjects.put(ADDITIONAL_CLUSTERS, additionalClustersProjects);
        return allProjects;
    }

    @Transactional(readOnly = true)
    StubProject getProject(StubProjectObjectManager manager, BigInteger projectId) {
        return manager.getById(projectId);
    }

    @Transactional(readOnly = true)
    BigInteger getInternalProjectIdByUuid(StubProjectObjectManager manager, String tenantId) {
        return manager.getEntityInternalIdByUuid(UUID.fromString(tenantId));
    }

    @Transactional(readOnly = true)
    BigInteger getInternalProjectIdByUuid(StubProjectObjectManager manager, UUID tenantId) {
        return manager.getEntityInternalIdByUuid(tenantId);
    }

    private Collection<StubProject> getProjectsFromDefault(
            Collection<StubProject> additionalClustersProjects) {
        Collection<StubProject> projectsFromDefaultCluster = getProjectsFromDefault();
        //exclude projects that migrated to another cluster (with the same uuid) but not deleted from default.
        projectsFromDefaultCluster.removeAll(additionalClustersProjects);
        return projectsFromDefaultCluster;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    Collection<StubProject> getProjectsFromDefault() {
        return (Collection<StubProject>) CoreObjectManager.getInstance()
                .getManager(StubProject.class).getAll();
    }
}
