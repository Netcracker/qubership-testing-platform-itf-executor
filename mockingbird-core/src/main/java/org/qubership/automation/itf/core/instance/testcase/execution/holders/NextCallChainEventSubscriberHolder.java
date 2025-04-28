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

package org.qubership.automation.itf.core.instance.testcase.execution.holders;

import java.util.Map;

import com.google.common.collect.Maps;

public class NextCallChainEventSubscriberHolder {

    private static NextCallChainEventSubscriberHolder instance = new NextCallChainEventSubscriberHolder();
    private Map<Object, SubscriberData> contextSubscriberHolder = Maps.newHashMap();

    private NextCallChainEventSubscriberHolder() {
    }

    public static NextCallChainEventSubscriberHolder getInstance() {
        return instance;
    }

    public void add(Object tcId, String subscriberId, String parentSubscriberId, Boolean needToContinue) {
        SubscriberData data = new SubscriberData(subscriberId, parentSubscriberId, needToContinue);
        contextSubscriberHolder.put(tcId, data);
    }

    public void remove(Object tcId) {
        contextSubscriberHolder.remove(tcId);
    }

    public SubscriberData getSubscriberData(Object tcId) {
        return contextSubscriberHolder.get(tcId);
    }
}
