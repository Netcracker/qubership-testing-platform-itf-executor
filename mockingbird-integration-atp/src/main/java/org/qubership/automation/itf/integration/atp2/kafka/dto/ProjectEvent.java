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

package org.qubership.automation.itf.integration.atp2.kafka.dto;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.UUID;

import org.qubership.automation.itf.core.util.constants.DatasetFormat;

//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectEvent {
    private UUID projectId;
    private String projectName;
    private DatasetFormat datasetFormat;
    private EventType type;
    private SimpleDateFormat dateFormat;
    private DateTimeFormatter timeFormat;
    private TimeZone timeZone;

    /**
     * TODO Add JavaDoc.
     */
    /*
    @JsonCreator
    public ProjectEvent(@JsonProperty("projectId") UUID projectId,
                        @JsonProperty("projectName") String projectName,
                        @JsonProperty("datasetFormat") DatasetFormat datasetFormat,
                        @JsonProperty("type") EventType type,
                        @JsonProperty("dateFormat") SimpleDateFormat dateFormat,
                        @JsonProperty("timeFormat") String timeFormat,
                        @JsonProperty("timeZone") String timeZone) {
        super();
        this.projectId = projectId;
        this.projectName = projectName;
        this.datasetFormat = datasetFormat;
        this.type = type;
        this.dateFormat = dateFormat;
        this.timeFormat = DateTimeFormatter.ofPattern(timeFormat);
        this.timeZone = TimeZone.getTimeZone(timeZone);
    }*/

    public ProjectEvent(@JsonProperty("projectId") String projectId,
                        @JsonProperty("projectName") String projectName,
                        @JsonProperty("datasetFormat") String datasetFormat,
                        @JsonProperty("type") String type,
                        @JsonProperty("dateFormat") String dateFormat,
                        @JsonProperty("timeFormat") String timeFormat,
                        @JsonProperty("timeZone") String timeZone) {
        super();
        this.projectId = UUID.fromString(projectId);
        this.projectName = projectName;
        this.datasetFormat = DatasetFormat.valueOf(datasetFormat.toUpperCase());
        this.type = EventType.valueOf(type.toUpperCase());
        this.dateFormat = new SimpleDateFormat(dateFormat);
        this.timeFormat = DateTimeFormatter.ofPattern(timeFormat);
        this.timeZone = TimeZone.getTimeZone(timeZone);
    }
}
