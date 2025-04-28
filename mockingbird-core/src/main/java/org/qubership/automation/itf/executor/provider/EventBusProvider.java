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

package org.qubership.automation.itf.executor.provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.automation.itf.core.model.event.Event;
import org.qubership.automation.itf.executor.cache.service.impl.CallchainSubscriberCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

@Service
public class EventBusProvider {
    private static final Logger BUS_PROVIDER_LOGGER = LoggerFactory.getLogger(EventBusProvider.class);

    private final EventBus normalPriorityEventBus =
            new EventBus(new LoggingSubscriberExceptionHandler("Normal priority bus"));
    private final EventBus highPriorityEventBus =
            new EventBus(new LoggingSubscriberExceptionHandler("High priority bus"));
    private CallchainSubscriberCacheService callchainSubscriberCacheService;

    @Autowired
    public EventBusProvider(CallchainSubscriberCacheService callchainSubscriberCacheService) {
        this.callchainSubscriberCacheService = callchainSubscriberCacheService;
    }

    public void post(Event event) {
        highPriorityEventBus.post(event);
        normalPriorityEventBus.post(event);
    }

    public void register(Object subscriber) {
        register(subscriber, Priority.NORMAL);
    }

    /**
     * Register subscriber in High or Normal Priority Bus, and in the cache.
     */
    public void register(Object subscriber, Priority priority) {
        if (Priority.HIGH.equals(priority)) {
            highPriorityEventBus.register(subscriber);
        } else {
            normalPriorityEventBus.register(subscriber);
        }
        callchainSubscriberCacheService.registerSubscriber(subscriber);
    }

    /**
     * Unregister subscriber from Buses and from cache.
     */
    public void unregister(Object subscriber) {
        callchainSubscriberCacheService.unregisterSubscriber(subscriber);
        try {
            highPriorityEventBus.unregister(subscriber);
        } catch (Exception e) { /*its ok*/ }
        try {
            normalPriorityEventBus.unregister(subscriber);
        } catch (Exception e) { /*its ok*/ }
    }

    /**
     * Unregister subscriber from Normal Priority Bus and from cache.
     */
    public void unregisterNormal(Object subscriber) {
        callchainSubscriberCacheService.unregisterSubscriber(subscriber);
        try {
            normalPriorityEventBus.unregister(subscriber);
        } catch (Exception e) {
            BUS_PROVIDER_LOGGER.warn("Failed unregistering of subscriber {}", subscriber, e);
        }
    }

    /**
     * Get Bus of String priority given.
     */
    public EventBus getBus(String priority) {
        return (EventBusProvider.Priority.HIGH.toString().equals(priority))
                ? highPriorityEventBus : normalPriorityEventBus;
    }

    /**
     * Get Bus of priority given.
     */
    public EventBus getBus(Priority priority) {
        return (EventBusProvider.Priority.HIGH.equals(priority))
                ? highPriorityEventBus : normalPriorityEventBus;
    }

    public enum Priority {
        NORMAL, HIGH
    }

    private static final class LoggingSubscriberExceptionHandler implements SubscriberExceptionHandler {
        private String name;

        LoggingSubscriberExceptionHandler(String name) {
            this.name = name;
        }

        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            String exceptionMessage = (StringUtils.isBlank(exception.getMessage()))
                    ? exception.toString()
                    : exception.getMessage();
            exceptionMessage += (exception.getCause() != null)
                    ? ("\nCaused by: " + exception.getCause().toString())
                    : ("\nStacktrace: " + ExceptionUtils.getStackTrace(exception));
            BUS_PROVIDER_LOGGER.error("Could not dispatch event {}; {} to {} at bus {}: Error: {}",
                    context.getEvent(), context.getSubscriber(), context.getSubscriberMethod(), name, exceptionMessage);
        }
    }
}
