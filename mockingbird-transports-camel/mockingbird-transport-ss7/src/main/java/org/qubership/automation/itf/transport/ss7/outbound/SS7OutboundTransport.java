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

package org.qubership.automation.itf.transport.ss7.outbound;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.transport.base.AbstractTransportImpl;
import org.qubership.automation.itf.core.util.transport.base.OutboundTransport;
import org.qubership.automation.itf.transport.ss7.SS7Constants;
import org.qubership.automation.ss7lib.connection.ConnectionHolder;
import org.qubership.automation.ss7lib.proxy.service.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UserName("Outbound SS7 Synchronous")
public class SS7OutboundTransport extends AbstractTransportImpl implements SS7Constants, OutboundTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SS7OutboundTransport.class);
    private static final ExecutionService EXECUTION_SERVICE = new ExecutionService();
    @Parameter(shortName = PORT_TANGO,
            longName = "port of tango",
            description = "SCTP Layer connection port on the Tango server",
            isRedefined = true)
    private String portTango;

    @Parameter(shortName = HOST_NAMES_TANGO,
            longName = "host name of tango. One or two e.g. 127.0.0.1,127.0.0.2",
            description = "Hosts",
            isRedefined = true)
    private String hostnameTango;

    @Parameter(shortName = IS_PROXY,
            longName = "is proxy",
            description = "mode of work with transport",
            isRedefined = true)
    @Options({"True", "False"})
    private String callTimeMax;

    @Parameter(shortName = PORT_APP,
            longName = "port of proxy app",
            description = "Connection port on the proxy app server",
            isRedefined = true)
    private String portApp;

    @Parameter(shortName = HOST_NAME_APP,
            longName = "host name of app. e.g. 127.0.0.1",
            description = "Hosts",
            isRedefined = true)
    private String hostnameApp;

    @Override
    public String getShortName() {
        return "SS7 Outbound";
    }

    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-ss7";
    }

    @Override
    public Message sendReceiveSync(Message message, BigInteger projectId) throws Exception {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        if (Boolean.parseBoolean(connectionProperties.obtain(IS_PROXY))) {
            if (!ping(createURIBuilder(connectionProperties, "/state"))) {
                return send(createURIBuilder(connectionProperties, "/execute"), message.getText());
            }
        } else {
            if (ConnectionHolder.getInstance().isConnected()) {
                return new Message(EXECUTION_SERVICE.execute(message.getText()));
            }
        }
        throw new IllegalStateException("Connection with Tango is not established yet.");
    }

    private boolean connect(int tangoPort) {
        try {
            LOGGER.info("SS7 proxy for ITF. Listening tango port: {}. Waiting for tango connection...", tangoPort);
            ConnectionHolder.getInstance().acceptConnection(tangoPort);
            LOGGER.info("Tango has been connected");
            Thread thread = ConnectionHolder.getInstance().runMainLoop();
            thread.join();
            return true;
        } catch (Exception e) {
            LOGGER.error("Proxy server is not started", e);
            return false;
        }
    }

    private boolean ping(URIBuilder uri) throws IOException, URISyntaxException {
        URI build = uri.build();
        String response = parseHttpResponse(doPost(build, ""));
        return Boolean.parseBoolean(response);
    }

    private Message send(URIBuilder uri, String message) throws URISyntaxException, IOException, TransportException {
        URI build = uri.build();
        HttpResponse httpResponse = doPost(build, message);
        String response = parseHttpResponse(httpResponse);
        if (httpResponse.getStatusLine().getStatusCode() == 500) {
            throw new TransportException("Unable to send SS7 message: " + response);
        }
        return new Message(response);
    }

    private HttpResponse doPost(URI uri, String body) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(uri);
        HttpEntity entity = new StringEntity(body);
        request.setEntity(entity);
        return client.execute(request);
    }

    private String parseHttpResponse(HttpResponse response) throws IOException {
        return IOUtils.toString(response.getEntity().getContent());
    }

    private URIBuilder createURIBuilder(ConnectionProperties connectionProperties, String path) {
        return new URIBuilder().setScheme("http").setHost(connectionProperties.obtain(HOST_NAME_APP).toString())
                .setPort(Integer.parseInt(connectionProperties.obtain(PORT_APP).toString())).setPath(path);
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }
}
