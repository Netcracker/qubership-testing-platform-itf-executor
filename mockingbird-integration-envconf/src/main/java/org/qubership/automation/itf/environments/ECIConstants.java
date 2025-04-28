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

package org.qubership.automation.itf.environments;

public enum ECIConstants {
    DIRECTION_EC_PARAM_NAME("direction"),
    ECI_STATUS_ORPHAN_VALUE("Orphan"),
    INBOUND_DIRECTION("inbound"),
    OUTBOUND_DIRECTION("outbound"),
    FOR_ENV("for_env"),
    FOR_TRANSPORT("for_transport"),
    CONNECTION_FOR_ENVIRONMENT_PARAMETER("special"),
    TRANSPORT_EC_LABEL("ITF_TRANSPORT_LABEL");

    private String value;

    ECIConstants(String value) {
        this.value = value;
    }

    public static ECIConstants fromStringValue(String str) {
        for (ECIConstants constant : values()) {
            if (constant.value.equals(str)) {
                return constant;
            }
        }
        return null;
    }

    public String value() {
        return value;
    }
}
