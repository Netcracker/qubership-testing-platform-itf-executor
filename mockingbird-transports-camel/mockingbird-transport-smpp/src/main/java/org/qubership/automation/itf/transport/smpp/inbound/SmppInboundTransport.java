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

package org.qubership.automation.itf.transport.smpp.inbound;

import static org.qubership.automation.itf.transport.smpp.Constants.CHARSET_NAME;
import static org.qubership.automation.itf.transport.smpp.Constants.COMMAND_STATUS;
import static org.qubership.automation.itf.transport.smpp.Constants.DEFAULT_SESSION_COUNTERS_ENABLED;
import static org.qubership.automation.itf.transport.smpp.Constants.EXPIRY_TIMEOUT;
import static org.qubership.automation.itf.transport.smpp.Constants.IS_STUB;
import static org.qubership.automation.itf.transport.smpp.Constants.JMX_ENABLED;
import static org.qubership.automation.itf.transport.smpp.Constants.MESSAGE_ID;
import static org.qubership.automation.itf.transport.smpp.Constants.PORT;
import static org.qubership.automation.itf.transport.smpp.Constants.SYSTEM_ID;
import static org.qubership.automation.itf.transport.smpp.Constants.WINDOW_SIZE;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;

@UserName("SMPP Inbound transport")
public class SmppInboundTransport extends AbstractInboundTransportImpl {
    @Parameter(shortName = DEFAULT_SESSION_COUNTERS_ENABLED, longName = "Default Session Counters Enabled",
            description = "Default Session Counters Enabled (Default: false)", optional = true)
    @Options({"No", "Yes"})
    protected String defaultSessionCountersEnabled = "No";
    @Parameter(shortName = JMX_ENABLED, longName = "JMX Enabled",
            description = "JMX Enabled (Default: false)", optional = true)
    @Options({"No", "Yes"})
    protected String jmxEnabled = "No";
    @Parameter(shortName = IS_STUB, longName = "Is Stub",
            description = "Is Stub: Default=No", optional = true)
    @Options({"No", "Yes"})
    protected String isStub = "No";
    @Parameter(shortName = PORT, longName = "Port",
            description = "Port number to listen (Default: 36888)", optional = true)
    private String port;
    @Parameter(shortName = EXPIRY_TIMEOUT, longName = "Expiry Timeout",
            description = "Default Request Expiry Timeout, ms (Default: 60000)", optional = true)
    private String expiryTimeout;
    @Parameter(shortName = WINDOW_SIZE, longName = "Default Window Size",
            description = "Default Window Size (Default: 5)", optional = true)
    private String windowSize;
    @Parameter(shortName = CHARSET_NAME, longName = "Charset Name",
            description = "Charset Name (Default: UCS-2)", optional = true)
    private String charsetName;
    @Parameter(shortName = SYSTEM_ID, longName = "System Id",
            description = "System Id (Default: uim)", optional = true)
    private String systemId;
    @Parameter(shortName = MESSAGE_ID, longName = "Response Message ID",
            description = "Response Message ID",
            optional = true, forServer = false, forTemplate = true, isDynamic = true)
    private String messageId;
    @Parameter(shortName = COMMAND_STATUS, longName = "Response Command Status",
            description = "Response Command Status (0 - success)",
            optional = true, forServer = false, forTemplate = true, isDynamic = true)
    private String commandStatus;

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-smpp";
    }

    @Override
    public String getShortName() {
        return "SMPP Inbound";
    }
}
