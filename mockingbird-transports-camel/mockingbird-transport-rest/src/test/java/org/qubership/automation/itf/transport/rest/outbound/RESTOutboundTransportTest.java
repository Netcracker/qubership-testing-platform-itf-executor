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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.transport.http.HTTPConstants;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sun.net.httpserver.HttpServer;

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
    public void testSameNameResponseHeadersSurviveRedirectWithDisabledRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8084), 0);
        server.createContext("/test/login/", exchange -> {
            exchange.getRequestBody().close();
            exchange.getResponseHeaders().add("Location", "http://localhost:8084/test/home/");
            exchange.getResponseHeaders().add("Set-Cookie", "a=1; Path=/");
            exchange.getResponseHeaders().add("Set-Cookie", "b=2; Path=/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        try {
            RESTOutboundTransport transport = new RESTOutboundTransport();
            Whitebox.setInternalState(transport, "method", "POST");
            Message message = new Message("login");
            message.getConnectionProperties().put(HTTPConstants.ENDPOINT, "/test/login/");
            message.getConnectionProperties().put(HTTPConstants.BASE_URL, "http://localhost:8084");
            message.getHeaders().put("disableRedirects", "true");
            Message response = transport.sendReceiveSync(message, new BigInteger("123445667889"));
            Object setCookie = null;
            for (Map.Entry<String, Object> header : response.getHeaders().entrySet()) {
                if ("Set-Cookie".equalsIgnoreCase(header.getKey())) {
                    setCookie = header.getValue();
                }
            }
            assertTrue("Set-Cookie should keep both values, got: " + setCookie,
                    setCookie instanceof List && ((List<?>) setCookie).size() == 2);
        } finally {
            server.stop(0);
        }
    }

    @Ignore("Looks up resolveEndpoint(Message), which HTTPOutboundTransport no longer declares")
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
