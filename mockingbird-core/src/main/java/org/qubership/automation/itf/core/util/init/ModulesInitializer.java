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

package org.qubership.automation.itf.core.util.init;

import java.util.Set;

import org.qubership.automation.itf.core.util.helper.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModulesInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModulesInitializer.class);

    public ModulesInitializer() {
        initModules();
    }

    private void initModules() {
        Set<Class<?>> classes = Reflection.getReflections().getTypesAnnotatedWith(Initializer.class);
        for (Class<?> aClass : classes) {
            try {
                aClass.newInstance();
            } catch (Exception e) {
                LOGGER.error("Error in initializer {}", aClass.getName(), e);
            }
        }
    }
}
