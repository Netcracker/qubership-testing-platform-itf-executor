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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.bv.dto.CopyWithNameRequestDto;
import org.qubership.automation.itf.core.util.feign.impl.BvApiResourceFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.BvPublicApiResourceFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.BvTestCaseResourceFeignClient;
import org.qubership.automation.itf.integration.bv.messages.request.BvReadMode;
import org.qubership.automation.itf.integration.bv.messages.request.BvReadType;
import org.qubership.automation.itf.integration.bv.messages.request.BvSource;
import org.qubership.automation.itf.integration.bv.messages.request.ReportData;
import org.qubership.automation.itf.integration.bv.messages.request.RequestData;
import org.qubership.automation.itf.integration.bv.messages.request.TestCase;
import org.qubership.automation.itf.integration.bv.messages.request.quickCompare.QuickCompareRequest;
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
@EnableFeignClients(clients = {BvTestCaseResourceFeignClient.class, BvPublicApiResourceFeignClient.class,
        BvApiResourceFeignClient.class})
@ContextConfiguration(classes = {BvFeignClientPactUnitTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class, FeignAutoConfiguration.class})
@TestPropertySource(properties = {"feign.atp.bv.name=atp-bv", "feign.atp.bv.route=",
        "feign.atp.bv.url=http://localhost:8889", "feign.httpclient.enabled=false"})
public class BvFeignClientPactUnitTest {

    private final UUID projectUuid = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9701");
    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-bv", "localhost", 8889, this);

    @Autowired
    private BvTestCaseResourceFeignClient bvTestCaseResourceFeignClient;
    @Autowired
    private BvPublicApiResourceFeignClient bvPublicApiResourceFeignClient;
    @Autowired
    private BvApiResourceFeignClient bvApiResourceFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<String> result1
                = bvTestCaseResourceFeignClient.create(projectUuid, objectToString(getRequestBody1()));
        Assert.assertEquals(200, result1.getStatusCode().value());
        Assert.assertTrue(result1.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody1(), result1.getBody());

        ResponseEntity<String> result2 = bvTestCaseResourceFeignClient.remove(projectUuid, getRequestBody2());
        Assert.assertEquals(200, result2.getStatusCode().value());
        Assert.assertTrue(result2.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody2(), result2.getBody());

        ResponseEntity<String> result3
                = bvTestCaseResourceFeignClient.getTestCaseStatus(projectUuid, getRequestBody3());
        Assert.assertEquals(200, result3.getStatusCode().value());
        Assert.assertTrue(result3.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody3(), result3.getBody());

        ResponseEntity<String> result4 = bvTestCaseResourceFeignClient.getParameters(projectUuid, getRequestBody4());
        Assert.assertEquals(200, result4.getStatusCode().value());
        Assert.assertTrue(result4.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody4(), result4.getBody());

        ResponseEntity<String> result5 = bvTestCaseResourceFeignClient.copyWithName(projectUuid, getRequestBody5());
        Assert.assertEquals(200, result5.getStatusCode().value());
        Assert.assertTrue(result5.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody5(), result5.getBody());

        ResponseEntity<Object> result6
                = bvPublicApiResourceFeignClient.createTr(projectUuid, objectToString(getRequestBody6()));
        Assert.assertEquals(200, result6.getStatusCode().value());
        Assert.assertTrue(result6.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody6(), objectToString(result6.getBody()));

        ResponseEntity<Object> result7
                = bvPublicApiResourceFeignClient.quickCompare(projectUuid, objectToString(getRequestBody7()));
        Assert.assertEquals(200, result7.getStatusCode().value());
        Assert.assertTrue(result7.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody7(), objectToString(result7.getBody()));

        ResponseEntity<String> result8 = bvApiResourceFeignClient.read(projectUuid, objectToString(getRequestBody8()));
        Assert.assertEquals(200, result8.getStatusCode().value());
        Assert.assertTrue(result8.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody8(), result8.getBody());

        ResponseEntity<String> result9
                = bvApiResourceFeignClient.readAndCompare(projectUuid, objectToString(getRequestBody9()));
        Assert.assertEquals(200, result9.getStatusCode().value());
        Assert.assertTrue(result9.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody9(), result9.getBody());

        ResponseEntity<String> result10
                = bvApiResourceFeignClient.compare(projectUuid, objectToString(getRequestBody10()));
        Assert.assertEquals(200, result10.getStatusCode().value());
        Assert.assertTrue(result10.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody10(), result10.getBody());
    }

    @Pact(consumer = "atp-itf-executor")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        PactDslResponse response = builder
                .uponReceiving("PUT /api/bvtool/project/{projectId}/testcases/v1/create OK")
                .path("/api/bvtool/project/" + projectUuid + "/testcases/v1/create")
                .method("PUT")
                .headers(headers)
                .body(objectToString(getRequestBody1()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody1())
                .status(200)

                .uponReceiving("PUT /api/bvtool/project/{projectId}/testcases/v1/remove OK")
                .path("/api/bvtool/project/" + projectUuid + "/testcases/v1/remove")
                .method("PUT")
                .headers(headers)
                .body(getRequestBody2())
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody2())
                .status(200)

                .uponReceiving("PUT /api/bvtool/project/{projectId}/testcases/v1/getTestCaseStatus OK")
                .path("/api/bvtool/project/" + projectUuid + "/testcases/v1/getTestCaseStatus")
                .method("PUT")
                .headers(headers)
                .body(getRequestBody3())
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody3())
                .status(200)

                .uponReceiving("PUT /api/bvtool/project/{projectId}/testcases/v1/getParameters OK")
                .path("/api/bvtool/project/" + projectUuid + "/testcases/v1/getParameters")
                .method("PUT")
                .headers(headers)
                .body(getRequestBody4())
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody4())
                .status(200)

                .uponReceiving("PUT /api/bvtool/project/{projectId}/testcases/v1/copyWithName OK")
                .path("/api/bvtool/project/" + projectUuid + "/testcases/v1/copyWithName")
                .method("PUT")
                .headers(headers)
                .body(objectToString(getRequestBody5()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody5())
                .status(200)

                .uponReceiving("PUT /api/bvtool/project/{projectId}/public/v1/createTr OK")
                .path("/api/bvtool/project/" + projectUuid + "/public/v1/createTr")
                .method("PUT")
                .headers(headers)
                .body(objectToString(getRequestBody6()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody6())
                .status(200)

                .uponReceiving("POST /api/bvtool/project/{projectId}/public/v1/quickCompare OK")
                .path("/api/bvtool/project/" + projectUuid + "/public/v1/quickCompare")
                .method("POST")
                .headers(headers)
                .body(objectToString(getRequestBody7()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody7())
                .status(200)

                .given("all ok")
                .uponReceiving("PUT /api/bvtool/project/{projectId}/api/v1/read OK")
                .path("/api/bvtool/project/" + projectUuid + "/api/v1/read")
                .method("PUT")
                .body(objectToString(getRequestBody8()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody8())
                .status(200)

                .given("all ok")
                .uponReceiving("PUT /api/bvtool/project/{projectId}/api/v1/readAndCompare OK")
                .path("/api/bvtool/project/" + projectUuid + "/api/v1/readAndCompare")
                .method("PUT")
                .body(objectToString(getRequestBody9()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody9())
                .status(200)

                .given("all ok")
                .uponReceiving("PUT /api/bvtool/project/{projectId}/api/v1/compare OK")
                .path("/api/bvtool/project/" + projectUuid + "/api/v1/compare")
                .method("PUT")
                .body(objectToString(getRequestBody10()))
                .willRespondWith()
                .headers(headers)
                .body(getResponseBody10())
                .status(200);

        return response.toPact();
    }

    private RequestData getRequestBody1() {
        RequestData requestData = new RequestData();
        requestData.setName("1Operation local / 1Situation local");
        requestData.setType("SIMPLE");

        BvSource source = new BvSource();
        source.setSourceName("1");
        requestData.setSources(new BvSource[]{source});

        requestData.setLabels(new ArrayList<>());
        return requestData;
    }

    private String getResponseBody1() {
        return "{\"created\":\"May 15, 2023 6:47:53 PM\",\"labels\":[],"
                + "\"name\":\"1Operation local / 1Situation local\","
                + "\"orderNum\":1,\"parameters\":[],\"rules\":[],\"sources\":[],"
                + "\"tcId\":\"dbff9af3-7cf9-4196-bd95-13570dba34a7\",\"validationObjects\":[]}";
    }

    private String getRequestBody2() {
        return "{\"tcId\":\"dbff9af3-7cf9-4196-bd95-13570dba34a7\"}";
    }

    private String getResponseBody2() {
        return "{\"statusCode\":10000}";
    }

    private String getRequestBody3() {
        return "{\"tcId\":\"dbff9af3-7cf9-4196-bd95-13570dba34a7\"}";
    }

    private String getResponseBody3() {
        return "{\"statusCode\":10000,\"tcStatus\":{\"containsObjects\":\"false\",\"containsTestruns\":\"false\","
                + "\"status\":\"TEST_CASE_EXISTS.\"}}";
    }

    private String getRequestBody4() {
        return "{\"testCasesIds\":[\"dbff9af3-7cf9-4196-bd95-13570dba34a7\"]}";
    }

    private String getResponseBody4() {
        return "{\"scopes\":[],\"statusCode\":10000,\"vObjects\":[]}";
    }

    private CopyWithNameRequestDto getRequestBody5() {
        CopyWithNameRequestDto copyWithNameRequestDto = new CopyWithNameRequestDto();
        copyWithNameRequestDto.setNewName("newName1");
        copyWithNameRequestDto.setSourceTcId("6bc32e25-af92-4dba-bdf0-5491525fb74a");
        return copyWithNameRequestDto;
    }

    private String getResponseBody5() {
        return "\"b4bf688d-548f-486d-856d-98031db8d278\"";
    }

    private RequestData getRequestBody6() {
        RequestData requestData = new RequestData();
        requestData.setName("1Operation local / 1Situation local");
        requestData.setType("SIMPLE");

        BvSource source = new BvSource();
        source.setSourceName("1");
        requestData.setSources(new BvSource[]{source});
        requestData.setSources(new BvSource[]{source});
        requestData.setLabels(new ArrayList<>());
        return requestData;
    }

    private String getResponseBody6() {
        return "{\"statusCode\":20000,\"statusMessage\":\"Empty read result!\"}";
    }

    private QuickCompareRequest getRequestBody7() {
        QuickCompareRequest requestData = new QuickCompareRequest();
        requestData.setTcId("6bc32e25-af92-4dba-bdf0-5491525fb74a");
        requestData.setLoadHighlight(true);
        requestData.setValidationObjects(new ArrayList<>());
        return requestData;
    }

    private String getResponseBody7() {
        return "{\"statusCode\":20000,\"statusMessage\":\"Empty read result!\"}";
    }

    private RequestData getRequestBody8() {
        RequestData requestData = new RequestData();
        TestCase testCase = new TestCase();
        testCase.setTcId("6bc32e25-af92-4dba-bdf0-5491525fb74a");
        requestData.setReadMode(BvReadMode.ER);
        requestData.setReadType(BvReadType.CLEAR_AND_REWRITE);
        requestData.setTestCases(new TestCase[]{testCase});
        return requestData;
    }

    private String getResponseBody8() {
        return "{\"statusCode\":10000,\"statusMessage\":\"Ethalons' read is completed successfully!\"}";
    }

    private RequestData getRequestBody9() {
        RequestData requestData = new RequestData();
        TestCase testCase = new TestCase();
        testCase.setTcId("6bc32e25-af92-4dba-bdf0-5491525fb74a");
        requestData.setTestCases(new TestCase[]{testCase});
        requestData.setReadMode(BvReadMode.AR);
        requestData.setReadType(BvReadType.READ);
        requestData.setReport(createReportData());
        return requestData;
    }

    private String getResponseBody9() {
        return "{\"statusCode\":20000,\"statusMessage\":\"Empty read result!\"}";
    }

    private RequestData getRequestBody10() {
        RequestData requestData = new RequestData();
        TestCase testCase = new TestCase();
        testCase.setTcId("61fc637f-888d-4a28-bdaa-c01be2300b04");
        requestData.setTestCases(new TestCase[]{testCase});
        requestData.setReport(createReportData());
        return requestData;
    }

    private ReportData createReportData() {
        ReportData reportData = new ReportData();
        reportData.setBuilder("ATP_RAM_REPORT");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("atpAdapterMode", "ntt");
        parameters.put("atpTestRunId", "7c9dafe9-2cd1-4ffc-ae54-45867f2b9704");
        parameters.put("atpSectionId", "7c9dafe9-2cd1-4ffc-ae54-45867f2b9705");
        parameters.put("atpRamUrl", "url");
        parameters.put("ram2TestRunId", "7c9dafe9-2cd1-4ffc-ae54-45867f2b9706");
        parameters.put("ram2SectionId", "7c9dafe9-2cd1-4ffc-ae54-45867f2b9707");
        parameters.put("ram2SectionName", "ram2SectionName");
        parameters.put("atpRamLoggerUrl", "url");
        parameters.put("atpReportTo", "ATP RAM 2");
        parameters.put("atpCustomer", "DT");
        parameters.put("atpProject", "Demo");
        reportData.setParameters(parameters);
        return reportData;
    }

    private String getResponseBody10() {
        return "{\"statusCode\":20000,\"statusMessage\":\"NTT Project Name or NTT Test Plan must be defined\"}";
    }

    private String objectToString(Object obj) {
        return new Gson().toJson(obj);
    }

    @Configuration
    public static class TestApp {
    }
}
