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

package org.qubership.automation.itf.transport.soap.http.inbound;

import java.util.Map;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.http.inbound.HTTPInboundTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UserName("Inbound SOAP Over HTTP Synchronous")
public class SOAPOverHTTPInboundTransport extends HTTPInboundTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SOAPOverHTTPInboundTransport.class);
    @Parameter(shortName = PropertyConstants.Commons.ENDPOINT_PROPERTIES, longName = "Extra Endpoint Properties",
            description = PropertyConstants.Commons.ENDPOINT_PROPERTIES_DESCRIPTION,
            forServer = true, forTemplate = false, isDynamic = true, optional = true)
    protected Map<String, Object> properties;
    @Parameter(shortName = PropertyConstants.Soap.WSDL_PATH, longName = "Path to WSDL file",
            description = PropertyConstants.Soap.WSDL_PATH_DESCRIPTION, fileDirectoryType = "wsdl-xsd")
    private String wsdlFile;
    @Parameter(shortName = PropertyConstants.Soap.WSDL_CONTAINS_XSD, longName = "Does WSDL File contain XSD?",
            description = "Does WSDL File contain XSD?")
    @Options(value = {"No", "Yes"})
    private String isWsdlContainsXSD;
    @Parameter(shortName = PropertyConstants.Soap.REQUEST_XSD_PATH, longName = "Validate Request by XSD",
            description = PropertyConstants.Soap.XSD_PATH_DESCRIPTION, optional = true, fileDirectoryType = "wsdl-xsd")
    private String requestXSD;
    @Parameter(shortName = PropertyConstants.Soap.RESPONSE_XSD_PATH, longName = "Validate Response by XSD",
            description = PropertyConstants.Soap.XSD_PATH_DESCRIPTION, optional = true, fileDirectoryType = "wsdl-xsd")
    private String responseXSD;

    @Override
    public String getShortName() {
        return "SOAP Inbound";
    }

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-soap-http";
    }
}
