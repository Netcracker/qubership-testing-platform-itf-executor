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

package org.qubership.automation.itf.transport.file.outbound;

import static org.qubership.automation.itf.transport.camel.CamelContextProvider.CAMEL_CONTEXT;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.FileUtils;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.transport.base.AbstractFileOverOutboundTransportImpl;
import org.qubership.automation.itf.transport.camel.Helper;
import org.qubership.automation.itf.transport.file.FileHelper;

import com.google.common.io.Files;

@UserName("Outbound File over SMB/FTP/SFTP")
public class FileOutbound extends AbstractFileOverOutboundTransportImpl {

    @Parameter(shortName = PropertyConstants.Commons.ENDPOINT_PROPERTIES, longName = "Extra Endpoint Properties",
            description = PropertyConstants.Commons.ENDPOINT_PROPERTIES_DESCRIPTION, isDynamic = true, optional = true)
    protected Map<String, Object> properties;

    @Parameter(shortName = PropertyConstants.File.HOST, longName = "Host",
            description = PropertyConstants.File.HOST_DESCRIPTION, optional = true)
    private String host;

    @Parameter(shortName = PropertyConstants.File.PATH, longName = "Path",
            description = PropertyConstants.File.PATH_DESCRIPTION, optional = true, isDynamic = true)
    private String path;

    @Parameter(shortName = PropertyConstants.File.PRINCIPAL, longName = "Security Principal",
            description = PropertyConstants.Commons.PRINCIPAL_DESCRIPTION, optional = true)
    private String principal;

    @Parameter(shortName = PropertyConstants.File.CREDENTIALS, longName = "Security Credentials",
            description = PropertyConstants.Commons.CREDENTIALS_DESCRIPTION, optional = true)
    private String credentials;

    @Parameter(shortName = PropertyConstants.File.DESTINATION_FILE_NAME, longName = "Destination File Name",
            description = PropertyConstants.File.FILE_NAME_DESCRIPTION, forServer = false, forTemplate = true,
            isDynamic = true)
    private String fileName;

    @Parameter(shortName = PropertyConstants.File.TYPE, longName = "Type",
            description = PropertyConstants.File.TYPE_DESCRIPTION)
    @Options({"file", "ftp", "sftp"})
    private String type;

    @Parameter(shortName = PropertyConstants.File.SSH_KEY, longName = PropertyConstants.File.SSH_KEY_DESCRIPTION,
            description = "Ssh private key string (pem format only)", optional = true)
    private List<String> sshKey;

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-file";
    }

    @Override
    public String getShortName() {
        return "File Outbound";
    }

    @Override
    public String send(Message message, String sessionId, UUID projectUuid) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ConnectionProperties properties = new ConnectionProperties(message.getConnectionProperties());
        /*  <extraProps>, <template> and <endpoint> are created as independent variables
            in order to manage their configuration and statuses and (for <extraProps>) add custom processing of
            properties
            instead of Camel processing or in addition to it.
            It was done in order to close FTP/SFTP connections.
            But finally the issue was fixed via Camel property disconnect=true (http://camel.apache.org/ftp.html)

            So these variables are currently unneeded but...
         */
        Map<String, Object> extraProps = Helper.setExtraPropertiesMap(
                properties.obtain(PropertyConstants.Commons.ENDPOINT_PROPERTIES));
        ProducerTemplate template = CAMEL_CONTEXT.createProducerTemplate();
        String uri = FileHelper.buildUri(properties, extraProps);
        Endpoint endpoint = template.getCamelContext().getEndpoint(uri);
        File fileToSend = createFile(message, properties);
        template.sendBody(endpoint, fileToSend);
        return "";
    }

    private File createFile(Message message, ConnectionProperties properties) throws Exception {
        File file = message.getFile();
        if (file == null) {
            String destinationFileName = FileHelper.checkDestinationName(
                    properties.obtain(PropertyConstants.File.DESTINATION_FILE_NAME));
            properties.put(PropertyConstants.File.DESTINATION_FILE_NAME, destinationFileName);
            String path = Files.createTempDir().getPath() + "\\";
            file = new File(path + destinationFileName);
            FileUtils.write(file, message.getText().replace("\n", System.lineSeparator()));
        } else {
            /*  There is a specific case:
                    - configured value of DESTINATION_FILE_NAME contains path.
                In that case we must extract path and concat it with filename (NITP-5262)
            */
            String destinationPath = "";
            String destinationFileName = properties.obtain(PropertyConstants.File.DESTINATION_FILE_NAME);
            int lastPos = destinationFileName.lastIndexOf("/");
            if (lastPos > -1) {
                destinationPath = destinationFileName.substring(0, lastPos + 1);
            } else {
                // May be it's Windows/DOS path?
                lastPos = destinationFileName.lastIndexOf("\\");
                if (lastPos > -1) {
                    destinationPath = destinationFileName.substring(0, lastPos + 1);
                }
            }
            properties.put(PropertyConstants.File.DESTINATION_FILE_NAME, destinationPath + file.getName());
        }
        return file;
    }
}
