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

import java.math.BigInteger;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItfLiteEvent {

    private UUID id;
    private UUID projectId;
    private String itfUrl;
    private BigInteger systemId;
    private BigInteger operationId;
    private BigInteger receiver;

    /**
     * TODO Add JavaDoc.
     */
    public ItfLiteEvent(UUID id, UUID projectId, String itfUrl,
                        BigInteger systemId, BigInteger operationId, BigInteger receiver) {
        this.id = id;
        this.projectId = projectId;
        this.itfUrl = itfUrl;
        this.systemId = systemId;
        this.operationId = operationId;
        this.receiver = receiver;
    }
}
