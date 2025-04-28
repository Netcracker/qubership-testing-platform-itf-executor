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

package org.qubership.automation.itf.transport.file.inbound;

import java.util.List;
import java.util.Map;

import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractInboundTransportImpl;

@UserName("Inbound File over SMB/FTP/SFTP")
public class FileInbound extends AbstractInboundTransportImpl {

    @Parameter(shortName = PropertyConstants.Commons.ENDPOINT_PROPERTIES, longName = "Extra Endpoint Properties",
            description = PropertyConstants.Commons.ENDPOINT_PROPERTIES_DESCRIPTION,
            forServer = true, forTemplate = false, isDynamic = true, optional = true)
    protected Map<String, Object> properties;

    @Parameter(shortName = PropertyConstants.File.HOST, longName = "Host",
            description = PropertyConstants.File.HOST_DESCRIPTION, optional = true)
    private String host;

    @Parameter(shortName = PropertyConstants.File.PATH, longName = "Path",
            description = PropertyConstants.File.PATH_DESCRIPTION)
    private String path;

    @Parameter(shortName = PropertyConstants.File.PRINCIPAL, longName = "Security Principal",
            description = PropertyConstants.Commons.PRINCIPAL_DESCRIPTION, optional = true)
    private String principal;

    @Parameter(shortName = PropertyConstants.File.CREDENTIALS, longName = "Security Credentials",
            description = PropertyConstants.Commons.CREDENTIALS_DESCRIPTION, optional = true)
    private String credentials;

    @Parameter(shortName = PropertyConstants.File.TYPE, longName = "Type",
            description = PropertyConstants.File.TYPE_DESCRIPTION)
    @Options({"file", "ftp", "sftp"})
    private String type;
    @Parameter(shortName = PropertyConstants.File.SSH_KEY, longName = "Ssh key", description = "Ssh key", optional =
            true)
    private List<String> sshKey;

    @Override
    public String getShortName() {
        return "File Inbound";
    }

    @Override
    public Mep getMep() {
        return Mep.INBOUND_REQUEST_ASYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-file";
    }
}
