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

package org.qubership.automation.itf.transport.soap.http.outbound;

import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.transport.http.HTTPConstants;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;

public class SOAPOverHTTPOutboundTransportTest {

    String body = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www"
            + ".w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:urn=\"urn:examples:helloservice\">\n" + "   <soapenv:Header/>\n" + "   <soapenv:Body>\n" + "    "
            + "  <urn:sayHello soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "         "
            + "<firstName xsi:type=\"xsd:string\">Salavat</firstName>\n" + "      </urn:sayHello>\n" + "   </soapenv"
            + ":Body>\n" + "</soapenv:Envelope>";

    @Test
    public void testSendMessageToTAProd() throws Exception {
        SOAPOverHTTPOutboundTransport transport = new SOAPOverHTTPOutboundTransport();
        Message message = new Message(body);
        message.fillConnectionProperties(new HashMap<>());
        Map<String, Object> properties = message.getConnectionProperties();
        properties.put(HTTPConstants.BASE_URL, "http://WSMSA-026:8088/");
        properties.put(HTTPConstants.ENDPOINT, "/mockHello_Binding");
        properties.put(HTTPConstants.METHOD, "POST");
        HashMap<Object, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml");
        headers.put("CamelHttpMethod", "POST");
        properties.put(HTTPConstants.HEADERS, headers);
        properties.put(PropertyConstants.Soap.WSDL_PATH, "C:\\wsdl\\test.wsdl");
        BigInteger pid = new BigInteger("43245365658769860");
        Message response = transport.sendReceiveSync(message, pid);
        System.out.println(response.getText());
        assertThat(response.getText(), StringContains.containsString("greeting"));
    }
}
