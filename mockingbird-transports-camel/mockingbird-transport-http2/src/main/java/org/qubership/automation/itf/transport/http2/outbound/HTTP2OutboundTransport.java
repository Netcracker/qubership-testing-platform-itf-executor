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

package org.qubership.automation.itf.transport.http2.outbound;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.ALLOW_STATUS;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;
import org.qubership.automation.itf.transport.http2.HTTP2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@UserName("Outbound HTTP2 Synchronous")
public class HTTP2OutboundTransport extends AbstractCamelOutboundTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTP2OutboundTransport.class);
    @Parameter(shortName = HTTP2Constants.ENDPOINT, longName = "Http Endpoint",
            description = HTTP2Constants.ENDPOINT_URI_DESCRIPTION, forTemplate = true, isDynamic = true)
    protected String endpoint;
    @Parameter(shortName = HTTP2Constants.METHOD, longName = "Method",
            description = HTTP2Constants.METHOD_DESCRIPTION, forTemplate = true)
    @Options({"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH"})
    private String method = "GET";
    @Parameter(shortName = HTTP2Constants.REMOTE_HOST, longName = "Remote host",
            description = HTTP2Constants.REMOTE_HOST_DESCRIPTION, isDynamic = true)
    private String host;
    @Parameter(shortName = HTTP2Constants.RESPONSE_CODE, longName = "Allow Status Code",
            description = ALLOW_STATUS, forTemplate = true, forServer = false, optional = true, isDynamic = true)
    private String allowStatus;

    @Parameter(shortName = HTTP2Constants.REMOTE_PORT, longName = "Remote Port",
            description = HTTP2Constants.REMOTE_PORT_DESCRIPTION, isDynamic = true)
    private String port;

    @Parameter(shortName = HTTP2Constants.HEADERS, longName = "Http Request Headers",
            description = HTTP2Constants.HEADERS_DESCRIPTION, optional = true, forTemplate = true, isDynamic = true)
    private Map<String, String> headers = new HashMap<>();
    private ConnectionProperties connectionProperties;

    @Override
    public String getShortName() {
        return "HTTP2 Outbound";
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-http2";
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        if (this.connectionProperties == null || !this.connectionProperties.equals(connectionProperties)) {
            this.connectionProperties = connectionProperties;
        }
        String resolvedEndpoint = resolveEndpoint(connectionProperties);
        connectionProperties.put("Resolved_Endpoint_URL", resolvedEndpoint);

        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
                .build();
        RequestBody formBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                message.getText());
        Call call;
        if (connectionProperties.get(HTTP2Constants.HEADERS) != null) {
            Headers headersMap = Headers.of((Map<String, String>) connectionProperties.get(HTTP2Constants.HEADERS));
            call = client.newCall(new Request.Builder()
                    .url(resolvedEndpoint)
                    .method(connectionProperties.get(HTTP2Constants.METHOD).toString(), formBody)
                    .headers(headersMap)
                    .build());
        } else {
            call = client.newCall(new Request.Builder()
                    .url(resolvedEndpoint)
                    .post(formBody)
                    .build());
        }
        Response response = call.execute();
        int responseHttpCode = response.code();

        Message responseMessage = new Message();
        responseMessage.setText((response.body() == null) ? "" : response.body().string());
        responseMessage.convertAndSetMultiHeaders(response.headers().toMultimap());
        responseMessage.getHeaders().put("CamelHttpResponseCode", responseHttpCode);

        List<MutablePair<Integer, Integer>> validCodeRanges = computeValidCodeRanges(message.getConnectionProperties());
        if (!validCodeRanges.isEmpty()) {
            boolean codeAllowed = checkResponseCode(responseHttpCode, validCodeRanges);
            if (codeAllowed) {
                if (responseMessage.getText() == null) {
                    responseMessage.setFailedMessage(response.toString());
                    // Commented logging here, because the corresponding RuntimeException
                    // is thrown in the IntegrationStepHelper#sendReceiveSync
                    // LOGGER.error("Failed sending request (or receiving response): ", responseMessage
                    // .getFailedMessage());
                }
            } else {
                setFailedMessage(response, responseMessage);
            }
        } else {
            if (!response.isSuccessful()) {
                setFailedMessage(response, responseMessage);
            }
        }
        return responseMessage;
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    private String resolveEndpoint(ConnectionProperties connectionProperties) {
        return "http://" + connectionProperties.get(HTTP2Constants.REMOTE_HOST) + ":"
                + connectionProperties.get(HTTP2Constants.REMOTE_PORT)
                + connectionProperties.get(HTTP2Constants.ENDPOINT);
    }

    /*  The method computes the ranges list for 'allow status code' checking.
     *   Returned value is the list of code ranges allowed.
     *   Empty returned value means no checking.
     */
    private List<MutablePair<Integer, Integer>> computeValidCodeRanges(Map<String, Object> connectionProperties) {
        /* If there is the property in this transport configuration, it prevails.
            Otherwise - global property is used if configured.
            Otherwise (if no global property) - no response code checking is performed.
        */
        String allowStatusCodes = (String) getOrDefault(connectionProperties, HTTP2Constants.RESPONSE_CODE, "");
        if (StringUtils.isBlank(allowStatusCodes)) {
            // Global setting rules
            return Config.getConfig().getValidCodeRanges();
        } else {
            return Config.parseCodeRanges(allowStatusCodes);
        }
    }

    private Object getOrDefault(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return (value == null) || (StringUtils.EMPTY.equals(value.toString()))
                ? defaultValue : value;
    }

    private boolean checkResponseCode(int responseHttpCode, List<MutablePair<Integer, Integer>> validCodeRanges) {
        for (MutablePair<Integer, Integer> range : validCodeRanges) {
            if (range.getLeft() <= responseHttpCode && range.getRight() >= responseHttpCode) return true;
        }
        return false;
    }

    public void setFailedMessage(Response response, Message responseMessage) {
        if (responseMessage.getText() == null) {
            responseMessage.setFailedMessage(response.toString());
        } else {
            responseMessage.setFailedMessage(responseMessage.getText());
        }
        // Commented logging here, because the corresponding RuntimeException
        // is thrown in the IntegrationStepHelper#sendReceiveSync
        //LOGGER.error("Failed sending request (or receiving response): ", responseMessage.getFailedMessage());
    }

}
