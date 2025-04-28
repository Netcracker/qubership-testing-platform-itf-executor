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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.datasets.dto.AbstractParameterDto;
import org.qubership.atp.datasets.dto.AttributeTypeDto;
import org.qubership.atp.datasets.dto.DataSetDto;
import org.qubership.atp.datasets.dto.DataSetListCreatedModifiedViewDto;
import org.qubership.atp.datasets.dto.DataSetTreeDto;
import org.qubership.atp.datasets.dto.TestPlanCreatedModifiedViewDto;
import org.qubership.atp.datasets.dto.VisibilityAreaFlatModelDto;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsAttachmentFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsAttributeFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsDatasetFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsDatasetListFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsVisibilityAreaFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.gson.Gson;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {DatasetsDatasetFeignClient.class, DatasetsAttachmentFeignClient.class,
        DatasetsAttributeFeignClient.class, DatasetsDatasetListFeignClient.class,
        DatasetsVisibilityAreaFeignClient.class})
@ContextConfiguration(classes = {DatasetsFeignClientPactUnitTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class, FeignAutoConfiguration.class})
@TestPropertySource(properties = {"feign.atp.datasets.name=atp-datasets", "feign.atp.datasets.route=",
        "feign.atp.datasets.url=http://localhost:8888", "feign.httpclient.enabled=false"})
public class DatasetsFeignClientPactUnitTest {

    private final UUID attachmentUuid = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9701");
    private final UUID dataSetId = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9702");
    private final UUID dataSetListId = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9703");
    private final String body = "";
    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-datasets", "localhost", 8888, this);
    @Autowired
    private DatasetsDatasetFeignClient dsDatasetFeignClient;
    @Autowired
    private DatasetsAttachmentFeignClient dsAttachmentFeignClient;
    @Autowired
    private DatasetsAttributeFeignClient dsAttributeFeignClient;
    @Autowired
    private DatasetsDatasetListFeignClient dsDatasetListFeignClient;
    @Autowired
    private DatasetsVisibilityAreaFeignClient dsVisibilityAreaFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<Resource> result1 = dsAttachmentFeignClient.getAttachmentByParameterId(attachmentUuid);
        Assert.assertEquals(200, result1.getStatusCode().value());
        Assert.assertTrue(result1.getHeaders().get("Content-Disposition").contains("attachment; filename=\"name\""));

        ResponseEntity<String> result2 = dsDatasetFeignClient.getItfContext(dataSetId);
        Assert.assertEquals(200, result2.getStatusCode().value());
        Assert.assertTrue(result2.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertNotNull(result2.getBody());

        ResponseEntity<DataSetTreeDto> result3 = dsDatasetFeignClient.getAtpContextFull(dataSetId, "true", body);
        Assert.assertEquals(200, result3.getStatusCode().value());
        Assert.assertTrue(result3.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody3_4_5_6(), result3.getBody());

        ResponseEntity<DataSetTreeDto> result4 = dsDatasetFeignClient.getAtpContextObject(dataSetId, "true", body);
        Assert.assertEquals(200, result4.getStatusCode().value());
        Assert.assertTrue(result4.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody3_4_5_6(), result4.getBody());

        ResponseEntity<DataSetTreeDto> result5
                = dsDatasetFeignClient.getAtpContextObjectExtended(dataSetId, "true", body);
        Assert.assertEquals(200, result5.getStatusCode().value());
        Assert.assertTrue(result5.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody3_4_5_6(), result5.getBody());

        ResponseEntity<DataSetTreeDto> result6
                = dsDatasetFeignClient.getAtpContextOptimized(dataSetId, "true", body);
        Assert.assertEquals(200, result6.getStatusCode().value());
        Assert.assertTrue(result6.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody3_4_5_6(), result6.getBody());

        ResponseEntity<List<VisibilityAreaFlatModelDto>> result7 = dsVisibilityAreaFeignClient.getVisibilityAreas();
        Assert.assertEquals(200, result7.getStatusCode().value());
        Assert.assertTrue(result7.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody7(), result7.getBody());

        ResponseEntity<List<DataSetListCreatedModifiedViewDto>> result8
                = dsDatasetListFeignClient.getDataSetListsByVaId(dataSetListId, null);
        Assert.assertEquals(200, result8.getStatusCode().value());
        Assert.assertTrue(result8.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody8(), result8.getBody());

        ResponseEntity<List<DataSetDto>> result9
                = dsDatasetListFeignClient.getDataSets(dataSetListId, null, "label");
        Assert.assertEquals(200, result9.getStatusCode().value());
        Assert.assertTrue(result9.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody9(), result9.getBody());

        ResponseEntity<Object> result10 = dsAttributeFeignClient.getAttributesInItfFormat(dataSetListId);
        Assert.assertEquals(200, result10.getStatusCode().value());
        Assert.assertTrue(result10.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(getResponseBody10(), result10.getBody());
    }

    @Pact(consumer = "atp-itf-executor")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers1 = new HashMap<>();
        headers1.put("Content-Disposition", "attachment; filename=\"name\"");

        Map<String, String> headers2 = new HashMap<>();
        headers2.put("Content-Type", "application/json");

        DslPart responseBody2 = new PactDslJsonBody()
                .stringValue("key", "context");

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /attachment/{parameterUuid} OK")
                .path("/attachment/" + attachmentUuid)
                .method("GET")
                .willRespondWith()
                .headers(headers1)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /ds/{dataSetId}/itf OK")
                .path("/ds/" + dataSetId + "/itf")
                .method("GET")
                .willRespondWith()
                .headers(headers2)
                .body(responseBody2)
                .status(200)

                .given("all ok")
                .uponReceiving("POST /ds/{dataSetId}/atp OK")
                .path("/ds/" + dataSetId + "/atp")
                .query("evaluate=true")
                .method("POST")
                .headers(headers2)
                .body(body)
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody3_4_5_6()))
                .status(200)

                .given("all ok")
                .uponReceiving("POST /ds/{dataSetId}/atp/object OK")
                .path("/ds/" + dataSetId + "/atp/object")
                .query("evaluate=true")
                .method("POST")
                .headers(headers2)
                .body(body)
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody3_4_5_6()))
                .status(200)

                .given("all ok")
                .uponReceiving("POST /ds/{dataSetId}/atp/objectExtended OK")
                .path("/ds/" + dataSetId + "/atp/objectExtended")
                .query("evaluate=true")
                .method("POST")
                .headers(headers2)
                .body(body)
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody3_4_5_6()))
                .status(200)

                .given("all ok")
                .uponReceiving("POST /ds/{dataSetId}/atp/optimized OK")
                .path("/ds/" + dataSetId + "/atp/optimized")
                .query("evaluate=true")
                .method("POST")
                .headers(headers2)
                .body(body)
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody3_4_5_6()))
                .status(200)

                .given("all ok")
                .uponReceiving("GET /va OK")
                .path("/va")
                .method("GET")
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody7()))
                .status(200)

                .given("all ok")
                .uponReceiving("GET /dsl/va/{vaId} OK")
                .path("/dsl/va/" + dataSetListId)
                .method("GET")
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody8()))
                .status(200)

                .given("all ok")
                .uponReceiving("GET /dsl/{dataSetListId}/ds OK")
                .path("/dsl/" + dataSetListId + "/ds")
                .query("label=label")
                .method("GET")
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody9()))
                .status(200)

                .given("all ok")
                .uponReceiving("GET /attribute/dsl/{dataSetListId}/itf OK")
                .path("/attribute/dsl/" + dataSetListId + "/itf")
                .method("GET")
                .willRespondWith()
                .headers(headers2)
                .body(objectToString(getResponseBody10()))
                .status(200);

        return response.toPact();
    }

    private VisibilityAreaFlatModelDto getVisibilityAreaFlatModelDto() {
        VisibilityAreaFlatModelDto visibilityAreaFlatModelDto = new VisibilityAreaFlatModelDto();
        visibilityAreaFlatModelDto.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));
        visibilityAreaFlatModelDto.setName("visibilityArea");

        List<UUID> uuids = new ArrayList<>();
        uuids.add(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa03"));
        visibilityAreaFlatModelDto.setDataSetLists(uuids);
        return visibilityAreaFlatModelDto;
    }

    private DataSetTreeDto getResponseBody3_4_5_6() {
        DataSetTreeDto dataSetTreeDto = new DataSetTreeDto();
        Map<String, AbstractParameterDto> parameters = new HashMap<>();
        AbstractParameterDto abstractParameterDto1 = new AbstractParameterDto();
        abstractParameterDto1.setType(AttributeTypeDto.TEXT);
        parameters.put("1ATTRIBUTE", abstractParameterDto1);

        dataSetTreeDto.setParameters(parameters);
        return dataSetTreeDto;
    }

    private List<VisibilityAreaFlatModelDto> getResponseBody7() {
        return Arrays.asList(getVisibilityAreaFlatModelDto());
    }

    private DataSetListCreatedModifiedViewDto getDataSetList() {
        DataSetListCreatedModifiedViewDto dataSetList = new DataSetListCreatedModifiedViewDto();
        dataSetList.setCreatedBy(UUID.fromString("f0c1d2ba-7c99-4e0b-a39d-0566f2ae9f25"));
        dataSetList.setModifiedBy(UUID.fromString("f0c1d2ba-7c99-4e0b-a39d-0566f2ae9f26"));

        TestPlanCreatedModifiedViewDto testPlanCreatedModifiedViewDto = new TestPlanCreatedModifiedViewDto();
        testPlanCreatedModifiedViewDto.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));
        dataSetList.setTestPlan(testPlanCreatedModifiedViewDto);

        return dataSetList;
    }

    private List<DataSetListCreatedModifiedViewDto> getResponseBody8() {
        return Arrays.asList(getDataSetList());
    }

    private DataSetDto getDataSet() {
        DataSetDto dataSet = new DataSetDto();
        dataSet.setName("dataSetName");
        dataSet.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01"));
        return dataSet;
    }

    private List<DataSetDto> getResponseBody9() {
        return Arrays.asList(getDataSet());
    }

    private List<String> getResponseBody10() {
        List<String> list = new ArrayList<>();
        list.add("1ATTRIBUTE");
        list.add("2ATTRIBUTE");
        return list;
    }

    private String objectToString(Object obj) {
        return new Gson().toJson(obj);
    }

    @Configuration
    public static class TestApp {
    }
}
