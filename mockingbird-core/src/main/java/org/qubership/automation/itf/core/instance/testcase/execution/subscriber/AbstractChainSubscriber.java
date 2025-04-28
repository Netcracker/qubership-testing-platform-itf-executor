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

import org.jetbrains.annotations.NotNull;
import org.qubership.automation.itf.core.model.event.AbstractEvent;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.provider.EventBusServiceProvider;
import org.slf4j.LoggerFactory;

public abstract class AbstractChainSubscriber<T extends AbstractEvent> {

    private final String id;
    private String parentId;

    protected AbstractChainSubscriber(String id, String parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    public void handleEvent(T event) {
        try {
            if (event.getID().equals(id)) {
                onEvent(event);
            } else {
                unregisterIfExpired();
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Failed processing event " + event, e);
            destroy();
        }
    }

    void sendFailEventToTheSubscriber(String subscriberId, Exception exception) {
        EventBusServiceProvider.getStaticReference().post(new NextCallChainEvent.Fail(subscriberId, exception));
    }

    protected abstract void unregisterIfExpired();

    protected abstract void onEvent(T event) throws Exception;

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    protected void destroy() {
        EventBusServiceProvider.getStaticReference().unregister(this);
    }

    protected <Subscriber extends AbstractChainSubscriber, Event extends AbstractEvent> void subscribeAndPostEvent(
            Subscriber stepSubscriber, Event stepEvent) {
        EventBusServiceProvider.getStaticReference().register(stepSubscriber, EventBusProvider.Priority.HIGH);
        EventBusServiceProvider.getStaticReference().post(stepEvent);
    }

    @NotNull
    protected abstract String getTenantId(T event);
}
