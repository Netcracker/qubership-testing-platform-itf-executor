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

package org.qubership.automation.itf.environments.object.impl;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.environments.ECIConstants;
import org.qubership.automation.itf.environments.util.validation.ECIErrorsCache;
import org.qubership.automation.itf.environments.util.validation.ValidationLevel;

public class ECConnection extends ConvertableECEntity<TransportConfiguration> {

    private Map<String, String> parameters;
    private String connectionType;

    public ECConnection() {
        setGenericType(TransportConfiguration.class);
        setParentClass(System.class);
    }

    public ECConnection(String ecId) {
        setEcId(ecId);
        setGenericType(TransportConfiguration.class);
        setParentClass(System.class);
    }

    public Pair<String, ECIConstants> getItfTypeOfConnection(UUID eciSessionId) {
        try {
            Map<String, String> transportTypes = TransportRegistryManager.getInstance().getTransportTypes();
            String direction = parameters.get(ECIConstants.DIRECTION_EC_PARAM_NAME.value());
            if (StringUtils.isNotEmpty(direction)) {
                String ecTransportName =
                        new StringBuilder(direction).append(" ").append(getName()).toString().toLowerCase();
                for (Map.Entry<String, String> entry : transportTypes.entrySet()) {
                    if (ecTransportName.contains(entry.getValue().toLowerCase())) {
                        return new ImmutablePair(entry.getKey(), ECIConstants.fromStringValue(direction));
                    }
                }
            }
        } catch (Exception e) {
            ECIErrorsCache.getInstance().put(eciSessionId, getName(), getClass().getSimpleName(), String.format(
                            "Error while trying to get the transport type for connection: %s", getName()),
                    ValidationLevel.ERROR);
        }
        ECIErrorsCache.getInstance().put(eciSessionId, getName(), getClass().getSimpleName(), String.format(
                "Unrecognized type of connection: %s. Please, check the name of connection and its direction.",
                getName()), ValidationLevel.ERROR);
        return null;
    }

    public boolean forEnvironment() {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (ECIConstants.CONNECTION_FOR_ENVIRONMENT_PARAMETER.value().equals(entry.getKey().toLowerCase())
                    && "true".equals(entry.getValue().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String getEcLabel() {
        return parameters.get(ECIConstants.TRANSPORT_EC_LABEL.value());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
}
