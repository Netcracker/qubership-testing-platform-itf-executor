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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ProjectEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectEventTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String eventJson = "{"
            + "\"projectId\": \"7619f7fb-6a5c-4405-907b-526baf0e9c69\","
            + "\"projectName\": \"ANKU Test Project 3\","
            + "\"datasetFormat\": \"Default\","
            + "\"type\": \"CREATE\","
            + "\"dateFormat\": \"d MMM yyyy\","
            + "\"timeFormat\": \"hh:mm:ss a\","
            + "\"timeZone\": \"GMT+03:00\""
            + "}";

    private static final String eventJsonWithUnrecognizedField = "{"
            + "\"projectId\": \"7619f7fb-6a5c-4405-907b-526baf0e9c69\","
            + "\"projectName\": \"ANKU Test Project 3\","
            + "\"datasetFormat\": \"Default\","
            + "\"type\": \"CREATE\","
            + "\"dateFormat\": \"d MMM yyyy\","
            + "\"timeFormat\": \"hh:mm:ss a\","
            + "\"timeZone\": \"GMT+03:00\","
            + "\"some_parameter\": \"some_value\"" //event hasn't got the time
            + "}";

    @Test
    public void testJsonParsing() throws JsonProcessingException {
        ProjectEvent projectEvent = objectMapper.readValue(eventJson, ProjectEvent.class);
        assertEquals(projectEvent.getProjectId().toString(), "7619f7fb-6a5c-4405-907b-526baf0e9c69");
    }

    @Test
    public void testJsonParsingWithUnrecognizedField() throws JsonProcessingException {
        ProjectEvent projectEvent = objectMapper.readValue(eventJsonWithUnrecognizedField, ProjectEvent.class);
        assertEquals(projectEvent.getProjectId().toString(), "7619f7fb-6a5c-4405-907b-526baf0e9c69");
    }
}
