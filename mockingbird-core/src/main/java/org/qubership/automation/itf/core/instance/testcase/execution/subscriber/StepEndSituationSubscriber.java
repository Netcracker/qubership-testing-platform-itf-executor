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

package org.qubership.automation.itf.core.instance.testcase.execution.subscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class StepEndSituationSubscriber extends AbstractSituationSubscriber {

    private final Map<Situation, Boolean> endSituations = new HashMap<>();

    private final boolean waitAllEndSituations;

    /**
     * Subscriber that will process finished end situations.
     *
     * @param context              - TcContext
     * @param endSituations        - End Situations set (Situation type)
     * @param callChainEventId     - NextCallChainSubscriber id
     * @param waitAllEndSituations - wait when all end situations will finish (boolean). This value configuring on
     *                             call chain step (tick box)
     */
    public StepEndSituationSubscriber(TcContext context, Set<Situation> endSituations, String callChainEventId,
                                      boolean waitAllEndSituations) {
        super(context, callChainEventId);
        setMapEndSituations(endSituations);
        this.waitAllEndSituations = waitAllEndSituations;
    }

    /**
     * This method that will process finished end situations.
     *
     * @param event - SituationEvent.EndExceptionalSituationFinish event that contains SituationInstance
     */
    @SuppressWarnings({"UnstableApiUsage", "unused"})
    @Subscribe
    @AllowConcurrentEvents
    public void handle(SituationEvent.EndExceptionalSituationFinish event) {
        TenantContext.setTenantInfo(getTenantId(event));
        Situation situation = event.getSituationInstance().getSituationById();
        if (event.getSituationInstance().getContext().tc().equals(getContext()) && endSituations.containsKey(
                situation)) {
            if (!waitAllEndSituations) {
                LOGGER.info("Got event Situation {} finished in context {}. Will trigger next call in chain...",
                        situation.getName(), getContext().getName());
                finish(event);
            } else {
                endSituations.replace(situation, true);
                if (allSituationsIsFinished()) {
                    LOGGER.info("Got event Situation {} finished in context {}. Will trigger next call in chain...",
                            situation.getName(), getContext().getName());
                    finish(event);
                }
            }
        } else {
            LOGGER.debug("Ignoring event {}", event);
        }
    }

    @Override
    protected NextCallChainEvent createEvent() {
        return new NextCallChainEvent(getParentEventID(), null);
    }

    private void setMapEndSituations(Set<Situation> endSituations) {
        for (Situation situation : endSituations) {
            this.endSituations.put(situation, false);
        }
    }

    private boolean allSituationsIsFinished() {
        for (boolean isFinished : endSituations.values()) {
            if (!isFinished) {
                return false;
            }
        }
        return true;
    }
}
