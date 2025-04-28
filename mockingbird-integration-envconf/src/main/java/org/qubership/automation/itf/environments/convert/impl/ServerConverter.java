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

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.environments.object.impl.ECServer;

public class ServerConverter extends AbstractConverter<Server, ECServer> {

    @Override
    public Server convert(ECServer ecServer, Storable parent, UUID eciSessionId, Object... objects) {
        Server server = findOrCreateStorableByEnvConfId(ecServer, parent, eciSessionId);
        setMainParams(server, ecServer);
        server.setUrl(ecServer.getUrl());
        server.setName(ecServer.getName());
        return server;
    }

    @Override
    public Server findOrCreateStorableByEnvConfId(ECServer ecServer, Storable parent, UUID eciSessionId,
                                                  Object... objects) {
        Server server = getConfigurableEntity(ecServer);
        if (server == null) {
            server = CoreObjectManager.managerFor(Server.class).create(parent);
            setECIParams(server, ecServer);
        } else if (server.getEcId() == null) {
            setECIParams(server, ecServer);
        }
        return server;
    }
}
