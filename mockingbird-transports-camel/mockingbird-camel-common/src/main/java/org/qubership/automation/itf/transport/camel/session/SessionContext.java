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

import java.util.HashMap;

import org.qubership.automation.itf.core.util.transport.base.Transport;

public class SessionContext extends HashMap {

    public static final String TRANSPORT = "transport";

    private Session session;

    SessionContext(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public Transport getTransport() {
        return get(TRANSPORT, Transport.class);
    }

    public void setTransport(Transport transport) {
        put(TRANSPORT, transport);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> clazz) {
        return (T) get(key);
    }
}
