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

package org.qubership.automation.itf.core.util;

import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_DIAMETER_CONNECTION_INFO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.qubership.automation.itf.core.model.diameter.DiameterConnectionInfo;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiameterConnectionInfoCacheService {

    private final HazelcastInstance hazelcastClient;

    /**
     * Get list of {@link DiameterConnectionInfo} from HazelcastCache.
     *
     * @return {@link DiameterConnectionInfo} list.
     */
    public List<DiameterConnectionInfo> getAllDiameterConnections() {
        List<DiameterConnectionInfo> connections = new ArrayList<>();
        for (Map.Entry<Object, DiameterConnectionInfo> connectionEntry : getDiameterConnectionCache()) {
            connections.add(connectionEntry.getValue());
        }
        return connections;
    }

    /**
     * Put diameter connection info into Hazelcast cache.
     *
     * @param distributedCacheId projectId_host_port_podName.
     * @param connectionInfo     {@link DiameterConnectionInfo} object.
     */
    public void put(String distributedCacheId, DiameterConnectionInfo connectionInfo) {
        getDiameterConnectionCache().put(distributedCacheId, connectionInfo);
    }

    /**
     * Remove diameter connection info from Hazelcast cache.
     *
     * @param distributedCacheId projectId_host_port_podName.
     */
    public void remove(String distributedCacheId) {
        getDiameterConnectionCache().evict(distributedCacheId);
        getDiameterConnectionCache().delete(distributedCacheId);
    }

    private IMap<Object, DiameterConnectionInfo> getDiameterConnectionCache() {
        return hazelcastClient.getMap(ATP_ITF_DIAMETER_CONNECTION_INFO);
    }
}
