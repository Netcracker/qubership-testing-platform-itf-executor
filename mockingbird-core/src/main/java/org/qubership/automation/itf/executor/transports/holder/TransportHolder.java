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

package org.qubership.automation.itf.executor.transports.holder;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants;
import org.qubership.automation.itf.core.util.helper.ClassResolver;
import org.qubership.automation.itf.core.util.provider.MeansCommunication;
import org.qubership.automation.itf.core.util.transport.base.Transport;
import org.qubership.automation.itf.executor.transports.classloader.TransportClassLoader;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class TransportHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportHolder.class);
    private static final TransportHolder INSTANCE = new TransportHolder();
    private final Set<Transport> transports = Sets.newHashSetWithExpectedSize(20);
    private final Map<String, String> transportNames = new HashMap<>();

    private TransportHolder() {
        deployTransports(false);
    }

    public static TransportHolder getInstance() {
        return INSTANCE;
    }

    public Set<Transport> getTransports() {
        return Collections.unmodifiableSet(transports);
    }

    public void redeploy() {
        deployTransports(true);
    }

    private void deployTransports(boolean isRedeploy) {
        Set<Class> subtypesOf = Sets.newHashSet();
        TransportClassLoader.getInstance()
                .load(ApplicationConfig.env.getProperty(InstanceSettingsConstants.TRANSPORT_FOLDER),
                        ApplicationConfig.env.getProperty(InstanceSettingsConstants.TRANSPORT_LIB));
        for (Map.Entry<String, ClassLoader> entry : TransportClassLoader.getInstance().getClassLoaderHolder()
                .entrySet()) {
            subtypesOf.addAll(ClassResolver.getInstance()
                    .getSubtypesOf(Transport.class, new Reflections("org.qubership", entry.getValue())));
        }
        for (Class aClass : subtypesOf) {
            if (!(Modifier.isAbstract(aClass.getModifiers()))) {
                try {
                    if (isRedeploy) { //We don't need to add deployed transports again
                        if (Objects.nonNull(checkAvailability(aClass.getName()))) {
                            continue;
                        }
                    }
                    Transport transport = (Transport) aClass.newInstance();
                    transports.add(transport);
                    transportNames.put(aClass.getName(), transport.getShortName());
                } catch (Exception e) {
                    LOGGER.error("Cannot instantiate transport {}", aClass, e);
                }
            }
        }
    }

    public MeansCommunication checkAvailability(String typeName) {
        for (Transport transport : transports) {
            if (transport.getClass().getName().equals(typeName)) {
                return transport;
            }
        }
        return null;
    }

    public String getShortName(String typeName) {
        return transportNames.getOrDefault(typeName, "");
    }

}
