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

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.automation.itf.core.model.communication.EnvironmentSample;
import org.qubership.automation.itf.core.model.communication.TransportType;
import org.qubership.automation.itf.core.model.communication.TriggerSample;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.ui.controls.integration.ItfStubsRequestsController;
import org.qubership.automation.itf.ui.controls.integration.Result;
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
import lombok.extern.slf4j.Slf4j;

@Provider("atp-itf-executor")
@PactUrl(urls = {"classpath:pacts/atp-itf-stubs-atp-itf-executor.json"})
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {ItfStubsRequestsController.class})
@ContextConfiguration(classes = {ExecutorAndStubsContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        ItfStubsRequestsController.class})
@TestPropertySource(locations = "classpath:bootstrap-test.properties")
@Slf4j
public class ExecutorAndStubsContractTest {

    private final BigInteger triggerId = new BigInteger("9167234930111872000");
    private final BigInteger environmentId = new BigInteger("9167234930111872001");
    private final String xProjectId = "3d6a138d-057b-4e35-8348-17aee2f2b0f8";
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ItfStubsRequestsController itfStubsRequestsController;

    public void beforeAll() {
        log.info("ExecutorAndStubsContractTest tests started");
        when(itfStubsRequestsController.getAllActiveTriggers()).thenReturn(getResponseBody1());
        when(itfStubsRequestsController.getTriggerById(any(BigInteger.class))).thenReturn(getResponseBody2());
        when(itfStubsRequestsController.getTriggersByEnvironment(any(BigInteger.class))).thenReturn(getResponseBody3());
        when(itfStubsRequestsController.updateTriggerStatus(any())).thenReturn(getResponseBody4());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        beforeAll();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("all ok")
    public void allPass() {
    }

    private List<TriggerSample> getResponseBody1() {
        return Collections.singletonList(getTriggerSample());
    }

    private TriggerSample getResponseBody2() {
        return getTriggerSample();
    }

    private EnvironmentSample getResponseBody3() {
        EnvironmentSample environmentSample = new EnvironmentSample();
        environmentSample.setEnvId(environmentId);
        environmentSample.setTurnedOn(true);
        environmentSample.setTriggerSamples(Collections.singletonList(getTriggerSample()));
        return environmentSample;
    }

    private Result getResponseBody4() {
        Result resultDto = new Result();
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        resultDto.setData(data);
        resultDto.setMessage("message");
        resultDto.setSuccess(true);
        return resultDto;
    }

    private TriggerSample getTriggerSample() {
        TriggerSample triggerSample = new TriggerSample();
        triggerSample.setTriggerId(triggerId);
        triggerSample.setTriggerName("trig_name");
        triggerSample.setProjectId(new BigInteger("9157230976135870000"));
        triggerSample.setProjectUuid(UUID.fromString("3d6a138d-057b-4e35-8348-17aee2f2b0f8"));
        triggerSample.setTriggerState(TriggerState.ACTIVE);
        ConnectionProperties triggerProperties = new ConnectionProperties();
        triggerProperties.put("responseCode", "200");
        triggerProperties.put("isStub", "Yes");
        triggerProperties.put("endpoint", "/test");
        triggerProperties.put("contentType", "application/json; charset=utf-8");
        triggerSample.setTriggerProperties(triggerProperties);
        triggerSample.setServerName("server_test");
        triggerSample.setTransportName("Outbound REST Synchronous");
        triggerSample.setTriggerTypeName("org.qubership.automation.itf.transport.rest.outbound.RESTOutboundTransport");
        triggerSample.setTransportType(TransportType.REST_INBOUND);
        return triggerSample;
    }

    @Configuration
    public static class TestApp {
    }
}
