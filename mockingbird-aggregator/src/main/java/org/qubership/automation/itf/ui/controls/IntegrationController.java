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

import static org.qubership.automation.itf.integration.bv.BvEndpoints.ENDPOINT_FOR_LINK_TO_TC;
import static org.qubership.automation.itf.integration.bv.utils.BvHelper.getProjectUUID;
import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.exceptions.configuration.ConfigurationException;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIIntegrationConfig;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Transactional
@RestController
public class IntegrationController extends AbstractController<UIIntegrationConfig, IntegrationConfig> {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/integrations/available", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Available Integrations, project {{#projectUuid}}")
    public Collection<String> getAvailableIntegrations(@RequestParam(value = "projectUuid") UUID projectUuid) {
        return EngineIntegrationRegistry.getInstance().getAvailableIntegrations();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/integrations/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Integration Configs in the project {{#projectId}}/{{#projectUuid}}")
    public Collection<UIIntegrationConfig> getConfigurations(
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId);
        if (project == null) {
            throw new ObjectNotFoundException(
                    "Project", String.valueOf(projectId), null, "get Integration Configurations");
        }
        Set<IntegrationConfig> confs = project.getIntegrationConfs();
        if (confs == null) {
            return Collections.emptySet();
        }
        Collection<UIIntegrationConfig> result = Sets.newHashSetWithExpectedSize(confs.size());
        for (IntegrationConfig conf : confs) {
            UIIntegrationConfig uiConfiguration = new UIIntegrationConfig(conf);
            result.add(uiConfiguration);
        }
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/integrations", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Integration Config with id {{#id}} in the project {{#projectUuid}}")
    public UIIntegrationConfig getConfiguration(
            @RequestParam(value = "id", defaultValue = "") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        IntegrationConfig integrationConfig =
                CoreObjectManager.getInstance().getManager(IntegrationConfig.class).getById(id);
        return new UIIntegrationConfig(integrationConfig);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/project/integrations/property", method = RequestMethod.GET, produces = "text/plain")
    @AuditAction(auditAction = "Get Integration Config property value by Configuration name {{#name}} and property "
            + "name {{#property}} in the project {{#projectId}}/{{#projectUuid}}")
    public String getConfigurationByNameAndGetProperty(
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "name", defaultValue = "") String name,
            @RequestParam(value = "property") String property,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId);
        if (project == null) {
            throw new ObjectNotFoundException(
                    "Project", String.valueOf(projectId), null, "get Integration Configurations");
        }
        List<IntegrationConfig> result = Lists.newArrayList();
        for (IntegrationConfig config : project.getIntegrationConfs()) {
            if (name.equals(config.getName())) {
                result.add(config);
                if (result.size() > 1) {
                    LOGGER.error("There are more than one configuration with name {} in ITF. Please, check the "
                            + "External tools integrations settings.", name);
                    return StringUtils.EMPTY;
                }
            }
        }
        if (result.isEmpty()) {
            LOGGER.warn("There is no configuration with name {} in ITF. Please, check the External tools integrations"
                    + " settings.", name);
            return StringUtils.EMPTY;
        } else {
            String uuid = getProjectUUID(projectId);
            return result.iterator().next().get(property) + String.format(ENDPOINT_FOR_LINK_TO_TC, uuid);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/project/integrations/bv/link", produces = "text/plain")
    public String getProjectIntegrationsBvLink(@RequestParam String property,
                                               @RequestParam UUID projectUuid) {
        String catalogueUrl = ApplicationConfig.env.getProperty(property);
        if (StringUtils.isBlank(catalogueUrl)) {
            LOGGER.warn("The property {} has an empty value. Please set a value for the property.", property);
            return String.format(ENDPOINT_FOR_LINK_TO_TC, projectUuid);
        } else {
            return catalogueUrl + String.format(ENDPOINT_FOR_LINK_TO_TC, projectUuid);
        }
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/project/integrations", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Integration Config with name {{#name}}, type {{#type}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public UIIntegrationConfig create(
            @RequestParam(value = "parentId", defaultValue = "") String parentId,
            @RequestParam(value = "name", defaultValue = "") String name,
            @RequestParam(value = "type", defaultValue = "") String type,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Empty name given");
        }
        StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId);
        if (project == null) {
            throw new ObjectNotFoundException(
                    "Project", String.valueOf(projectId), null, "create Integration Configuration");
        }
        if (project.getIntegrationConfs() != null) {
            for (IntegrationConfig config : project.getIntegrationConfs()) {
                if (config.getTypeName().equals(name)) {
                    throw new ConfigurationException("Project already has configuration " + name);
                }
            }
        }
        return super.create(project.getID().toString(), name, name);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/project/integrations", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Integration Config with id {{#configuration.id}} in the project "
            + "{{#projectUuid}}")
    public UIIntegrationConfig update(@RequestBody UIIntegrationConfig configuration,
                                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.update(configuration);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/project/integrations", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Integration Configs from project {{#projectUuid}}")
    public List<UIObject> delete(@RequestBody UIIds uiIds,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.delete(uiIds);
    }

    @Override
    protected Class<IntegrationConfig> _getGenericUClass() {
        return IntegrationConfig.class;
    }

    @Override
    protected UIIntegrationConfig _newInstanceTClass(IntegrationConfig object) {
        return new UIIntegrationConfig(object);
    }

    @Override
    protected StubProject _getParent(String parentId) {
        return getManager(StubProject.class).getById(parentId);
    }

    @Override
    protected IntegrationConfig _beforeUpdate(UIIntegrationConfig uiObject, IntegrationConfig object) {
        object.setTypeName(uiObject.getUserTypeName());
        for (UIProperty uiProperty : uiObject.getProperties()) {
            object.put(uiProperty.getName(), uiProperty.getValue());
        }
        return super._beforeUpdate(uiObject, object);
    }

    private void addPropertiesToConfig(IntegrationConfig config) {
        List<PropertyDescriptor> properties = EngineIntegrationRegistry.getInstance().getProperties(config.getName());
        for (PropertyDescriptor property : properties) {
            if (property.getDefaultValue() != null) {
                config.put(property.getShortName(), property.getDefaultValue().toString());
            }
        }
    }
}
