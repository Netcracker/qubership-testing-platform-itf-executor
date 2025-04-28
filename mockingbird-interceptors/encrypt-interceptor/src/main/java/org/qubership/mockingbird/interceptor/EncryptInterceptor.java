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

package org.qubership.mockingbird.interceptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.annotation.ApplyToTransport;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

@ApplyToTransport(
        transports = {
                "org.qubership.automation.itf.transport.file.outbound.FileOutbound"
        })
@Named(value = "Encrypting")
public class EncryptInterceptor extends ContentInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptInterceptor.class);
    private final String RECIPIENT_ID = "Recipient Id";
    private final String RECIPIENT_ID_DESCRIPTION = "Encrypt the message for this user";
    private final String ENCRYPTED_FILE_EXTENSION = "Encrypted file extension";
    private final String ENCRYPTED_FILE_EXTENSION_DESCRIPTION = "Extension for the encrypted file";
    private final String[] ENCRYPTED_FILE_EXTENSION_OPTIONS = new String[]{"", "gpg"};
    private final String INTERCEPTOR_COMMAND_FILE = "./interceptors/encrypt-interceptor/interceptor.command";
    private final String DESTINATION_FILENAME_PROPERTY = "destinationFileName";

    public EncryptInterceptor() {
    }

    public EncryptInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    public Message apply(Message message) throws Exception {
        LOGGER.info("Encrypting started...");
        File fileForEncrypting = message.getFile();
        if (fileForEncrypting == null) {
            Object destinationFileName = message.getConnectionProperties().get(DESTINATION_FILENAME_PROPERTY);
            String fileNameString = destinationFileName != null
                    ? FilenameUtils.getName(destinationFileName.toString()) : StringUtils.EMPTY;
            if (!StringUtils.EMPTY.equals(fileNameString)) {
                if (fileNameString.contains("../") || fileNameString.contains("\\..")) {
                    throw new IllegalArgumentException("Destination File Name contains path elements which can "
                            + "cause Path Traversal vulnerability ('../' and/or '\\..').");
                }
                fileForEncrypting = createSourceFileWithContent(fileNameString, message.getText());
            } else {
                LOGGER.error("Encrypting won't be performed, because the destination filename is empty.");
            }
        }
        if (fileForEncrypting != null) {
            String fileForEncryptingPath = fileForEncrypting.getPath().replace("\\", "/");
            File encryptedFile = new File(fileForEncryptingPath + ".result");
            InterceptorParams interceptorParams = interceptor.getParameters();
            String command = getEncryptCommand(
                    (interceptorParams == null) ? null : interceptorParams.get(RECIPIENT_ID),
                    encryptedFile.getPath().replace("\\", "/"),
                    fileForEncryptingPath);
            if (!StringUtils.isEmpty(command)) {
                Runtime rt = Runtime.getRuntime();
                Process pr = rt.exec(command);
                int i = pr.waitFor();
                if (i == 0) {
                    fileForEncrypting.delete();
                    String ext = (interceptorParams == null) ? null : interceptorParams.get(ENCRYPTED_FILE_EXTENSION);
                    File resultFile = new File(fileForEncryptingPath + (StringUtils.isEmpty(ext) ? "" : "." + ext));
                    encryptedFile.renameTo(resultFile);
                    message.setFile(resultFile);
                    LOGGER.info("Encrypting completed successfully.");
                } else {
                    LOGGER.error("Encrypting failed. No changes have been applied. Status of process: " + i);
                }
            } else {
                LOGGER.warn("Command to perform encryption is empty. No changes have been applied.");
            }
        }
        return message;
    }

    @Override
    public String validate() {
        return InterceptorHelper.validate(interceptor, this);
    }

    public List<InterceptorPropertyDescriptor> getParameters() {
        List<InterceptorPropertyDescriptor> parameters = new ArrayList<>();
        InterceptorParams interceptorParams = interceptor.getParameters();
        InterceptorPropertyDescriptor recipientId = new InterceptorPropertyDescriptor(RECIPIENT_ID,
                RECIPIENT_ID,
                RECIPIENT_ID_DESCRIPTION,
                InterceptorConstants.TEXTFIELD,
                (interceptorParams == null) ? null : interceptorParams.get(RECIPIENT_ID),
                false);
        parameters.add(recipientId);
        InterceptorPropertyDescriptor fileExtension = new InterceptorPropertyDescriptor(ENCRYPTED_FILE_EXTENSION,
                ENCRYPTED_FILE_EXTENSION,
                ENCRYPTED_FILE_EXTENSION_DESCRIPTION,
                InterceptorConstants.LIST,
                ENCRYPTED_FILE_EXTENSION_OPTIONS,
                (interceptorParams == null) ? null : interceptorParams.get(ENCRYPTED_FILE_EXTENSION),
                true);
        parameters.add(fileExtension);
        return parameters;
    }

    private String getEncryptCommand(String... args) {
        String command = readCommandFromFile();
        if (!StringUtils.isEmpty(command)) {
            for (int i = 0; i < args.length; i++) {
                command = command.replace("args" + i, args[i]);
            }
            LOGGER.info("Command for start the encrypting: " + command);
            return command;
        }
        return StringUtils.EMPTY;
    }

    private File createSourceFileWithContent(String filename, String content) throws IOException {
        File fileWithCorrectName = new File(Files.createTempDir().getPath() + "/" + filename);
        FileUtils.write(fileWithCorrectName, content);
        return fileWithCorrectName;
    }

    private String readCommandFromFile() {
        try {
            return FileUtils.readFileToString(new File(INTERCEPTOR_COMMAND_FILE));
        } catch (IOException e) {
            LOGGER.error("File with path \"{}\" wasn't found. Command for interceptor will be empty.",
                    INTERCEPTOR_COMMAND_FILE);
        }
        return StringUtils.EMPTY;
    }
}
