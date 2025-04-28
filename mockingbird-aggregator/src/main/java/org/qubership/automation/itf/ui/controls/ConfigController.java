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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.ui.messages.objects.UIConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ConfigController {

    private final org.springframework.core.env.Environment env;
    private final ProjectSettingsService projectSettingsService;

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/config/set", method = RequestMethod.POST)
    @AuditAction(auditAction = "Set properties of project {{#projectUuid}}")
    public void setProperties(@RequestBody Properties properties,
                              @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        Config.getConfig().merge(properties);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/config/get", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get property '{{#property}}' of project {{#projectUuid}}")
    public UIConfig getProperty(@RequestParam(value = "property", defaultValue = "") String property,
                                @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        return new UIConfig(property, env.getProperty(property));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/config/getByProjectId", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get property '{{#property}}' with default value '{{#defaultValue}}' of project "
            + "{{#projectId}}/{{#projectUuid}}")
    public UIConfig getProperty(
            @RequestParam(value = "property", defaultValue = "") String property,
            @RequestParam(value = "projectId", defaultValue = "") String projectId,
            @RequestParam(value = "defaultValue", defaultValue = "") String defaultValue,
            @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        return new UIConfig(property, projectSettingsService.get(projectId, property, defaultValue));
    }

    // TODO: Need to be revised after changing UI to angular 2+
    @RequestMapping(value = "/config/getAuthType", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get property by name '{{#property}}'")
    public UIConfig getAuthTypeProperty(
            @RequestParam(value = "property", defaultValue = "authentication.type") String property) {
        return new UIConfig(property, env.getProperty(property));
    }

    /*
     *  Get group of config settings by prefix.
     *      Currently, it's used with parameters: ("startParam", true), ("tcpdump", false).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/config/get/properties/for_run", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get properties for run with prefix {{#prefix}}, truncPrefix {{#truncPrefix}} of "
            + "project {{#projectId}}/{{#projectUuid}}")
    public List<UIConfig> getPropertiesForRun(
            @RequestParam(value = "prefix", defaultValue = "") String prefix,
            @RequestParam(value = "truncPrefix", defaultValue = "true") String truncPrefix,
            @RequestParam(value = "projectId") BigInteger projectId,
            @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, String> properties = projectSettingsService.getByPrefix(projectId, prefix,
                Boolean.parseBoolean(truncPrefix));
        List<UIConfig> propertiesForRun = Lists.newArrayListWithCapacity(properties.size());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertiesForRun.add(new UIConfig(entry.getKey(), entry.getValue()));
        }
        return propertiesForRun;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/integration/config/get", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Integration config property with config name {{#configName}} and property name "
            + "{{#property}} of project {{#projectId}}/{{#projectUuid}}")
    public UIConfig getIntegrationConfigProperty(
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "configName", defaultValue = "") String configName,
            @RequestParam(value = "property", defaultValue = "") String property,
            @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId);
        if (project == null) {
            throw new ObjectNotFoundException(
                    "Project", String.valueOf(projectId), null, "get configuration '" + configName + "'");
        }
        List<IntegrationConfig> result = Lists.newArrayList();
        for (IntegrationConfig config : project.getIntegrationConfs()) {
            if (configName.equals(config.getName())) {
                result.add(config);
                if (result.size() > 1) {
                    log.error("There are more than one configuration with name '{}' in ITF. "
                            + "Please, check 'Integrations' tab", configName);
                    return new UIConfig(property, StringUtils.EMPTY);
                }
            }
        }
        return result.isEmpty() ? new UIConfig(property, StringUtils.EMPTY) : new UIConfig(property,
                result.iterator().next().get(property));
    }

}
