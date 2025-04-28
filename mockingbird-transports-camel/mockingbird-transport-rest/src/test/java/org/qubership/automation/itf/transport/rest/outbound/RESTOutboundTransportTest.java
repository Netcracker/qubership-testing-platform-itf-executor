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

package org.qubership.automation.itf.transport.rest.outbound;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.transport.http.HTTPConstants;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class RESTOutboundTransportTest {

    private static final String RESPONSE_BODY = "success";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8083);

    @Before
    public void setUp() throws Exception {
        stubFor(post(urlEqualTo("/test/post/"))
                .withRequestBody(matching("test_post"))
                .willReturn(aResponse().withBody(RESPONSE_BODY).withStatus(200)));
    }

    @Test
    public void testSendPostRequestToMockWithResponse() throws Exception {
        RESTOutboundTransport transport = new RESTOutboundTransport();
        Whitebox.setInternalState(transport, "method", "POST");
        Message message = new Message("test_post");
        message.getConnectionProperties().put(HTTPConstants.ENDPOINT, "/test/post/");
        message.getConnectionProperties().put(HTTPConstants.BASE_URL, "http://localhost:8083");
        BigInteger pid = new BigInteger("123445667889");
        Message response = transport.sendReceiveSync(message, pid);
        assertEquals(RESPONSE_BODY, response.getText());
    }

    @Test
    public void testUriResolverSlashes() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        RESTOutboundTransport transport = new RESTOutboundTransport();
        Message message = new Message();
        ConnectionProperties properties = (ConnectionProperties) message.getConnectionProperties();
        properties.put(HTTPConstants.BASE_URL, "http://localhost:8081");
        properties.put(HTTPConstants.ENDPOINT, "test/post/");
        assertUri(transport, message);
        properties.put(HTTPConstants.ENDPOINT, "/test/post/");
        assertUri(transport, message);
        properties.put(HTTPConstants.BASE_URL, "http://localhost:8081/");
        assertUri(transport, message);
    }

    private void assertUri(RESTOutboundTransport transport, Message message) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Method resolveEndpoint = RESTOutboundTransport.class.getDeclaredMethod("resolveEndpoint", Message.class);
        resolveEndpoint.setAccessible(true);
        Object endpoint = resolveEndpoint.invoke(transport, message);
        assertEquals("http://localhost:8081/test/post/", endpoint);
    }
}
