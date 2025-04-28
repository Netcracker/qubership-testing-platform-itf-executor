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

package org.qubership.automation.itf.transport.snmp.outbound;

import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.HOST;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.HOST_DESCRIPTION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PORT;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PORT_DESCRIPTION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PROPERTIES;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.PROPERTIES_DESCRIPTION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.SNMP_COMPONENT;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.SNMP_VERSION;
import static org.qubership.automation.itf.transport.snmp.SNMPTransportConstants.SNMP_VERSION_DESCRIPTION;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.component.snmp.SnmpComponent;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Async;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.google.common.collect.Maps;

@UserName("Outbound SNMP Asynchronous")
@Async
public class SNMPOutboundTransport extends AbstractCamelOutboundTransport {
    @Parameter(shortName = HOST, longName = HOST_DESCRIPTION,
            description = HOST_DESCRIPTION)
    private String host;

    @Parameter(shortName = PORT, longName = PORT_DESCRIPTION,
            description = PORT_DESCRIPTION)
    private String port;

    @Parameter(shortName = SNMP_VERSION, longName = SNMP_VERSION,
            description = SNMP_VERSION_DESCRIPTION)
    @Options({"0 (SNMPv1)", "1 (SNMPv2c)", "3 (SNMPv3)"})
    private String snmpVersion;

    @Parameter(shortName = PROPERTIES, longName = "Extra Properties",
            description = PROPERTIES_DESCRIPTION, isDynamic = true, optional = true, userSettings = true)
    private Map<String, Object> properties = Maps.newHashMap();

    @Override
    public String getShortName() {
        return "SNMP Outbound";
    }

    @Override
    public String send(Message message, String sessionId, UUID projectUuid) throws Exception {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();

        if (CAMEL_CONTEXT.hasComponent(SNMP_COMPONENT) == null) {
            CAMEL_CONTEXT.addComponent(SNMP_COMPONENT, new SnmpComponent());
        }
        DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
        try {
            String host = connectionProperties.obtain(HOST);
            String port = connectionProperties.obtain(PORT);
            int snmpVersion = determineVersion(connectionProperties.obtain(SNMP_VERSION));
            HashMap<String, String> extraProperties = connectionProperties.obtain(PROPERTIES);
            transport.listen();
            Snmp snmp = new Snmp(transport);
            CommunityTarget target = configureCommunityTarget(extraProperties, host, port, snmpVersion);
            PDU pdu = configurePdu(extraProperties, host, port, message, target);
            snmp.send(pdu, target);
            snmp.close();
        } finally {
            transport.close();
        }

        return null;
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_ASYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-snmp";
    }

    private int determineVersion(String stringVersion) {
        switch (stringVersion) {
            case "0 (SNMPv1)":
                return 0;
            case "1 (SNMPv2c)":
                return 1;
            case "3 (SNMPv3)":
                return 3;
            default:
        }
        return 0;
    }

    private CommunityTarget configureCommunityTarget(HashMap<String, String> extraProperties, String host,
                                                     String port, int snmpVersion) {
        Address targetAddress = new UdpAddress(host + "/" + port);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(extraProperties.getOrDefault("community", "public")));
        target.setVersion(snmpVersion);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(5000);
        return target;
    }

    private PDU configurePdu(HashMap<String, String> extraProperties, String host, String port, Message message,
                             CommunityTarget target) {
        PDU pdu = new PDU();
        String pduType = extraProperties.get("type");
        switch (pduType) {
            case "TRAP":
            case "NOTIFICATION":
                pdu.setType((target.getVersion() == 0) ? PDU.V1TRAP : PDU.TRAP);
                break;
            case "GET":
                pdu.setType(PDU.GET);
                break;
            case "GETNEXT":
                pdu.setType(PDU.GETNEXT);
                break;
            case "RESPONSE":
                pdu.setType(PDU.RESPONSE);
                break;
            case "REPORT":
                pdu.setType(PDU.REPORT);
                break;
            case "INFORM":
                pdu.setType(PDU.INFORM);
                break;
            default:
        }
        /*
            Generally speaking, such split and further logic are too rood,
             and possibly don't cover all string pattern variants.
             So, later it should be revised.
         */
        String[] messageOids = message.getText().split("\\s+");
        for (int i = 0; i < messageOids.length; i++) {
            if (messageOids[i] == null || messageOids[i].isEmpty()) {
                continue;
            }
            if (messageOids.length >= i + 1 && messageOids[i + 1].startsWith("\"")) {
                // In case possible spaces inside quoted string, it could be split into a number of array elements,
                // which should be combined back here...
                // Condition to stop combining is: trailing " character.
                String variableString = "";
                int k = i + 1;
                for (; k < messageOids.length; k++) {
                    variableString = variableString + ((k == i + 1) ? "" : " ") + messageOids[k];
                    if (messageOids[k].endsWith("\"")) {
                        break;
                    }
                }
                pdu.add(new VariableBinding(new OID(messageOids[i]),
                        new OctetString(variableString.replaceAll("\"", ""))));
                i = k;
            } else {
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(messageOids[i])));
            }
        }
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(host)));
        pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(111111L))); // Why this value? Requires
        // any non-null?
        return pdu;
    }
}
