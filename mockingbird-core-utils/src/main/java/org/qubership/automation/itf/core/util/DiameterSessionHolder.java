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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiameterSessionHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiameterSessionHolder.class);

    private static final DiameterSessionHolder INSTANCE = new DiameterSessionHolder();
    private final Map<String, Object> sessions = new HashMap<>();
    private final Set<String> finishedSessions = new HashSet<>();

    public static DiameterSessionHolder getInstance() {
        return INSTANCE;
    }

    public boolean add(String sessionId, Object contextId) {
        Object absent = sessions.putIfAbsent(sessionId, contextId);
        return Objects.isNull(absent);
    }

    public void remove(Object tcContextId) {
        if (!sessions.containsValue(tcContextId.toString())) {
            LOGGER.debug("Diameter sessions does not contain context id " + tcContextId);
            return;
        }
        Set<Map.Entry<String, Object>> entries = sessions.entrySet();
        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, Object> entry : entries) {
            if (entry.getValue().equals(tcContextId.toString())) {
                toRemove.add(entry.getKey());
            }
        }
        if (toRemove.isEmpty()) {
            LOGGER.debug("Context id " + tcContextId + " does not contain diameter sessions. "
                            + "Diameter sessions (interceptors) clearing will not be done");
            return;
        }
        finishedSessions.addAll(toRemove);
        LOGGER.debug("Context id " + tcContextId + " contains diameter sessions: " + toRemove + " It(they) was marked "
                        + "as finished");
        sessions.keySet().removeAll(toRemove);
    }

    public Set<String> getFinished() {
        return finishedSessions;
    }

    public void removeFinished(String sessionId) {
        finishedSessions.remove(sessionId);
    }

    public void removeSetFinished(Set<String> sessionIds) {
        finishedSessions.removeAll(sessionIds);
    }

    public void clearAllFinished() {
        finishedSessions.clear();
    }
}
