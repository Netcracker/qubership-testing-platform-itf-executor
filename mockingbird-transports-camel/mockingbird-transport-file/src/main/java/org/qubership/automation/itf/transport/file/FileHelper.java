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

package org.qubership.automation.itf.transport.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.camel.Helper;

public class FileHelper {

    public static String buildUri(ConnectionProperties properties, Map<String, Object> extraProps)
            throws IOException {
        String type = properties.obtain(PropertyConstants.File.TYPE);
        String host = properties.obtain(PropertyConstants.File.HOST);
        String remotePath = properties.obtain(PropertyConstants.File.PATH);
        String username = properties.obtain(PropertyConstants.File.PRINCIPAL);
        String password = properties.obtain(PropertyConstants.File.CREDENTIALS);
        Object fileName = properties.obtain(PropertyConstants.File.DESTINATION_FILE_NAME);
        boolean isSftp = "sftp".equals(type);
        boolean passIsBlank = Objects.isNull(password) || isNull(password);
        String sshKey = getSshKey(properties);
        boolean isSftpWithKey = isSftp && Strings.isNotEmpty(sshKey);
        stopIfRequiredPropertiesEmpty(remotePath, type, isSftp, sshKey, password, username);
        return type
                + "://"
                + appendIfHas(username, "@")
                + appendIfHas(host, "/")
                + remotePath
                + appendIfHas("?password=", password)
                + ((passIsBlank) ? '?' : '&')
                + "fileName=" + fileName
                + (isSftp ? "&preferredAuthentications=publickey,password" : Strings.EMPTY)
                + (isSftpWithKey ? "&privateKeyFile=" + createTempPemFile(sshKey).getPath() : Strings.EMPTY)
                + Helper.setExtraProperties(extraProps);
    }

    private static void stopIfRequiredPropertiesEmpty(String remotePath, String type, boolean isSftp, String sshKey,
                                                      String password, String username) {
        if (isNull(remotePath) || isNull(type)) {
            throw new IllegalArgumentException("Path/type can't be empty");
        }
        if (isSftp && Strings.isEmpty(sshKey) && Strings.isEmpty(password)) {
            throw new IllegalArgumentException("Password/ssh_key can't be empty! Please fill one of them");
        }
        if (isSftp && Strings.isEmpty(username)) {
            throw new IllegalArgumentException("Username can't be empty!");
        }
    }

    public static String checkDestinationName(String destFileName) {
        if (StringUtils.isBlank(destFileName)) {
            throw new IllegalArgumentException("Destination File Name is empty!");
        }
        return destFileName;
    }

    public static boolean isNull(String s) {
        return StringUtils.isBlank(s) || "null".equals(s);
    }

    private static String appendIfHas(String... s) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s1 : s) {
            if (isNull(s1)) {
                return "";
            }
            stringBuilder.append(s1);
        }
        return stringBuilder.toString();
    }

    private static String getSshKey(ConnectionProperties properties) {
        Object sshKeyPropertiesValue = properties.get(PropertyConstants.File.SSH_KEY);
        //noinspection unchecked
        return Objects.nonNull(sshKeyPropertiesValue) && !((List<String>) sshKeyPropertiesValue).isEmpty()
                ? String.join("\n", ((List<String>) sshKeyPropertiesValue))
                : null;
    }

    @Nonnull
    private static File createTempPemFile(String sshKey) throws IOException {
        File tmpfile = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".pem");
        tmpfile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));
        writer.write(sshKey);
        writer.close();
        return tmpfile;
    }
}
