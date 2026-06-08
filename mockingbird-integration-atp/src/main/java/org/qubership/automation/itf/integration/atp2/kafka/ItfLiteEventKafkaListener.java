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

package org.qubership.automation.itf.integration.atp2.kafka;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ItfLiteEventDiameter;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ItfLiteEventRestSoap;
import org.qubership.automation.itf.integration.atp2.kafka.dto.RequestTransportType;
import org.qubership.automation.itf.integration.atp2.kafka.service.impl.ItfLiteExportServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(value = "kafka.enable")
@RequiredArgsConstructor
public class ItfLiteEventKafkaListener implements ProjectEventListener {

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
    }

    private final ItfLiteExportServiceImpl itfLiteExportService;
    private final CoreObjectManagerService coreObjectManagerService;
    @Value("${atp.multi-tenancy.enabled}")
    private boolean multiTenancyEnabled;

    @Override
    @KafkaListener(topics = "${kafka.topic.itf.lite.export.start}")
    public void listen(@Payload String event) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(event);
            if (jsonNode.get("itfUrl").asText().contains("atp-itf-executor")) {
                String projectUuid = jsonNode.get("projectId").asText();
                if (multiTenancyEnabled) {
                    TenantContext.setTenantInfo(projectUuid);
                    processEvent(event, jsonNode, projectUuid);
                    TenantContext.setDefaultTenantInfo();
                } else {
                    processEvent(event, jsonNode, projectUuid);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong while processing itf lite export event: '" + event + "'"
                    , e);
        }
    }

    private void processEvent(String event, JsonNode jsonNode, String projectUuid) throws JsonProcessingException,
            IllegalArgumentException {
        checkProjectExist(projectUuid);
        String transportType = jsonNode.get("request").get("transportType").asText();
        switch (RequestTransportType.valueOf(transportType.toUpperCase())) {
            case REST:
            case SOAP: {
                ItfLiteEventRestSoap itfLiteEventRestSoap = MAPPER.readValue(event, ItfLiteEventRestSoap.class);
                MdcUtils.put(MdcField.PROJECT_ID.toString(), itfLiteEventRestSoap.getProjectId());
                itfLiteExportService.createTemplateFromRestSoapTransportType(event, itfLiteEventRestSoap);
                break;
            }
            case DIAMETER: {
                ItfLiteEventDiameter itfLiteEventDiameter = MAPPER.readValue(event, ItfLiteEventDiameter.class);
                MdcUtils.put(MdcField.PROJECT_ID.toString(), itfLiteEventDiameter.getProjectId());
                itfLiteExportService.createTemplateFromDiameterTransportType(event, itfLiteEventDiameter);
                break;
            }
            default: {
                throw new RuntimeException("Unknown transport event type: " + transportType);
            }
        }
    }

    private void checkProjectExist(String projectUuid) {
        BigInteger projectId = coreObjectManagerService.getSpecialManager(StubProject.class, SearchManager.class)
                .getEntityInternalIdByUuid(UUID.fromString(projectUuid));
        if (Objects.isNull(projectId)) {
            throw new IllegalArgumentException("Project id was not found for the specified project UUID: "
                    + projectUuid);
        }
        StubProject project = coreObjectManagerService.getManager(StubProject.class).getById(projectId);
        if (Objects.isNull(project)) {
            throw new IllegalArgumentException("Project is not found by id: " + projectId);
        }
    }
}
