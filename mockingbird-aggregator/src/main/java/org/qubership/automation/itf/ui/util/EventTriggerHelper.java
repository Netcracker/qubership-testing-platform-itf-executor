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

import java.math.BigInteger;
import java.util.Collection;
import java.util.stream.Collectors;

import org.qubership.automation.itf.core.hibernate.spring.managers.executor.OperationEventTriggerObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SituationEventTriggerObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.converter.IdConverter;
import org.qubership.automation.itf.core.util.exception.TriggerException;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.event.trigger.EventTriggerSwitcherFactory;
import org.qubership.automation.itf.executor.event.trigger.IEventTriggerSwitcher;
import org.qubership.automation.itf.ui.controls.util.ControllerConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventTriggerHelper {

    public static void activateEventTriggers(BigInteger projectId) {
        activate(EventTriggerHelper.getAllActiveByProject(projectId), projectId);
    }

    private static void activate(Collection<? extends EventTrigger> eventTriggers, BigInteger projectId) {
        String logProjectString = projectId == null ? "" : "Project " + projectId + ":";
        log.info("{} Activation of Event Triggers is started...", logProjectString);
        int total = eventTriggers.size();
        int count = 0;
        int totalActive = 0;
        int countActive = 0;
        for (EventTrigger trigger : eventTriggers) {
            if (trigger.getState() != null && TriggerState.ACTIVE.equals(trigger.getState())) {
                totalActive++;
                try {
                    IEventTriggerSwitcher switcher = EventTriggerSwitcherFactory
                            .getSwitcherByEventTriggerType(trigger.getType());
                    switcher.activate(trigger);
                    countActive++;
                    log.debug("Trigger {} is activated", trigger);
                } catch (TriggerException e) {
                    log.error("Error activating trigger {}: {}", trigger, e.getMessage());
                }
            }
            if ((++count) % 50 == 0) {
                log.info("{} Processed {} of {}; activated: {} of {}...", logProjectString, count, total,
                        countActive, totalActive);
            }
        }
        log.info("{} Activation of Event Triggers is finished - total {}; activated {} of {}.",
                logProjectString, total, countActive, totalActive);
    }

    private static Collection<EventTrigger> getAllActiveByProject(BigInteger projectId) {
        return CoreObjectManager.getInstance()
                .getSpecialManager(SituationEventTrigger.class, SituationEventTriggerObjectManager.class)
                .getActiveByProject(projectId)
                .stream()
                .map(EventTrigger.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Get EventTrigger by id. Not recommended to use, due to overhead queries in 50% of cases.
     * Use getByIdAndType() instead.
     */
    public static EventTrigger getById(Object id) {
        SituationEventTrigger situationEventTrigger = CoreObjectManager.getInstance()
                .getSpecialManager(SituationEventTrigger.class, SituationEventTriggerObjectManager.class)
                .getByIdOnly(IdConverter.toBigInt(id));
        return situationEventTrigger != null ? situationEventTrigger :
                CoreObjectManager.getInstance()
                        .getSpecialManager(OperationEventTrigger.class, OperationEventTriggerObjectManager.class)
                        .getByIdOnly(IdConverter.toBigInt(id));
    }

    /**
     * Get EventTrigger by id and type.
     */
    public static EventTrigger getByIdAndType(Object id, String type) {
        return ControllerConstants.OPERATION_EVENT_TRIGGER_TYPE.getStringValue().equals(type)
                ? CoreObjectManager.getInstance().getManager(OperationEventTrigger.class).getById(id)
                : CoreObjectManager.getInstance().getManager(SituationEventTrigger.class).getById(id);
    }

    /**
     * Create EventTrigger of type, under parent.
     */
    public static EventTrigger create(Storable parent, String type) {
        return ControllerConstants.OPERATION_EVENT_TRIGGER_TYPE.getStringValue().equals(type)
                ? CoreObjectManager.getInstance().getManager(OperationEventTrigger.class).create(parent, type)
                : CoreObjectManager.getInstance().getManager(SituationEventTrigger.class).create(parent, type);
    }
}
