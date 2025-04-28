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

import java.util.Objects;

import org.qubership.automation.diameter.connection.ConnectionFactory;
import org.qubership.automation.itf.core.model.diameter.DiameterConnectionInfo;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryEvictedListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiameterConnectionInfoEvictedListener implements EntryEvictedListener<Object, DiameterConnectionInfo> {

    @Override
    public void entryEvicted(EntryEvent<Object, DiameterConnectionInfo> entryEvent) {
        String localCacheId = entryEvent.getOldValue().getConnectionId();
        log.info("Evict event is received for diameter connection id: {}", localCacheId);
        if (Objects.isNull(ConnectionFactory.getExisting(localCacheId))) {
            log.info("Connection was already invalidated from local cache. Diameter connection id: {}", localCacheId);
            return;
        }
        ConnectionFactory.destroy(localCacheId);
        log.info("Diameter connection is destroyed from local cache. Diameter connection id: {}", localCacheId);
    }
}
