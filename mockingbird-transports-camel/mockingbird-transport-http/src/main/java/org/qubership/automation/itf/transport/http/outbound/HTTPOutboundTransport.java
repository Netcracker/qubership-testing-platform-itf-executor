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

package org.qubership.automation.itf.transport.http.outbound;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.PROPERTIES;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.PROPERTIES_DESCRIPTION;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.http.SSLContextParametersSecureProtocolSocketFactory;
import org.apache.camel.component.http4.CompositeHttpConfigurer;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.component.http4.ProxyHttpClientConfigurer;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.http.HttpVersion;
import org.apache.http.conn.HttpHostConnectException;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.transport.http.HTTPConstants;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.camel.outbound.AbstractCamelOutboundTransport;
import org.qubership.automation.itf.transport.http.DynamicHttpClientConfigurer;
import org.qubership.automation.itf.transport.http.Helper;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class HTTPOutboundTransport extends AbstractCamelOutboundTransport {

    private static final DefaultHeaderFilterStrategy ITFHeaderFilterStrategy = new DefaultHeaderFilterStrategy();
    private static final Cache<String, SSLContextParameters> SSL_CONTEXT_PARAMETERS_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS).build();
    private static final Cache<String, ProxyHttpClientConfigurer> PROXY_HTTP_CLIENT_CONFIGURER_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.HOURS).build();
    private static final Cache<String, DynamicHttpClientConfigurer> DYNAMIC_HTTP_CLIENT_CONFIGURER_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.HOURS).build();

    static {
        ITFHeaderFilterStrategy.setOutFilter(null);
        ITFHeaderFilterStrategy.setInFilter(null);
    }

    @Parameter(shortName = HTTPConstants.METHOD,
            longName = "Method",
            description = PropertyConstants.Http.METHOD,
            forServer = false,
            forTemplate = true)
    @Options({"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH"})
    protected String method = "GET";

    @Parameter(shortName = HTTPConstants.ENDPOINT,
            longName = "Http Endpoint",
            description = PropertyConstants.Http.ENDPOINT_URI,
            forTemplate = true,
            optional = true,
            isDynamic = true)
    protected String endpoint;

    @Parameter(shortName = "baseUrl",
            longName = "Base Server URL",
            description = PropertyConstants.Http.BASE_URL,
            isRedefined = true,
            isDynamic = true)
    protected String baseUrl;

    @Parameter(shortName = "contentType",
            longName = "Http Request Content Type",
            description = PropertyConstants.Http.CONTENT_TYPE,
            optional = true,
            forTemplate = true,
            isDynamic = true)
    protected String contentType = "text/html";

    @Parameter(shortName = "headers",
            longName = "Http Request Headers",
            description = PropertyConstants.Http.HEADERS,
            optional = true,
            forTemplate = true,
            isDynamic = true)
    protected Map<String, String> headers = new HashMap<>();

    @Parameter(shortName = "secureProtocol",
            longName = "Use secure protocol(if use https)",
            optional = true,
            description = "")
    @Options({"SSL", "TLSv1.2"})
    protected String secureProtocol = "TLSv1.2";

    /*   Property "Allow Status Code" is to support [DF] RestOverHttpOutbound functionality
     *   (DF-transports will be deleted soon)
     *   Without this property there is an easy WA:
     *       - camel endpoint option can be added to the end of Endpoint, like this:
     *           /productOrderingManagement/v1/productOrder
     *           /productOrderingManagement/v1/productOrder?okStatusCodeRange=400-405 - it seems that not all servers
     *  process it properly
     */
    /*  Response checking algorithm after discussions with Nikolay Durasov and Alena Mironova, 2019-04-16:
     *      1. If <allow status code> property is NOT empty (*):
     *          1.1. If response http code is valid (in the allowed range) - step is Passed
     *          1.2. Otherwise step is Failed (and tcContext too)
     *      2. If <allow status code> property is empty:
     *          2.1. If exchange.isFailed() - step is Failed (and tcContext too)
     *          2.2. Otherwise step is Passed
     *
     *      (*) - there are two layers of <allow status code> configuration:
     *          1) Local settings (on the Template, on the system/Transport, on the Environment),
     *          2) Global setting - via 'http.response.code.success' variable in the config.properties
     *          Local and global settings are processed as follows:
     *              - Local settings prevail if configured,
     *              - Otherwise global property is used if configured,
     *              - Otherwise (if no global property) - no response code checking is performed.
     */
    @Parameter(shortName = HTTPConstants.RESPONSE_CODE,
            longName = "Allow Status Code",
            description = PropertyConstants.Http.ALLOW_STATUS,
            forTemplate = true,
            forServer = false,
            optional = true)
    private String allowStatus;

    @Parameter(shortName = PROPERTIES,
            longName = "Properties",
            description = PROPERTIES_DESCRIPTION,
            isDynamic = true,
            optional = true,
            forTemplate = true,
            userSettings = true)
    private Map<String, String> properties;

    @Parameter(shortName = PropertyConstants.Http.CACHE_RESPONSE_FOR_SECONDS,
            longName = "Cache Response for Seconds",
            description = PropertyConstants.Http.CACHE_RESPONSE_FOR_SECONDS_DESCRIPTION,
            optional = true,
            forTemplate = true)
    private Integer cacheResponseForSeconds;

    @Override
    public String getShortName() {
        return "HTTP Outbound";
    }

    /**
     * This method sends synchronized request to endpoint, which would be build in
     * {@link #createRequestExchange(Message, ProducerTemplate, Map, String)}.
     *
     * @param message {@link Message} that contains message connection properties like Endpoint, Base URL, Headers,
     *                Message Body.
     * @return {@link Message} response of request
     * @throws Exception if something goes wrong
     */
    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        validateRequest(message);
        String baseUrl = (String) message.getConnectionProperties().get(HTTPConstants.BASE_URL);
        Preconditions.checkArgument(StringUtils.isNotBlank(baseUrl), "Base url can't be null or empty");
        Preconditions.checkArgument(baseUrl.startsWith(HTTPConstants.HTTP),
                "Base url isn't valid, it should start with http/https");
        boolean isSecure = isSecure(baseUrl);
        if (isSecure) {
            // Rood WA fixing https problem (PKIX validator exceptions for all certificates)
            message.getHeaders().putIfAbsent("trustAll", "true");
        }
        HttpComponent httpComponent = prepareHttpComponent(isSecure, message);
        String endpointUri = resolveEndpoint(message, baseUrl);

        /*
         *   Clearing the outFilter in DefaultHeaderFilterStrategy(Camel) to allow editing forbidden camel headers
         * (like "date", "host", ...) and sending those.
         *   Apache Camel doc links: https://camel.apache
         * .org/manual/latest/faq/how-to-avoid-sending-some-or-all-message-headers.html
         * */
        Helper.clearForbiddenHeaders(httpComponent, ITFHeaderFilterStrategy);
        configureSendViaProxy(httpComponent, message);
        configureDynamicHttpClient(httpComponent, message);

        /*
            We use a new instance of ProducerTemplate instead of static one to avoid old cookies affecting new requests.
        */
        Exchange exchange = createRequestExchange(message,
                /* CamelContextProvider.template */ CAMEL_CONTEXT.createProducerTemplate(), addDefaultHeaders(message),
                endpointUri, httpComponent);
        List<MutablePair<Integer, Integer>> validCodeRanges = computeValidCodeRanges(message.getConnectionProperties());
        int responseHttpCode = getResponseCode(exchange);
        Message responseMessage;
        if (!validCodeRanges.isEmpty()) {
            boolean codeAllowed = checkResponseCode(responseHttpCode, validCodeRanges);
            responseMessage = buildMessage(exchange, responseHttpCode, codeAllowed);
        } else {
            responseMessage = buildMessage(exchange);
        }
        responseMessage.getHeaders().putIfAbsent("CamelHttpResponseCode", responseHttpCode);
        return responseMessage;
    }

    protected abstract Exchange createRequestExchange(Message message, ProducerTemplate template,
                                                      Map<String, Object> headers, String endpoint)
            throws Exception;

    protected abstract Exchange createRequestExchange(Message message, ProducerTemplate template,
                                                      Map<String, Object> headers, String endpoint,
                                                      HttpComponent httpComponent)
            throws Exception;

    protected abstract org.apache.camel.Message composeBody(org.apache.camel.Message camelMessage,
                                                            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage)
            throws Exception;

    private boolean getBooleanValue(Object obj) {
        if (obj == null) {
            return false;
        }
        String s = obj.toString();
        return !(StringUtils.isBlank(s) || !Boolean.parseBoolean(s));
    }

    private void configureDynamicHttpClient(HttpComponent httpComponent, Message message) {
        Map<String, Object> headers = message.getHeaders();
        if (headers == null || headers.isEmpty()) {
            return;
        }
        boolean disableRedirects = getBooleanValue(headers.get("disableRedirects"));
        boolean trustAll = getBooleanValue(headers.get("trustAll"));
        if (!disableRedirects && !trustAll) {
            return;
        }
        String key = getCacheKey(String.valueOf(disableRedirects), String.valueOf(trustAll));
        DynamicHttpClientConfigurer dynamicHttpClientConfigurer =
                DYNAMIC_HTTP_CLIENT_CONFIGURER_CACHE.getIfPresent(key);
        if (dynamicHttpClientConfigurer == null) {
            dynamicHttpClientConfigurer = createDynamicHttpClientConfigurer(disableRedirects, trustAll, key);
        }
        HttpClientConfigurer httpClientConfigurer = httpComponent.getHttpClientConfigurer();
        if (httpClientConfigurer == null) {
            httpComponent.setHttpClientConfigurer(dynamicHttpClientConfigurer);
        } else {
            if (!dynamicHttpClientConfigurer.equals(httpClientConfigurer)) {
                CompositeHttpConfigurer compositeHttpConfigurer = new CompositeHttpConfigurer();
                compositeHttpConfigurer.addConfigurer(httpClientConfigurer);
                compositeHttpConfigurer.addConfigurer(dynamicHttpClientConfigurer);
                httpComponent.setHttpClientConfigurer(compositeHttpConfigurer);
            }
        }
    }

    private void configureSendViaProxy(HttpComponent httpComponent, Message message) {
        Map<String, String> properties = getMessageProperties(message);
        if (properties == null) {
            return;
        }
        String host = properties.get("proxyAuthHost");
        if (StringUtils.isBlank(host)) {
            return;
        }
        HttpClientConfigurer httpClientConfigurer = httpComponent.getHttpClientConfigurer();
        if (httpClientConfigurer == null) {
            if (httpComponent.getHttpConfiguration() == null) {
                String scheme = properties.get("proxyAuthScheme");
                String username = properties.get("proxyAuthUsername");
                String password = properties.get("proxyAuthPassword");
                String domain = properties.get("proxyAuthDomain");
                String ntHost = properties.get("proxyAuthNtHost");
                String portString = properties.get("proxyAuthPort");
                int port = (StringUtils.isBlank(portString))
                        ? -1
                        : Integer.parseInt(portString);
                String key = getCacheKey(host, String.valueOf(port), scheme, username, password, domain, ntHost);
                ProxyHttpClientConfigurer proxyHttpClientConfigurer =
                        PROXY_HTTP_CLIENT_CONFIGURER_CACHE.getIfPresent(key);
                if (proxyHttpClientConfigurer == null) {
                    proxyHttpClientConfigurer = createProxyHttpClientConfigurer(host, port, scheme, username, password,
                            domain, ntHost, key);
                }
                httpComponent.setHttpClientConfigurer(proxyHttpClientConfigurer);
            }
        }
    }

    private boolean isSecure(String schemeOrUrl) {
        return schemeOrUrl.startsWith(HTTPConstants.HTTPS);
    }

    private HttpComponent prepareHttpComponent(boolean isSecure, Message message) {
        HttpComponent httpComponent = new org.apache.camel.component.http4.HttpComponent();
        if (isSecure) {
            registerSecureComponent(httpComponent, message); // There must be parameters configured in the
            // config.properties file
        }
        return httpComponent;
    }

    private void registerSecureComponent(HttpComponent httpComponent, Message message) {
        String keyStoreFile =
                ApplicationConfig.env.getProperty("local.storage.directory", "local.storage.directory").concat(
                        "/keystore/keystore.jks");
        String keyStorePassword = ApplicationConfig.env.getProperty("keystore.password");
        String secProtocol = (String) message.getConnectionProperties().get(HTTPConstants.SECURE_PROTOCOL);
        String sslContextCacheKey = getCacheKey(keyStoreFile, keyStorePassword, secProtocol);
        SSLContextParameters sslContext = SSL_CONTEXT_PARAMETERS_CACHE.getIfPresent(sslContextCacheKey);
        if (sslContext == null) {
            sslContext = createSSLContextParameters(keyStoreFile, keyStorePassword, secProtocol, sslContextCacheKey);
        }
        httpComponent.setSslContextParameters(sslContext);
    }

    protected String resolveEndpoint(Message message, String url) {
        String endpoint = (String) message.getConnectionProperties().get(HTTPConstants.ENDPOINT);
        Preconditions.checkArgument(StringUtils.isNotBlank(endpoint), "Endpoint can't be null or empty");
        return URI.create(url).resolve("/").resolve(endpoint).toString();
    }

    /*  The method to URL-encode query parameters.
     *   But, Apache Camel Http4 component Library v.2.20.4 normalizes URI and decodes-encodes query string
     * automatically.
     *   So, our encoding can only spoil a correct work of Http4 camel software.
     *   So, our encoding is turned off.
     *   Extra information: a part of query string can be enclosed into RAW() Camel macro.
     *       It notifies Camel that the data is 'raw', so it should NOT be decoded but encoded only.
     *       Example: /uim/notifications?subscriberId=63268&startDate=RAW($tc.Params.CurrentTimestamp)&endDate=RAW
     * (2019-12-09T08:00:00.873+06:00[UTC])
     *       Result:  /uim/notifications?endDate=2019-12-09T08%3A00%3A00
     * .873%2B06%3A00%5BUTC%5D&startDate=2019-12-13T15%3A40%3A36.123Z&subscriberId=63268
     *
     *   The method is to be deleted soon.
     *   ...
     *   The method is used again, due to problems with spaces:
     *       - ...&name=left right&... - Camel fails with an error: java.lang.IllegalArgumentException: Illegal
     * character in query at index 47... and con not resolve the URL
     *       - ...&name=RAW(left right)&... - Camel fails with the same error
     *   So, our encoding is used again. In case some problems we can try to replace spaces only, instead of encoding
     *  the whole value
     *       in case value contains spaces, correct configuration is:
     *           ...&name=left right&...
     *       resolved endpoint is:
     *           ...&name=left+right&...
     *   ...
     *   The method is not used, finally.
     *   Instead, a user should use 'encodeUrl' Velocity directive for each parameter value
     *   which contains (or could contain) whitespaces, special chars, nationals etc.
     *   For example:
     *       /api/v1/catalogManagement/productOffering?name=#encodeUrl("30 Flexi Mins & 2GB Data (1 Month)","UTF-8")
     * &fields=id
     */
    protected String escapeParameters(String endpoint) {
        int pos = endpoint.indexOf("?");
        if (pos == -1) {
            return endpoint;
        }
        StringBuilder encodedEndpoint = new StringBuilder(endpoint.substring(0, pos));
        String[] pairs = endpoint.substring(pos).split("&");
        int count = 0;
        for (String pair : pairs) {
            int pos1 = pair.indexOf("=");
            if (pos1 > -1) {
                String val = pair.substring(pos1 + 1);
                try {
                    encodedEndpoint.append(((count == 0)
                            ? ""
                            : "&")).append(pair, 0, pos1).append("=").append(URLEncoder.encode(val, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
            } else {
                encodedEndpoint.append(((count == 0)
                        ? ""
                        : "&")).append(pair);
            }
            count++;
        }
        return encodedEndpoint.toString();
    }

    protected Map<String, Object> addDefaultHeaders(Message message) {
        message.getHeaders().putIfAbsent(Exchange.HTTP_METHOD, method);
        message.getHeaders().putIfAbsent(Exchange.CONTENT_TYPE, contentType);
        return message.getHeaders();
    }

    /*  Message processing in case checking of response http code is configured.
     *   Algorithm:
     *       - if codeAllowed==true step is Passed,
     *       - otherwise step is Failed.
     */
    protected Message buildMessage(Exchange exchange,
                                   int responseHttpCode,
                                   boolean codeAllowed) {
        Message message = new Message(exchange.getOut().getBody(String.class));
        message.convertAndSetHeaders(exchange.getOut().getHeaders());
        if (codeAllowed) {
            /* Special case... I'm not sure that it's correct behaviour but
                - if there is an empty message in case of success (due to code is in the allowed range),
                it will definitely confuse users.
                I have faced it for 302 response code.
                And there is one more possibility - set text to the result of processHttpOperationFailedException(),
                ignore inner setting
            */
            if (message.getText() == null) {
                if (exchange.getException() != null && exchange.getException() instanceof HttpOperationFailedException) {
                    processHttpOperationFailedException(exchange, message);
                }
            }
            validateResponse(message);
        } else {
            responseCodeNotAllowedExceptionProcess(exchange, message, responseHttpCode);
        }
        return message;
    }

    /*  Message processing in case NO checking of response http code is configured.
     *   Algorithm:
     *       - if exchange.isFailed() then we invoke message.setFailedMessage(exceptionMessage),
     *           and RuntimeException is thrown in the IntegrationStepHelper after it, so step is Failed.
     *       - otherwise step is Passed.
     */
    protected Message buildMessage(Exchange exchange) {
        Message message = new Message(exchange.getOut().getBody(String.class));
        message.convertAndSetHeaders(exchange.getOut().getHeaders());
        if (exchange.isFailed()) {
            exchangeExceptionProcess(exchange, message);
        } else {
            validateResponse(message);
        }
        return message;
    }

    private DynamicHttpClientConfigurer createDynamicHttpClientConfigurer(boolean disableRedirects, boolean trustAll,
                                                                          String key) {
        DynamicHttpClientConfigurer dynamicHttpClientConfigurer = new DynamicHttpClientConfigurer();
        dynamicHttpClientConfigurer.setDisableRedirects(disableRedirects);
        dynamicHttpClientConfigurer.setTrustAll(trustAll);
        DYNAMIC_HTTP_CLIENT_CONFIGURER_CACHE.put(key, dynamicHttpClientConfigurer);
        return dynamicHttpClientConfigurer;
    }

    private ProxyHttpClientConfigurer createProxyHttpClientConfigurer(String host, Integer port, String scheme,
                                                                      String username, String password, String domain,
                                                                      String ntHost, String key) {
        ProxyHttpClientConfigurer httpClientConfigurer = new ProxyHttpClientConfigurer(host, port, scheme, username,
                password, domain, ntHost);
        PROXY_HTTP_CLIENT_CONFIGURER_CACHE.put(key, httpClientConfigurer);
        return httpClientConfigurer;
    }

    private SSLContextParameters createSSLContextParameters(String keyStoreFile, String keyStorePassword,
                                                            String secProtocol, String key) {
        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        if (!StringUtils.isBlank(keyStoreFile)) {
            keyStoreParameters.setResource(keyStoreFile);
            keyStoreParameters.setPassword(keyStorePassword);
        }
        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyStore(keyStoreParameters);
        keyManagersParameters.setKeyPassword(keyStorePassword);
        //init trust store
        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(keyStoreParameters);
        //set ssl context
        SSLContextParameters sslContext = new SSLContextParameters();
        sslContext.setKeyManagers(keyManagersParameters);
        sslContext.setTrustManagers(trustManagersParameters);
        //set protocol
        sslContext.setSecureSocketProtocol(secProtocol);
        //register http4 protocol
        try {
            ProtocolSocketFactory factory = new SSLContextParametersSecureProtocolSocketFactory(sslContext,
                    CAMEL_CONTEXT);
            Protocol.registerProtocol(HTTPConstants.HTTPS, new Protocol(HTTPConstants.HTTPS, factory, 6443));
            SSL_CONTEXT_PARAMETERS_CACHE.put(key, sslContext);
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Error while register http4(https) protocol ", e);
        }
    }

    private String getCacheKey(String... keys) {
        StringBuilder result = new StringBuilder("_");
        for (String key : keys) {
            result.append(key).append("_");
        }
        return result.toString();
    }

    private void exchangeExceptionProcess(Exchange exchange, Message message) {
        String exceptionMessage = (StringUtils.isBlank(exchange.getException().getMessage()))
                ? exchange.getException().toString()
                : exchange.getException().getMessage();
        exceptionMessage += (exchange.getException().getCause() != null)
                ? ("\nCaused by: " + exchange.getException().getCause().toString())
                : ("\nStacktrace: " + ExceptionUtils.getStackTrace(exchange.getException()));
        if (exchange.getException() instanceof HttpOperationFailedException) {
            exceptionMessage = exceptionMessage + processHttpOperationFailedException(exchange, message);
        } else if (exchange.getException().getCause() != null && exchange.getException().getCause() instanceof SocketException) {
            exceptionMessage =
                    "Trying to connect to: " + exchange.getFromEndpoint().getEndpointUri() + " - " + exceptionMessage;
        } else {
            message.setText(exceptionMessage);
        }
        message.setFailedMessage(exceptionMessage);
        if (StringUtils.isEmpty(message.getText())) {
            message.setText(exceptionMessage);
        }
        // LOGGER.error(exceptionMessage); // Commented because these exceptions are logged twice or more and spam logs
    }

    private void responseCodeNotAllowedExceptionProcess(Exchange exchange, Message message, int responseHttpCode) {
        String exceptionMessage = "HTTP Response code (" + responseHttpCode + ") is not in the allowed range!";
        Exception exception = exchange.getException();
        if (exception instanceof HttpOperationFailedException) {
            exceptionMessage = exceptionMessage + processHttpOperationFailedException(exchange, message);
        } else if (exception instanceof HttpHostConnectException) {
            exceptionMessage = exceptionMessage + "\n" + processHttpHostConnectException(exchange, message);
        } else if (exception != null) {
            exceptionMessage = exceptionMessage + "\n" + ((StringUtils.isBlank(exception.getMessage()))
                    ? exception.toString()
                    : exception.getMessage()) + ((exception.getCause() == null)
                    ? ("\nStacktrace" + ": " + ExceptionUtils.getStackTrace(exception))
                    : ("\nCaused by: " + exception.getCause().toString()));
        }
        message.setFailedMessage(exceptionMessage);
        if (StringUtils.isEmpty(message.getText())) {
            message.setText(exceptionMessage);
        }
        // LOGGER.error(exceptionMessage);  // Commented because these exceptions are logged twice or more and spam logs
    }

    private String processHttpOperationFailedException(Exchange exchange, Message message) {
        HttpOperationFailedException httpException = (HttpOperationFailedException) exchange.getException();
        Map<String, Object> exceptionHeaders = new HashMap<>(httpException.getResponseHeaders());
        message.convertAndSetHeaders(exceptionHeaders);
        String httpExceptionInfo = fillHttpExceptionInfo(httpException);
        message.setText((StringUtils.isBlank(httpException.getResponseBody()))
                ? "Response body is empty; brief " + "exception info is: " + httpExceptionInfo
                : httpException.getResponseBody());
        return httpExceptionInfo;
    }

    private String processHttpHostConnectException(Exchange exchange, Message message) {
        HttpHostConnectException exception = (HttpHostConnectException) exchange.getException();
        message.setText(exception.getMessage() + ((exception.getCause() == null)
                ? ""
                : "\nCaused by: " + exception.getCause().toString()));
        return message.getText();
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
        String allowStatusCodes = (String) getOrDefault(connectionProperties, HTTPConstants.RESPONSE_CODE, "");
        if (StringUtils.isBlank(allowStatusCodes)) {
            // Global setting rules
            return Config.getConfig().getValidCodeRanges();
        } else {
            return Config.parseCodeRanges(allowStatusCodes);
        }
    }

    private int getResponseCode(Exchange exchange) {
        if (exchange.hasOut()) {
            if (exchange.getOut().hasHeaders()) {
                Object strResponseCode = exchange.getOut().getHeader("CamelHttpResponseCode");
                if (strResponseCode instanceof Integer) {
                    return (int) strResponseCode;
                } else {
                    try {
                        return Integer.parseInt((String) strResponseCode);
                    } catch (Exception ignore) {
                    }
                }
            }
        } else if (exchange.isFailed()) {
            Exception ex = exchange.getException();
            if (ex instanceof HttpOperationFailedException) {
                return ((HttpOperationFailedException) ex).getStatusCode();
            }
        }
        return 0;
    }

    private boolean checkResponseCode(int responseHttpCode, List<MutablePair<Integer, Integer>> validCodeRanges) {
        for (MutablePair<Integer, Integer> range : validCodeRanges) {
            if (range.getLeft() <= responseHttpCode && range.getRight() >= responseHttpCode) {
                return true;
            }
        }
        return false;
    }

    protected String fillHttpExceptionInfo(HttpOperationFailedException httpException) {
        String msg = "";
        msg += addIfNotEmpty("\nResponse body: ", httpException.getResponseBody());
        msg += addIfNotEmpty("\nStatus code: ", httpException.getStatusCode());
        msg += addIfNotEmpty("\nStatus text: ", httpException.getStatusText());
        msg += addIfNotEmpty("\nURI: ", httpException.getUri());
        msg += addIfNotEmpty("\nError message: ", httpException.getMessage());
        return msg;
    }

    private String addIfNotEmpty(String title, String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return (title + value);
    }

    private String addIfNotEmpty(String title, int value) {
        if (value == 0) {
            return "";
        }
        return (title + value);
    }

    protected void validateResponse(Message message) {
        //Override this method to execute validations like XSD validation
    }

    protected void validateRequest(Message message) {
        //Override this method to execute validations like XSD validation
    }

    protected Processor fillOutputExchange(Message message, Map<String, Object> headers) {
        return exchange -> {
            org.apache.camel.Message in = exchange.getIn();
            in.getHeaders().putAll(headers);
            in.setHeader(Exchange.HTTP_METHOD, getOrDefault(message.getConnectionProperties(), HTTPConstants.METHOD,
                    method));
            in.setHeader(Exchange.CONTENT_TYPE, getOrDefault(message.getConnectionProperties(),
                    HTTPConstants.CONTENT_TYPE, contentType));

            /* To avoid org.apache.http.ConnectionClosedException:
                    Premature end of chunk coded message body: closing chunk expected
             */
            if (headers.getOrDefault("forceHTTP1.0", "false").equals("true")) {
                in.setHeader(Exchange.HTTP_PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
            }

            composeBody(in, message);
            removeHeadersIfConfigured(in, message);
            message.addConnectionProperty("Resolved_Endpoint_URL", in.getExchange().getFromEndpoint().toString());
            message.convertAndSetHeaders(in.getHeaders());
        };
    }

    protected Map<String, String> getMessageProperties(org.qubership.automation.itf.core.model.jpa.message.Message itfMessage) {
        Object obj = itfMessage.getConnectionProperties().get(PROPERTIES);
        if (obj == null || obj instanceof String) {
            return null;
        }
        Map<String, String> properties = (Map<String, String>) obj;
        if (properties.isEmpty()) {
            return null;
        }
        return properties;
    }

    /* 1st attempt to solve the problem
     *   'Properties' property can contain 'removeHeaders' entry. The value is a list of header names, like:
     *       removeHeaders=Content-Type,Cache-control
     *   All these headers will be removed from camelMessage headers.
     */
    protected void removeHeadersIfConfigured(org.apache.camel.Message camelMessage,
                                             org.qubership.automation.itf.core.model.jpa.message.Message itfMessage) {
        Map<String, String> properties = getMessageProperties(itfMessage);
        if (properties == null) {
            return;
        }
        String removeHeaders = properties.get("removeHeaders");
        if (StringUtils.isBlank(removeHeaders)) {
            return;
        }
        String[] list = removeHeaders.split(",");
        for (String elem : list) {
            if (StringUtils.isBlank(elem)) {
                continue;
            }
            camelMessage.removeHeader(elem);
            itfMessage.getHeaders().remove(elem);
        }
    }

    private Object getOrDefault(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return (value == null) || (StringUtils.EMPTY.equals(value.toString()))
                ? defaultValue : value;
    }
}
