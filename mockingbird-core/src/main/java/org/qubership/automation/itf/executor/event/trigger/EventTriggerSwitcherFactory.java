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

package org.qubership.automation.itf.executor.event.trigger;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventTriggerSwitcherFactory {

    private static Map<String, EventTriggerSwitcher> eventTriggerSwitcher = new HashMap<>();
    private SituationEventTriggerSwitcher situationEventTriggerSwitcher;
    private OperationEventTriggerSwitcher operationEventTriggerSwitcher;

    public static IEventTriggerSwitcher getSwitcherByEventTriggerType(String eventTriggerType) {
        return eventTriggerSwitcher.get(eventTriggerType);
    }

    @PostConstruct
    private void initEventTriggerSwitchers() {
        eventTriggerSwitcher.put(SituationEventTrigger.TYPE, situationEventTriggerSwitcher);
        eventTriggerSwitcher.put(OperationEventTrigger.TYPE, operationEventTriggerSwitcher);
    }

    @Autowired
    public void setSituationEventTriggerSwitcher(SituationEventTriggerSwitcher situationEventTriggerSwitcher) {
        this.situationEventTriggerSwitcher = situationEventTriggerSwitcher;
    }

    @Autowired
    public void setOperationEventTriggerSwitcher(OperationEventTriggerSwitcher operationEventTriggerSwitcher) {
        this.operationEventTriggerSwitcher = operationEventTriggerSwitcher;
    }
}
