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

package org.qubership.automation.itf.transport.snmp.inbound;

import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.HOST;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.HOST_DESCRIPTION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PORT;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PORT_DESCRIPTION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PROPERTIES;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PROPERTIES_DESCRIPTION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.SNMP_VERSION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.SNMP_VERSION_DESCRIPTION;

import java.util.Map;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;

@UserName("Inbound SNMP Asynchronous")
public class SNMPInboundTransport extends AbstractInboundTransportImpl {

    @Parameter(shortName = HOST, longName = HOST_DESCRIPTION,
            description = HOST_DESCRIPTION)
    private String host;

    @Parameter(shortName = PORT, longName = PORT_DESCRIPTION,
            description = PORT_DESCRIPTION)
    private String port;

    @Parameter(shortName = SNMP_VERSION, longName = SNMP_VERSION_DESCRIPTION,
            description = SNMP_VERSION_DESCRIPTION)
    @Options({"0", "1", "2"})
    private String snmpVersion;

    @Parameter(shortName = PROPERTIES, longName = "Properties",
            description = PROPERTIES_DESCRIPTION, isDynamic = true, optional = true, userSettings = true)
    private Map<String, String> properties;

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-snmp";
    }

    @Override
    public String getShortName() {
        return "SNMP Inbound";
    }
}
