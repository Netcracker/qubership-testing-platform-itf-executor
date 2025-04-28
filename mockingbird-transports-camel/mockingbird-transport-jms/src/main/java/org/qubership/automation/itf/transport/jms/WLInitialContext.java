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

package org.qubership.automation.itf.transport.jms;

import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WLInitialContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(WLInitialContext.class);

    public static synchronized InitialContext init(String principal, String auth, String credentials,
                                                   String brokerUrl, Map<String, String> customProperties) {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_AUTHENTICATION, auth);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
        env.put(Context.PROVIDER_URL, brokerUrl);
        if (customProperties != null) {
            env.putAll(customProperties);
        }
        try {
            return new InitialContext(env);
        } catch (NamingException e) {
            LOGGER.error("Failed initialization of WL InitialContext", e);
        }
        throw new IllegalStateException("InitialContext is not initialized");
    }
}
