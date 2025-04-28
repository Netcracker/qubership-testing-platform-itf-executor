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

import java.util.UUID;

import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ObjectCreationByTypeManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.object.impl.ECConnection;

public class ConnectionConverter extends AbstractConverter<TransportConfiguration, ECConnection> {

    @Override
    public TransportConfiguration convert(ECConnection ecConnection, Storable parent, UUID eciSessionId,
                                          Object... objects) {
        TransportConfiguration transport = findOrCreateStorableByEnvConfId(ecConnection, parent, eciSessionId);
        if (transport != null) {
            setMainParams(transport, ecConnection);
            transport.putAll(ecConnection.getParameters());
        }
        return transport;
    }

    @Override
    public TransportConfiguration findOrCreateStorableByEnvConfId(ECConnection ecConnection, Storable parent,
                                                                  UUID eciSessionId, Object... objects) {
        TransportConfiguration transport = getConfigurableEntity(ecConnection);
        if (transport == null) {
            transport = (TransportConfiguration) CoreObjectManager.getInstance()
                    .getSpecialManager(TransportConfiguration.class, ObjectCreationByTypeManager.class)
                    .create(parent, ecConnection.getName(), ecConnection.getConnectionType(),
                            ecConnection.getParameters());
            setECIParams(transport, ecConnection);
        }
        return transport;
    }
}
