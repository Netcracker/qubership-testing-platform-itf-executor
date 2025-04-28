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

package org.qubership.automation.itf.transport.http.inbound;

import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.CONTENT_TYPE;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.ENDPOINT_URI;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.HEADERS;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.IS_STUB;
import static org.qubership.automation.itf.core.util.constants.PropertyConstants.Http.RESPONSE_CODE;

import java.util.Map;

import org.qubership.automation.itf.core.transport.http.HTTPConstants;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HTTPInboundTransport extends AbstractInboundTransportImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPInboundTransport.class);

    @Parameter(shortName = HTTPConstants.ENDPOINT,
            longName = "Endpoint URI",
            description = ENDPOINT_URI,
            forServer = false,
            forTemplate = true,
            isDynamic = true)
    protected String endpointUri;

    @Parameter(shortName = HTTPConstants.RESPONSE_CODE,
            longName = "Response Code",
            description = RESPONSE_CODE,
            fromServer = true,
            forTemplate = true,
            isDynamic = true)
    protected Integer allowStatus;

    @Parameter(shortName = HTTPConstants.HEADERS,
            longName = "Response Headers",
            description = HEADERS,
            optional = true,
            forServer = false,
            forTemplate = true,
            isDynamic = true)
    protected Map<String, Object> headers;

    @Parameter(shortName = HTTPConstants.CONTENT_TYPE,
            longName = "Content Type",
            description = CONTENT_TYPE,
            optional = true,
            forServer = false,
            forTemplate = true,
            isDynamic = true)
    protected String contentType = "text/html";

    @Parameter(shortName = HTTPConstants.IS_STUB, longName = "Is Stub", description = IS_STUB, optional = true)
    @Options({"No", "Yes"})
    protected String isStub = "No";

    @Override
    public String getShortName() {
        return "HTTP Inbound";
    }
}
