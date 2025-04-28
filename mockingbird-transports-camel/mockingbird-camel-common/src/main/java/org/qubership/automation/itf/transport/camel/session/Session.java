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

package org.qubership.automation.itf.transport.camel.session;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Roman Aksenenko
 * @since 03.04.2016
 */
public class Session {

    private Logger log = LoggerFactory.getLogger(Session.class);
    private SessionContext sessionContext;
    private SessionState state = SessionState.NOT_STARTED;
    private Throwable error;
    private boolean terminationRequested = false;
    private SessionRunnable runnable;
    private boolean async;
    private String id;

    public Session(SessionRunnable runnable) {
        this.sessionContext = new SessionContext(this);
        this.runnable = runnable;
        id = UUID.randomUUID().toString();
    }

    public SessionContext getSessionContext() {
        return sessionContext;
    }

    final void process() throws Exception {
        try {
            state = SessionState.IN_PROGRESS;
            log.debug("Starting processing session" + sessionContext);
            runnable.run(sessionContext);
            state = SessionState.FINISHED;
        } catch (Throwable e) {
            state = SessionState.TERMINATED;
            setError(e);
            log.error("Failed processing of session", e);
            throw e;
        }
    }

    public String getId() {
        return id;
    }

    public void setRunnable(SessionRunnable runnable) {
        this.runnable = runnable;
    }

    public SessionState getState() {
        return state;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable throwable) {
        error = throwable;
    }

    public void requestTermination() {
        terminationRequested = true;
    }

    public boolean isTerminationRequested() {
        return terminationRequested;
    }

    protected void _terminate() {
        state = SessionState.TERMINATED;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
