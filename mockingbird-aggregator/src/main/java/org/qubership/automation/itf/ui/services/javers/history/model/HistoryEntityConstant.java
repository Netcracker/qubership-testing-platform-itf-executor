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

package org.qubership.automation.itf.ui.services.javers.history.model;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;

public enum HistoryEntityConstant {

    SYSTEM_TEMPLATE(SystemTemplate.class.getSimpleName().toLowerCase(), SystemTemplate.class),
    OPERATION_TEMPLATE(OperationTemplate.class.getSimpleName().toLowerCase(), OperationTemplate.class),
    CALL_CHAIN(CallChain.class.getSimpleName().toLowerCase(), CallChain.class),
    SYSTEM_PARSING_RULE(SystemParsingRule.class.getSimpleName().toLowerCase(), SystemParsingRule.class),
    OPERATION_PARSING_RULE(OperationParsingRule.class.getSimpleName().toLowerCase(), OperationParsingRule.class),
    SITUATION_STEP(SituationStep.class.getSimpleName().toLowerCase(), SituationStep.class),
    EMBEDDED_STEP(EmbeddedStep.class.getSimpleName().toLowerCase(), EmbeddedStep.class),
    STUB_PROJECT(StubProject.class.getSimpleName().toLowerCase(), StubProject.class),
    INTEGRATION_CONFIG(IntegrationConfig.class.getSimpleName().toLowerCase(), IntegrationConfig.class),
    OPERATION(Operation.class.getSimpleName().toLowerCase(), Operation.class),
    SITUATION(Situation.class.getSimpleName().toLowerCase(), Situation.class),
    OPERATION_EVENT_TRIGGER(OperationEventTrigger.class.getSimpleName().toLowerCase(), OperationEventTrigger.class),
    SITUATION_EVENT_TRIGGER(SituationEventTrigger.class.getSimpleName().toLowerCase(), SituationEventTrigger.class);

    private final String value;
    private final Class<? extends Storable> clazz;

    HistoryEntityConstant(String value, Class<? extends Storable> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public static HistoryEntityConstant fromValue(String value) {
        for (HistoryEntityConstant entityConstant : HistoryEntityConstant.values()) {
            if (entityConstant.getStringValue().equals(value)) {
                return entityConstant;
            }
        }
        throw new IllegalArgumentException("Unexpected history entity type value '" + value + "'.");
    }

    public String getStringValue() {
        return value;
    }

    public Class<? extends Storable> getEntityClass() {
        return clazz;
    }
}
