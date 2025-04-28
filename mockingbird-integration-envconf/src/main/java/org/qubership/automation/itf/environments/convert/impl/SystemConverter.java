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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvConfigurationManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.ECIConstants;
import org.qubership.automation.itf.environments.convert.ConverterFactory;
import org.qubership.automation.itf.environments.object.impl.ECConnection;
import org.qubership.automation.itf.environments.object.impl.ECSystem;
import org.qubership.automation.itf.environments.util.validation.ECIErrorsCache;
import org.qubership.automation.itf.environments.util.validation.ValidationLevel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SystemConverter extends AbstractConverter<System, ECSystem> {

    @Override
    public System convert(ECSystem ecSystem, Storable parent, UUID eciSessionId, Object... objects) {
        System system = findSystemByEcLabel(ecSystem, parent.getProjectId());
        if (system == null) {
            system = findOrCreateStorableByEnvConfId(ecSystem, parent, eciSessionId);
            setMainParams(system, ecSystem);
            system.setEcLabel(ecSystem.getName());
        }
        convertSystem(ecSystem, system, eciSessionId, parent.getProjectId());
        return system;
    }

    @Override
    public System findOrCreateStorableByEnvConfId(ECSystem ecSystem, Storable parent, UUID eciSessionId,
                                                  Object... objects) {
        System system = getConfigurableEntity(ecSystem);
        if (system == null) {
            system = CoreObjectManager.managerFor(System.class).create(parent);
            setECIParams(system, ecSystem);
        }
        return system;
    }

    private void convertSystem(ECSystem ecSystem, System system, UUID eciSessionId, BigInteger projectId) {
        Server server = convertServer(ecSystem, system, eciSessionId, projectId);
        updateExistingAndCreateNewEnvironmentTransportConfiguration(ecSystem, system, server, eciSessionId);
        updateExistingAndCreateNewTransports(ecSystem, system, server, eciSessionId);
    }

    private System findSystemByEcLabel(ECSystem ecSystem, BigInteger projectId) {
        return (System) CoreObjectManager.getInstance().getSpecialManager(System.class,
                EnvConfigurationManager.class).findByEcLabel(ecSystem.getName(), projectId);
    }

    private void updateExistingAndCreateNewTransports(ECSystem ecSystem,
                                                      System system,
                                                      Server server,
                                                      UUID eciSessionId) {
        Map<ECIConstants, Map<String, Map<ECIConstants, List<ECConnection>>>> connectionsByDirectionTypeTarget =
                getConnectionsByDirectionTypeTarget(ecSystem, eciSessionId);
        Map<String, Map<ECIConstants, List<ECConnection>>> connectionsByTypeTarget =
                connectionsByDirectionTypeTarget.get(ECIConstants.INBOUND_DIRECTION);
        if (connectionsByTypeTarget != null) {
            convertInboundConnections(connectionsByTypeTarget, system, server, eciSessionId);
        }
        connectionsByTypeTarget = connectionsByDirectionTypeTarget.get(ECIConstants.OUTBOUND_DIRECTION);
        if (connectionsByTypeTarget != null) {
            convertOutboundConnections(connectionsByTypeTarget, system, server, eciSessionId);
        }
    }

    private void updateExistingAndCreateNewEnvironmentTransportConfiguration(ECSystem ecSystem,
                                                                             System system,
                                                                             Server server,
                                                                             UUID eciSessionId) {
        Map<ECIConstants, Map<String, Map<ECIConstants, List<ECConnection>>>> connectionsByDirectionTypeTarget =
                getConnectionsByDirectionTypeTarget(ecSystem, eciSessionId);
        Map<String, Map<ECIConstants, List<ECConnection>>> connectionsByTypeTarget =
                connectionsByDirectionTypeTarget.get(ECIConstants.OUTBOUND_DIRECTION);
        if (connectionsByTypeTarget != null) {
            convertOutboundTransportConfiguration(connectionsByTypeTarget, system, server, eciSessionId);
        }
        connectionsByTypeTarget = connectionsByDirectionTypeTarget.get(ECIConstants.INBOUND_DIRECTION);
        if (connectionsByTypeTarget != null) {
            convertInboundTransportConfiguration(connectionsByTypeTarget, system, server, eciSessionId);
        }
    }

    private void convertInboundConnections(
            Map<String, Map<ECIConstants, List<ECConnection>>> connectionsByTypeTarget,
            System system,
            Server server,
            UUID eciSessionId) {
        for (Map.Entry<String, Map<ECIConstants, List<ECConnection>>> byType : connectionsByTypeTarget.entrySet()) {
            Map<ECIConstants, List<ECConnection>> byTarget = byType.getValue();
            for (ECConnection ecConnection : byTarget.get(ECIConstants.FOR_TRANSPORT)) {
                ecConnection.setConnectionType(byType.getKey());
                TransportConfiguration transport = ConverterFactory.getConverter(TransportConfiguration.class)
                        .convert(ecConnection, system, eciSessionId);
                system.getTransports().add(transport);
            }
        }
    }

    private void convertOutboundConnections(
            Map<String, Map<ECIConstants, List<ECConnection>>> connectionsByTypeTarget,
            System system,
            Server server,
            UUID eciSessionId) {
        for (Map.Entry<String, Map<ECIConstants, List<ECConnection>>> byType : connectionsByTypeTarget.entrySet()) {
            Map<ECIConstants, List<ECConnection>> byTarget = byType.getValue();
            ECConnection connectionForEnvironment = getConnectionForEnvironment(system, server, byType.getKey(),
                    byTarget, eciSessionId);
            if (connectionForEnvironment != null) {
                ConverterFactory.getConverter(OutboundTransportConfiguration.class)
                        .convert(connectionForEnvironment, server, eciSessionId, system);
            }
            //TODO: May be, otherwise we must create transport under the system based on the FOR_ENV value? Need to
            // discuss
            if (byTarget.containsKey(ECIConstants.FOR_TRANSPORT)) {
                for (ECConnection ecConnection : byTarget.get(ECIConstants.FOR_TRANSPORT)) {
                    ecConnection.setConnectionType(byType.getKey());
                    TransportConfiguration transport = ConverterFactory.getConverter(TransportConfiguration.class)
                            .convert(ecConnection, system, eciSessionId);
                    system.getTransports().add(transport);
                }
            }
        }
    }

    private void convertInboundTransportConfiguration(
            Map<String, Map<ECIConstants, List<ECConnection>>> connectionsByTypeTarget,
            System system,
            Server server,
            UUID eciSessionId) {
        for (Map.Entry<String, Map<ECIConstants, List<ECConnection>>> byType : connectionsByTypeTarget.entrySet()) {
            Map<ECIConstants, List<ECConnection>> byTarget = byType.getValue();
            List<ECConnection> ecConnections = byTarget.get(ECIConstants.FOR_TRANSPORT);
            if (ecConnections != null) {
                for (ECConnection ecConnection : ecConnections) {
                    String ecLabel = ecConnection.getEcLabel();
                    if (StringUtils.isNotEmpty(ecLabel)) {
                        TransportConfiguration transportByEcLabel = getTransportByEcLabel(system.getTransports(),
                                ecLabel);
                        if (transportByEcLabel != null) {
                            ConverterFactory.getConverter(InboundTransportConfiguration.class)
                                    .convert(ecConnection, server, eciSessionId, transportByEcLabel);
                        }
                    }
                }
            }
        }
    }

    private void convertOutboundTransportConfiguration(
            Map<String, Map<ECIConstants, List<ECConnection>>> connectionsByTypeTarget,
            System system,
            Server server,
            UUID eciSessionId) {
        for (Map.Entry<String, Map<ECIConstants, List<ECConnection>>> byType : connectionsByTypeTarget.entrySet()) {
            Map<ECIConstants, List<ECConnection>> byTarget = byType.getValue();
            ECConnection connectionForEnvironment = getConnectionForEnvironment(system, server, byType.getKey(),
                    byTarget, eciSessionId);
            if (connectionForEnvironment != null) {
                ConverterFactory.getConverter(OutboundTransportConfiguration.class)
                        .convert(connectionForEnvironment, server, eciSessionId, system);
            }
        }
    }

    private ECConnection getConnectionForEnvironment(System system,
                                                     Server server,
                                                     String connectionType,
                                                     Map<ECIConstants, List<ECConnection>> byTarget,
                                                     UUID eciSessionId) {
        ECConnection connectionForEnvironment = null;
        if (byTarget.get(ECIConstants.FOR_ENV) != null) {
            int connectionForEnvCount = byTarget.get(ECIConstants.FOR_ENV).size();
            if (connectionForEnvCount > 1) {
                ECIErrorsCache.getInstance().put(eciSessionId,
                        "",
                        connectionType,
                        String.format("There are more then 1 special connection for %s transports", connectionType),
                        ValidationLevel.INFO);
            } else if (connectionForEnvCount == 1 && !server.getOutbounds().isEmpty()
                    && server.getOutbound(system, connectionType).getEcId() != null
                    && !server.getOutbound(system, connectionType).getEcId()
                    .equals(byTarget.get(ECIConstants.FOR_ENV).get(0).getEcId())) {
                ECIErrorsCache.getInstance().put(eciSessionId,
                        "",
                        connectionType,
                        String.format("Environment already has the %s transport configuration for %s/%s",
                                connectionType, system.getName(), server.getName()), ValidationLevel.INFO);
            } else {
                connectionForEnvironment = byTarget.get(ECIConstants.FOR_ENV).get(0);
                connectionForEnvironment.setConnectionType(connectionType);
            }
        }
        return connectionForEnvironment;
    }

    private TransportConfiguration getTransportByEcLabel(Set<TransportConfiguration> transports, String ecLabel) {
        for (TransportConfiguration transport : transports) {
            if (ecLabel.equals(transport.get(ECIConstants.TRANSPORT_EC_LABEL.value()))) {
                return transport;
            }
        }
        return null;
    }

    private void unbindTransportsWhichAreNotExistInEC(ECSystem ecSystem, System system, UUID eciSessionId) {
        for (TransportConfiguration transport : system.getTransports()) {
            boolean transportIsFound = false;
            for (ECConnection ecConnection : ecSystem.getConnections()) {
                if (transport.getEcId() != null && transport.getEcId().equals(ecConnection.getEcId())) {
                    transportIsFound = true;
                    break;
                }
            }
            if (!transportIsFound && transport.getEcId() != null) {
                transport.unbindEntityWithHierarchy();
                ECIErrorsCache.getInstance().put(eciSessionId, transport.getName(), transport.getTypeName(),
                        String.format("Transport [id=%s, name=%s] has become unbind from EC during updating",
                                transport.getID(), transport.getName()), ValidationLevel.INFO);
            }
        }
    }

    private Server convertServer(ECSystem ecSystem, System system, UUID eciSessionId, BigInteger projectId) {
        ecSystem.getServer().setEcId(ecSystem.getEcId());
        ecSystem.getServer().setEcProjectId(system.getEcProjectId());
        return ConverterFactory.getConverter(Server.class).convert(ecSystem.getServer(),
                CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId).getServers(),
                eciSessionId);
    }

    private Map<ECIConstants, Map<String, Map<ECIConstants, List<ECConnection>>>> getConnectionsByDirectionTypeTarget(
            ECSystem ecSystem, UUID eciSessionId) {
        Map<ECIConstants, Map<String, Map<ECIConstants, List<ECConnection>>>> result =
                Maps.newHashMapWithExpectedSize(2);
        for (ECConnection connection : ecSystem.getConnections()) {
            Pair<String, ECIConstants> itfTypeOfConnection = connection.getItfTypeOfConnection(eciSessionId);
            if (itfTypeOfConnection != null) {
                Map<String, Map<ECIConstants, List<ECConnection>>> byDirection =
                        result.get(itfTypeOfConnection.getRight());
                if (byDirection == null) {
                    byDirection = Maps.newHashMap();
                    Map<ECIConstants, List<ECConnection>> target2connection = Maps.newHashMap();
                    if (connection.forEnvironment()) {
                        target2connection.put(ECIConstants.FOR_ENV, Lists.newArrayList(connection));
                    } else {
                        target2connection.put(ECIConstants.FOR_TRANSPORT, Lists.newArrayList(connection));
                    }
                    byDirection.put(itfTypeOfConnection.getLeft(), target2connection);
                    result.put(itfTypeOfConnection.getRight(), byDirection);
                } else {
                    Map<ECIConstants, List<ECConnection>> byType = byDirection.get(itfTypeOfConnection.getLeft());
                    if (byType == null) {
                        Map<ECIConstants, List<ECConnection>> target2connection = Maps.newHashMap();
                        if (connection.forEnvironment()) {
                            target2connection.put(ECIConstants.FOR_ENV, Lists.newArrayList(connection));
                        } else {
                            target2connection.put(ECIConstants.FOR_TRANSPORT, Lists.newArrayList(connection));
                        }
                        byDirection.put(itfTypeOfConnection.getLeft(), target2connection);
                    } else {
                        ECIConstants target = connection.forEnvironment() ? ECIConstants.FOR_ENV :
                                ECIConstants.FOR_TRANSPORT;
                        List<ECConnection> ecConnections = byType.get(target);
                        if (ecConnections == null) {
                            ecConnections = Lists.newArrayList(connection);
                            byType.put(target, ecConnections);
                        } else {
                            ecConnections.add(connection);
                        }
                    }
                }
            }
        }
        return result;
    }
}
