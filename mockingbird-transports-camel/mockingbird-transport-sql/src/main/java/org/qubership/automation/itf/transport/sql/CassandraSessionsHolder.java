/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.transport.sql;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.util.concurrent.Striped;

public class CassandraSessionsHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraSessionsHolder.class);

    private static final int STRIPES = 100;
    private static final Striped<Lock> LOCK_STRIPED = Striped.lazyWeakLock(STRIPES);
    private static final long baseDelayMs = 100L;
    private static final long maxDelayMs = 51200L;
    private static final ScheduledExecutorService configCacheMaintenanceService =
            Executors.newSingleThreadScheduledExecutor();
    private static final CassandraSessionsHolder INSTANCE = new CassandraSessionsHolder();
    private static boolean isCacheCleanupScheduled = false;
    private final Cache<String, CqlSession> sessionsHolder = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .removalListener((RemovalListener<String, CqlSession>) removalNotification -> {
                if (removalNotification.getCause().equals(RemovalCause.EXPIRED)) {
                    CqlSession session = removalNotification.getValue();
                    if (session != null && !session.isClosed()) {
                        session.close();
                    }
                }
            })
            .build();

    private CassandraSessionsHolder() {
    }

    public static CassandraSessionsHolder getInstance() {
        return INSTANCE;
    }

    public CqlSession getSession(String url, String user, String pass) throws URISyntaxException {
        CqlSession session;
        synchronized (LOCK_STRIPED.get(url)) {
            session = sessionsHolder.getIfPresent(url);
            if (session != null) {
                // Check if session is closed
                if (session.isClosed()) {
                    session = createSession(url, user, pass);
                    sessionsHolder.put(url, session);
                }
            } else {
                session = createSession(url, user, pass);
                sessionsHolder.put(url, session);
            }
        }
        scheduleCacheCleanupIfNeeded();
        return session;
    }

    private synchronized void scheduleCacheCleanupIfNeeded() {
        if (!isCacheCleanupScheduled) {
            if (sessionsHolder.size() > 0) {
                configCacheMaintenanceService.scheduleWithFixedDelay(() -> {
                    try {
                        sessionsHolder.cleanUp();
                    } catch (Throwable t) {
                        LOGGER.error("Error while Cassandra Sessions cache cleanUp: {}", t.toString());
                    }
                }, 61L, 20L, TimeUnit.MINUTES);
                isCacheCleanupScheduled = true;
            }
        }
    }

    private CqlSession createSession(String url, String user, String pass) throws URISyntaxException {
        //CassandraClientURI uri = new CassandraClientURI(url, user, pass);

        // Parse URL format: "cassandra://host:port/keyspace"
        // Example: "cassandra://localhost:9042/my_keyspace"
        java.net.URI uri = new java.net.URI(url);
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 9042;
        String keyspace = uri.getPath() != null && uri.getPath().length() > 1
                ? uri.getPath().substring(1) : null;

        CqlSessionBuilder builder = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter("datacenter1"); // Adjust to your environment (will get it from options)

        // Add authentication if provided
        if (user != null && pass != null) {
            builder = builder.withAuthCredentials(user, pass);
        }

        // Set keyspace if provided in URL
        if (keyspace != null && !keyspace.isEmpty()) {
            builder = builder.withKeyspace(keyspace);
        }

        return builder.build();
    }
}
