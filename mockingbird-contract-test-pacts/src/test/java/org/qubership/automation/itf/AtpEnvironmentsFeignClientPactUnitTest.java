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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.environments.openapi.dto.ConnectionDto;
import org.qubership.atp.environments.openapi.dto.ConnectionFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.EnvironmentFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.EnvironmentNameViewDto;
import org.qubership.atp.environments.openapi.dto.EnvironmentResDto;
import org.qubership.atp.environments.openapi.dto.ProjectFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.ProjectNameViewDto;
import org.qubership.atp.environments.openapi.dto.SystemFullVer1ViewDto;
import org.qubership.atp.environments.openapi.dto.SystemNameViewDto;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsConnectionFeignClient;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsEnvironmentFeignClient;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsProjectFeignClient;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsSystemFeignClient;
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

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {AtpEnvironmentsEnvironmentFeignClient.class, AtpEnvironmentsSystemFeignClient.class,
        AtpEnvironmentsProjectFeignClient.class, AtpEnvironmentsConnectionFeignClient.class})
@ContextConfiguration(classes = {AtpEnvironmentsFeignClientPactUnitTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class, FeignAutoConfiguration.class})
@TestPropertySource(properties = {"feign.atp.environments.name=atp-environments", "feign.atp.environments.route=",
        "feign.atp.environments.url=http://localhost:8888", "feign.httpclient.enabled=false"})
public class AtpEnvironmentsFeignClientPactUnitTest {

    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-environments", "localhost", 8888, this);
    @Autowired
    private AtpEnvironmentsEnvironmentFeignClient atpEnvironmentsEnvironmentFeignClient;
    @Autowired
    private AtpEnvironmentsSystemFeignClient atpEnvironmentsSystemFeignClient;
    @Autowired
    private AtpEnvironmentsProjectFeignClient atpEnvironmentsProjectFeignClient;
    @Autowired
    private AtpEnvironmentsConnectionFeignClient atpEnvironmentsConnectionFeignClient;
    private UUID projectUuid = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9701");
    private UUID environmentUuid = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9702");
    private UUID connectionId = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9703");
    private UUID systemUuid = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9704");

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<List<ProjectNameViewDto>> result1 = atpEnvironmentsProjectFeignClient.getAllShort(false);
        Assert.assertEquals(200, result1.getStatusCode().value());
        Assert.assertTrue(result1.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<ProjectFullVer1ViewDto> result2
                = atpEnvironmentsProjectFeignClient.getProject(projectUuid, false);
        Assert.assertEquals(200, result2.getStatusCode().value());
        Assert.assertTrue(result2.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<EnvironmentNameViewDto>> result3
                = atpEnvironmentsProjectFeignClient.getEnvironmentsShort(projectUuid);
        Assert.assertEquals(200, result3.getStatusCode().value());
        Assert.assertTrue(result3.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<EnvironmentResDto>> result4
                = atpEnvironmentsProjectFeignClient.getEnvironments(projectUuid, true);
        Assert.assertEquals(200, result4.getStatusCode().value());
        Assert.assertTrue(result4.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<EnvironmentFullVer1ViewDto> result5
                = atpEnvironmentsEnvironmentFeignClient.getEnvironment(environmentUuid, false);
        Assert.assertEquals(200, result5.getStatusCode().value());
        Assert.assertTrue(result5.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<EnvironmentFullVer1ViewDto> result6
                = atpEnvironmentsEnvironmentFeignClient.getEnvironment(environmentUuid, true);
        Assert.assertEquals(200, result6.getStatusCode().value());
        Assert.assertTrue(result6.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<SystemNameViewDto>> result7
                = atpEnvironmentsEnvironmentFeignClient.getSystemsShort(environmentUuid);
        Assert.assertEquals(200, result7.getStatusCode().value());
        Assert.assertTrue(result7.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<ConnectionDto> result8
                = atpEnvironmentsConnectionFeignClient.getConnection(connectionId, false);
        Assert.assertEquals(200, result8.getStatusCode().value());
        Assert.assertTrue(result8.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<SystemFullVer1ViewDto> result9
                = atpEnvironmentsSystemFeignClient.getSystem(systemUuid, false);
        Assert.assertEquals(200, result9.getStatusCode().value());
        Assert.assertTrue(result9.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<SystemFullVer1ViewDto> result10
                = atpEnvironmentsSystemFeignClient.getSystem(systemUuid, true);
        Assert.assertEquals(200, result10.getStatusCode().value());
        Assert.assertTrue(result10.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<ConnectionFullVer1ViewDto>> result11
                = atpEnvironmentsSystemFeignClient.getSystemConnections(systemUuid, false);
        Assert.assertEquals(200, result11.getStatusCode().value());
        Assert.assertTrue(result11.getHeaders().get("Content-Type").contains("application/json"));
    }

    @Pact(consumer = "atp-itf-executor")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart projectNameViewDto = new PactDslJsonBody()
                .uuid("id")
                .stringType("name");

        DslPart projectNameViewDtoList = new PactDslJsonArray()
                .template(projectNameViewDto);

        DslPart fullProject = new PactDslJsonBody()
                .uuid("id")
                .stringType("name")
                .stringType("description")
                .integerType("created")
                .uuid("createdBy")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("shortName")
                .eachLike("environments").uuid("id").stringType("name").closeArray();

        DslPart environmentNameViewDto = new PactDslJsonBody()
                .uuid("id")
                .stringType("name");

        DslPart environmentNameViewDtoList = new PactDslJsonArray()
                .template(environmentNameViewDto);

        DslPart environmentResDtoObject = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .stringType("graylogName")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .array("systems").object().closeArray();
        DslPart environmentRes = new PactDslJsonArray().template(environmentResDtoObject);

        DslPart environmentFullVer1ViewRes = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .stringType("graylogName")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .array("systems").object().closeArray();

        DslPart systemNameViewDto = new PactDslJsonBody()
                .uuid("id")
                .stringType("name");

        DslPart systemNameViewDtoList = new PactDslJsonArray()
                .template(systemNameViewDto);

        DslPart connectionRes = new PactDslJsonBody()
                .stringType("connectionType")
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("sourceTemplateId")
                .uuid("systemId")
                .object("parameters").closeObject()
                .array("services").closeArray();

        DslPart systemFullVer1ViewRes = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .integerType("dateOfCheckVersion")
                .integerType("dateOfLastCheck")
                .stringType("description")
                .stringType("externalName")
                .uuid("externalId")
                .uuid("id")
                .uuid("linkToSystemId")
                .booleanType("mergeByName")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("parentSystemId")
                .object("parametersGettingVersion").closeObject()
                .object("serverITF").closeObject()
                .array("connections").object().closeArray()
                .array("environmentIds").object().closeArray();

        DslPart systemConnections = PactDslJsonArray.arrayEachLike()
                .uuid("id")
                .stringType("name")
                .stringType("description")
                .integerType("created")
                .uuid("createdBy")
                .integerType("modified")
                .uuid("modifiedBy")
                .uuid("sourceTemplateId")
                .stringType("connectionType")
                .uuid("systemId")
                .object("parameters").closeObject()
                .closeObject();

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /api/projects/short OK")
                .path("/api/projects/short")
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(projectNameViewDtoList)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId} OK")
                .path("/api/projects/" + projectUuid)
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(fullProject)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/environments/short OK")
                .path("/api/projects/" + projectUuid + "/environments/short")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(environmentNameViewDtoList)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/environments OK")
                .path("/api/projects/" + projectUuid + "/environments")
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(environmentRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId} OK")
                .path("/api/environments/" + environmentUuid)
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(environmentFullVer1ViewRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId} OK")
                .path("/api/environments/" + environmentUuid)
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(environmentFullVer1ViewRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId}/systems/short OK")
                .path("/api/environments/" + environmentUuid + "/systems/short")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(systemNameViewDtoList)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/connections/{connectionId} OK")
                .path("/api/connections/" + connectionId)
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(connectionRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/systems/{systemId} OK")
                .path("/api/systems/" + systemUuid)
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(systemFullVer1ViewRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/systems/{systemId} OK")
                .path("/api/systems/" + systemUuid)
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(systemFullVer1ViewRes)
                .status(200)

                .given("all ok")
                .uponReceiving("GET /api/systems/{systemId}/connections OK")
                .path("/api/systems/" + systemUuid + "/connections")
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(systemConnections)
                .status(200);
        return response.toPact();
    }

    @Configuration
    public static class TestApp {
    }
}
