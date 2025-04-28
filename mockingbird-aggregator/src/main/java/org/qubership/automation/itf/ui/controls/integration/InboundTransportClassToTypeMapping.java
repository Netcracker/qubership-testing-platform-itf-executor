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

package org.qubership.automation.itf.ui.controls.integration;

import java.util.HashMap;
import java.util.Map;

import org.qubership.automation.itf.core.model.communication.TransportType;

public class InboundTransportClassToTypeMapping {

    private static final Map<String, TransportType> transportClassToType = initializeMap();

    private static Map<String, TransportType> initializeMap() {
        Map<String, TransportType> map = new HashMap<>();
        map.put("org.qubership.automation.itf.transport.cli.inbound.CLIInboundTransport",
                TransportType.CLI_INBOUND);
        map.put("org.qubership.automation.itf.transport.file.inbound.FileInbound",
                TransportType.FILE_INBOUND);
        map.put("org.qubership.automation.itf.transport.http.inbound.HTTPInboundTransport",
                TransportType.HTTP_INBOUND);
        map.put("org.qubership.automation.itf.transport.http2.inbound.HTTP2InboundTransport",
                TransportType.HTTP2_INBOUND);
        map.put("org.qubership.automation.itf.transport.jms.inbound.JMSInboundTransport",
                TransportType.JMS_INBOUND);
        map.put("org.qubership.automation.itf.transport.kafka.inbound.KafkaInboundTransport",
                TransportType.KAFKA_INBOUND);
        map.put("org.qubership.automation.itf.transport.rest.inbound.RESTInboundTransport",
                TransportType.REST_INBOUND);
        map.put("org.qubership.automation.itf.transport.snmp.inbound.SNMPInboundTransport",
                TransportType.SNMP_INBOUND);
        map.put("org.qubership.automation.itf.transport.smpp.inbound.SmppInboundTransport",
                TransportType.SMPP_INBOUND);
        map.put("org.qubership.automation.itf.transport.soap.http.inbound.SOAPOverHTTPInboundTransport",
                TransportType.SOAP_OVER_HTTP_INBOUND);
        return map;
    }

    public static TransportType getTransportType(String transportClassName) {
        return transportClassToType.get(transportClassName);
    }

    /**
     * Get Transport Class Name by TransportType given.
     */
    public static String getTransportClassName(TransportType transportType) {
        for (Map.Entry<String, TransportType> entry : transportClassToType.entrySet()) {
            if (entry.getValue().equals(transportType)) {
                return entry.getKey();
            }
        }
        return "";
    }
}
