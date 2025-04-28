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

package org.qubership.automation.itf.ui.controls.service.export;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ServiceScopeEntities {
    ENTITY_ITF_CALL_CHAIN_FOLDERS("itfCallChainFolders"),
    ENTITY_ITF_CALL_CHAINS("itfCallChains"),
    ENTITY_ITF_SYSTEM_FOLDERS("itfSystemFolders"),
    ENTITY_ITF_SYSTEMS("itfSystems"),
    ENTITY_ITF_ENVIRONMENTS("itfEnvironments"),
    ENTITY_ITF_ENVIRONMENT_FOLDERS("itfEnvironmentFolders"),
    ENTITY_ITF_INTEGRATION_CONFIGS("itfIntegrationConfigs"),
    ENTITY_ITF_PROJECT_SETTINGS("itfProjectSettings");

    private final String value;

    public boolean equals(String value) {
        return this.value.equals(value);
    }

    public String getValue() {
        return value;
    }
}
