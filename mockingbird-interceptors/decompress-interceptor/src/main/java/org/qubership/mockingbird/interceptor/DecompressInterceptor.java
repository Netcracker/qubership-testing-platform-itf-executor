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
@Named(value = "Decompressing")
public class DecompressInterceptor extends ContentInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecompressInterceptor.class);
    private final String DECOMPRESSING_TYPE = "Decompressing Type";
    private final String DECOMPRESSING_TYPE_DESCRIPTION = "Type of decompressing the file";
    private final String[] DECOMPRESSING_TYPE_OPTIONS = new String[]{"unzip", "gzip", "tar", "unrar"};
    private final String GZIP_DECOMPRESSING_TYPE = "gzip";
    private final String TAR_DECOMPRESSING_TYPE = "tar";
    private final String UNZIP_DECOMPRESSING_TYPE = "unzip";
    private final String UNRAR_DECOMPRESSING_TYPE = "unrar";
    private final String INTERCEPTOR_COMMAND_FILEPATH = "./interceptors/decompress-interceptor/";
    private final String INTERCEPTOR_COMMAND_FILE_POSTFIX = "interceptor.command";
    private final String DECOMPRESSING_RESULT_INTERNAL_DIR_NAME = "result";

    public DecompressInterceptor() {
    }

    public DecompressInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    @Override
    public Message apply(Message message) throws Exception {
        LOGGER.info("Decompressing started...");
        File sourcefileForDecompressing = message.getFile();
        File dirForDecompressing = new File(sourcefileForDecompressing.getParent());
        String decompressingType = (interceptor.getParameters() == null)
                ? null : interceptor.getParameters().get(DECOMPRESSING_TYPE);
        File dirWithDecompressingResult = getDirWithDecompressingResult(dirForDecompressing, decompressingType);
        String command = getCommand(getDecompressCommand(decompressingType), sourcefileForDecompressing,
                dirWithDecompressingResult, decompressingType);
        if (!StringUtils.isEmpty(command)) {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.directory(dirForDecompressing);
            Process pr = processBuilder.start();
            int i = pr.waitFor();
            if (i == 0) {
                message.setFile(getResultFile(dirWithDecompressingResult));
                LOGGER.info("Decompressing completed successfully.");
            } else {
                LOGGER.error("Decompressing failed. No changes has been applied. Status of process: " + i);
            }
        } else {
            LOGGER.info("Command for start the decompressing is empty. No changes have been applied.");
        }
        return message;
    }

    @Override
    public String validate() {
        return InterceptorHelper.validate(interceptor, this);
    }

    @Override
    public List<InterceptorPropertyDescriptor> getParameters() {
        List<InterceptorPropertyDescriptor> parameters = new ArrayList<>();
        InterceptorPropertyDescriptor compressingType = new InterceptorPropertyDescriptor(DECOMPRESSING_TYPE,
                DECOMPRESSING_TYPE,
                DECOMPRESSING_TYPE_DESCRIPTION,
                InterceptorConstants.LIST,
                DECOMPRESSING_TYPE_OPTIONS,
                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(DECOMPRESSING_TYPE),
                false);
        parameters.add(compressingType);
        return parameters;
    }

    private File getDirWithDecompressingResult(File dirForDecompressing, String decompressingType) {
        File dirWithDecompressingResult;
        if (GZIP_DECOMPRESSING_TYPE.equals(decompressingType)) {
            dirWithDecompressingResult = dirForDecompressing;
        } else {
            dirWithDecompressingResult =
                    new File(dirForDecompressing.getPath() + "\\" + DECOMPRESSING_RESULT_INTERNAL_DIR_NAME);
            dirWithDecompressingResult.mkdirs();
        }
        return dirWithDecompressingResult;
    }

    private String getDecompressCommand(String decompressingType) {
        return readCommandFromFile(decompressingType);
    }

    private String getCommand(String command, File fileForDecompressing, File dirForDecompressing,
                              String decompressingType) {
        if (!StringUtils.isEmpty(command)) {
            List<String> args = getArgsForCommand(fileForDecompressing, dirForDecompressing, decompressingType);
            for (int i = 0; i < args.size(); i++) {
                command = command.replace("args" + i, args.get(i));
            }
            LOGGER.info("Command for start decompressing: {}", command);
        }
        return command;
    }

    private List<String> getArgsForCommand(File fileForDecompressing, File dirForDecompressing,
                                           String decompressingType) {
        List<String> args = new LinkedList<>();
        switch (decompressingType) {
            case GZIP_DECOMPRESSING_TYPE:
                args.add(fileForDecompressing.getName());
            case UNRAR_DECOMPRESSING_TYPE:
            case TAR_DECOMPRESSING_TYPE:
            case UNZIP_DECOMPRESSING_TYPE:
                args.add(fileForDecompressing.getName());
                args.add(dirForDecompressing.getName());
                break;
            default:
                break;
        }
        return args;
    }

    private File getResultFile(File file) throws IOException {
        File[] files = file.listFiles();
        if (files != null) {
            return files[0];
        }
        return file;
    }

    private String readCommandFromFile(String decompressingType) {
        try {
            String path = INTERCEPTOR_COMMAND_FILEPATH + decompressingType + '.' + INTERCEPTOR_COMMAND_FILE_POSTFIX;
            return FileUtils.readFileToString(new File(path));
        } catch (IOException e) {
            LOGGER.error("File with path \"{}\" wasn't found. Command for interceptor will be empty.",
                    INTERCEPTOR_COMMAND_FILEPATH);
        }
        return StringUtils.EMPTY;
    }
}
