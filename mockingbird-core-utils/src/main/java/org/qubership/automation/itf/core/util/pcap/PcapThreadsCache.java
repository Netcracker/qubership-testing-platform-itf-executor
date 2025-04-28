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

public class PcapThreadsCache {

    private static PcapThreadsCache instance = new PcapThreadsCache();
    private Map<String, PcapThread> threadMap = new HashMap<>();

    private PcapThreadsCache() {
    }

    public static PcapThreadsCache getInstance() {
        return instance;
    }

    public void add(String id, PcapThread thread) {
        threadMap.put(id, thread);
    }

    public PcapThread get(String id) {
        return threadMap.get(id);
    }

    public void delete(String id) {
        threadMap.remove(id);
    }
}
