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
import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.annotation.ApplyToTransport;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

@ApplyToTransport(transports = {"org.qubership.automation.itf.transport.file.outbound.FileOutbound"})
@Named(value = "Compressing")
public class CompressInterceptor extends ContentInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressInterceptor.class);
    private final String COMPRESSING_TYPE = "Compressing Type";
    private final String COMPRESSING_TYPE_DESCRIPTION = "Type of compressing the file";
    private final String[] COMPRESSING_TYPE_OPTIONS = new String[]{"zip", "gzip", "tar", "rar"};
    private final String GZIP_COMPRESSING_TYPE = "gzip";
    private final String TAR_COMPRESSING_TYPE = "tar";
    private final String ZIP_COMPRESSING_TYPE = "zip";
    private final String RAR_COMPRESSING_TYPE = "rar";
    private final String INTERCEPTOR_COMMAND_FILEPATH = "./interceptors/compress-interceptor/";
    private final String INTERCEPTOR_COMMAND_FILE_POSTFIX = "interceptor.command";
    private final String DESTINATION_FILENAME_PROPERTY = "destinationFileName";

    public CompressInterceptor() {
    }

    public CompressInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    public Message apply(Message message) throws Exception {
        LOGGER.info("Compressing started...");
        File fileForCompressing = message.getFile();
        if (fileForCompressing == null) {
            Object destinationFileName = message.getConnectionProperties().get(DESTINATION_FILENAME_PROPERTY);
            String fileNameString = destinationFileName != null
                    ? FilenameUtils.getName(destinationFileName.toString()) : StringUtils.EMPTY;
            if (!StringUtils.EMPTY.equals(fileNameString)) {
                if (fileNameString.contains("../") || fileNameString.contains("\\..")) {
                    throw new IllegalArgumentException("Destination File Name contains path elements which can "
                            + "cause Path Traversal vulnerability ('../' and/or '\\..').");
                }
                fileForCompressing = createSourceFileWithContent(fileNameString, message.getText());
            } else {
                LOGGER.error("Compression won't be performed, because the destination filename is empty.");
            }
        }
        if (fileForCompressing != null) {
            String compressingType = (interceptor.getParameters() == null)
                    ? null : interceptor.getParameters().get(COMPRESSING_TYPE);
            String command = getCompressCommand(fileForCompressing.getName(), compressingType);
            if (!StringUtils.isEmpty(command)) {
                ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
                processBuilder.directory(fileForCompressing.getParentFile());
                Process pr = processBuilder.start();
                int i = pr.waitFor();
                if (i == 0) {
                    String fullFilename = fileForCompressing.getPath().replace("\\", "/") + "."
                            + getFileExtension(compressingType);
                    message.setFile(new File(fullFilename));
                    LOGGER.info("Compressing completed successfully.");
                } else {
                    LOGGER.error("Compressing failed. No changes have been applied. Status of process: " + i);
                }
            } else {
                LOGGER.info("Command for start the compressing is empty. No changes have been applied.");
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
        InterceptorPropertyDescriptor compressingType = new InterceptorPropertyDescriptor(COMPRESSING_TYPE,
                COMPRESSING_TYPE,
                COMPRESSING_TYPE_DESCRIPTION,
                InterceptorConstants.LIST,
                COMPRESSING_TYPE_OPTIONS,
                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(COMPRESSING_TYPE),
                false);
        parameters.add(compressingType);
        return parameters;
    }

    private File createSourceFileWithContent(String filename, String content) throws IOException {
        File fileWithCorrectName = new File(Files.createTempDir().getPath() + "/" + filename);
        FileUtils.write(fileWithCorrectName, content);
        return fileWithCorrectName;
    }

    private String getCompressCommand(String filename, String compressingType) {
        String command = readCommandFromFile(compressingType);
        if (!StringUtils.isEmpty(command)) {
            List<String> args = getArgsForCommand(filename, compressingType);
            for (int i = 0; i < args.size(); i++) {
                command = command.replace("args" + i, args.get(i));
            }
            LOGGER.info("Command for start compressing: {}", command);
            return command;
        }
        return StringUtils.EMPTY;
    }

    private String readCommandFromFile(String compressingType) {
        try {
            String path = INTERCEPTOR_COMMAND_FILEPATH + compressingType + '.' + INTERCEPTOR_COMMAND_FILE_POSTFIX;
            return FileUtils.readFileToString(new File(path));
        } catch (IOException e) {
            //FIXME SZ: I'm not sure, that here we can catch only FileNotFound.
            LOGGER.error("File with path \"{}\" wasn't found. Command for interceptor will be empty.",
                    INTERCEPTOR_COMMAND_FILEPATH);
        }
        return StringUtils.EMPTY;
    }

    private List<String> getArgsForCommand(String filename, String compressingType) {
        List<String> args = new LinkedList<>();
        switch (compressingType) {
            case GZIP_COMPRESSING_TYPE:
                args.add(filename);
                break;
            case TAR_COMPRESSING_TYPE:
            case ZIP_COMPRESSING_TYPE:
            case RAR_COMPRESSING_TYPE:
                args.add(filename + "." + compressingType);
                args.add(filename);
                break;
            default:
                break;
        }
        return args;
    }

    private String getFileExtension(String compressingType) {
        return GZIP_COMPRESSING_TYPE.equals(compressingType) ? "gz" : compressingType;
    }
}
