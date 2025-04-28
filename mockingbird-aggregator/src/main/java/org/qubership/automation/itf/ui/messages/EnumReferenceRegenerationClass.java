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

package org.qubership.automation.itf.ui.messages;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EnumReferenceRegenerationClass {

    CALL_CHAIN(CallChain.class.getCanonicalName(), CallChain.class),
    CHAIN_FOLDER(ChainFolder.class.getCanonicalName(), ChainFolder.class),
    SYSTEM(System.class.getCanonicalName(), System.class),
    SYSTEM_FOLDER(SystemFolder.class.getCanonicalName(), SystemFolder.class),
    SITUATION_STEP(SituationStep.class.getCanonicalName(), SituationStep.class),
    EMBEDDED_STEP(EmbeddedStep.class.getCanonicalName(), EmbeddedStep.class);

    private final String value;
    private final Class<? extends Storable> clazz;

    EnumReferenceRegenerationClass(String value, Class<? extends Storable> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    @JsonCreator
    public static EnumReferenceRegenerationClass fromValue(String value) {
        for (EnumReferenceRegenerationClass enumReferenceRegenerationClass : EnumReferenceRegenerationClass.values()) {
            if (enumReferenceRegenerationClass.getStringValue().equals(value)) {
                return enumReferenceRegenerationClass;
            }
        }
        throw new IllegalArgumentException("Unexpected enum reference regeneration class value '" + value + "'.");
    }

    public String getStringValue() {
        return value;
    }

    public Class<? extends Storable> getEntityClass() {
        return clazz;
    }
}
