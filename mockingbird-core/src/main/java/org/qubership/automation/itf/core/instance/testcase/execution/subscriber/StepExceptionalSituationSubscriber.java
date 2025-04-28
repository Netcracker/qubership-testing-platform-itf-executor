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

import java.util.Set;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class StepExceptionalSituationSubscriber extends AbstractSituationSubscriber {

    private final Set<Situation> exceptionalSituations;

    private String exceptionMessage = "";

    public StepExceptionalSituationSubscriber(TcContext context, Set<Situation> exceptionalSituations,
                                              String parentEventId) {
        super(context, parentEventId);
        this.exceptionalSituations = exceptionalSituations;
    }

    @SuppressWarnings({"UnstableApiUsage", "unused"})
    @Subscribe
    @AllowConcurrentEvents
    public void handle(SituationEvent.EndExceptionalSituationFinish event) {
        TenantContext.setTenantInfo(getTenantId(event));
        SituationInstance situationInstance = event.getSituationInstance();
        Situation situation = situationInstance.getSituationById();
        if (situationInstance.getContext().tc().equals(getContext()) && exceptionalSituations.contains(situation)) {
            LOGGER.info("Got event Situation {} finished in context {}. Will trigger exception call in chain...",
                    situation.getName(), getContext().getName());
            exceptionMessage = "Occurred exceptional situation: " + situation.getName();
            finish(event);
        } else {
            LOGGER.debug("Ignoring event {}", event);
        }
    }

    @Override
    protected NextCallChainEvent createEvent() {
        return new NextCallChainEvent.Exception(getParentEventID(), null, exceptionMessage);
    }
}
