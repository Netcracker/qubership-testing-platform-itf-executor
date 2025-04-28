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

package org.qubership.automation.itf.environments.convert;

import java.util.Map;

import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.environments.convert.impl.ConnectionConverter;
import org.qubership.automation.itf.environments.convert.impl.EnvironmentConverter;
import org.qubership.automation.itf.environments.convert.impl.InboundTransportConfigurationConverter;
import org.qubership.automation.itf.environments.convert.impl.OutboundTransportConfigurationConverter;
import org.qubership.automation.itf.environments.convert.impl.ServerConverter;
import org.qubership.automation.itf.environments.convert.impl.SystemConverter;
import org.qubership.automation.itf.environments.object.ECEntity;

import com.google.common.collect.Maps;

public class ConverterFactory {

    private static final Map<Class<? extends EciConfigurable>, Converter> converterMap = Maps.newHashMap();

    static {
        converterMap.put(Environment.class, new EnvironmentConverter());
        converterMap.put(System.class, new SystemConverter());
        converterMap.put(TransportConfiguration.class, new ConnectionConverter());
        converterMap.put(Server.class, new ServerConverter());
        converterMap.put(OutboundTransportConfiguration.class, new OutboundTransportConfigurationConverter());
        converterMap.put(InboundTransportConfiguration.class, new InboundTransportConfigurationConverter());
    }

    public static <T extends EciConfigurable, V extends ECEntity<? extends EciConfigurable>> Converter<T, V> getConverter(Class<T> clazz) {
        return converterMap.get(clazz);
    }
}
