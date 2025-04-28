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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.StandardServletEnvironment;

@RestController
public class EnvPropertiesController {

    private Environment environment;

    @Autowired
    public EnvPropertiesController(Environment environment) {
        this.environment = environment;
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/getEnvironmentProperties", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Environment properties, project {{#projectUuid}}")
    public Map<String, String> getEnvironmentProperties(@RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, String> environmentProperties = new HashMap<>();
        MutablePropertySources propertySources = ((StandardServletEnvironment) environment).getPropertySources();
        fillMap(environmentProperties, propertySources.get("applicationConfig: [file:application.properties]"));
        fillMap(environmentProperties, propertySources.get("applicationConfig: [file:bootstrap.properties]"));
        return environmentProperties;
    }

    private void fillMap(Map<String, String> environmentProperties, PropertySource propertySource) {
        String[] properties = ((OriginTrackedMapPropertySource) propertySource).getPropertyNames();
        for (String property : properties) {
            environmentProperties.put(property, environment.getProperty(property));
        }
    }

}
