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

package org.qubership.automation.itf.transport.http2.inbound;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.IS_STUB;

import java.util.Map;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;
import org.qubership.automation.itf.transport.http2.HTTP2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UserName("Inbound HTTP2 Synchronous")
public class HTTP2InboundTransport extends AbstractInboundTransportImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTP2InboundTransport.class);
    @Parameter(shortName = HTTP2Constants.ENDPOINT, longName = "Endpoint URI",
            description = HTTP2Constants.ENDPOINT_URI_DESCRIPTION)
    protected String endpointUri;
    @Parameter(shortName = HTTP2Constants.RESPONSE_CODE, longName = "Response Code",
            description = HTTP2Constants.RESPONSE_CODE, fromServer = true, forTemplate = true, isDynamic = true)
    protected Integer allowStatus;
    @Parameter(shortName = HTTP2Constants.HEADERS, longName = "Response Headers",
            description = HTTP2Constants.HEADERS_DESCRIPTION, optional = true, forTemplate = true, isDynamic = true)
    protected Map<String, Object> headers;
    @Parameter(shortName = HTTP2Constants.ADD_PROJECTUUID_ENDPOINT_PREFIX,
            longName = "Add Project UUID Prefix into Endpoint",
            description = HTTP2Constants.ADD_PROJECTUUID_ENDPOINT_PREFIX_DESCRIPTION,
            optional = true)
    @Options({"No", "Yes"})
    protected String addProjectUuidEndpointPrefix = "Yes";
    @Parameter(shortName = "isStub", longName = "Is Stub",
            description = IS_STUB, optional = true)
    @Options({"No", "Yes"})
    protected String isStub = "No";
    @Parameter(shortName = HTTP2Constants.REMOTE_HOST, longName = "Local host",
            description = HTTP2Constants.LOCAL_HOST_DESCRIPTION)
    private String host;
    @Parameter(shortName = HTTP2Constants.REMOTE_PORT, longName = "Local Port",
            description = HTTP2Constants.LOCAL_PORT_DESCRIPTION)
    private Integer port;

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        /*
            Returned value is changed from "/mockingbird-transport-http2" to "",
            because, in fact, requests should be with empty prefix, for example:
            http://some-address:98765/75ff7376-7231-4f8f-8144-ede2a5ea9762/http2kag
        */
        return "";
    }

    @Override
    public String getShortName() {
        return "HTTP2 Inbound";
    }
}
