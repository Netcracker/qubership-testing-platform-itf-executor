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

import java.io.IOException;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ProjectEvent;
import org.qubership.automation.itf.integration.atp2.kafka.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableKafka
@Component
@ComponentScan("org.qubership.automation.itf.integration.atp2.kafka")
@ConditionalOnProperty(value = "kafka.enable")
public class ProjectEventKafkaListener implements ProjectEventListener {
    private final ObjectMapper objectMapper = initMapper();
    private final ProjectService projectService;

    @Autowired
    public ProjectEventKafkaListener(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    @KafkaListener(id = "${spring.kafka.consumer.group-id}", topics = "${kafka.topic}")
    public void listen(@Payload String event) {
        ProjectEvent projectEvent;
        try {
            projectEvent = objectMapper.readValue(event, ProjectEvent.class);
        } catch (IOException e) {
            throw new RuntimeException("Can't parse project event received: '" + event + "'", e);
        }
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectEvent.getProjectId());
        switch (projectEvent.getType()) {
            case CREATE: {
                projectService.create(event, projectEvent);
                break;
            }
            case UPDATE: {
                projectService.update(event, projectEvent);
                break;
            }
            case DELETE: {
                projectService.delete(event, projectEvent);
                break;
            }
            default: {
                throw new RuntimeException("Unknown project event type: " + projectEvent.getType());
            }
        }
    }

    private ObjectMapper initMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
