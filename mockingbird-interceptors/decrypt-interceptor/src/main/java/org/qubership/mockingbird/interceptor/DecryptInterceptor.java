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
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.annotation.ApplyToTransport;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplyToTransport(transports = {"org.qubership.automation.itf.transport.file.inbound.FileInbound"})
@Named(value = "Decrypting")
public class DecryptInterceptor extends ContentInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecryptInterceptor.class);
    private final String PASSWORD = "Password";
    private final String PASSWORD_DESCRIPTION = "Password for the decrypting";
    private final String INTERCEPTOR_COMMAND_FILE = "./interceptors/decrypt-interceptor/interceptor.command";

    public DecryptInterceptor() {
    }

    public DecryptInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    public Message apply(Message message) throws Exception {
        LOGGER.info("Decrypting started...");
        File fileForDecrypting = message.getFile();
        File decryptedFile = createDecryptedFile(fileForDecrypting);
        String command = getDecryptCommand(
                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(PASSWORD),
                decryptedFile.getPath().replace("\\", "/"),
                fileForDecrypting.getPath().replace("\\", "/"));
        if (!StringUtils.isEmpty(command)) {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);
            int i = pr.waitFor();
            if (i == 0) {
                message.setFile(decryptedFile);
                LOGGER.info("Decrypting completed successfully.");
            } else {
                LOGGER.error("Decrypting failed. No changes have been applied. Status of process: " + i);
            }
        } else {
            LOGGER.info("Command for start the decrypting is empty. No changes have been applied.");
        }
        return message;
    }

    @Override
    public String validate() {
        return InterceptorHelper.validate(interceptor, this);
    }

    public List<InterceptorPropertyDescriptor> getParameters() {
        List<InterceptorPropertyDescriptor> parameters = new ArrayList<>();
        InterceptorPropertyDescriptor publicKeyringFile = new InterceptorPropertyDescriptor(PASSWORD,
                PASSWORD,
                PASSWORD_DESCRIPTION,
                InterceptorConstants.TEXTFIELD,
                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(PASSWORD),
                false);
        parameters.add(publicKeyringFile);
        return parameters;
    }

    private String getDecryptCommand(String... args) {
        String command = readCommandFromFile();
        if (!StringUtils.isEmpty(command)) {
            for (int i = 0; i < args.length; i++) {
                command = command.replace("args" + i, args[i]);
            }
            LOGGER.info("Command for start decrypting: {}", command);
            return command;
        }
        return StringUtils.EMPTY;
    }

    private File createDecryptedFile(File fileForDecrypting) {
        File directory = new File(fileForDecrypting.getParent() + "\\" + "result");
        directory.mkdirs();
        return new File(directory.getPath() + "\\" + getDecryptedFilename(fileForDecrypting.getName()));
    }

    private String getDecryptedFilename(String fileForDecrypting) {
        return fileForDecrypting.endsWith(".gpg")
                ? fileForDecrypting.substring(0, fileForDecrypting.length() - 4) : fileForDecrypting;
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
