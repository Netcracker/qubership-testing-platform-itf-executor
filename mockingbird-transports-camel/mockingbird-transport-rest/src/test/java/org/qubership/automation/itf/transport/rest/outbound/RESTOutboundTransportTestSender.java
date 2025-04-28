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
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.IOException;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.beust.jcommander.internal.Maps;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class RESTOutboundTransportTestSender {

    private static final CamelContext CAMEL_CONTEXT = new DefaultCamelContext();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Before
    public void setUp() throws Exception {
        stubFor(post(urlEqualTo("/test/post/"))/*.withRequestBody(matching("test"))
         */.willReturn(aResponse().withBody("success").withStatus(200)));
        stubFor(get(urlEqualTo("/test/get/")).willReturn(aResponse().withBody("success").withStatus(200)));
        stubFor(delete(urlEqualTo("/test/del/")).withRequestBody(matching("test")).willReturn(aResponse().withBody(
                "success").withStatus(200)));
        stubFor(delete(urlEqualTo("/test/put/")).willReturn(aResponse().withBody("success").withStatus(200)));
    }

    @Test
    public void testSendRequestToGoogleWithApacheCamel() throws IOException {
        ProducerTemplate template = CAMEL_CONTEXT.createProducerTemplate();
        Map<String, Object> headers = Maps.newHashMap();
        headers.put(Exchange.HTTP_METHOD, "GET");
        String response = template.requestBodyAndHeaders("http://localhost:8080/test/post/", null, headers,
                String.class);
        System.out.println(response);
    }
}
