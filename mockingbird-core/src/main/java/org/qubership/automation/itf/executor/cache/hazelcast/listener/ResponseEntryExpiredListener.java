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

package org.qubership.automation.itf.executor.cache.hazelcast.listener;

import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.constants.CacheNames;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryExpiredListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseEntryExpiredListener implements EntryExpiredListener<Object, Message> {

    /**
     * This method calls only when expire event comes from Hazelcast service for response Message to all
     * atp-itf-executor pods.
     *
     * @param entryEvent that contains expired Message.
     */
    @Override
    public void entryExpired(EntryEvent entryEvent) {
        log.debug("Response message by key [{}] is removed from {} cache due to expired.",
                entryEvent.getKey(), CacheNames.ATP_ITF_RESPONSE_MESSAGES);
    }
}
