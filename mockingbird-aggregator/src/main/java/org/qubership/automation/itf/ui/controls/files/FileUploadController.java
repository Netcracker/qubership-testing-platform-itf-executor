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

package org.qubership.automation.itf.ui.controls.files;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.exceptions.operation.FileProcessingException;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.eds.ExternalDataManagementService;
import org.qubership.automation.itf.core.util.eds.model.FileEventType;
import org.qubership.automation.itf.core.util.eds.service.EdsContentType;
import org.qubership.automation.itf.core.util.eds.service.FileManagementService;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.util.FileUploadHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class FileUploadController {

    private final ExternalDataManagementService externalDataManagementService;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;

    @Autowired
    public FileUploadController(ExternalDataManagementService externalDataManagementService,
                                ExecutorToMessageBrokerSender executorToMessageBrokerSender) {
        this.externalDataManagementService = externalDataManagementService;
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
    }

    @Transactional
    @PreAuthorize("(@entityAccess.checkAccess(#projectUuid, \"READ\") and "
            + "!T(org.qubership.automation.itf.core.util.eds.service.EdsContentType).KEYSTORE.equals(#contentType) and "
            + "!T(org.qubership.automation.itf.core.util.eds.service.EdsContentType).FAST_STUB.equals(#contentType)) "
            + "or (@entityAccess.isSupport() or @entityAccess.isAdmin())")
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @AuditAction(auditAction = "Upload files with {{#contentType}} contentType, {{#filePath}} filePath for project "
            + "{{#projectId}}")
    public UIResult uploadAttachments(@RequestParam(value = "contentType") EdsContentType contentType,
                                      @RequestParam(value = "filePath", required = false, defaultValue = "") String filePath,
                                      @RequestParam(value = "projectId") BigInteger projectId,
                                      @RequestParam(value = "projectUuid") UUID projectUuid,
                                      @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId,
                                      @RequestParam(value = "file") MultipartFile[] files) throws IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userName = "Undefined";
        UUID userId = null;
        if (principal instanceof KeycloakPrincipal) {
            AccessToken accessToken = ((KeycloakPrincipal) principal).getKeycloakSecurityContext().getToken();
            userId = UUID.fromString(((KeycloakPrincipal) principal).getName());
            userName = accessToken.getName();
        }
        log.info("Upload files to: contentType '{}', filePath '{}', projectId '{}', by user [name: {}, id: {}]...",
                contentType, filePath, projectId, userName, userId);
        FileManagementService fileManagementService = externalDataManagementService.getFileManagementService();
        filePath = fileManagementService.calcPredefinedPath(contentType.getStringValue(), filePath);
        String predefinedFileName = fileManagementService.calcPredefinedFileName(contentType.getStringValue(), null);
        if (fileManagementService.isContentTypeExist(contentType.getStringValue())) {
            if (fileManagementService.dontUseProjectForCreatingDirectory(contentType.getStringValue())) {
                log.info("Prepare to upload non-project files...");
                return uploadFiles(contentType.getStringValue(), null, filePath, files, tenantId, predefinedFileName,
                        userName, userId);
            } else {
                StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class).getById(projectId);
                if (project != null) {
                    log.info("Prepare to upload files, project uuid = {}, id = {}...", projectUuid, projectId);
                    return uploadFiles(contentType.getStringValue(), projectUuid, filePath, files, tenantId,
                            predefinedFileName, userName, userId);
                } else {
                    log.error("Project with id = {} is not found", projectId);
                    return new UIResult(false, String.format("Project with id = %s is not found", projectId));
                }
            }
        } else {
            log.error("Content type = {} is not supported", contentType);
            return new UIResult(false, String.format("Content type=%s is not supported", contentType));
        }
    }

    private UIResult uploadFiles(String contentType, UUID projectUuid, String filePath, MultipartFile[] files,
                                 String tenantId, String predefinedFileName, String userName, UUID userId) {
        int cnt = 0;
        for (MultipartFile file : files) {
            try {
                log.info("Prepare to store file '{}' ...", file.getName());
                String fileName = file.getOriginalFilename();
                if (predefinedFileName != null) {
                    log.info("File '{}' will be renamed to '{}' because it's name predefined by system logic...",
                            file.getName(), predefinedFileName);
                    fileName = predefinedFileName;
                }
                ObjectId storedObjectId = externalDataManagementService.getExternalStorageService()
                        .store(contentType, projectUuid, userName, userId,
                                filePath, fileName, file.getInputStream());
                log.info("File '{}' is stored. Prepare to duplicate/notify others via topic...", file.getName());
                FileUploadHelper.checkStoredObjectIdAndSendMessageToExternalDataStorageUpdateTopic(storedObjectId,
                        fileName, filePath, contentType, projectUuid,
                        new ByteArrayInputStream(file.getBytes()), FileEventType.UPLOAD,
                        executorToMessageBrokerSender, tenantId);
                file.getInputStream().close();
                log.info("File '{}' processing is completed", file.getName());
                cnt++;
            } catch (IOException e) {
                throw new FileProcessingException(e.getMessage());
            }
        }
        String msg = (cnt > 0) ? "All (" + cnt + ") files are uploaded successfully." : "No files to process!";
        log.info(msg);
        return new UIResult(true, msg);
    }
}
