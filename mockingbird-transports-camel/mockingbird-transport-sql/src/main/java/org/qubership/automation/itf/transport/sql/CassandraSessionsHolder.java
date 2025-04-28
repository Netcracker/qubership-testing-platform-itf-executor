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

package org.qubership.automation.itf.transport.sql;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
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
    private static CassandraSessionsHolder INSTANCE = new CassandraSessionsHolder();
    private static boolean isCacheCleanupScheduled = false;
    private volatile Cache<String, Session> sessionsHolder = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .removalListener((RemovalListener<String, Session>) removalNotification -> {
                if (removalNotification.getCause().equals(RemovalCause.EXPIRED)) {
                    Session session = removalNotification.getValue();
                    if (session != null) {
                        Cluster cluster = session.getCluster();
                        if (!session.isClosed()) {
                            session.close();
                        }
                        if (cluster != null && !cluster.isClosed()) {
                            cluster.close();
                        }
                    }
                }
            })
            .build();

    private CassandraSessionsHolder() {
    }

    public static CassandraSessionsHolder getInstance() {
        return INSTANCE;
    }

    public Session getSession(String url, String user, String pass) throws UnknownHostException {
        Session session;
        synchronized (LOCK_STRIPED.get(url)) {
            session = sessionsHolder.getIfPresent(url);
            if (session != null) {
                // Check if session is closed
                if (session.isClosed()) {
                    Cluster cluster = session.getCluster();
                    if (cluster != null && !cluster.isClosed()) {
                        cluster.close();
                    }
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

    private Session createSession(String url, String user, String pass) throws UnknownHostException {
        CassandraClientURI uri = new CassandraClientURI(url, user, pass);
        ExponentialReconnectionPolicy exponentialReconnectionPolicy = new ExponentialReconnectionPolicy(baseDelayMs,
                maxDelayMs);
        Cluster cluster = uri.createBuilder(exponentialReconnectionPolicy);
        return cluster.connect(uri.getDatabase());
    }
}
