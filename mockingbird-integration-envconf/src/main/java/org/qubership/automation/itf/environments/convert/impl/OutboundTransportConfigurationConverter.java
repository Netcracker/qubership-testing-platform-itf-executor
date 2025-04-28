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

import java.util.Collection;
import java.util.UUID;

import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvConfigurationManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.object.impl.ECConnection;

public class OutboundTransportConfigurationConverter extends AbstractConverter<OutboundTransportConfiguration,
        ECConnection> {

    @Override
    public OutboundTransportConfiguration convert(ECConnection ecConnection, Storable parent, UUID eciSessionId,
                                                  Object... objects) {
        OutboundTransportConfiguration otc = findOrCreateStorableByEnvConfId(ecConnection, parent, eciSessionId,
                objects);
        if (otc != null) {
            setMainParams(otc, ecConnection);
            otc.putAll(ecConnection.getParameters());
        }
        return otc;
    }

    @Override
    public OutboundTransportConfiguration findOrCreateStorableByEnvConfId(ECConnection ecConnection, Storable parent,
                                                                          UUID eciSessionId, Object... objects) {
        OutboundTransportConfiguration otc = getConfigurableEntity(ecConnection);
        if (otc == null) {
            otc = getOTCByType(parent, ecConnection.getConnectionType());
            if (otc == null) {
                otc = new OutboundTransportConfiguration(ecConnection.getConnectionType(), (Server) parent,
                        (System) objects[0]);
                ((Server) parent).getOutbounds().add(otc);
                setECIParams(otc, ecConnection);
            } else if (otc.getEcId() == null) {
                setECIParams(otc, ecConnection);
            }
        }
        return otc;
    }

    @Override
    public OutboundTransportConfiguration getConfigurableEntity(ECConnection ecConnection) {
        return (OutboundTransportConfiguration) CoreObjectManager.getInstance().getSpecialManager(OutboundTransportConfiguration.class, EnvConfigurationManager.class).getByEcId(ecConnection.getEcId());
    }

    private OutboundTransportConfiguration getOTCByType(Storable parent, String typeName) {
        Collection<? extends OutboundTransportConfiguration> otcByParentId =
                CoreObjectManager.getInstance().getManager(OutboundTransportConfiguration.class).getAllByParentId(parent.getID());
        for (OutboundTransportConfiguration otc : otcByParentId) {
            if (typeName.equals(otc.getTypeName())) {
                return otc;
            }
        }
        return null;
    }
}
