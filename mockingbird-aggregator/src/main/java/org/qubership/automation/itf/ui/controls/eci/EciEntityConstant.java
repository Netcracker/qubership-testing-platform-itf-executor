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

package org.qubership.automation.itf.ui.controls.eci;

import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;

public enum EciEntityConstant {

    ENVIRONMENT(Environment.class.getCanonicalName(), Environment.class),
    SYSTEM(System.class.getCanonicalName(), System.class),
    TRANSPORT_CONFIGURATION(TransportConfiguration.class.getCanonicalName(), TransportConfiguration.class);

    private final String value;
    private final Class<? extends EciConfigurable> clazz;

    EciEntityConstant(String value, Class<? extends EciConfigurable> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public static EciEntityConstant fromValue(String value) {
        for (EciEntityConstant eciEntityConstant : EciEntityConstant.values()) {
            if (eciEntityConstant.getStringValue().equals(value)) {
                return eciEntityConstant;
            }
        }
        throw new IllegalArgumentException("Unexpected eci entity type value '" + value + "'.");
    }

    public String getStringValue() {
        return value;
    }

    public Class<? extends EciConfigurable> getEntityClass() {
        return clazz;
    }
}
