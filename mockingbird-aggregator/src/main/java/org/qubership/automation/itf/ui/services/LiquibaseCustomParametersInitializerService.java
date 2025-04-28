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

package org.qubership.automation.itf.ui.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.stereotype.Service;

@Service
public class LiquibaseCustomParametersInitializerService {

    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${service.entities.migration.enabled:true}")
    private String serviceEntitiesMigrationEnabled;

    public void putCustomParameters(LiquibaseProperties liquibaseProperties) {
        Map<String, String> parameters = getLiquibaseParameters(liquibaseProperties);
        parameters.put("spring.application.name", serviceName);
        parameters.put("service.entities.migration.enabled", serviceEntitiesMigrationEnabled);
        liquibaseProperties.setParameters(parameters);
    }

    private Map<String, String> getLiquibaseParameters(LiquibaseProperties liquibaseProperties) {
        Map<String, String> parameters = liquibaseProperties.getParameters();
        if (Objects.isNull(parameters)) {
            parameters = new HashMap<>();
        }
        return parameters;
    }
}
