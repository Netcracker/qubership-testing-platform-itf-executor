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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.component.netty4.NettyComponent;
import org.apache.camel.component.netty4.NettyEndpoint;
import org.apache.camel.component.ssh.SshComponent;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Async;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

@UserName("CLI Outbound TCP/IP")
@Async
@Slf4j
public class CLIOutboundTransport extends AbstractCamelOutboundTransport {

    private static final String SSH_COMPONENT_KEY = "ssh";
    private static final String NETTY_COMPONENT_KEY = "netty4";
    private static final int DEFAULT_REQUEST_TIMEOUT = 5000;
    private static final String CONNECTION_TYPE_SSH = "SSH";
    private static final LoadingCache<String, Object> LOCKS = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(CacheLoader.from(Object::new));

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
        Endpoint cliEndPoint;
        Map<String, Object> properties = message.getConnectionProperties();
        String lockKey = buildLockKey(properties);
        Object lock = LOCKS.get(lockKey);
        synchronized (lock) {
            try {
                boolean isSsh = CONNECTION_TYPE_SSH.equals(properties.get(PropertyConstants.Cli.CONNECTION_TYPE));
                addComponentIfAbsent(isSsh);
                cliEndPoint = CAMEL_CONTEXT.getEndpoint(buildUri(properties, isSsh));
                if (!isSsh) {
                    NettyEndpoint nettyEndpoint = (NettyEndpoint) cliEndPoint;
                    if ("Yes".equals(properties.getOrDefault(PropertyConstants.Cli.WAIT_RESPONSE, "Yes"))) {
                        nettyEndpoint.setExchangePattern(ExchangePattern.InOut);
                        nettyEndpoint.getConfiguration().setSync(true);
                    } else {
                        nettyEndpoint.setExchangePattern(ExchangePattern.OutOnly);
                        nettyEndpoint.getConfiguration().setSync(false);
                    }
                }
                log.debug("{} Endpoint is: {}", properties.get(PropertyConstants.Cli.CONNECTION_TYPE), cliEndPoint);
            } catch (Exception e) {
                throw new Exception("Unable to configure CLI endpoint", e);
            }
        }
        log.info("Getting response from: {} ...", cliEndPoint);
        Exchange exchange = cliEndPoint.createExchange();
        exchange.getIn().setBody(message.getText());
        exchange = PRODUCER_TEMPLATE.send(cliEndPoint, exchange); // If needed, future with timeout can be used.
        if (!exchange.hasOut() || exchange.getOut().isFault()) {
            return makeExceptionMessage(exchange);
        } else {
            Message response = getResponseMessage(exchange);
            log.debug("Response from {} is: {}", cliEndPoint, response.getText());
            return response;
        }
    }

    @NotNull
    private static Message getResponseMessage(Exchange exchange) {
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
        return response;
    }

    private String buildLockKey(Map<String, Object> connectionProperties) {
        // Make TreeMap because it preserves map keys order
        TreeMap<String, Object> properties = new TreeMap<>(connectionProperties);

        // Remove 'ContextId' property because it's about a specific run, not about configuration.
        properties.remove("ContextId");

        // Compose lockKey as simple String like {key1=value1, key2=value2, ...}
        return properties.toString();
    }

    private void addComponentIfAbsent(boolean isSsh) {
        String componentKey = isSsh ? SSH_COMPONENT_KEY : NETTY_COMPONENT_KEY;
        if (CAMEL_CONTEXT.hasComponent(componentKey) == null) {
            synchronized (CLIOutboundTransport.class) {
                if (CAMEL_CONTEXT.hasComponent(componentKey) == null) {
                    CAMEL_CONTEXT.addComponent(componentKey,
                            isSsh ? new SshComponent() : new NettyComponent());
                }
            }
        }
    }

    private Message makeExceptionMessage(Exchange exchange) {
        Message message = new Message();
        Exception ex = exchange.getException();
        if (ex != null) {
            String exceptionMessage = ex.getMessage();
            if (exceptionMessage == null || exceptionMessage.trim().isEmpty()) {
                exceptionMessage = ex.getClass().getSimpleName();
            }
            message.setFailedMessage(
                    exceptionMessage + ((exceptionMessage.contains("Caused by:") || ex.getCause() == null)
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

    private String buildUri(Map<String, Object> properties, boolean isSsh) throws IOException {
        return isSsh ? buildSshUri(properties) : buildNettyUri(properties);
    }

    private String buildSshUri(Map<String, Object> properties) throws IOException {
        StringBuilder uri = new StringBuilder();
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
        return uri.toString();
    }

    private String buildNettyUri(Map<String, Object> properties) {
        return "netty4:"
                + properties.get(PropertyConstants.Cli.CONNECTION_TYPE)
                + "://" + properties.get(PropertyConstants.Cli.REMOTE_IP)
                + ':' + properties.get(PropertyConstants.Cli.REMOTE_PORT)
                + "?textline=true&requestTimeout="
                + DEFAULT_REQUEST_TIMEOUT;
    }

    @Nonnull
    private File getTempPemFile(String sshKey) throws IOException {
        File tmpfile = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".pem");
        tmpfile.deleteOnExit();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile))) {
            writer.write(sshKey);
        }
        return tmpfile;
    }

}
