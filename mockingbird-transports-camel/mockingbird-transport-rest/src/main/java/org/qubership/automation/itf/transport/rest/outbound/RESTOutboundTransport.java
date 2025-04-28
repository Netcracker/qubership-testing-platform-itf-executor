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

package org.qubership.automation.itf.transport.rest.outbound;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.component.http4.HttpEndpoint;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.transport.http.HTTPConstants;
import org.qubership.automation.itf.core.util.annotation.Async;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.transport.http.Helper;
import org.qubership.automation.itf.transport.http.outbound.HTTPOutboundTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Async
@UserName("Outbound REST Synchronous")
public class RESTOutboundTransport extends HTTPOutboundTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(RESTOutboundTransport.class);

    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    protected Exchange createRequestExchange(Message message, ProducerTemplate template, Map<String, Object> headers,
                                             String endpoint) {
        int loopIndex = 0;
        boolean autoRedirect = checkRemoveFollowRedirectsHeader(headers);
        return createRequestExchange(message, template, headers, endpoint, autoRedirect, loopIndex);
    }

    @Override
    protected Exchange createRequestExchange(Message message, ProducerTemplate template, Map<String, Object> headers,
                                             String endpoint, HttpComponent httpComponent) throws Exception {
        int loopIndex = 0;
        boolean autoRedirect = checkRemoveFollowRedirectsHeader(headers);
        return createRequestExchange(message, template, headers, endpoint, autoRedirect, loopIndex, httpComponent);
    }

    @Override
    protected org.apache.camel.Message composeBody(org.apache.camel.Message camelMessage, Message itfMessage) throws Exception {
        return Helper.composeBodyForREST(camelMessage, itfMessage);
    }

    private Exchange createRequestExchange(Message message, ProducerTemplate template, Map<String, Object> headers,
                                           String endpoint, boolean autoRedirect, int loopIndex) {
        return createRequestExchange(message, template, headers, endpoint, autoRedirect, loopIndex, null);
    }

    private Exchange createRequestExchange(Message message, ProducerTemplate template, Map<String, Object> headers,
                                           String endpoint, boolean autoRedirect, int loopIndex,
                                           HttpComponent httpComponent) {
        Exchange resp;
        if (httpComponent == null) {
            resp = template.request(endpoint, fillOutputExchange(message, headers));
        } else {
            String randomUuid = "out" + UUID.randomUUID();
            CAMEL_CONTEXT.addComponent(randomUuid, httpComponent);
            HttpEndpoint endpointObj;
            try {
                URI uri = new URI(endpoint);
                endpointObj = new HttpEndpoint(endpoint, httpComponent, uri);
                endpointObj.setHttpClientConfigurer(httpComponent.getHttpClientConfigurer());
                endpointObj.setClientConnectionManager(new PoolingHttpClientConnectionManager());
                endpointObj.setHeaderFilterStrategy(httpComponent.getHeaderFilterStrategy());
                if (endpoint.contains("deleteWithBody=true")) {
                    endpointObj.setDeleteWithBody(true);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            resp = template.request(endpointObj, fillOutputExchange(message, headers));
            CAMEL_CONTEXT.removeComponent(randomUuid);
        }

        //Auto Redirect when status code 302 for REST Sync (POST), see NITP-4451
        if (resp.getException() instanceof HttpOperationFailedException) {
            HttpOperationFailedException ex = (HttpOperationFailedException) resp.getException();
            int statusCode = ex.getStatusCode();
            if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                if (getOrDefault(message.getConnectionProperties(), HTTPConstants.METHOD, method).equals("POST") &&
                        loopIndex < HTTPConstants.LOOP_MAX_SIZE && autoRedirect) {
                    loopIndex++;
                    logRedirect(endpoint, resp, loopIndex);
                    String redirectLocation = ex.getRedirectLocation();
                    resp = createRequestExchange(message, template, headers,
                            checkAndFixNonAbsoluteRedirectLocation(redirectLocation, ex.getUri()),
                            autoRedirect, loopIndex, httpComponent);
                }
            }
        }
        return resp;
    }

    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-rest";
    }

    private Object getOrDefault(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return (value == null) || (StringUtils.EMPTY.equals(value.toString()))
                ? defaultValue : value;
    }

    private void logRedirect(String endpoint, Exchange resp, int loopIndex) {
        HttpOperationFailedException ex = (HttpOperationFailedException) resp.getException();
        LOGGER.debug("Redirection attempt #{}, original endpoint '{}': Response status: {}\n"
                        + " Response:\n"
                        + "   Exception: {}\n" +
                        "   - URI: {}\n" +
                        "   - RedirectLocation: {}\n" +
                        "   - ResponseBody: {}\n" +
                        "   - ResponseHeaders: {}\n",
                loopIndex, endpoint, ex.getStatusCode(), ex,
                ex.getUri(),
                ex.getRedirectLocation(),
                ex.getResponseBody(),
                ex.getResponseHeaders());
    }

    private String checkAndFixNonAbsoluteRedirectLocation(String redirectLocation, String endpoint) {
        try {
            URI redirectLocationUri = new URI(redirectLocation);
            if (redirectLocationUri.isAbsolute()) {
                return redirectLocation;
            } else {
                URL url = new URI(endpoint).toURL();
                return url.getProtocol() + "://" + url.getHost()
                        + ((url.getPort() == -1) ? "" : ":" + url.getPort())
                        + (redirectLocation.startsWith("/") ? "" : "/")
                        + redirectLocation;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkRemoveFollowRedirectsHeader(Map<String, Object> headers) {
        boolean autoRedirect = headers.containsKey("FollowRedirects");
        if (autoRedirect) {
            headers.remove("FollowRedirects");
        }
        return autoRedirect;
    }

    @Override
    public String getShortName() {
        return "Rest Outbound";
    }
}
