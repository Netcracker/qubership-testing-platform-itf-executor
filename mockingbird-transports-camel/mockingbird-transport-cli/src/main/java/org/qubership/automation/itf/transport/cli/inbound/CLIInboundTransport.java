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

package org.qubership.automation.itf.transport.cli.inbound;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;

@UserName("CLI Inbound TCP/IP")
public class CLIInboundTransport extends AbstractInboundTransportImpl {

    @Parameter(shortName = PropertyConstants.Cli.Inbound.COMMAND_DELIMITER,
            longName = "Command delimiter",
            description = "Command delimiter; empty delimiter means new-line character",
            optional = true)
    private String delimiter;

    @Parameter(shortName = PropertyConstants.Cli.Inbound.ALLOWED_EMPTY,
            longName = "Allow empty commands",
            description = "Empty commands are allowed? - Yes or No (default)",
            optional = true)
    @Options({"No", "Yes"})
    private String allowedEmpty;

    @Parameter(shortName = PropertyConstants.Cli.Inbound.GREETING,
            longName = "Greeting message",
            description = "Greeting message",
            optional = true)
    private String greeting;

    @Parameter(shortName = PropertyConstants.Cli.REMOTE_IP,
            longName = "IP-address",
            description = "IP-address; don't use 127.0.0.1 for local machine")
    private String remoteIp;

    @Parameter(shortName = PropertyConstants.Cli.REMOTE_PORT, longName = "Port", description = "Port")
    private Integer remotePort;

    @Parameter(shortName = PropertyConstants.Cli.CONNECTION_TYPE,
            longName = "Connection type",
            description = "Connection type: TCP or UDP")
    @Options({"UDP", "TCP"})
    private String type;

    @Override
    public String getShortName() {
        return "Cli Inbound";
    }

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-cli";
    }
}
