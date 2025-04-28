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

package org.qubership.automation.itf.executor.event.trigger.switcher;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Listener;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.exception.TriggerException;
import org.qubership.automation.itf.core.util.holder.EventTriggerHolder;
import org.qubership.automation.itf.executor.event.trigger.EventTriggerSwitcherFactory;
import org.qubership.automation.itf.executor.event.trigger.IEventTriggerSwitcher;
import org.qubership.automation.itf.executor.event.trigger.OperationEventTriggerSwitcher;
import org.qubership.automation.itf.executor.event.trigger.SituationEventTriggerSwitcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EventTriggerSwitcherFactory.class, SituationEventTriggerSwitcher.class,
        OperationEventTriggerSwitcher.class})
public class EventTriggerSwitcherTest {

    @Test
    public void situationEventTriggerTest_apply() throws TriggerException {
        EventTrigger trigger = createSituationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.apply(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assert.assertNotNull(listenerMap);
        Assert.assertFalse(listenerMap.isEmpty());
        Assert.assertEquals(trigger.getState(), TriggerState.ACTIVE);
    }

    @Test
    public void situationEventTriggerTest_activate() throws TriggerException {
        EventTrigger trigger = createSituationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.activate(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assert.assertNotNull(listenerMap);
        Assert.assertFalse(listenerMap.isEmpty());
        Assert.assertEquals(trigger.getState(), TriggerState.ACTIVE);
    }

    @Test
    public void situationEventTriggerTest_deactivate() throws TriggerException {
        EventTrigger trigger = createSituationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.deactivate(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assert.assertNotNull(listenerMap);
        Assert.assertTrue(listenerMap.isEmpty());
        Assert.assertEquals(trigger.getState(), TriggerState.INACTIVE);
    }

    @Test
    public void situationEventTriggerTest_errorWhileActivate() {
        SituationEventTrigger trigger = (SituationEventTrigger) createSituationEventTriggerUnderSituation();
        trigger.setSituation(null);
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        try {
            switcherByEventTriggerType.activate(trigger);
            Assert.fail();
        } catch (TriggerException e) {
            Assert.assertNotEquals("", e.getMessage());
        }
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assert.assertTrue(listenerMap.isEmpty());
        Assert.assertEquals(trigger.getState(), TriggerState.ERROR);
    }

    @Test
    public void operationEventTriggerTest_activate() throws TriggerException {
        EventTrigger trigger = createOperationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.activate(trigger);
        Map<Object, Listener> listenerMap = EventTriggerHolder.getInstance().getAll();
        Assert.assertNotNull(listenerMap);
        Assert.assertTrue(listenerMap.isEmpty());
        Assert.assertEquals(trigger.getState(), TriggerState.ACTIVE);
    }

    @Test
    public void operationEventTriggerTest_deactivate() throws TriggerException {
        EventTrigger trigger = createOperationEventTriggerUnderSituation();
        IEventTriggerSwitcher switcherByEventTriggerType =
                EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType());
        switcherByEventTriggerType.deactivate(trigger);
        Assert.assertEquals(trigger.getState(), TriggerState.INACTIVE);
    }

    private EventTrigger createSituationEventTriggerUnderSituation() {
        SituationEventTrigger trigger = new SituationEventTrigger();
        Situation situation = new Situation();
        situation.fillSituationEventTriggers(Collections.singleton(trigger));
        trigger.setSituation(situation);
        trigger.setState(TriggerState.ACTIVE);
        trigger.setOn(SituationEventTrigger.On.FINISH);
        return trigger;
    }

    private EventTrigger createOperationEventTriggerUnderSituation() {
        OperationEventTrigger trigger = new OperationEventTrigger();
        Situation situation = new Situation();
        situation.fillOperationEventTriggers(Collections.singleton(trigger));
        trigger.setState(TriggerState.ACTIVE);
        return trigger;
    }
}
