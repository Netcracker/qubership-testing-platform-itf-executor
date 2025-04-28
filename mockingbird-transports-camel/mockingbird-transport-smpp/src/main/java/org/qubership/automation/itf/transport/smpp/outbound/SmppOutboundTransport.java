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

package org.qubership.automation.itf.transport.smpp.outbound;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TC_TIMEOUT_FAIL;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TC_TIMEOUT_FAIL_DEFAULT_VALUE;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TC_TIMEOUT_FAIL_TIME_UNIT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TC_TIMEOUT_FAIL_TIME_UNIT_DEFAULT_VALUE;
import static org.qubership.automation.itf.transport.smpp.Constants.ADDRESS;
import static org.qubership.automation.itf.transport.smpp.Constants.CHARSET_NAME;
import static org.qubership.automation.itf.transport.smpp.Constants.CONNECTION_TYPE;
import static org.qubership.automation.itf.transport.smpp.Constants.DEFAULT_CHARSET;
import static org.qubership.automation.itf.transport.smpp.Constants.DEFAULT_EXPIRY_TIMEOUT;
import static org.qubership.automation.itf.transport.smpp.Constants.DEFAULT_SENDER_NAME;
import static org.qubership.automation.itf.transport.smpp.Constants.EXPIRY_TIMEOUT;
import static org.qubership.automation.itf.transport.smpp.Constants.HOST;
import static org.qubership.automation.itf.transport.smpp.Constants.PASSWORD;
import static org.qubership.automation.itf.transport.smpp.Constants.PORT;
import static org.qubership.automation.itf.transport.smpp.Constants.SENDER_NAME;
import static org.qubership.automation.itf.transport.smpp.Constants.SYSTEM_ID;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.core.util.transport.base.AbstractOutboundTransportImpl;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UserName("Outbound SMPP transport")
public class SmppOutboundTransport extends AbstractOutboundTransportImpl {

    @Parameter(shortName = CONNECTION_TYPE, longName = "Type of Connection",
            description = "TRANSCEIVER, TRANSMITTER or RECEIVER type")
    @Options({"TRANSCEIVER", "TRANSMITTER", "RECEIVER"})
    private String type;

    @Parameter(shortName = HOST, longName = "Host",
            description = "Host")
    private String host;

    @Parameter(shortName = PORT, longName = "Port",
            description = "Port number")
    private String port;

    @Parameter(shortName = SYSTEM_ID, longName = "System Id",
            description = "System Id (default: uim)", optional = true)
    private String systemId = "uim";

    @Parameter(shortName = PASSWORD, longName = "Password",
            description = "Remote password or empty", optional = true)
    private String password;

    @Parameter(shortName = SENDER_NAME, longName = "Sender name",
            description = "Sender name  (default: ITF Instance)", optional = true)
    private String senderName;

    @Parameter(shortName = ADDRESS, longName = "Address",
            description = "Address of the recipient")
    private String address;

    @Parameter(shortName = CHARSET_NAME, longName = "Charset Name",
            description = "Charset name (default: UCS-2)", forTemplate = true, optional = true)
    private String charsetName;

    @Parameter(shortName = EXPIRY_TIMEOUT, longName = "Expiry Timeout",
            description = "Default response expiry timeout, ms (default: 60000)",
            forTemplate = true, optional = true)
    private String expiryTimeout;

    /**
     * TODO Add JavaDoc.
     */
    public static SubmitSm createSubmitSm(String src, String dst, String text, String charset)
            throws SmppInvalidArgumentException {
        SubmitSm submitSm = new SubmitSm();
        // For alpha numeric will use TON=5, NPI=0
        submitSm.setSourceAddress(new Address((byte) 5, (byte) 0, src));
        // For national numbers will use TON=1, NPI=1
        submitSm.setDestAddress(new Address((byte) 1, (byte) 1, dst));
        // Set datacoding to UCS-2
        submitSm.setDataCoding((byte) 8);
        // Encode text
        submitSm.setShortMessage(CharsetUtil.encode(text, charset));
        //We would like to get delivery receipt
        submitSm.setRegisteredDelivery((byte) 1);
        Tlv scInterfaceVersion = new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[]{Byte.parseByte("4")});
        submitSm.setOptionalParameter(scInterfaceVersion);
        return submitSm;
    }

    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        return sendMessageAndGetResponse(message, connectionProperties, projectId);
    }

    private Message sendMessageAndGetResponse(Message message, ConnectionProperties connectionProperties,
                                              BigInteger projectId) {
        SmppClient client = new DefaultSmppClient();
        SmppSessionConfiguration sessionConfig = getSmppSessionConfiguration(connectionProperties);
        disableDefaultLoggingSmpp(sessionConfig);
        SmppSession session = null;
        Message response = new Message();
        try {
            session = getSmppSession(client, sessionConfig);
            this.expiryTimeout = (String) getOrDefault(connectionProperties, EXPIRY_TIMEOUT, DEFAULT_EXPIRY_TIMEOUT);
            long expiryTimeout = Long.parseLong(this.expiryTimeout);
            long maxTimeout = Long.parseLong(CoreServices.getProjectSettingsService().get(projectId, TC_TIMEOUT_FAIL,
                    TC_TIMEOUT_FAIL_DEFAULT_VALUE));
            maxTimeout = TimeUnit.valueOf(CoreServices.getProjectSettingsService().get(projectId,
                            TC_TIMEOUT_FAIL_TIME_UNIT, TC_TIMEOUT_FAIL_TIME_UNIT_DEFAULT_VALUE).toUpperCase())
                    .toMillis(maxTimeout);
            if (expiryTimeout > maxTimeout) {
                expiryTimeout = maxTimeout;
                log.warn("Used maximum response timeout, because the set time is greater than the maximum. "
                        + "Given time: {}, Maximum time: {}", expiryTimeout, maxTimeout);
            }
            String senderName = (String) getOrDefault(connectionProperties, SENDER_NAME, DEFAULT_SENDER_NAME);
            String address = (String) connectionProperties.get(ADDRESS);
            String charsetName = (String) getOrDefault(connectionProperties, CHARSET_NAME, DEFAULT_CHARSET);
            SubmitSm sm = createSubmitSm(senderName, address, message.getText(), charsetName);
            SubmitSmResp submitSmResp = session.submit(sm, expiryTimeout);
            response.setText(submitSmResp.getResultMessage());
        } catch (Exception e) {
            log.error("Message sending is failed or response isn't received ", e);
            throw new RuntimeException("Message sending is failed or response isn't received ", e);
        } finally {
            closeConnection(session, client);
        }
        checkErrors(response, Long.parseLong(expiryTimeout));
        return response;
    }

    private SmppSession getSmppSession(SmppClient client, SmppSessionConfiguration sessionConfig) {
        SmppSession session;
        try {
            session = client.bind(sessionConfig, new DefaultSmppSessionHandler());
            log.info("Establish connection with host: {}:{}.", sessionConfig.getHost(), sessionConfig.getPort());
        } catch (Exception e) {
            String error = "Error occurred while connect remote host. ";
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
        return session;
    }

    private SmppSessionConfiguration getSmppSessionConfiguration(ConnectionProperties props) {
        SmppSessionConfiguration sessionConfig = new SmppSessionConfiguration();
        sessionConfig.setType(SmppBindType.valueOf((String) props.get(CONNECTION_TYPE)));
        sessionConfig.setHost((String) props.get(HOST));
        sessionConfig.setPort(Integer.parseInt((String) props.get(PORT)));
        sessionConfig.setSystemId((String) getOrDefault(props, SYSTEM_ID, systemId));
        if (!StringUtils.isBlank((String) props.get(PASSWORD))) {
            sessionConfig.setPassword((String) props.get(PASSWORD));
        }
        sessionConfig.setConnectTimeout(
                ApplicationConfig.env.getProperty("transport.smpp.connect.timeout", Long.class, 10000L));
        sessionConfig.setBindTimeout(ApplicationConfig.env.getProperty("transport.smpp.bind.timeout", Long.class,
                5000L));
        return sessionConfig;
    }

    private void disableDefaultLoggingSmpp(SmppSessionConfiguration smppSessionConfiguration) {
        LoggingOptions loggingOptions = new LoggingOptions();
        loggingOptions.setLogPdu(
                ApplicationConfig.env.getProperty("transport.smpp.logging.package", Boolean.class, false));
        loggingOptions.setLogBytes(
                ApplicationConfig.env.getProperty("transport.smpp.logging.hex.dump", Boolean.class, false));
        smppSessionConfiguration.setLoggingOptions(loggingOptions);
    }

    private void checkErrors(Message response, Long expiryTimeout) {
        if (Objects.isNull(response.getText())) {
            throw new IllegalStateException("No response from remote host. "
                    + "Possible reasons are: response wasn't received and request timed out after "
                    + expiryTimeout / 1000 + " seconds.");
        }
    }

    private void closeConnection(SmppSession session, SmppClient client) {
        if (Objects.nonNull(session)) {
            session.close();
            session.destroy();
        }
        client.destroy();
        log.info("Resources SmppClient and SmppSession were closed successfully.");
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-smpp";
    }

    @Override
    public String getShortName() {
        return "SMPP Outbound";
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    private Object getOrDefault(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return (Objects.isNull(value)) || (StringUtils.isBlank(value.toString())) ? defaultValue : value;
    }
}
