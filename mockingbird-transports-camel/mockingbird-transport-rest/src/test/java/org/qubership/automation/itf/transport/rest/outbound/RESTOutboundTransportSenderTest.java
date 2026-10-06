/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest(httpPort = 8080)
public class RESTOutboundTransportSenderTest {

    private static final CamelContext CAMEL_CONTEXT = new DefaultCamelContext();

    @BeforeAll
    public static void startCamelContext() {
        CAMEL_CONTEXT.start();
    }

    @AfterAll
    public static void stopCamelContext() {
        CAMEL_CONTEXT.stop();
    }

    @BeforeEach
    public void setUp() {
        stubFor(post(urlEqualTo("/test/post/")).willReturn(aResponse().withBody("success").withStatus(200)));
        stubFor(get(urlEqualTo("/test/get/")).willReturn(aResponse().withBody("success").withStatus(200)));
        stubFor(delete(urlEqualTo("/test/del/")).withRequestBody(matching("test")).willReturn(aResponse()
                .withBody("success").withStatus(200)));
        stubFor(delete(urlEqualTo("/test/put/")).willReturn(aResponse().withBody("success").withStatus(200)));
    }

    @Test
    public void testSendGetRequestWithApacheCamel() {
        ProducerTemplate template = CAMEL_CONTEXT.createProducerTemplate();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "GET");
        String response = template.requestBodyAndHeaders("http://localhost:8080/test/get/", null,
                headers, String.class);
        assertEquals("success", response);
    }
}
