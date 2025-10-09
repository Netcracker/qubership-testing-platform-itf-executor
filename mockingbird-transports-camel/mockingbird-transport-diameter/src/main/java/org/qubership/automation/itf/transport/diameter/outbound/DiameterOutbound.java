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

package org.qubership.automation.itf.transport.diameter.outbound;

import static org.qubership.automation.itf.transport.diameter.DiameterSessionTypes.CREDIT_CONTROL_SESSION;
import static org.qubership.automation.itf.transport.diameter.DiameterSessionTypes.SY_SESSION;
import static org.qubership.automation.itf.transport.diameter.interceptors.ExternalInterceptor.InterceptorFactory;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.automation.diameter.config.ConfigReader;
import org.qubership.automation.diameter.config.DiameterParser;
import org.qubership.automation.diameter.config.DiameterParserType;
import org.qubership.automation.diameter.connection.ConnectionFactory;
import org.qubership.automation.diameter.connection.DiameterConnection;
import org.qubership.automation.diameter.connection.ExtraChannel;
import org.qubership.automation.diameter.connection.TransportType;
import org.qubership.automation.diameter.data.Encoder;
import org.qubership.automation.diameter.data.XmlDecoder;
import org.qubership.automation.diameter.dictionary.DictionaryConfig;
import org.qubership.automation.diameter.interceptor.CEAInterceptor;
import org.qubership.automation.diameter.interceptor.DPRInterceptor;
import org.qubership.automation.diameter.interceptor.DWRInterceptor;
import org.qubership.automation.diameter.interceptor.Interceptor;
import org.qubership.automation.diameter.interceptor.InterceptorTypes;
import org.qubership.automation.itf.core.model.diameter.DiameterConnectionInfo;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.DiameterConnectionInfoProvider;
import org.qubership.automation.itf.core.util.DiameterSessionHolder;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractOutboundTransportImpl;
import org.qubership.automation.itf.transport.diameter.DiameterSessionTypes;
import org.qubership.automation.itf.transport.diameter.EncoderFactory;
import org.qubership.automation.itf.transport.diameter.interceptors.ASRInterceptor;
import org.qubership.automation.itf.transport.diameter.interceptors.RARInterceptor;
import org.qubership.automation.itf.transport.diameter.interceptors.SNRInterceptor;
import org.qubership.automation.itf.transport.diameter.interceptors.util.DiameterInterceptorCleaner;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Striped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UserName("Outbound Diameter Synchronous")
public class DiameterOutbound extends AbstractOutboundTransportImpl {

    private static final boolean SHARED_CONNECTIONS = true; // May be, later we switch behavior depending on the value.
    private static final int STRIPES = 250;
    private static final Striped<Lock> LOCK_STRIPED = Striped.lazyWeakLock(STRIPES);
    private static final String CONTEXT_ID = "ContextId";
    private static final String PROPERTY_TEMPLATE = "${%s}";
    private static final String DROP_CONNECTION = "Drop Connection";
    private static final String TRANSPORT_ID = "transportId";
    private static final String UNDERLINE = "_";

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.CER,
            longName = PropertyConstants.DiameterTransportConstants.CER_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.CER_DESCRIPTION, optional = true,
            loadTemplate = true)
    public String cer;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.HOST,
            longName = PropertyConstants.DiameterTransportConstants.HOST_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.HOST_DESCRIPTION)
    private String host;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.PORT,
            longName = PropertyConstants.DiameterTransportConstants.PORT_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.PORT_DESCRIPTION)
    private String port;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.DWA,
            longName = PropertyConstants.DiameterTransportConstants.DWA_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.DWA_DESCRIPTION, loadTemplate = true)
    private String dwr;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.CONFIG_PATH,
            longName = PropertyConstants.DiameterTransportConstants.CONFIG_PATH_LONG_NAME,
            description = PropertyConstants.DiameterTransportConstants.CONFIG_PATH_DESCRIPTION,
            fileDirectoryType = "diameter-dictionary",
            validatePattern = PropertyConstants.DiameterTransportConstants.CONFIG_PATH_VALIDATE_PATTERN)
    private String configurationPath;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.WAIT_RESPONSE_TIMEOUT,
            longName = "Response Timeout",
            description = PropertyConstants.DiameterTransportConstants.WAIT_RESPONSE_TIMEOUT_DESCRIPTION,
            optional = true)
    private String waitTimeout;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.CONNECTION_TYPE,
            longName = PropertyConstants.DiameterTransportConstants.CONNECTION_TYPE_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.CONNECTION_TYPE_DESCRIPTION)
    @Options({"TCP", "SCTP"})
    private String connectionType;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.INTERCEPTOR_NAME,
            longName = PropertyConstants.DiameterTransportConstants.INTERCEPTOR_NAME_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.INTERCEPTOR_NAME_DESCRIPTION, forTemplate = true,
            forServer = false)
    @Options({"DWA", "CCA", "CEA", "ASA", "RAA", "SLA", "STA"})
    private String interceptorName;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.PROPERTIES, longName = "Properties",
            description = PropertyConstants.DiameterTransportConstants.PROPERTIES_DESCRIPTION, isDynamic = true,
            optional = true, userSettings = true)
    private Map<String, String> properties;
    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.MESSAGE_FORMAT_NAME,
            longName = PropertyConstants.DiameterTransportConstants.MESSAGE_FORMAT_DESCRIPTION,
            description = "XML template configuration or Wireshark. Default is XML", optional = true)
    @Options({"XML", "Wireshark"})
    private String messageFormat;

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.WAIT_RESPONSE,
            longName = PropertyConstants.DiameterTransportConstants.WAIT_RESPONSE_DESCRIPTION,
            description = "Will wait response for message or not", optional = true, forTemplate = true)
    @Options({"Yes", "No"})
    private String waitResponse;

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.DPR,
            longName = PropertyConstants.DiameterTransportConstants.DPR_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.DPR_DESCRIPTION, optional = true,
            loadTemplate = true)
    private String dpr;

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.SESSION_ID,
            longName = PropertyConstants.DiameterTransportConstants.SESSION_ID_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.SESSION_ID_DESCRIPTION, optional = true,
            isDynamic = true)
    private String sessionId;

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.CUSTOM_DPR,
            longName = PropertyConstants.DiameterTransportConstants.CUSTOM_DPR_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.CUSTOM_DPR_DESCRIPTION, optional = true)
    @Options({"Yes", "No"})
    private String customDpr;

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.DPA,
            longName = PropertyConstants.DiameterTransportConstants.DPA_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.DPA_DESCRIPTION, loadTemplate = true)
    private String dpa;

    @Parameter(shortName = PropertyConstants.DiameterTransportConstants.DICTIONARY_TYPE,
            longName = PropertyConstants.DiameterTransportConstants.DICTIONARY_TYPE_DESCRIPTION,
            description = PropertyConstants.DiameterTransportConstants.DICTIONARY_TYPE_DESCRIPTION)
    @Options({DiameterParserType.STANDARD, DiameterParserType.MARBEN})
    private String dictionaryType;
    private long currentTimeout;

    @Override
    public String getShortName() {
        return "Diameter Outbound";
    }

    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        int port = Integer.parseInt(
                this.getRequired(connectionProperties, PropertyConstants.DiameterTransportConstants.PORT));
        if (DROP_CONNECTION.equalsIgnoreCase(message.getText())) {
            String connectionId =
                    connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.HOST).toString() + port;
            ConnectionFactory.destroy(connectionId);
            String podName = Config.getConfig().getRunningHostname();
            DiameterConnectionInfoProvider.getDiameterConnectionInfoCacheService()
                    .remove(String.format("%s%s", connectionId, podName));
            return new Message(StringUtils.EMPTY);
        }
        String interceptorName = connectionProperties.obtain(
                PropertyConstants.DiameterTransportConstants.INTERCEPTOR_NAME);
        String required = this.getRequired(connectionProperties, PropertyConstants.DiameterTransportConstants.HOST);
        Interceptor interceptor = InterceptorFactory.getInstance().create(interceptorName);
        String sessionID = connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.SESSION_ID);
        interceptor.setSessionId(sessionID);
        message = prepareMessage(message,
                connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.PROPERTIES));
        Supplier<CEAInterceptor> ceaSupplier = createCeaSupplier();
        if ("No".equals(connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.WAIT_RESPONSE))) {
            sendMessage(message, connectionProperties, port, required, interceptor, ceaSupplier, sessionID, projectId);
            return new Message(StringUtils.EMPTY);
        }
        Message response = sendMessageAndGetResponse(message, connectionProperties, port, required, interceptor,
                ceaSupplier, sessionID, projectId);
        checkErrors(interceptor, response, ceaSupplier);
        return response;
    }

    private Message prepareMessage(Message message, Map<String, String> properties) {
        if (properties == null) {
            return message;
        }
        properties.forEach((key, value) -> message.setText(replacePlaceholder(key, value, message.getText())));
        return message;
    }

    private String replacePlaceholder(String key, String value, String text) {
        return text.replaceAll(Pattern.quote(String.format(PROPERTY_TEMPLATE, key)), value);
    }

    private Message sendMessageAndGetResponse(Message message, ConnectionProperties connectionProperties, int port,
                                              String required, Interceptor interceptor,
                                              Supplier<CEAInterceptor> ceaSupplier, String sessionID,
                                              BigInteger projectId) throws Exception {
        sendMessage(message, connectionProperties, port, required, interceptor, ceaSupplier, sessionID, projectId);
        waitResponseAndExpireInterceptor(connectionProperties, interceptor);
        return new Message(interceptor.getResponse());
    }

    private void waitResponseAndExpireInterceptor(ConnectionProperties connectionProperties, Interceptor interceptor)
            throws Exception {
        currentTimeout = Long.parseLong((String) getOrDefault(connectionProperties,
                PropertyConstants.DiameterTransportConstants.WAIT_RESPONSE_TIMEOUT, "3000"));
        boolean isReceived = waitResponse(interceptor, currentTimeout);
        if (!isReceived) {
            log.warn(String.format("timeout for request session: %s",
                    connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.SESSION_ID).toString()));
        }
        interceptor.setExpired(true);
    }

    private void sendMessage(Message message, ConnectionProperties connectionProperties, int port, String required,
                             Interceptor interceptor, Supplier<CEAInterceptor> ceaSupplier, String sessionID,
                             BigInteger projectId) throws Exception {
        DiameterConnection connection = getDiameterConnection(connectionProperties, required, port,
                Collections.singletonList(interceptor), ceaSupplier, projectId);
        try {
            Object contextId = connectionProperties.obtain(CONTEXT_ID);
            Object transportId = connectionProperties.obtain(TRANSPORT_ID);
            boolean isInit = DiameterSessionHolder.getInstance().add(sessionID, contextId);
            if (isInit) {
                createExternalInterceptors(interceptor, connection, sessionID, transportId, contextId);
            }
            connection.send(message.getText());
        } catch (Exception e) {
            log.error("Message sending is failed or CEA response isn't received", e);
            if (!SHARED_CONNECTIONS) {
                connection.stopListening();
                String connectionId =
                        connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.HOST).toString()
                                + port;
                ConnectionFactory.destroy(connectionId);
                String podName = Config.getConfig().getRunningHostname();
                DiameterConnectionInfoProvider.getDiameterConnectionInfoCacheService()
                        .remove(String.format("%s%s", connectionId, podName));
            }
            throw new RuntimeException("Message sending is failed or CEA response isn't received", e);
        }
    }

    private void createExternalInterceptors(Interceptor interceptor, DiameterConnection connection, String sessionID,
                                            Object transportId, Object contextId) {
        String sessionType = DiameterSessionTypes.getType(interceptor);
        switch (sessionType) {
            case CREDIT_CONTROL_SESSION: {
                InterceptorFactory factory = InterceptorFactory.getInstance();
                RARInterceptor rar = (RARInterceptor) factory.create(InterceptorTypes.RAR, sessionID, contextId,
                        transportId, false);
                ASRInterceptor asr = (ASRInterceptor) factory.create(InterceptorTypes.ASR, sessionID, contextId,
                        transportId, false);
                connection.addInterceptors(Lists.newArrayList(rar, asr));
                break;
            }
            case SY_SESSION: {
                InterceptorFactory factory = InterceptorFactory.getInstance();
                SNRInterceptor snr = (SNRInterceptor) factory.create(InterceptorTypes.SNR, sessionID, contextId,
                        transportId, false);
                connection.addInterceptors(Lists.newArrayList(snr));
                break;
            }
            default:
                throw new IllegalStateException(
                        "Unable to create diameter interceptor - unexpected diameter session type: " + sessionType);
        }
    }

    private void sendCer(ConnectionProperties connectionProperties, Interceptor interceptor,
                         DiameterConnection connection) throws Exception {
        connection.addInterceptors(Collections.singletonList(interceptor));
        connection.send(prepareText(connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.CER),
                connectionProperties));
        waitResponseAndExpireInterceptor(connectionProperties, interceptor);
        if (StringUtils.isBlank(interceptor.getResponse())) {
            throw new IllegalStateException("CEA response is not received. Can't establish connection");
        }
    }

    private String prepareText(String text, ConnectionProperties connectionProperties) {
        Map<String, String> properties = connectionProperties.obtain(
                PropertyConstants.DiameterTransportConstants.PROPERTIES);
        text = replacePlaceholders(text, properties);
        return text;
    }

    private <U> String replacePlaceholders(String text, Map<String, U> properties) {
        if (properties == null) {
            return text;
        }
        for (Map.Entry<String, U> entry : properties.entrySet()) {
            text = replacePlaceholder(entry.getKey(), entry.getValue().toString(), text);
        }
        return text;
    }

    private boolean waitResponse(final Interceptor interceptor, long timeOut) throws InterruptedException {
        long endTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + Long.parseLong(String.valueOf(timeOut));
        long delta;
        synchronized (interceptor) {
            delta = endTime - TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            while (!interceptor.isReceived() && delta > 0) {
                interceptor.wait(delta);
                delta = endTime - TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            }
        }
        return interceptor.isReceived();
    }

    private DiameterConnection getDiameterConnection(ConnectionProperties props, String host, int port,
                                                     List<Interceptor> interceptors,
                                                     Supplier<CEAInterceptor> ceaSupplier,
                                                     BigInteger projectId) throws Exception {
        String localCacheId = String.format("%s%s%s%s%d", projectId, UNDERLINE,
                props.obtain(PropertyConstants.DiameterTransportConstants.HOST).toString(), UNDERLINE, port);
        synchronized (LOCK_STRIPED.get(localCacheId)) {
            DiameterConnection connection = ConnectionFactory.getExisting(localCacheId);
            if (connection != null) {
                connection.addInterceptors(interceptors);
                return connection;
            }
            String dictionaryPath = getRequired(props, PropertyConstants.DiameterTransportConstants.CONFIG_PATH);
            String parserType = getRequired(props, PropertyConstants.DiameterTransportConstants.DICTIONARY_TYPE);
            Class<? extends DiameterParser> parserClass = DiameterParserType.defineParserClass(parserType);
            DictionaryConfig dictionaryConfig = new DictionaryConfig(dictionaryPath, parserClass, projectId);
            ConfigReader.read(dictionaryConfig, false);
            connection = createDiameterConnection(props, projectId);
            connection.setDecoder(new XmlDecoder(dictionaryConfig));
            Encoder encoder = EncoderFactory.getInstance()
                    .getEncoder(props.obtain(PropertyConstants.DiameterTransportConstants.MESSAGE_FORMAT_NAME),
                            dictionaryConfig);
            connection.setEncoder(encoder);
            String dpaTemplate = replacePlaceholders(
                    getRequired(props, PropertyConstants.DiameterTransportConstants.DPA),
                    props.obtain(PropertyConstants.DiameterTransportConstants.PROPERTIES));
            DPRInterceptor dprInterceptor = new DPRInterceptor(dpaTemplate, encoder);
            connection.addInterceptors(Collections.singletonList(dprInterceptor));
            String dwrTemplate = replacePlaceholders(
                    getRequired(props, PropertyConstants.DiameterTransportConstants.DWA),
                    props.obtain(PropertyConstants.DiameterTransportConstants.PROPERTIES));
            DWRInterceptor dwrInterceptor = new DWRInterceptor(dwrTemplate, encoder);
            connection.addInterceptors(Collections.singletonList(dwrInterceptor));
            connection.addInterceptors(interceptors);
            TransportType transportType = TransportType.getType(String.valueOf(
                    getOrDefault(props, PropertyConstants.DiameterTransportConstants.CONNECTION_TYPE, "TCP")));
            ExtraChannel channel = ExtraChannel.open(transportType);
            channel.connect(new InetSocketAddress(host, port));
            connection.setSocketChannel(channel);
            connection.startListening(localCacheId);
            String podName = Config.getConfig().getRunningHostname();
            DiameterConnectionInfo diameterConnectionInfo = createDiameterConnectionInfo(localCacheId, connection,
                    podName, dictionaryPath, projectId);
            DiameterConnectionInfoProvider.getDiameterConnectionInfoCacheService()
                    .put(localCacheId + UNDERLINE + podName, diameterConnectionInfo);
            ConnectionFactory.cache(localCacheId, connection);
            if (props.obtain(PropertyConstants.DiameterTransportConstants.CER) != null) {
                sendCer(props, ceaSupplier.get(), connection);
            }
            log.info("Establish connection with host: {}:{}. With configuration: {}", host, port, dictionaryPath);
            return connection;
        }
    }

    @NotNull
    private DiameterConnectionInfo createDiameterConnectionInfo(String cacheId, DiameterConnection connection,
                                                                String podName, String dictionaryPath,
                                                                BigInteger projectId) {
        DiameterConnectionInfo diameterConnectionInfo = new DiameterConnectionInfo();
        diameterConnectionInfo.setConnectionId(cacheId);
        diameterConnectionInfo.setChannel(connection.getChannel().toString());
        diameterConnectionInfo.setPodName(podName);
        diameterConnectionInfo.setDictionaryPath(dictionaryPath);
        diameterConnectionInfo.setProjectId(String.valueOf(projectId));
        return diameterConnectionInfo;
    }

    private DiameterConnection createDiameterConnection(ConnectionProperties connectionProperties,
                                                        BigInteger projectId) {
        DiameterConnection connection;
        if (connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.DPR) != null && "Yes".equals(
                connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.CUSTOM_DPR))) {
            connection = ConnectionFactory.createConnection(
                    connectionProperties.obtain(PropertyConstants.DiameterTransportConstants.DPR));
        } else {
            connection = ConnectionFactory.createConnection();
        }
        DiameterInterceptorCleaner.getInstance().scheduleCleanupIfNeeded(projectId);
        return connection;
    }

    private String getRequired(ConnectionProperties connectionProperties, String field) {
        return String.valueOf(connectionProperties.computeIfAbsent(field, key -> {
            throw new IllegalArgumentException(String.format("Required %s doesn't configured", key));
        }));
    }

    private void checkErrors(Interceptor interceptor, Message response, Supplier<CEAInterceptor> ceaSupplier) {
        if (interceptor.isFailed()) {
            response.setFailedMessage(interceptor.getResponse());
        }
        CEAInterceptor ceaInterceptor = ceaSupplier.get();
        if (ceaInterceptor.isFailed()) {
            response.setFailedMessage(ceaInterceptor.getResponse());
        }
        if (response.getText() == null && !interceptor.isFailed()) {
            throw new IllegalStateException("No response from remote diameter host. "
                    + "Possible reasons are: CCA response wasn't received and request timed out after "
                    + currentTimeout / 1000 + " seconds" + ((ceaInterceptor.getResponse() != null)
                    ? " or CEA response is invalid: " + ceaInterceptor.getResponse()
                    : "."));
        }
    }

    private Supplier<CEAInterceptor> createCeaSupplier() {
        return new Supplier<CEAInterceptor>() {
            private CEAInterceptor ceaInterceptor;

            @Override
            public CEAInterceptor get() {
                if (ceaInterceptor == null) {
                    ceaInterceptor = new CEAInterceptor();
                }
                return ceaInterceptor;
            }
        };
    }

    private <T extends Interceptor> Supplier<T> createInterceptorSupplier(Class<T> clazz) {
        return new Supplier<T>() {
            private T interceptor;

            @Override
            public T get() {
                if (interceptor == null) {
                    try {
                        interceptor = clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        log.error("Interceptor for '{}' can't be created", clazz, e);
                    }
                }
                return interceptor;
            }
        };
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
        return "/mockingbird-transport-diameter";
    }

    private Object getOrDefault(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return (value == null) || !properties.containsKey(key) || (StringUtils.EMPTY.equals(value.toString()))
                ? defaultValue
                : value;
    }
}
