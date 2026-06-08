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

package org.qubership.automation.itf.integration.atp2.kafka.dto.messages.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiameterRequestData extends ItfLiteRequestData {

    private String host;
    private String port;
    private String capabilitiesExchangeRequest;
    private String watchdogDefaultTemplate;
    private String connectionLayer;
    private String responseType;
    private String messageFormat;
    private String dictionaryType;
    private String responseTimeout;
    private BodyData body;

    /**
     * TODO Add JavaDoc.
     */
    public DiameterRequestData(@JsonProperty("id") UUID id,
                               @JsonProperty("name") String name,
                               @JsonProperty("transportType") String transportType,
                               @JsonProperty("host") String host,
                               @JsonProperty("port") String port,
                               @JsonProperty("capabilitiesExchangeRequest") String capabilitiesExchangeRequest,
                               @JsonProperty("watchdogDefaultTemplate") String watchdogDefaultTemplate,
                               @JsonProperty("connectionLayer") String connectionLayer,
                               @JsonProperty("responseType") String responseType,
                               @JsonProperty("messageFormat") String messageFormat,
                               @JsonProperty("dictionaryType") String dictionaryType,
                               @JsonProperty("responseTimeout") String responseTimeout,
                               @JsonProperty("body") BodyData body) {
        super(id, name, transportType);
        this.host = host;
        this.port = port;
        this.capabilitiesExchangeRequest = capabilitiesExchangeRequest;
        this.watchdogDefaultTemplate = watchdogDefaultTemplate;
        this.connectionLayer = connectionLayer;
        this.responseType = responseType;
        this.messageFormat = messageFormat;
        this.dictionaryType = dictionaryType;
        this.responseTimeout = responseTimeout;
        this.body = body;
    }
}
