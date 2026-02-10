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

package org.qubership.automation.itf.transport.cli.outbound;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.netty4.NettyComponent;
import org.apache.camel.component.netty4.NettyEndpoint;
import org.apache.camel.component.ssh.SshComponent;
import org.apache.logging.log4j.util.Strings;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Async;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@UserName("CLI Outbound TCP/IP")
@Async
@Slf4j
public class CLIOutboundTransport extends AbstractCamelOutboundTransport {

    private static final String SSH_COMPONENT_KEY = "ssh";
    private static final String NETTY_COMPONENT_KEY = "netty4";
    private static final Cache<ConfiguredTransport, CLIConfig> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(5, MINUTES)
            .removalListener((RemovalListener<ConfiguredTransport, CLIConfig>) notification -> {
                CLIConfig cliConfig = notification.getValue();
                try {
                    CAMEL_CONTEXT.removeEndpoint(cliConfig.getEndpoint());
                    cliConfig.getEndpoint().stop();
                } catch (Throwable t) {
                    log.error("Error while Camel Context cleaning up", t);
                }
            }).build();
    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    static {
        service.scheduleWithFixedDelay(() -> {
            try {
                CACHE.cleanUp();
                if (CACHE.size() == 0) {
                    template.stop();
                    CAMEL_CONTEXT.removeComponent(SSH_COMPONENT_KEY);
                    CAMEL_CONTEXT.removeComponent(NETTY_COMPONENT_KEY);
                    template.start();
                }
            } catch (Throwable t) {
                log.error("Error while Cache cleaning up", t);
            }
        }, 10, 5, MINUTES);
    }

    @Parameter(shortName = PropertyConstants.Cli.REMOTE_IP, longName = "Remote IP", description = "Remote host IP",
            isDynamic = true)
    private String endpoint;

    @Parameter(shortName = PropertyConstants.Cli.CONNECTION_TYPE, longName = "Type of Connection",
            description = "TCP, UDP or SSH type")
    @Options({"UDP", "TCP", "SSH"})
    private String type;

    @Parameter(shortName = PropertyConstants.Cli.REMOTE_PORT, longName = "Remote Port",
            description = "Remote host port")
    private Integer port;

    @Parameter(shortName = PropertyConstants.Cli.USER, longName = "User", description = "Remote user name",
            optional = true)
    private String user;

    @Parameter(shortName = PropertyConstants.Cli.PASSWORD, longName = "Password", description = "Remote password",
            optional = true)
    private String password;

    @Parameter(shortName = PropertyConstants.Cli.SSH_KEY, longName = "Ssh key", description = "Ssh key",
            optional = true)
    private List<String> sshKey;

    @Parameter(shortName = PropertyConstants.Cli.WAIT_RESPONSE, longName = "Should wait response?",
            description = "Should wait response? - Yes (default) / No", optional = true, forTemplate = true)
    @Options({"Yes", "No"})
    private String waitResponse;

    @Override
    public String getShortName() {
        return "Cli outbound";
    }

    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ProducerTemplate producerTemplate;
        Endpoint cliEndPoint;
        ConnectionProperties properties = new ConnectionProperties(message.getConnectionProperties());
        properties.remove("ContextId");
        String transportId = (String) (properties.get("transportId"));
        ConfiguredTransport configuredTransport = new ConfiguredTransport(transportId, properties);
        Integer hash = configuredTransport.hashCode();
        synchronized (hash) {
            CLIConfig cliConfig = CACHE.getIfPresent(configuredTransport);
            if (cliConfig == null) {
                try {
                    String componentId;
                    boolean isSsh = "SSH".equals(properties.get(PropertyConstants.Cli.CONNECTION_TYPE));
                    if (isSsh) {
                        componentId = SSH_COMPONENT_KEY;
                        addSshComponent();
                        cliEndPoint = CAMEL_CONTEXT.getEndpoint(buildUri(properties, true));
                    } else {
                        componentId = NETTY_COMPONENT_KEY;
                        addNettyComponent();
                        cliEndPoint = CAMEL_CONTEXT.getEndpoint(buildUri(properties, false));
                        if ("Yes".equals(properties.getOrDefault(PropertyConstants.Cli.WAIT_RESPONSE, "Yes"))) {
                            ((NettyEndpoint) cliEndPoint).setExchangePattern(ExchangePattern.InOut);
                            ((NettyEndpoint) cliEndPoint).getConfiguration().setSync(true);
                        } else {
                            ((NettyEndpoint) cliEndPoint).setExchangePattern(ExchangePattern.OutOnly);
                            ((NettyEndpoint) cliEndPoint).getConfiguration().setSync(false);
                        }
                    }
                    log.debug("{} Endpoint is: {}", properties.get(PropertyConstants.Cli.CONNECTION_TYPE), cliEndPoint);
                    producerTemplate = template;
                    cliConfig = new CLIConfig(CAMEL_CONTEXT.getComponent(componentId), producerTemplate, cliEndPoint);
                    CACHE.put(configuredTransport, cliConfig);
                } catch (Exception e) {
                    throw new Exception("Unable to configure CLI endpoint", e);
                }
            } else {
                cliEndPoint = cliConfig.getEndpoint();
                producerTemplate = cliConfig.getProducer();
            }
        }
        log.info("Getting response from: {}", cliEndPoint);
        Exchange exchange = cliEndPoint.createExchange();
        exchange.getIn().setBody(message.getText());
        exchange = producerTemplate.send(cliEndPoint, exchange);
        if (!exchange.hasOut() || exchange.getOut().isFault()) {
            return makeExceptionMessage(exchange);
        } else {
            Message response;
            Object answerObject = exchange.getOut().getBody();
            if (answerObject == null) {
                response = new Message();
            } else if (answerObject instanceof ByteArrayInputStream) {
                ByteArrayInputStream answer = (ByteArrayInputStream) answerObject;
                int n = answer.available();
                if (n > 0) {
                    byte[] bytes = new byte[n];
                    int cnt = answer.read(bytes, 0, n);
                    response = new Message(new String(bytes, 0, cnt, StandardCharsets.UTF_8));
                } else {
                    response = new Message();
                }
            } else {
                response = new Message(answerObject.toString());
            }
            if (exchange.getOut().hasHeaders()) {
                response.convertAndSetHeaders(exchange.getOut().getHeaders());
            }
            log.debug("Response from {} is: {}", cliEndPoint, response.getText());
            return response;
        }
    }

    private synchronized void addSshComponent() {
        SshComponent sshComponent = (SshComponent) CAMEL_CONTEXT.hasComponent(SSH_COMPONENT_KEY);
        if (sshComponent == null) {
            sshComponent = new SshComponent();
            CAMEL_CONTEXT.addComponent(SSH_COMPONENT_KEY, sshComponent);
        }
    }

    private synchronized void addNettyComponent() {
        NettyComponent nettyComponent = (NettyComponent) CAMEL_CONTEXT.hasComponent(NETTY_COMPONENT_KEY);
        if (nettyComponent == null) {
            nettyComponent = new NettyComponent();
            CAMEL_CONTEXT.addComponent(NETTY_COMPONENT_KEY, nettyComponent);
        }
    }

    private Message makeExceptionMessage(Exchange exchange) {
        Message message = new Message();
        Exception ex = exchange.getException();
        if (ex != null) {
            message.setFailedMessage(
                    ex.getMessage() + ((ex.getMessage().contains("Caused by:") || ex.getCause() == null)
                            ? "" : "Caused by: " + ex.getCause().getMessage())
            );
        } else {
            message.setFailedMessage("Unknown exception is occurred");
        }
        if (exchange.hasOut() && exchange.getOut().hasHeaders()) {
            message.convertAndSetHeaders(exchange.getOut().getHeaders());
        }
        return message;
    }

    private String buildUri(ConnectionProperties properties, boolean isSsh) throws IOException {
        StringBuilder uri = new StringBuilder();
        if (isSsh) {
            composeSshUri(properties, uri);
            return uri.toString();
        }
        return uri.append("netty4:")
                .append(properties.get(PropertyConstants.Cli.CONNECTION_TYPE))
                .append("://").append(properties.get(PropertyConstants.Cli.REMOTE_IP))
                .append(':').append(properties.get(PropertyConstants.Cli.REMOTE_PORT))
                .append("?textline=true&requestTimeout=5000")
                .toString();
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-cli";
    }

    private void composeSshUri(ConnectionProperties properties, StringBuilder uri) throws IOException {
        Object user = properties.get(PropertyConstants.Cli.USER);
        Object password = properties.get(PropertyConstants.Cli.PASSWORD);
        Object sshKeyObj = properties.get(PropertyConstants.Cli.SSH_KEY);
        //noinspection unchecked
        String sshKey = Objects.nonNull(sshKeyObj)
                ? String.join("\n", ((List<String>) sshKeyObj))
                : Strings.EMPTY;
        boolean userIsBlank = Objects.isNull(user) || Strings.isBlank(String.valueOf(user));
        boolean passIsBlank = Objects.isNull(password) || Strings.isBlank(String.valueOf(password));
        boolean sshKeyIsBlank = Strings.isBlank(sshKey);
        if (passIsBlank && sshKeyIsBlank) {
            throw new IllegalArgumentException("Password/ssh_key can't be empty! Please fill one of them.");
        }
        if (userIsBlank) {
            throw new IllegalArgumentException("User can't be empty!");
        }
        uri.append("ssh:")
                .append(user)
                .append((passIsBlank) ? '@' : ':' + password.toString() + '@')
                .append(properties.get(PropertyConstants.Cli.REMOTE_IP))
                .append(':').append(properties.get(PropertyConstants.Cli.REMOTE_PORT))
                .append((sshKeyIsBlank) ? "" : "?certResource=file:" + getTempPemFile(sshKey).getPath());
    }

    @Nonnull
    private File getTempPemFile(String sshKey) throws IOException {
        File tmpfile = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".pem");
        tmpfile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));
        writer.write(sshKey);
        writer.close();
        return tmpfile;
    }

    @Getter
    protected static class CLIConfig {

        private final Component component;
        @lombok.Setter
        private ProducerTemplate producer;
        @lombok.Setter
        private Endpoint endpoint;

        public CLIConfig(Component component) {
            this.component = component;
        }

        public CLIConfig(Component component, ProducerTemplate producer) {
            this.component = component;
            this.producer = producer;
        }

        public CLIConfig(Component component, ProducerTemplate producer, Endpoint endpoint) {
            this.component = component;
            this.producer = producer;
            this.endpoint = endpoint;
        }

    }

    @Getter
    private class ConfiguredTransport {

        TreeMap<String, Object> properties;
        @lombok.Setter
        private String transportId;

        public ConfiguredTransport() {
            this.transportId = "";
            this.properties = new TreeMap<>();
        }

        public ConfiguredTransport(String transportId, ConnectionProperties properties) {
            this.transportId = transportId;
            this.properties = new TreeMap<>(properties);
        }

        public void setProperties(ConnectionProperties properties) {
            this.properties = new TreeMap<>(properties);
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
