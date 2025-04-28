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

package org.qubership.automation.itf.transport.jms.outbound;

import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.ADDITIONAL_JNDI_PROPERTIES;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.AUTHENTICATION;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.CONNECTION_FACTORY;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.CREDENTIALS;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.DESTINATION;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.DESTINATION_TYPE;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.INITIAL_CONTEXT_FACTORY;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.JMS_HEADERS;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.PRINCIPAL;
import static org.qubership.automation.itf.transport.jms.outbound.JMSConstants.PROVIDER_URL;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Async;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.helper.Reflection;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;
import org.qubership.automation.itf.transport.jms.JmsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

@Async
@UserName("Outbound JMS Asynchronous")
public class JMSOutboundTransport extends AbstractCamelOutboundTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSOutboundTransport.class);
    private static final String CONFIGURATION_EXCEPTION = "Error configuring transport to send JMS Message";
    private static final String MESSAGE_SENDING_EXCEPTION = "Error sending JMS Message: ";
    private static final String CONNECTING_EXCEPTION = "Error while making Jms outbound connection: ";
    private static final LoadingCache<ConfiguredTransport, JMSConfig> CONFIG_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1L, TimeUnit.HOURS)
            .removalListener((RemovalListener<ConfiguredTransport, JMSConfig>) removalNotification -> {
                if (removalNotification.getCause().equals(RemovalCause.EXPIRED)) {
                    removeComponentById(removalNotification.getKey());
                }
            })
            .build(new CacheLoader<ConfiguredTransport, JMSConfig>() {
                @Override
                public JMSConfig load(@Nonnull ConfiguredTransport id) throws Exception {
                    try {
                        removeComponentById(id);
                        String destinationName = (String) id.getProperties().get(DESTINATION);
                        String destinationType = (String) id.getProperties().get(DESTINATION_TYPE);
                        String componentId = id.getComponentId();
                        JMSConfig jmsConfig = initConnectionProperties(id.getProperties(), componentId,
                                destinationName);
                        ProducerTemplate producer = CAMEL_CONTEXT.createProducerTemplate();
                        producer.start();
                        jmsConfig.setProducer(producer);
                        Endpoint endpoint = makeEndpoint(componentId, destinationType.toLowerCase(), destinationName,
                                jmsConfig, CAMEL_CONTEXT);
                        jmsConfig.setEndpoint(endpoint);
                        return jmsConfig;
                    } catch (Exception e) {
                        LOGGER.error(CONFIGURATION_EXCEPTION, e);
                        throw new Exception(CONFIGURATION_EXCEPTION, e);
                    }
                }
            });
    private static final ScheduledExecutorService configCacheMaintenanceService =
            Executors.newSingleThreadScheduledExecutor();
    private static boolean isCacheCleanupScheduled = false;
    @Parameter(shortName = DESTINATION_TYPE,
            longName = "JMS destination type. Queue/Topic",
            description = PropertyConstants.Jms.DESTINATION_TYPE_DESCRIPTION,
            forServer = false,
            isRedefined = true)
    @Options({"Queue", "Topic"})
    private String destinationType;

    @Parameter(shortName = DESTINATION,
            longName = "Out Data Destination",
            description = PropertyConstants.Jms.DESTINATION_DESCRIPTION,
            forServer = false,
            isRedefined = true)
    private String destination;

    @Parameter(shortName = CONNECTION_FACTORY,
            longName = "Out Connection Factory",
            description = PropertyConstants.Jms.CONNECTION_FACTORY_DESCRIPTION,
            isRedefined = true)
    private String connectionFactoryName;

    @Parameter(shortName = CREDENTIALS,
            longName = "Security Credentials",
            description = PropertyConstants.Commons.CREDENTIALS_DESCRIPTION,
            optional = true)
    private String credentials;

    @Parameter(shortName = PRINCIPAL,
            longName = "Security Principal",
            description = PropertyConstants.Commons.PRINCIPAL_DESCRIPTION,
            optional = true)
    private String principal;

    @Parameter(shortName = INITIAL_CONTEXT_FACTORY,
            longName = "Initial Context Factory",
            description = PropertyConstants.Jms.INITIAL_CONTEXT_FACTORY_DESCRIPTION,
            isRedefined = true)
    private String initialContextFactory;

    @Parameter(shortName = ADDITIONAL_JNDI_PROPERTIES,
            longName = "Additional JNDI Properties",
            description = PropertyConstants.Jms.ADDITIONAL_JNDI_PROPERTIES_DESCRIPTION,
            optional = true)
    private Map<String, String> addJndiProps;

    @Parameter(shortName = AUTHENTICATION,
            longName = "Authentication",
            description = PropertyConstants.Commons.AUTHENTICATION_DESCRIPTION,
            optional = true)
    private String authentication;

    @Parameter(shortName = PROVIDER_URL,
            longName = "Provider URL",
            description = PropertyConstants.Commons.PROVIDER_URL_DESCRIPTION,
            fromServer = true,
            isDynamic = true)
    private String providerUrl;

    @Parameter(shortName = JMS_HEADERS,
            longName = "JMS Headers",
            description = PropertyConstants.Jms.JMS_HEADERS_DESCRIPTION,
            optional = true,
            isDynamic = true)
    private Map<String, String> jmsHeaders;

    private static void removeComponentById(ConfiguredTransport id) {
        String componentId = id.getComponentId();
        if (CAMEL_CONTEXT.hasComponent(componentId) != null) {
            CAMEL_CONTEXT.removeComponent(componentId);
        }
    }

    private static JMSConfig initConnectionProperties(TreeMap<String, Object> connectionProperties,
                                                      String id,
                                                      String destinationName) throws NamingException {
        String connectionFactory = (String) connectionProperties.get(CONNECTION_FACTORY);
        ConnectionFactory factory;
        Destination remoteDestination = null;
        InitialContext initialContext = null;
        try {
            initialContext = createContext(connectionProperties);
            factory = (ConnectionFactory) initialContext.lookup(connectionFactory);
            if (JmsHelper.isJNDIName(destinationName)) {
                remoteDestination = (Destination) initialContext.lookup(destinationName);
            }
        } catch (Exception e) {
            throw new IllegalStateException(CONNECTING_EXCEPTION + e.getMessage());
        } finally {
            if (initialContext != null) {
                initialContext.close();
            }
        }
        JmsComponent component = JmsComponent.jmsComponent(factory);
        CAMEL_CONTEXT.addComponent(id, component);
        return new JMSConfig(component, remoteDestination);
    }

    private static Endpoint makeEndpoint(String id, String destinationType,
                                         String destinationName,
                                         JMSConfig jmsConfig,
                                         CamelContext context) throws JMSException {
        Endpoint endpoint;
        //It's backport for destinationName format like
        // "NCJMSServer_clust1/NCJMSModule!SMF_PRODFULFILLMENT_RMK_RD_clust1"
        if (jmsConfig.getDestination() != null) {
            //So, let's take a destination from JNDI catalog (it can be QUEUE or TOPIC, we don't care about it).
            endpoint = JmsEndpoint.newInstance(jmsConfig.getDestination(), jmsConfig.getComponent());
        } else {
            endpoint = context.getEndpoint(id + ':' + destinationType + ':' + destinationName);
        }
        return endpoint;
    }

    private static InitialContext createContext(TreeMap<String, Object> connectionProperties) throws NamingException {
        Properties env = new Properties();
        JmsHelper.putSafe(env, Context.INITIAL_CONTEXT_FACTORY, connectionProperties.get(INITIAL_CONTEXT_FACTORY));
        JmsHelper.putSafe(env, Context.SECURITY_PRINCIPAL, connectionProperties.get(PRINCIPAL));
        JmsHelper.putSafe(env, Context.SECURITY_CREDENTIALS, connectionProperties.get(CREDENTIALS));
        String[] providers = Reflection.toArray(String.class, (String) connectionProperties.get(PROVIDER_URL));
        if (providers == null || providers.length < 1) {
            throw new IllegalStateException("Provider URL is not defined");
        }
        if (providers[0].isEmpty()) {
            throw new IllegalStateException("Provider URL is empty. Please check Server config (Environment tab).");
        }
        //pick first provider url
        JmsHelper.putSafe(env, Context.PROVIDER_URL, providers[0]);
        JmsHelper.putSafe(env, Context.SECURITY_AUTHENTICATION, connectionProperties.get(AUTHENTICATION));
        if (connectionProperties.get(ADDITIONAL_JNDI_PROPERTIES) != null) {
            env.putAll((Map<?, ?>) connectionProperties.get(ADDITIONAL_JNDI_PROPERTIES));
        }
        return new InitialContext(env);
    }

    @Override
    public String send(final Message message, String sessionId, UUID projectUuid) throws Exception {

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        /*  message.getConnectionProperties() contain "ContextId" property,
            this property prevents using cache for different contexts using the same system/transport and environment,
            so let's clone properties and remove "ContextId" property, instead of using all message properties.
         */
        ConnectionProperties properties = new ConnectionProperties(message.getConnectionProperties());
        properties.remove("ContextId");
        String destinationName = properties.obtain(DESTINATION);
        String destinationType = (String) properties.get(DESTINATION_TYPE);
        if (Strings.isNullOrEmpty(destinationType) || StringUtils.isBlank(destinationName)) {
            throw new IllegalArgumentException("'Destination type' and 'Destination' can't be empty");
        }
        String transportId = (String) (message.getConnectionProperties().get("transportId"));
        ConfiguredTransport configuredTransport = new ConfiguredTransport(transportId, properties);
        JMSConfig jmsConfig = CONFIG_CACHE.get(configuredTransport);
        try {
            Processor processor = getProcessor(message, properties);
            Exchange exchange = jmsConfig.getProducer().send(jmsConfig.getEndpoint(), processor);
            if (exchange.isFailed()) {
                if (exchange.getException().getCause() == null) {
                    throw exchange.getException();
                } else {
                    throw new Exception(exchange.getException().getMessage(), exchange.getException().getCause());
                }
            }
        } catch (Exception e) {
            LOGGER.error(MESSAGE_SENDING_EXCEPTION, e);
            if (e.getCause() == null) {
                throw new Exception(MESSAGE_SENDING_EXCEPTION + e.getMessage());
            } else {
                throw new Exception(MESSAGE_SENDING_EXCEPTION + e.getMessage(), e.getCause());
            }
        } finally {
            scheduleCacheCleanupIfNeeded();
        }
        return null;
    }

    private synchronized void scheduleCacheCleanupIfNeeded() {
        if (!isCacheCleanupScheduled) {
            if (CONFIG_CACHE.size() > 0) {
                configCacheMaintenanceService.scheduleWithFixedDelay(() -> {
                    try {
                        CONFIG_CACHE.cleanUp();
                    } catch (Throwable t) {
                        LOGGER.error("Error while JmsOutboundTransport cache cleanUp: {}", t.toString());
                    }
                }, 61L, 20L, TimeUnit.MINUTES);
                isCacheCleanupScheduled = true;
            }
        }
    }

    private Processor getProcessor(Message message, ConnectionProperties connectionProperties) {
        return exchange -> {
            Map<String, Object> headers = Maps.newHashMapWithExpectedSize(message.getHeaders().size());
            headers.putAll(message.getHeaders());
            Map<String, Object> properties = (Map<String, Object>) connectionProperties.get(JMS_HEADERS);
            if ((properties != null) && (!properties.isEmpty())) {
                headers.putAll(properties);
            }
            exchange.getIn().setHeaders(headers);
            exchange.getIn().setBody(message.getText());
        };
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_ASYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-jms";
    }

    @Override
    public String getShortName() {
        return "JMS Outbound";
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public void start() {
        super.start();
    }

    @Getter
    private class ConfiguredTransport {

        private TreeMap<String, Object> properties;
        private String componentId;
        @Setter
        private String transportId;

        public ConfiguredTransport(String transportId, ConnectionProperties properties) {
            this.transportId = transportId;
            setComponentId();
            this.properties = new TreeMap<>(properties);
        }

        private void setComponentId() {
            this.componentId = "out" + this.transportId + "-" + UUID.randomUUID();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.transportId);
            hash = 97 * hash + Objects.hashCode(this.properties);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConfiguredTransport other = (ConfiguredTransport) obj;
            if (!Objects.equals(this.transportId, other.transportId)) {
                return false;
            }
            return Objects.equals(this.properties, other.properties);
        }
    }
}
