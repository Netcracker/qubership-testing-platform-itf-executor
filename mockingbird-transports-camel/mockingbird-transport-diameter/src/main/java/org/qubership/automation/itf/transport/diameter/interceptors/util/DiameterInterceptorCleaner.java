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

package org.qubership.automation.itf.transport.diameter.interceptors.util;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.qubership.automation.diameter.connection.ConnectionFactory;
import org.qubership.automation.diameter.connection.DiameterConnection;
import org.qubership.automation.diameter.interceptor.Interceptor;
import org.qubership.automation.itf.core.util.DiameterSessionHolder;
import org.qubership.automation.itf.core.util.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiameterInterceptorCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiameterInterceptorCleaner.class);
    private static final DiameterInterceptorCleaner INSTANCE = new DiameterInterceptorCleaner();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private boolean isOn = false;

    public static DiameterInterceptorCleaner getInstance() {
        return INSTANCE;
    }

    private void cleanIfAny() {
        DiameterSessionHolder sessionHolder = DiameterSessionHolder.getInstance();
        if (sessionHolder.getFinished().isEmpty()) {
            LOGGER.debug("No finished Diameter sessions. Diameter External Interceptors cleanup will not be done");
            return;
        }
        Collection<DiameterConnection> connections = ConnectionFactory.getAll().asMap().values();
        Set<String> idsToDelete = new HashSet<>(sessionHolder.getFinished());
        for (DiameterConnection connection : connections) {
            for (Interceptor interceptor : connection.getInterceptors()) {
                for (String sessionId : sessionHolder.getFinished()) {
                    if (interceptor.getSessionId().equals(sessionId)) {
                        connection.removeInterceptor(interceptor);
                        LOGGER.debug("Diameter interceptor with session id: " + sessionId + " was removed");
                    }
                }
            }
        }
        sessionHolder.removeSetFinished(idsToDelete);
    }

    public synchronized void scheduleCleanupIfNeeded(BigInteger projectId) {
        if (!isOn) {
            final Long cleanInterval = (long) Config.getConfig()
                    .getIntOrDefault("diameter.interceptor.clean.interval", 5);
            service.scheduleWithFixedDelay(() -> {
                try {
                    cleanIfAny();
                } catch (Exception e) {
                    LOGGER.error("Error while Diameter interceptors cleaning up", e);
                }
            }, 2L, cleanInterval, TimeUnit.MINUTES);
            LOGGER.debug("Diameter interceptor cleaner was init successfully. Params: initialDelay: 2 minutes, delay:"
                    + cleanInterval + " minutes");
            isOn = true;
        }
    }
}
