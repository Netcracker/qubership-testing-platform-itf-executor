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
import org.qubership.atp.users.clients.dto.ProjectDto;
import org.qubership.automation.itf.integration.users.UsersProjectFeignClient;
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
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {UsersProjectFeignClient.class})
@ContextConfiguration(classes = {UsersProjectFeignClientPactTest.TestApp.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(
        properties = {"feign.atp.users.name=atp-users-backend", "feign.atp.users.route=",
                "feign.atp.users.url=http://localhost:8888", "feign.httpclient.enabled=false"})
public class UsersProjectFeignClientPactTest {

    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-users-backend", "localhost", 8888, this);
    @Autowired
    private UsersProjectFeignClient usersProjectFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<List<ProjectDto>> result1 = usersProjectFeignClient.getAllProjects();
        Assert.assertEquals(200, result1.getStatusCode().value());
        Assert.assertTrue(result1.getHeaders().get("Content-Type").contains("application/json"));

        UUID projectId = UUID.fromString("5518c918-b816-40a6-aa1d-9efd0df476f8");
        ResponseEntity<ProjectDto> result2 = usersProjectFeignClient.getProjectUsersByProjectId(projectId);
        Assert.assertEquals(200, result2.getStatusCode().value());
        Assert.assertTrue(result2.getHeaders().get("Content-Type").contains("application/json"));
    }

    @Pact(consumer = "atp-itf-executor")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart permissionsDto = new PactDslJsonBody()
                .object("leads")
                .eachKeyLike("DEFAULT")
                .booleanType("create", true)
                .booleanType("read", true)
                .booleanType("update", true)
                .booleanType("delete", true)
                .booleanType("execute", true)
                .booleanType("lock", true)
                .booleanType("unlock", true)
                .closeObject()
                .closeObject()
                .object("qaTaEngineers")
                .eachKeyLike("DEFAULT")
                .booleanType("create", true)
                .booleanType("read", true)
                .booleanType("update", true)
                .booleanType("delete", false)
                .booleanType("execute", true)
                .booleanType("lock", true)
                .booleanType("unlock", false)
                .closeObject()
                .closeObject()
                .object("devOpsEngineers")
                .eachKeyLike("DEFAULT")
                .booleanType("create", false)
                .booleanType("read", true)
                .booleanType("update", false)
                .booleanType("delete", false)
                .booleanType("execute", true)
                .booleanType("lock")
                .booleanType("unlock")
                .closeObject()
                .closeObject()
                .object("atpSupports")
                .eachKeyLike("DEFAULT")
                .booleanType("create", false)
                .booleanType("read", true)
                .booleanType("update", false)
                .booleanType("delete", false)
                .booleanType("execute", true)
                .booleanType("lock", false)
                .booleanType("unlock", false)
                .closeObject()
                .closeObject()
                .object("atpRunners")
                .eachKeyLike("DEFAULT")
                .booleanType("create", false)
                .booleanType("read", true)
                .booleanType("update", false)
                .booleanType("delete", false)
                .booleanType("execute", true)
                .booleanType("lock", false)
                .booleanType("unlock", false)
                .closeObject()
                .closeObject();

        DslPart roles = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.stringType("role1"));

        DslPart userInfoDto = new PactDslJsonBody()
                .uuid("id", "775c0c85-bbcf-4ecd-857f-6f082ea9f4c6")
                .stringType("username", "john_doe")
                .stringType("firstName", "john")
                .stringType("lastName", "doe")
                .stringType("email", "test@test")
                .object("roles", roles);

        DslPart userIds = PactDslJsonArray
                .arrayEachLike(PactDslJsonRootValue.uuid("775c0c85-bbcf-4ecd-857f-6f082ea9f4c6"));

        DslPart projectUsersDtoObj = new PactDslJsonBody()
                .uuid("uuid", "775c0c85-bbcf-4ecd-857f-6f082ea9f4c6")
                .object("leads", userIds)
                .object("qaTaEngineers", userIds)
                .object("devOpsEngineers", userIds)
                .object("atpSupports", userIds)
                .object("atpRunners", userIds);

        DslPart projectDtoObjList = PactDslJsonArray
                .arrayEachLike()
                .uuid("uuid")
                .datetime("createdWhen", Constants.ISO_DATE_TIME_1)
                .datetime("modifiedWhen", Constants.ISO_DATE_TIME_1)
                .object("createdBy", userInfoDto)
                .object("modifiedBy", userInfoDto)
                .object("permissions", permissionsDto)
                .object("leads", userIds)
                .object("qaTaEngineers", userIds)
                .object("devOpsEngineers", userIds)
                .object("atpSupports", userIds)
                .object("atpRunners", userIds)
                .closeObject();

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /api/v1/users/projects OK")
                .path("/api/v1/users/projects")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(projectDtoObjList)

                .given("all ok")
                .uponReceiving("GET /api/v1/users/projects/5518c918-b816-40a6-aa1d-9efd0df476f8/users OK")
                .path("/api/v1/users/projects/5518c918-b816-40a6-aa1d-9efd0df476f8/users")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(projectUsersDtoObj);

        return response.toPact();
    }

    @Configuration
    public static class TestApp {
    }
}
