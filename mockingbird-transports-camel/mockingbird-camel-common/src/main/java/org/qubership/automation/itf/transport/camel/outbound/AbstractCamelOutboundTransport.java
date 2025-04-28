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

package org.qubership.automation.itf.transport.camel.outbound;

import java.util.UUID;

import org.qubership.automation.itf.core.util.transport.base.AbstractTransportImpl;
import org.qubership.automation.itf.core.util.transport.base.OutboundTransport;
import org.qubership.automation.itf.transport.camel.CamelContextProvider;

public abstract class AbstractCamelOutboundTransport extends AbstractTransportImpl implements CamelContextProvider,
        OutboundTransport {

    private String id = "out" + UUID.randomUUID().toString();

    {
        start();
    }

    @Override
    public String getId() {
        return this.id;
    }
}
