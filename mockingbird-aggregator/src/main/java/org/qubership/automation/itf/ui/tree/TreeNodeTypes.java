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

package org.qubership.automation.itf.ui.tree;

public enum TreeNodeTypes {
    TC_CONTEXT("TcContext"),
    SITUATION_INSTANCE("SituationInstance"),
    STEP_INSTANCE("StepInstance"),
    INCOMING_MESSAGE("incoming message"),
    OUTGOING_MESSAGE("outgoing message");

    private final String type;

    TreeNodeTypes(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
