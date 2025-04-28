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

package org.qubership.automation.itf.environments.convert.impl;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.ECIConstants;
import org.qubership.automation.itf.environments.convert.ConverterFactory;
import org.qubership.automation.itf.environments.object.impl.ECEnvironment;
import org.qubership.automation.itf.environments.object.impl.ECSystem;
import org.qubership.automation.itf.environments.util.validation.ECIErrorsCache;
import org.qubership.automation.itf.environments.util.validation.ValidationLevel;

import com.google.common.collect.Maps;

public class EnvironmentConverter extends AbstractConverter<Environment, ECEnvironment> {

    @Override
    public Environment convert(ECEnvironment ecEnvironment, Storable parent, UUID eciSessionId, Object... objects) {
        Environment env = findOrCreateStorableByEnvConfId(ecEnvironment, parent, eciSessionId);
        setMainParams(env, ecEnvironment);
        unbindSystemsWhichAreNotExistInEC(ecEnvironment, env.getInbound(), eciSessionId);
        unbindSystemsWhichAreNotExistInEC(ecEnvironment, env.getOutbound(), eciSessionId);
        Map<System, Server> convertedSystemServerPairs = updateExistingAndCreateNewSystemsWithServers(ecEnvironment,
                env, eciSessionId);
        createNewSystemServerPairs(env.getInbound(), convertedSystemServerPairs, ECIConstants.INBOUND_DIRECTION);
        createNewSystemServerPairs(env.getOutbound(), convertedSystemServerPairs, ECIConstants.OUTBOUND_DIRECTION);
        return env;
    }

    @Override
    public Environment findOrCreateStorableByEnvConfId(ECEnvironment ecEnvironment, Storable parent,
                                                       UUID eciSessionId, Object... objects) {
        Environment environment = getConfigurableEntity(ecEnvironment);
        if (environment == null) {
            environment = CoreObjectManager.managerFor(Environment.class).create(parent);
            setECIParams(environment, ecEnvironment);
            environment.setProjectId((BigInteger) ((EnvFolder) parent).getProject().getID());
        }
        return environment;
    }

    private Map<System, Server> updateExistingAndCreateNewSystemsWithServers(ECEnvironment ecEnvironment,
                                                                             Environment environment,
                                                                             UUID eciSessionId) {
        Map<System, Server> result = Maps.newHashMapWithExpectedSize(ecEnvironment.getSystems().size());
        StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class)
                .getById(environment.getProjectId());
        Folder<System> systemFolder = project.getSystems();
        for (ECSystem ecSystem : ecEnvironment.getSystems()) {
            System system = ConverterFactory.getConverter(System.class).convert(ecSystem, systemFolder, eciSessionId);
            Server server = ConverterFactory.getConverter(Server.class).getConfigurableEntity(ecSystem.getServer());
            result.put(system, server);
        }
        return result;
    }

    private void unbindSystemsWhichAreNotExistInEC(ECEnvironment ecEnvironment, Map<System, Server> systemServerPairs,
                                                   UUID eciSessionId) {
        for (Map.Entry<System, Server> systemServerPair : systemServerPairs.entrySet()) {
            System system = systemServerPair.getKey();
            if (StringUtils.isNotEmpty(system.getEcId())) {
                boolean systemIsFound = false;
                for (ECSystem ecSystem : ecEnvironment.getSystems()) {
                    if (ecSystem.getEcId().equals(system.getEcId()) || ecSystem.getName().equals(system.getEcLabel())) {
                        systemIsFound = true;
                        break;
                    }
                }
                if (!systemIsFound) {
                    system.unbindEntityWithHierarchy();
                    ECIErrorsCache.getInstance().put(eciSessionId, system.getName(),
                            system.getClass().getSimpleName(), String.format("System [id=%s, name=%s] has become "
                                    + "unbind from EC during updating", system.getID(), system.getName()),
                            ValidationLevel.INFO);
                }
            }
        }
    }

    private void createNewSystemServerPairs(Map<System, Server> pairs, Map<System, Server> convertedSystemServerPairs,
                                            ECIConstants direction) {
        for (Map.Entry<System, Server> convertedEntry : convertedSystemServerPairs.entrySet()) {
            System convertedSystem = convertedEntry.getKey();
            Server convertedServer = convertedEntry.getValue();
            boolean pairWasFound = false;
            for (Map.Entry<System, Server> existingSystemServerPairs : pairs.entrySet()) {
                if (convertedSystem.getID().toString().equals(existingSystemServerPairs.getKey().getID().toString())
                        && convertedServer.getID().toString().equals(
                        existingSystemServerPairs.getValue().getID().toString())) {
                    pairWasFound = true;
                    break;
                }
            }
            if (!pairWasFound) {
                if ((ECIConstants.INBOUND_DIRECTION.equals(direction) && itIsInbound(convertedSystem))
                        || (ECIConstants.OUTBOUND_DIRECTION.equals(direction) && itIsOutbound(convertedSystem))) {
                    pairs.put(convertedSystem, convertedServer);
                }
            }
        }
    }

    private boolean itIsOutbound(System system) {
        for (TransportConfiguration transport : system.getTransports()) {
            if (transport.getMep().isOutbound()) {
                return true;
            }
        }
        return false;
    }

    private boolean itIsInbound(System system) {
        for (TransportConfiguration transport : system.getTransports()) {
            if (transport.getMep().isInbound()) {
                return true;
            }
        }
        return false;
    }
}
