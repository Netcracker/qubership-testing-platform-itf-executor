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

package org.qubership.automation.itf.ui.controls.entities.project;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.entities.Operation;
import org.qubership.atp.auth.springbootstarter.entities.Project;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.project.ProjectSettings;
import org.qubership.automation.itf.core.util.descriptor.ProjectSettingsDescriptor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.integration.users.AtpUsersService;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIConfig;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIStubProject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.properties.UIProjectSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Transactional
@RestController
@CrossOrigin
public class ProjectController extends AbstractController<UIStubProject, StubProject> {

    private final ProjectSettingsDescriptor projectSettings;
    private final AtpUsersService atpUsersService;
    @Lazy
    private final PolicyEnforcement policyEnforcement;
    private final ProjectSettingsService projectSettingsService;

    @Value("${atp.multi-tenancy.enabled}")
    private Boolean isMultiTenant;

    @Value("${spring.profiles.active}")
    private String springProfile;

    @Autowired
    public ProjectController(ProjectSettingsDescriptor projectSettings,
                             AtpUsersService atpUsersService,
                             PolicyEnforcement policyEnforcement,
                             ProjectSettingsService projectSettingsService) {
        this.projectSettings = projectSettings;
        this.atpUsersService = atpUsersService;
        this.policyEnforcement = policyEnforcement;
        this.projectSettingsService = projectSettingsService;
    }

    @Transactional(readOnly = true)
    @RequestMapping(value = "/project/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Projects list")
    public List<UIStubProject> getAll() {
        Collection<StubProject> projects = getProjects();
        if ("disable-security".equals(springProfile)) {
            return projects.stream().map(UIStubProject::new).collect(Collectors.toList());
        }
        Map<UUID, Project> projectsFromUserService = atpUsersService
                .getAllProjects().stream().collect(Collectors.toMap(Project::getUuid, Function.identity()));
        return projects.stream().filter(project -> {
                    if (policyEnforcement.isAdmin() || policyEnforcement.isSupport()) {
                        return true;
                    }
                    if (projectsFromUserService.containsKey(project.getUuid())) {
                        return policyEnforcement.checkPoliciesForOperation(
                                projectsFromUserService.get(project.getUuid()), Operation.READ);
                    }
                    return false;
                })
                .map(UIStubProject::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(@securityHelper.getCurrentProjectUuid(#id), \"READ\")")
    @RequestMapping(value = "/project", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Project by id {{#id}}")
    public UIStubProject getById(@RequestParam(value = "id") final BigInteger id) {
        return super.getById(id.toString());
    }

    @Transactional(readOnly = true)
    @RequestMapping(value = "/project/uuid/{uuid}", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Project by uuid {{#uuid}}")
    public UIStubProject getByUuid(@PathVariable String uuid) {
        SearchManager<StubProject> manager = CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class);
        BigInteger projectId = manager.getEntityInternalIdByUuid(UUID.fromString(uuid));
        if (projectId == null) {
            LOGGER.error("Can't find project by uuid '{}'. Empty object is returned.", uuid);
            return new UIStubProject();
        }
        return new UIStubProject(manager.getById(projectId));
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/project", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Project with id {{#uiObject.id}} and uuid {{#projectUuid}}")
    public UIObject create(@RequestBody final UIObject uiObject,
                           @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        StubProject stubProject = manager().create(_getParent(null), _getGenericUClass().getSimpleName(),
                projectSettings.asMapWithDefaultValues());
        stubProject.setName(uiObject.getName());
        LOGGER.info("Project {} is created", stubProject);
        return _newInstanceTClass(stubProject);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/project", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Project with id {{#uiProject.id}} and uuid {{#projectUuid}}")
    public UIStubProject update(@SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid,
                                @RequestBody UIStubProject uiProject) {
        return super.update(uiProject);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/project", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Project with id {{#id}} and uuid {{#projectUuid}}")
    public void delete(@SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid,
                       @RequestParam(value = "id", defaultValue = "0") String id) {
        StubProject stubProject = CoreObjectManager.getInstance().getManager(StubProject.class).getById(id);
        ControllerHelper.throwExceptionIfNull(stubProject, "", id, StubProject.class, "delete project");
        LOGGER.info("Project {} will be deleted now...", stubProject);
        stubProject.remove();
        LOGGER.info("Project {} is deleted", stubProject);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/settings/get", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Settings of Project by id {{#projectId}} and uuid {{#projectUuid}}")
    public UIConfiguration getAllProjectSettings(@RequestParam(value = "projectId") BigInteger projectId,
                                                 @SuppressWarnings("unused")
                                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        UIProjectSettings uiProjectSettings =
                new UIProjectSettings(new ProjectSettings(projectSettingsService.getAll(projectId)));
        uiProjectSettings.setId(projectId.toString());
        uiProjectSettings.setClassName(StubProject.class.getName());
        return uiProjectSettings;
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/setting/get", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Setting of Project by property {{#property}} and id {{#projectId}} and uuid " +
            "{{#projectUuid}}")
    public UIConfig getProjectSetting(@RequestParam(value = "projectId") BigInteger projectId,
                                      @RequestParam(value = "property") String property,
                                      @SuppressWarnings("unused")
                                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        return new UIConfig(property, projectSettingsService.get(projectId, property));
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/settings/update", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Settings of Project by id {{#projectId}}")
    public void updateProjectSettings(@RequestParam(value = "projectId") String projectId,
                                      @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid,
                                      @RequestBody UIProjectSettings properties) {
        StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId);
        project.setName(properties.getName());
        Map<String, String> currentSettings = projectSettingsService.getAll(projectId);
        for (UIProperty uiProperty : properties.getProperties()) {
            currentSettings.put(uiProperty.getName(), uiProperty.getValue());
        }
        projectSettingsService.fillCache(project, currentSettings);
        project.setStorableProp(currentSettings);
        project.store();
    }

    @Override
    protected Class<StubProject> _getGenericUClass() {
        return StubProject.class;
    }

    @Override
    protected UIStubProject _newInstanceTClass(StubProject object) {
        return new UIStubProject(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        return null;
    }

    private Collection<StubProject> getProjects() {
        Collection<StubProject> allTenantProjectsList = new ArrayList<>();
        if (isMultiTenant) {
            for (String tenantId : TenantContext.getTenantIds(false)) {
                TenantContext.setTenantInfo(tenantId);
                Collection<? extends StubProject> allProjectsList = manager().getAll();
                allTenantProjectsList.addAll(allProjectsList);
            }
            TenantContext.setDefaultTenantInfo();
        }
        allTenantProjectsList.addAll(manager().getAll());
        return allTenantProjectsList;
    }
}

