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

package org.qubership.automation.itf.ui.controls.util;

public enum ControllerConstants {
    DEFAULT_LABELS_COUNT(30),
    DEFAULT_OBJECTS_COUNT_FOUND_BY_NAME(30),
    SITUATION_EVENT_TRIGGER_TYPE("Situation Event Trigger"),
    OPERATION_EVENT_TRIGGER_TYPE("Operation Event Trigger");

    private int intValue;
    private String stringValue;

    ControllerConstants(int value) {
        this.intValue = value;
    }

    ControllerConstants(String value) {
        this.stringValue = value;
    }

    public int getIntValue() {
        return intValue;
    }

    public String getStringValue() {
        return stringValue;
    }
}
