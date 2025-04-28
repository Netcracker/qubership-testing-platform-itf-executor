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

package org.qubership.automation.itf.core.util.pcap;

import java.util.HashMap;
import java.util.Map;

public class PcapManagersCache {

    private static PcapManagersCache instance = new PcapManagersCache();
    private Map<String, PcapManager> pcapManagers = new HashMap<>();

    private PcapManagersCache() {
    }

    public static PcapManagersCache getInstance() {
        return instance;
    }

    public PcapManager getPcapManager(String id) {
        return pcapManagers.get(id);
    }

    public void add(String id, PcapManager pcapManager) {
        pcapManagers.put(id, pcapManager);
    }

    public void delete(String id) {
        pcapManagers.remove(id);
    }
}
