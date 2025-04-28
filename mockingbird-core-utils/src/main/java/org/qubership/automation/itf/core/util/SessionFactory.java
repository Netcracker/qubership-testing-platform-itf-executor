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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionFactory.class);
    private ConnectionFactory jndiConnectionFactory;
    private Connection connection;

    private SessionFactory() {
    }

    public ConnectionFactory getJndiConnectionFactory() {
        return jndiConnectionFactory;
    }

    public void setJndiConnectionFactory(ConnectionFactory jndiConnectionFactory) {
        this.jndiConnectionFactory = jndiConnectionFactory;
    }

    public Session createSession() {
        if (connection == null) {
            init();
        }
        if (connection != null) {
            try {
                return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            } catch (JMSException e) {
                LOGGER.error("Session didn't create");
                throw new RuntimeException(e);
            }
        } else {
            throw new NoBrokerConnectionException("No connection established");
        }
    }

    public void init() {
        try {
            if (connection == null) {
                connection = jndiConnectionFactory.createConnection();
            }
            connection.start();
            LOGGER.info("Connection has been started");
        } catch (JMSException e) {
            LOGGER.warn("Connection report execution didn't start: {}", e.getMessage());
        }
    }

    public void destroy() {
        LOGGER.info("Destroy session factory");
        if (connection != null) {
            try {
                // According to comments in \javax\jms\jms-api\1.1-rev-1\jms-api-1.1-rev-1-sources.jar;
                // javax/jms/Connection.java,
                //  'stop()' method:
                //      Temporarily stops a connection's delivery of incoming messages.
                //      * Delivery can be restarted using the connection's <CODE>start</CODE>
                //      * method.
                //
                //  So, connection isn't closed!!! So we don't need to invoke jndiConnectionFactory.createConnection
                //  () again(!?) - it should be tested
                //
                //  Or, may be we want really destroy connection? In that case we should invoke .close() instead of
                //  .stop()
                //connection.stop();
                connection.close();
                connection = null;
            } catch (JMSException e) {
                if (e.getErrorCode().equals("CONNECTION_CLOSED")) {
                    // Connection has already been closed before. Simply ignore this exception
                    connection = null;
                } else {
                    LOGGER.error("Connection report execution didn't stop", e);
                }
            }
        }
    }
}
