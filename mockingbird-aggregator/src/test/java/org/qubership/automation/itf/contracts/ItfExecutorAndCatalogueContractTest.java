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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.automation.itf.executor.objects.ei.SimpleItfEntity;
import org.qubership.automation.itf.ui.controls.AtpExportImportController;
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
@PactUrl(urls = {"classpath:pacts/atp-catalogue-atp-itf-executor.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {AtpExportImportController.class})
@ContextConfiguration(classes = {ItfExecutorAndCatalogueContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        AtpExportImportController.class,
})
@TestPropertySource(locations = "classpath:bootstrap-test.properties")
public class ItfExecutorAndCatalogueContractTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AtpExportImportController atpExportImportController;

    private void beforeAll() {
        when(atpExportImportController.getRootCallchainFolderByAtpExport(any(UUID.class)))
                .thenReturn(getResponseSimpleItfEntity());
        when(atpExportImportController.getCallchainSubFoldersByAtpExport(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getCallchainsByAtpExport(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getRootSystemFolderByAtpExport(any(UUID.class)))
                .thenReturn(getResponseSimpleItfEntity());
        when(atpExportImportController.getSystemSubFoldersByAtpExport(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getSystemsByAtpExport(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getRootEnvironmentFolderByAtpExport(any(UUID.class)))
                .thenReturn(getResponseSimpleItfEntity());
        when(atpExportImportController.getEnvironmentFoldersByAtpExport(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getEnvironmentsByAtpExport(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getIntegrationConfigsByProjectId(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getProjectSettingsByProjectId(any(UUID.class)))
                .thenReturn(getResponseListSimpleItfEntity());
        when(atpExportImportController.getBvTcByItfChains(any())).thenReturn(getResponseListUuid());
        when(atpExportImportController.getBvTcByItfSystems(any())).thenReturn(getResponseListUuid());
        when(atpExportImportController.getBvTcByItfEnvs(any())).thenReturn(getResponseListUuid());
        when(atpExportImportController.getDslByItfChains(any())).thenReturn(getResponseListUuid());
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

    private SimpleItfEntity getResponseSimpleItfEntity() {
        SimpleItfEntity entity = new SimpleItfEntity();
        entity.setName("entityDto");
        entity.setId("9167234930111872003");
        entity.setParentId("9167234930111872004");
        return entity;
    }

    private List<SimpleItfEntity> getResponseListSimpleItfEntity() {
        List<SimpleItfEntity> entities = new ArrayList<>();
        entities.add(getResponseSimpleItfEntity());
        return entities;
    }

    private List<UUID> getResponseListUuid() {
        List<UUID> response = new ArrayList<>();
        response.add(UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9702"));
        response.add(UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9703"));
        return response;
    }

    @Configuration
    public static class TestApp {
    }
}
