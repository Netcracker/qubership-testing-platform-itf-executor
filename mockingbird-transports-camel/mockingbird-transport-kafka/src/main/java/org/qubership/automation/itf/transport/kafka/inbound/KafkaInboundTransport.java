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

package org.qubership.automation.itf.transport.kafka.inbound;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.BROKERS;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.BROKERS_DESCRIPTION;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.GROUP;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.GROUP_DESCRIPTION;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.TOPIC;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.TOPIC_DESCRIPTION;

import java.util.Map;

import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;

@UserName("Inbound Kafka Asynchronous")
public class KafkaInboundTransport extends AbstractInboundTransportImpl {
    @Parameter(shortName = BROKERS, longName = "Brokers",
            description = BROKERS_DESCRIPTION)
    protected String brokers;

    @Parameter(shortName = TOPIC, longName = "Topic",
            description = TOPIC_DESCRIPTION)
    protected String topic;

    @Parameter(shortName = GROUP, longName = "Group",
            description = GROUP_DESCRIPTION, optional = true)
    protected String groupId;

    @Parameter(shortName = PropertyConstants.Commons.ENDPOINT_PROPERTIES, longName = "Extra Endpoint Properties",
            description = PropertyConstants.Commons.ENDPOINT_PROPERTIES_DESCRIPTION,
            forServer = true, forTemplate = false, isDynamic = true, optional = true)
    protected Map<String, Object> properties;

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_ASYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-kafka";
    }

    @Override
    public String getShortName() {
        return "Kafka Inbound";
    }
}

