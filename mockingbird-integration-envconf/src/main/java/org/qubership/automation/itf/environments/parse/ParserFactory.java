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

package org.qubership.automation.itf.environments.parse;

import java.util.Map;

import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.environments.object.ECEntity;
import org.qubership.automation.itf.environments.parse.parsers.ECConnectionParser;
import org.qubership.automation.itf.environments.parse.parsers.ECSystemParser;
import org.qubership.automation.itf.environments.parse.parsers.EnvironmentParser;

import com.google.common.collect.Maps;

public class ParserFactory {

    private static final Map<Class<? extends EciConfigurable>, Parser> FACTORY = Maps.newHashMap();

    static {
        FACTORY.put(Environment.class, new EnvironmentParser());
        FACTORY.put(System.class, new ECSystemParser());
        FACTORY.put(TransportConfiguration.class, new ECConnectionParser());
    }

    /**
     * produces the required parser depending on the class
     *
     * @param clazz type of object to be parsed
     * @return parser of type
     */
    public static <T extends EciConfigurable & ECEntity<? extends EciConfigurable>, V extends Object> Parser<T, V> getParser(Class<? extends EciConfigurable> clazz) {
        return FACTORY.get(clazz);
    }
}
