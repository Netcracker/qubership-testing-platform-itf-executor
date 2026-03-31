/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.executor.event.trigger.switcher;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Listener;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.exception.TriggerException;
import org.qubership.automation.itf.core.util.holder.EventTriggerHolder;
import org.qubership.automation.itf.executor.cache.service.impl.CallchainSubscriberCacheService;
import org.qubership.automation.itf.executor.event.trigger.EventTriggerSwitcherFactory;
import org.qubership.automation.itf.executor.event.trigger.IEventTriggerSwitcher;
import org.qubership.automation.itf.executor.event.trigger.OperationEventTriggerSwitcher;
import org.qubership.automation.itf.executor.event.trigger.SituationEventTriggerSwitcher;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {CallchainSubscriberCacheService.class,
        EventBusProvider.class,
        EventTriggerSwitcherFactory.class,
        SituationEventTriggerSwitcher.class,
        OperationEventTriggerSwitcher.class})
public class EventTriggerSwitcherTest {

    @Test
    public void situationEventTriggerTest_apply() throws TriggerException {
        EventTrigger trigger = createSituationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.apply(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assertions.assertNotNull(listenerMap);
        Assertions.assertFalse(listenerMap.isEmpty());
        Assertions.assertEquals(TriggerState.ACTIVE, trigger.getState());
    }

    @Test
    public void situationEventTriggerTest_activate() throws TriggerException {
        EventTrigger trigger = createSituationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.activate(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assertions.assertNotNull(listenerMap);
        Assertions.assertFalse(listenerMap.isEmpty());
        Assertions.assertEquals(TriggerState.ACTIVE, trigger.getState());
    }

    @Test
    public void situationEventTriggerTest_deactivate() throws TriggerException {
        EventTrigger trigger = createSituationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.deactivate(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assertions.assertNotNull(listenerMap);
        Assertions.assertTrue(listenerMap.isEmpty());
        Assertions.assertEquals(TriggerState.INACTIVE, trigger.getState());
    }

    @Test
    public void situationEventTriggerTest_errorWhileActivate() {
        SituationEventTrigger trigger = (SituationEventTrigger) createSituationEventTriggerUnderSituation();
        trigger.setSituation(null);
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        try {
            switcherByEventTriggerType.activate(trigger);
            Assertions.fail();
        } catch (TriggerException e) {
            Assertions.assertNotEquals("", e.getMessage());
        }
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assertions.assertTrue(listenerMap.isEmpty());
        Assertions.assertEquals(TriggerState.ERROR, trigger.getState());
    }

    @Test
    public void operationEventTriggerTest_activate() throws TriggerException {
        EventTrigger trigger = createOperationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.activate(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assertions.assertNotNull(listenerMap);
        Assertions.assertTrue(listenerMap.isEmpty());
        Assertions.assertEquals(TriggerState.ACTIVE, trigger.getState());
    }

    @Test
    public void operationEventTriggerTest_deactivate() throws TriggerException {
        EventTrigger trigger = createOperationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.deactivate(trigger);
        Assertions.assertEquals(TriggerState.INACTIVE, trigger.getState());
    }

    private EventTrigger createSituationEventTriggerUnderSituation() {
        SituationEventTrigger trigger = new SituationEventTrigger();
        trigger.setID(BigInteger.valueOf(Math.round((Math.random() + 1) * 1000000)));
        Situation situation = new Situation();
        situation.fillSituationEventTriggers(Collections.singleton(trigger));
        trigger.setSituation(situation);
        trigger.setState(TriggerState.ACTIVE);
        trigger.setOn(SituationEventTrigger.On.FINISH);
        return trigger;
    }

    private EventTrigger createOperationEventTriggerUnderSituation() {
        OperationEventTrigger trigger = new OperationEventTrigger();
        trigger.setID(BigInteger.valueOf(Math.round((Math.random() + 1) * 1000000)));
        Situation situation = new Situation();
        situation.fillOperationEventTriggers(Collections.singleton(trigger));
        trigger.setState(TriggerState.ACTIVE);
        return trigger;
    }
}
