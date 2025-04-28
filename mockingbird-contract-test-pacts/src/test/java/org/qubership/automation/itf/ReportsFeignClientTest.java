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

package org.qubership.automation.itf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.automation.itf.integration.reports.ReportsFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.gson.Gson;

@RunWith(SpringRunner.class)

@EnableFeignClients(clients = {ReportsFeignClient.class})
@ContextConfiguration(classes = {ReportsFeignClientTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(properties = {"feign.atp.reports.name=atp-itf-reports", "feign.atp.reports.route=",
        "feign.atp.reports.url=http://localhost:8888", "feign.httpclient.enabled=false"})
public class ReportsFeignClientTest {

    private final String contextId = "9167234930111872000";
    private final UUID projectUuid = UUID.fromString("39cae351-9e3b-4fb6-a384-1c3616f4e76f");
    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-itf-reports", "localhost", 8888, this);
    @Autowired
    private ReportsFeignClient reportsFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<List<List<Object>>> result1 = reportsFeignClient.getContextProperties(contextId, projectUuid);
        Assert.assertEquals(200, result1.getStatusCode().value());
        Assert.assertTrue(Objects.requireNonNull(result1.getHeaders().get("Content-Type")).contains("application/json"
        ));

        ResponseEntity<String> result2 = reportsFeignClient.getContextVariables(contextId, projectUuid);
        Assert.assertEquals(200, result2.getStatusCode().value());
        Assert.assertTrue(Objects.requireNonNull(result2.getHeaders().get("Content-Type")).contains("text/plain"));

        ResponseEntity<Set<String>> result3 = reportsFeignClient.getKeys(contextId, projectUuid);
        Assert.assertEquals(200, result3.getStatusCode().value());
        Assert.assertTrue(Objects.requireNonNull(result3.getHeaders().get("Content-Type")).contains("application/json"
        ));

        ResponseEntity<Map<String, Integer>> result4 = reportsFeignClient.getCurrentPartitionNumbers();
        Assert.assertEquals(200, result4.getStatusCode().value());
        Assert.assertTrue(Objects.requireNonNull(result4.getHeaders().get("Content-Type")).contains("application/json"
        ));
    }

    @Pact(consumer = "atp-itf-executor")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> textPlainHeaders = new HashMap<>();
        textPlainHeaders.put("Content-Type", "text/plain");

        Map<String, String> appJsonHeaders = new HashMap<>();
        appJsonHeaders.put("Content-Type", "application/json");

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /context/getProperties OK")
                .path("/context/getProperties")
                .query("contextId=" + contextId + "&projectUuid=" + projectUuid)
                .method("GET")
                .willRespondWith()
                .headers(appJsonHeaders)
                .body(objectToJson(getPropertiesBody()))
                .status(200)

                .given("all ok")
                .uponReceiving("GET /context/getContextVariables OK")
                .path("/context/getContextVariables")
                .query("contextId=" + contextId + "&projectUuid=" + projectUuid)
                .method("GET")
                .willRespondWith()
                .headers(textPlainHeaders)
                .body(getContextVariablesBody())
                .status(200)

                .given("all ok")
                .uponReceiving("GET /context/getKeys OK")
                .path("/context/getKeys")
                .query("contextId=" + contextId + "&projectUuid=" + projectUuid)
                .method("GET")
                .willRespondWith()
                .headers(appJsonHeaders)
                .body(objectToJson(getKeysBody()))
                .status(200)

                .given("all ok")
                .uponReceiving("GET /partition/current OK")
                .path("/partition/current")
                .method("GET")
                .willRespondWith()
                .headers(appJsonHeaders)
                .body(objectToJson(getCurrentPartitionsBody()))
                .status(200);

        return response.toPact();
    }

    private String getContextVariablesBody() {
        return "testVariable";
    }

    private List<List<Object>> getPropertiesBody() {
        List<Object> objectList = new ArrayList<>();
        objectList.add("testProperty");
        return Collections.singletonList(objectList);
    }

    private List<String> getKeysBody() {
        return Collections.singletonList("testKey");
    }

    private Map<String, Integer> getCurrentPartitionsBody() {
        Map<String, Integer> map = new HashMap<>();
        map.put("Default", 1);
        map.put(projectUuid.toString(), 2);
        return map;
    }

    private String objectToJson(Object object) {
        return new Gson().toJson(object);
    }

    @Configuration
    public static class TestApp {
    }
}
