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

package org.qubership.automation.itf.transport.diameter.outbound;

import org.junit.Ignore;
import org.junit.Test;

public class DiameterOutboundTest {

    @Ignore
    @Test
    public void testSendRequestToDDRS() {
//        DiameterOutbound outbound = new DiameterOutbound();
        /*
        Message cer = new Message(IOUtils.toString(getClass().getResourceAsStream("/cer.xml")));
        Map<String, Object> connectionProperties = cer.getConnectionProperties();
        connectionProperties.put("ContextId", "123");
        connectionProperties.put(HOST, "127.0.0.1");
        connectionProperties.put(PORT, 9876);
        connectionProperties.put(DWA_TEMPLATE_LINK, IOUtils.toString(getClass().getResourceAsStream("/dwa.xml")));
        connectionProperties.put(CONFIG_PATH, "./src/test/resources/config");
        connectionProperties.put(INTERCEPTOR_NAME, "CEA");
        Message response = outbound.sendReceiveSync(cer);
        assertThat(response.getText(), StringContains.containsString("CEA"));
        */

//        Message ccr = new Message(IOUtils.toString(getClass().getResourceAsStream("/ccr.ws.txt")));
////        Message ccr = new Message(IOUtils.toString(getClass().getResourceAsStream("/ccr.xml")));
//        Map<String, Object> properties = ccr.getConnectionProperties();
//        properties.put(PropertyConstants.DiameterTransportConstants.DWA, IOUtils.toString(getClass()
//        .getResourceAsStream("/dwa.xml")));
//        properties.put(PropertyConstants.DiameterTransportConstants.CONFIG_PATH, "./src/test/resources/config");
//        properties = ccr.getConnectionProperties();
//        properties.put("ContextId", "123");
//        properties.put(PropertyConstants.DiameterTransportConstants.HOST, "1127.0.0.1");
//        properties.put(PropertyConstants.DiameterTransportConstants.PORT, 9876);
//        properties.put(PropertyConstants.DiameterTransportConstants.INTERCEPTOR_NAME, "CCA");
//        properties.put(PropertyConstants.DiameterTransportConstants.MESSAGE_FORMAT_NAME, "WireShark");
//        BigInteger pid = new BigInteger("123456876758476476");
//        Message ccrResponse = outbound.sendReceiveSync(ccr, pid);
//        assertThat(ccrResponse.getText(), StringContains.containsString("CCA"));
//        Thread.sleep(TimeUnit.MINUTES.toMillis(5));
    }
}
