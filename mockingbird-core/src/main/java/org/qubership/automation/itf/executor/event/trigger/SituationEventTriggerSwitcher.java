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

import static org.qubership.automation.itf.executor.event.trigger.EventTriggerService.executeSituationBySituationEventTrigger;

import java.util.Objects;

import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Listener;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.exception.TriggerException;
import org.qubership.automation.itf.core.util.holder.EventTriggerHolder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Service
public class SituationEventTriggerSwitcher extends EventTriggerSwitcher {

    private static final String RUNNING_HOSTNAME = Config.getConfig().getRunningHostname();

    private transient Listener listener;
    private final EventBusProvider eventBusProvider;

    @Autowired
    public SituationEventTriggerSwitcher(EventBusProvider eventBusProvider) {
        this.eventBusProvider = eventBusProvider;
    }

    protected void _activate(EventTrigger eventTrigger) throws TriggerException {
        SituationEventTrigger situationEventTrigger = (SituationEventTrigger) eventTrigger;
        if (situationEventTrigger.getSituation() == null) {
            throw new TriggerException(
                    String.format("Situation for trigger is not specified. Situation trigger container is: %s",
                            eventTrigger.getParent().toString()));
        }
        if (situationEventTrigger.getOn() == null) {
            throw new TriggerException(
                    String.format("On condition is not specified. Situation trigger container is: %s",
                            eventTrigger.getParent().toString()));
        }
        deactivateOldThenActivateNew(situationEventTrigger.getID(), situationEventTrigger.getOn(),
                situationEventTrigger.getSituation().getID());
    }

    protected void _deactivate(EventTrigger eventTrigger) {
        synchronized (eventTrigger.getID()) {
            listener = EventTriggerHolder.getInstance().get(eventTrigger.getID());
            // Check for null. It is possible if the trigger was not activated due to some errors (i.e.
            // misconfiguration)
            if (listener != null) {
                // As we can see in _activate() method, only onFinish triggers are registered in the EventBusProvider
                // So, we should unregister only their listeners
                boolean isStartListener = listener instanceof StartListener;
                if (!isStartListener) {
                    eventBusProvider.unregisterNormal(listener);
                }
                EventTriggerHolder.getInstance().remove(listener, isStartListener);
            }
        }
    }

    private void deactivateOldThenActivateNew(Object triggerId, SituationEventTrigger.On onEvent, Object situationId)
            throws TriggerException {
        synchronized (triggerId) {
            boolean isStartListener = false;
            if (SituationEventTrigger.On.FINISH.equals(onEvent)) {
                listener = new SituationEventTriggerSwitcher.EndListener(triggerId, situationId);
                /*
                    Only EndListeners are registered in the bus.
                    StartListeners are processed directly in the SituationExecutor,
                    due to unstable behavior of bus in case of chains of on-start events.
                 */
                /*
                    Check if Holder already contains listener for the trigger. Unregister it if needed.
                    Then remove from Holder.
                 */
                Listener oldListener = EventTriggerHolder.getInstance().get(triggerId);
                if (oldListener != null) {
                    eventBusProvider.unregisterNormal(oldListener);
                    EventTriggerHolder.getInstance().remove(oldListener, false);
                }
                eventBusProvider.register(listener, EventBusProvider.Priority.NORMAL);
            } else if (SituationEventTrigger.On.START.equals(onEvent)) {
                listener = new SituationEventTriggerSwitcher.StartListener(triggerId, situationId);
                isStartListener = true;
                /*
                    Check if Holder already contains listener for the trigger.
                    Remove it from Holder.
                 */
                Listener oldListener = EventTriggerHolder.getInstance().get(triggerId);
                if (oldListener != null) {
                    EventTriggerHolder.getInstance().remove(oldListener, true);
                }
            }
            if (listener == null) {
                throw new TriggerException("Cannot activate situation trigger %s. Listener isn't initialized");
            }
            EventTriggerHolder.getInstance().add(listener, isStartListener);
        }
    }

    private static class StartListener implements Listener {

        final Object triggerId;
        private final Object situationId;

        private StartListener(Object triggerId, Object situationId) {
            this.triggerId = triggerId;
            this.situationId = situationId;
        }

        @Subscribe
        @AllowConcurrentEvents
        public void on(SituationEvent.Start event) {
            if (situationId.equals(event.getSituationInstance().getStepContainer().getID()) && !event.isStopped()) {
                SituationEventTrigger eventTrigger = CoreObjectManager.getInstance()
                        .getManager(SituationEventTrigger.class).getById(triggerId);
                executeSituationBySituationEventTrigger(event, eventTrigger);
            }
        }

        @Override
        public Object getId() {
            return this.triggerId;
        }

        @Override
        public Object getSituationId() {
            return situationId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StartListener that = (StartListener) o;
            return triggerId.equals(that.triggerId) && situationId.equals(that.situationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(triggerId, situationId);
        }
    }

    private static class EndListener implements Listener {

        final Object triggerId;
        private final Object situationId;

        private EndListener(Object triggerId, Object situationId) {
            this.triggerId = triggerId;
            this.situationId = situationId;
        }

        @Subscribe
        @AllowConcurrentEvents
        public void on(SituationEvent.Finish event) {
            if (situationId.equals(event.getSituationInstance().getStepContainer().getID())
                    && !event.isStopped()
                    && event.getRunningHostname().equals(RUNNING_HOSTNAME)) {
                SituationEventTrigger eventTrigger = CoreObjectManager.getInstance()
                        .getManager(SituationEventTrigger.class).getById(triggerId);
                executeSituationBySituationEventTrigger(event, eventTrigger);
            }
        }

        @Override
        public Object getId() {
            return this.triggerId;
        }

        @Override
        public Object getSituationId() {
            return situationId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EndListener that = (EndListener) o;
            return triggerId.equals(that.triggerId) && situationId.equals(that.situationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(triggerId, situationId);
        }
    }
}
