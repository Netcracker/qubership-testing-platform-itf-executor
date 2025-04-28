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

package org.qubership.automation.itf.transport.kafka.outbound;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.BROKERS;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.BROKERS_DESCRIPTION;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.HEADERS;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.HEADERS_DESCRIPTION;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.MESSAGE_KEY;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.MESSAGE_KEY_DESCRIPTION;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.TOPIC;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Kafka.TOPIC_DESCRIPTION;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.KafkaEndpoint;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.camel.Helper;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;

@UserName("Outbound Kafka Asynchronous")
public class KafkaOutboundTransport extends AbstractCamelOutboundTransport {
    private static final List<String> AUTHORIZATION_PARAMS
            = Arrays.asList("securityProtocol", "saslMechanism", "saslModule", "saslUsername", "saslPassword");
    @Parameter(shortName = BROKERS, longName = "Brokers",
            description = BROKERS_DESCRIPTION, isDynamic = true)
    protected String brokers;
    @Parameter(shortName = TOPIC, longName = "Topic",
            description = TOPIC_DESCRIPTION, forTemplate = true, isDynamic = true)
    protected String topic;
    @Parameter(shortName = HEADERS, longName = "Kafka Message Headers",
            description = HEADERS_DESCRIPTION, optional = true, forTemplate = true, isDynamic = true)
    protected Map<String, String> headers = new HashMap<>();
    @Parameter(shortName = MESSAGE_KEY, longName = "Kafka Message Key",
            description = MESSAGE_KEY_DESCRIPTION, optional = true, forTemplate = true, isDynamic = true)
    protected String key;
    @Parameter(shortName = PropertyConstants.Commons.ENDPOINT_PROPERTIES, longName = "Extra Endpoint Properties",
            description = PropertyConstants.Commons.ENDPOINT_PROPERTIES_DESCRIPTION,
            forServer = true, forTemplate = false, isDynamic = true, optional = true)
    protected Map<String, Object> properties;

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_ASYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-kafka";
    }

    @Override
    public String getShortName() {
        return "Kafka Outbound";
    }

    @Override
    public String send(final Message message, String sessionId, UUID projectUuid) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        Endpoint endpoint = createKafkaEndpoint(message, projectUuid);
        Producer producer = endpoint.createProducer();
        try {
            producer.start();
            producer.process(createExchange(endpoint, message));
        } finally {
            producer.stop();
        }
        return null;
    }

    private Endpoint createKafkaEndpoint(Message message, UUID projectUuid) {
        ConnectionProperties properties = new ConnectionProperties(message.getConnectionProperties());
        String topic = properties.get(TOPIC).toString().trim();
        String brokers = properties.get(BROKERS).toString().trim();
        Map<String, Object> extraProps =
                Helper.setExtraPropertiesMap(properties.obtain(PropertyConstants.Commons.ENDPOINT_PROPERTIES));

        boolean isAuthParametersValid = checkAuthParameters(extraProps);
        StringBuilder builder = new StringBuilder("kafka:").append(topic).append("?brokers=").append(brokers);
        Map<String, String> authProps = isAuthParametersValid ? fillAuthParameters(extraProps) : null;

        String endpointUri = builder.append(Helper.setExtraProperties(extraProps)).toString();
        KafkaEndpoint endpoint = new KafkaEndpoint(endpointUri, new KafkaComponent(CAMEL_CONTEXT));
        endpoint.getConfiguration().setTopic(topic);
        endpoint.getConfiguration().setBrokers(brokers);
        endpoint.getConfiguration().setProducerBatchSize(0);
        endpoint.getConfiguration().setClientId(projectUuid.toString());
        if (isAuthParametersValid) {
            setAuthParameters(endpoint, authProps);
        }
        return endpoint;
    }

    private Exchange createExchange(Endpoint endpoint, Message message) {
        Exchange exchange = endpoint.createExchange();
        exchange.getIn().setHeaders(message.getHeaders());
        exchange.getIn().setHeader(KafkaConstants.KEY, message.getConnectionProperties().get(MESSAGE_KEY));
        exchange.getIn().setBody(message.getText());
        return exchange;
    }

    private Map<String, String> fillAuthParameters(Map<String, Object> extraProps) {
        Map<String, String> authProps = new HashMap<>();
        for (String param : AUTHORIZATION_PARAMS) {
            authProps.put(param, extraProps.get(param).toString());
            extraProps.remove(param);
        }
        return authProps;
    }

    private void setAuthParameters(KafkaEndpoint endpoint, Map<String, String> authProps) {
        if (!authProps.isEmpty()) {
            String saslJaasConfig = String.format("%s required username=\"%s\" password=\"%s\";",
                    authProps.get("saslModule"), authProps.get("saslUsername"), authProps.get("saslPassword"));
            endpoint.getConfiguration().setSaslJaasConfig(saslJaasConfig);
            endpoint.getConfiguration().setSaslMechanism(authProps.get("saslMechanism"));
            endpoint.getConfiguration().setSecurityProtocol(authProps.get("securityProtocol"));
        }
    }

    private boolean checkAuthParameters(Map<String, Object> extraProps) {
        for (String param : AUTHORIZATION_PARAMS) {
            if (!extraProps.containsKey(param)) {
                return false;
            }
        }
        return true;
    }
}
