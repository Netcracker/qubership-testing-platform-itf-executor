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

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.event.SituationEvent;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.provider.EventBusServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractSituationSubscriber {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSituationSubscriber.class);

    @Getter(lombok.AccessLevel.PROTECTED)
    private final TcContext context;

    @Getter(lombok.AccessLevel.PROTECTED)
    @Setter(lombok.AccessLevel.PROTECTED)
    private String parentEventID;

    public AbstractSituationSubscriber(TcContext context, String parentEventID) {
        this.context = context;
        this.parentEventID = parentEventID;
    }

    protected void finish(SituationEvent.EndExceptionalSituationFinish finishEvent) {
        finishEvent.stop();
        NextCallChainEvent event = createEvent();
        event.setID(parentEventID);
        LOGGER.warn("Executed from situation");
        EventBusServiceProvider.getStaticReference().post(event);
        EventBusServiceProvider.getStaticReference().unregister(this);
        CacheServices.getAwaitingContextsCacheService()
                .evict(String.format("%s_%s", finishEvent.getSituationInstance().getContext().tc().getID(),
                        finishEvent.getSituationInstance().getSituationId()));
    }

    protected abstract NextCallChainEvent createEvent();

    @Nonnull
    protected String getTenantId(SituationEvent.EndExceptionalSituationFinish event) {
        return event.getSituationInstance().getContext().getProjectUuid().toString();
    }
}
