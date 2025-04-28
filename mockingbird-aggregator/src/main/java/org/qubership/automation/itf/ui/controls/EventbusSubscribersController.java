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

package org.qubership.automation.itf.ui.controls;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.ui.messages.objects.UIEventbusSubscribers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.eventbus.EventBus;

@RestController
public class EventbusSubscribersController {

    private EventBusProvider eventBusProvider;

    @Autowired
    public EventbusSubscribersController(EventBusProvider eventBusProvider) {
        this.eventBusProvider = eventBusProvider;
    }

    @PreAuthorize("@entityAccess.isSupport()")
    @GetMapping(value = "/tools/eventbusSubscribers")
    @AuditAction(auditAction = "Get Eventbus Subscribers, project {{#projectId}}")
    public UIEventbusSubscribers getEventbusSubscribers(@RequestParam BigInteger projectId)
            throws NoSuchFieldException, IllegalAccessException {
        UIEventbusSubscribers uiEventbusSubscribers = new UIEventbusSubscribers();
        uiEventbusSubscribers.setNormalPrioritySubscribers(modifyMap(getNormalPrioritySubscribers()));
        uiEventbusSubscribers.setHighPrioritySubscribers(modifyMap(getHighPrioritySubscribers()));
        return uiEventbusSubscribers;
    }

    private Map getNormalPrioritySubscribers() throws NoSuchFieldException, IllegalAccessException {
        return getSubscribers(eventBusProvider.getBus(EventBusProvider.Priority.NORMAL));
    }

    private Map getHighPrioritySubscribers() throws NoSuchFieldException, IllegalAccessException {
        return getSubscribers(eventBusProvider.getBus(EventBusProvider.Priority.HIGH));
    }

    private Map getSubscribers(EventBus eventBus) throws IllegalAccessException, NoSuchFieldException {
        Field subscribersField = eventBus.getClass().getDeclaredField("subscribers");
        subscribersField.setAccessible(true);
        Object subscribers = subscribersField.get(eventBus);
        Field subscriberListField = subscribers.getClass().getDeclaredField("subscribers");
        subscriberListField.setAccessible(true);
        return (Map) subscriberListField.get(subscribers);
    }

    private Map modifyMap(Map map) {
        Map concurrentHashMap = new ConcurrentHashMap();
        for (Object object : map.entrySet()) {
            Map.Entry<Class, Collection> entry = (Map.Entry<Class, Collection>) object;
            concurrentHashMap.put(entry.getKey().getName(), String.valueOf(entry.getValue().size()));
        }
        return concurrentHashMap;
    }
}
