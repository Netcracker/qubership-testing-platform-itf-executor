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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.catalogue.openapi.dto.ObjectOperationDto;
import org.qubership.atp.catalogue.openapi.dto.ProjectDto;
import org.qubership.atp.catalogue.openapi.dto.UserInfoDto;
import org.qubership.automation.itf.integration.catalogue.CatalogueProjectFeignClient;
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
@EnableFeignClients(clients = {CatalogueProjectFeignClient.class})
@ContextConfiguration(classes = {CatalogueFeignClientPactUnitTest.TestApp.class})
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(properties = {
        "feign.atp.catalogue.name=atp-catalogue-backend",
        "feign.atp.catalogue.route=",
        "feign.atp.catalogue.url=http://localhost:8888",
        "feign.httpclient.enabled=false"})
public class CatalogueFeignClientPactUnitTest {

    private final UUID projectUuid = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9701");
    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-catalogue-backend", "localhost", 8888, this);

    @Autowired
    private CatalogueProjectFeignClient catalogueProjectFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<ProjectDto> result = catalogueProjectFeignClient.getProjectById(projectUuid);
        Assert.assertEquals(200, result.getStatusCode().value());
        Assert.assertTrue(result.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(formProject(), result.getBody());
    }

    @Pact(consumer = "atp-itf-executor")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /catalog/api/v1/projects/{uuid} OK")
                .path("/catalog/api/v1/projects/" + projectUuid)
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(new Gson().toJson(formProject()))
                .status(200);
        return response.toPact();
    }

    public ProjectDto formProject() {
        UUID id = UUID.fromString("ef73f503-c483-4ce9-bee7-31e40135b98f");
        ProjectDto project = new ProjectDto();
        project.setNumberOfThreshold(1);
        project.setDataSets(Collections.singletonList("str"));
        project.setLeads(Collections.singletonList(id));
        project.setQaTaEngineers(Collections.singletonList(id));
        project.setDevOpsEngineers(Collections.singletonList(id));
        project.setAtpRunners(Collections.singletonList(id));
        project.setAtpSupports(Collections.singletonList(id));
        project.setTaTools(Collections.singletonList(id));
        project.setDisableWarnMsgSizeExceed(true);
        project.setDisableWarnOutOfSyncTime(true);
        project.setDisableAutoSyncAtpTestCasesWithJiraTickets(true);
        project.setProjectLabel("label");
        project.setNotificationMessageSubjectTemplate("template");
        project.setProjectType(ProjectDto.ProjectTypeEnum.IMPLEMENTATION);
        project.setTshooterUrl("ts");
        project.setMonitoringToolUrl("url");
        project.setMissionControlToolUrl("mission");
        project.setChildrenOperations(Collections.singletonList(createObjectOperation()));
        project.setDateFormat("dateformat");
        project.setTimeFormat("time");
        project.setTimeZone("zone");
        project.setCreatedBy(createUserInfo());
        project.setModifiedBy(createUserInfo());
        project.setUuid(id);
        project.setName("name");
        project.setDescription("desr");
        project.setSourceId(id);

        return project;
    }

    public UserInfoDto createUserInfo() {
        UUID id = UUID.fromString("ef73f503-c483-4ce9-bee7-31e40135b98f");
        UserInfoDto userInfo = new UserInfoDto();
        userInfo.setId(id);
        userInfo.setUsername("user");
        userInfo.setFirstName("first");
        userInfo.setLastName("last");
        userInfo.setEmail("email");
        userInfo.setRoles(Collections.singletonList("str"));
        return userInfo;
    }

    public ObjectOperationDto createObjectOperation() {
        ObjectOperationDto objectOperation = new ObjectOperationDto();
        objectOperation.setName("name");
        objectOperation.setOperationType(ObjectOperationDto.OperationTypeEnum.ADD);
        return objectOperation;
    }

    @Configuration
    public static class TestApp {
    }
}
