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

import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvConfigurationManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.object.impl.ECConnection;

public class InboundTransportConfigurationConverter extends AbstractConverter<InboundTransportConfiguration,
        ECConnection> {

    @Override
    public InboundTransportConfiguration convert(ECConnection ecConnection, Storable parent, UUID eciSessionId,
                                                 Object... objects) {
        InboundTransportConfiguration itc = findOrCreateStorableByEnvConfId(ecConnection, parent, eciSessionId,
                objects);
        if (itc != null) {
            itc.putAll(ecConnection.getParameters());
            setMainParams(itc, ecConnection);
        }
        return itc;
    }

    @Override
    public InboundTransportConfiguration findOrCreateStorableByEnvConfId(ECConnection ecConnection, Storable parent,
                                                                         UUID eciSessionId, Object... objects) {
        InboundTransportConfiguration itc = getConfigurableEntity(ecConnection);
        if (itc == null) {
            itc = new InboundTransportConfiguration((TransportConfiguration) objects[0], (Server) parent);
            ((Server) parent).getInbounds().add(itc);
            setECIParams(itc, ecConnection);
        }
        return itc;
    }

    @Override
    public InboundTransportConfiguration getConfigurableEntity(ECConnection ecConnection) {
        return (InboundTransportConfiguration) CoreObjectManager.getInstance()
                .getSpecialManager(InboundTransportConfiguration.class, EnvConfigurationManager.class)
                .getByEcId(ecConnection.getEcId());
    }
}
