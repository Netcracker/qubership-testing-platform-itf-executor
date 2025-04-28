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

package org.qubership.automation.itf.ui.util;

import lombok.Getter;

public enum UserManagementEntities {

    CALLCHAIN("Callchain"),
    INTERCEPTOR("Interceptor"),
    OPERATION("Operation"),
    PARSING_RULE("Parsing Rule"),
    SITUATION("Situation"),
    TEMPLATE("Template"),
    TRANSPORT("Transport"),
    TRIGGER("Trigger");

    @Getter
    private String name;

    UserManagementEntities(String name) {
        this.name = name;
    }

}
