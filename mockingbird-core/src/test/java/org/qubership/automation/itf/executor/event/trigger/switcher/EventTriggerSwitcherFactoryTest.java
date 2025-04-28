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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.executor.event.trigger.EventTriggerSwitcherFactory;
import org.qubership.automation.itf.executor.event.trigger.IEventTriggerSwitcher;
import org.qubership.automation.itf.executor.event.trigger.OperationEventTriggerSwitcher;
import org.qubership.automation.itf.executor.event.trigger.SituationEventTriggerSwitcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EventTriggerSwitcherFactory.class, SituationEventTriggerSwitcher.class,
        OperationEventTriggerSwitcher.class})
public class EventTriggerSwitcherFactoryTest {

    @Test
    public void getSituationEventTriggerSwitcherBySituationEventTriggerTypeTest() {
        SituationEventTrigger situationEventTrigger = new SituationEventTrigger();
        String type = situationEventTrigger.getType();
        IEventTriggerSwitcher switcher = EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(type);
        Assert.assertNotNull(switcher);
        Assert.assertTrue(switcher instanceof SituationEventTriggerSwitcher);
    }

    @Test
    public void getOperationEventTriggerSwitcherByOperationEventTriggerTypeTest() {
        OperationEventTrigger eventTrigger = new OperationEventTrigger();
        String eventTriggerType = eventTrigger.getType();
        IEventTriggerSwitcher switcher = EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(eventTriggerType);
        Assert.assertNotNull(switcher);
        Assert.assertTrue(switcher instanceof OperationEventTriggerSwitcher);
    }
}
