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

package org.qubership.automation.itf.contracts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.automation.itf.ui.controls.ContextController;
import org.qubership.automation.itf.ui.controls.VelocityController;
import org.qubership.automation.itf.ui.messages.objects.ResponseObject;
import org.qubership.automation.itf.ui.messages.objects.UIVelocityRequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;

@Provider("atp-itf-executor")
@PactUrl(urls = {"classpath:pacts/atp-itf-lite-atp-itf-executor.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {ContextController.class, VelocityController.class})
@ContextConfiguration(classes = {ItfExecutorAndItfLiteContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        ContextController.class,
        VelocityController.class
})
@TestPropertySource(locations = "classpath:bootstrap-test.properties")
public class ItfExecutorAndItfLiteContractTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ContextController contextController;
    @MockBean
    private VelocityController velocityController;

    public void beforeAll() throws ParseException, IllegalAccessException, InstantiationException {
        when(contextController.get(any(String.class), any(UUID.class))).thenReturn(getContextResponse());
        when(velocityController.parseContent(any(UIVelocityRequestBody.class), any(), any(UUID.class)))
                .thenReturn(getResponse());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) throws Exception {
        beforeAll();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("all ok")
    public void allPass() {
    }

    private ResponseObject getResponse() {
        ResponseObject response = new ResponseObject();
        response.setResponse("value");
        return response;
    }

    private String getContextResponse() {
        return "{\"status\":\"STARTED\"}";
    }

    @Configuration
    public static class TestApp {
    }

}
