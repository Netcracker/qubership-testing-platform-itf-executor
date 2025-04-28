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

package org.qubership.automation.itf.core.message.parser;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractTransportImpl;
import org.qubership.automation.itf.core.util.transport.base.OutboundTransport;

public class TestRestOverHttpOutbound extends AbstractTransportImpl implements OutboundTransport {

    @Parameter(shortName = "headers",
            longName = "Request Headers",
            description = PropertyConstants.Http.HEADERS,
            forTemplate = true,
            forServer = false,
            isDynamic = true)
    private Map<String, Object> headers;

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public String send(Message message, String sessionId, UUID projectUuid) throws Exception {
        return null;
    }

    @Override
    public Message receive(String sessionId) throws Exception {
        return null;
    }

    @Override
    public Message sendReceiveSync(Message messageToSend, BigInteger projectId) throws Exception {
        return messageToSend;
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "";
    }

    @Override
    public String getShortName() {
        return "Test Rest Outbound";
    }

}
