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

import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.exception.TriggerException;

public abstract class EventTriggerSwitcher implements IEventTriggerSwitcher {

    @Override
    public void apply(EventTrigger eventTrigger) throws TriggerException {
        if (eventTrigger.getState().isOn()) {
            try {
                _deactivate(eventTrigger);
                _activate(eventTrigger);
            } catch (Exception e) {
                throw new TriggerException(e);
            }
        }
    }

    public void activate(EventTrigger eventTrigger) throws TriggerException {
        try {
            eventTrigger.setState(TriggerState.STARTING);
            _activate(eventTrigger);
            eventTrigger.setState(TriggerState.ACTIVE);
        } catch (Throwable e) {
            eventTrigger.setState(TriggerState.ERROR);
            eventTrigger.setException(e);
            throw new TriggerException(String.format("Error while starting trigger %s", this), e);
        }
    }

    public void deactivate(EventTrigger eventTrigger) throws TriggerException {
        try {
            eventTrigger.setState(TriggerState.SHUTTING_DOWN);
            _deactivate(eventTrigger);
            eventTrigger.setState(TriggerState.INACTIVE);
        } catch (Throwable e) {
            eventTrigger.setState(TriggerState.ERROR);
            eventTrigger.setException(e);
            throw new TriggerException(String.format("Error while stopping trigger %s", this), e);
        }
    }

    protected abstract void _activate(EventTrigger eventTrigger) throws Exception;

    protected abstract void _deactivate(EventTrigger eventTrigger) throws Exception;
}
