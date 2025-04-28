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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.qubership.automation.itf.core.util.eds.ExternalDataManagementService;
import org.qubership.automation.itf.core.util.eds.model.FileEventType;
import org.qubership.automation.itf.core.util.eds.model.FileInfo;
import org.qubership.automation.itf.core.util.eds.model.UIFileInfo;
import org.qubership.automation.itf.core.util.eds.service.EdsContentType;
import org.qubership.automation.itf.core.util.eds.service.EdsMetaInfo;
import org.qubership.automation.itf.core.util.eds.service.FileManagementService;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.file.TreeNode;
import org.qubership.automation.itf.ui.util.FileUploadHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileManagerService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private final ExternalDataManagementService externalDataManagementService;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final FileManagementService fileManagementService;
    @Value("${eds.gridfs.enabled}")
    private boolean edsGridfsEnabled;

    public FileManagerService(ExternalDataManagementService externalDataManagementService,
                              ExecutorToMessageBrokerSender executorToMessageBrokerSender,
                              FileManagementService fileManagementService) {
        this.externalDataManagementService = externalDataManagementService;
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
        this.fileManagementService = fileManagementService;
    }

    public TreeNode[] getPathTreeNode(UUID projectUuid) {
        TreeNode rootTreeNode = new TreeNode();
        rootTreeNode.setText("/");
        rootTreeNode.setContentType("ROOT");
        List<TreeNode> nodeList = rootTreeNode.getNodes();

        TreeNode datasetNode = new TreeNode(EdsContentType.DATASET.getStringValue(),
                EdsContentType.DATASET.getStringValue());
        buildTree(datasetNode, datasetNode.getFilePath(), datasetNode.getContentType(), projectUuid);
        nodeList.add(datasetNode);

        TreeNode diameterNode = new TreeNode(EdsContentType.DIAMETER_DICTIONARY.getStringValue(),
                EdsContentType.DIAMETER_DICTIONARY.getStringValue());
        buildTree(diameterNode, diameterNode.getFilePath(), diameterNode.getContentType(), projectUuid);
        nodeList.add(diameterNode);

        TreeNode fastStubNode = new TreeNode(EdsContentType.FAST_STUB.getStringValue(),
                EdsContentType.FAST_STUB.getStringValue());
        buildTree(fastStubNode, fastStubNode.getFilePath(), fastStubNode.getContentType(), projectUuid);
        nodeList.add(fastStubNode);

        TreeNode keystoreNode = new TreeNode(EdsContentType.KEYSTORE.getStringValue(),
                EdsContentType.KEYSTORE.getStringValue());
        nodeList.add(keystoreNode);

        TreeNode wsdlXsdNode = new TreeNode(EdsContentType.WSDL_XSD.getStringValue(),
                EdsContentType.WSDL_XSD.getStringValue());
        buildTree(wsdlXsdNode, wsdlXsdNode.getFilePath(), wsdlXsdNode.getContentType(), projectUuid);

        nodeList.add(wsdlXsdNode);
        return new TreeNode[]{rootTreeNode};
    }

    public Set<UIFileInfo> getFilesByPath(String filePath, EdsContentType contentType, UUID projectUuid) {
        String sanitizedFilePath = FilenameUtils.normalize(filePath);
        return edsGridfsEnabled
                ? getFilesInfoFromExternalStorage(sanitizedFilePath, contentType.getStringValue(), projectUuid)
                : getFilesInfoFromCurrentService(sanitizedFilePath, contentType.getStringValue(), projectUuid);
    }

    public UIResult remove(List<UIFileInfo> filesInfo, UUID projectUuid, String tenantId) throws IOException {
        try {
            for (UIFileInfo uiFileInfo : filesInfo) {
                externalDataManagementService.getExternalStorageService()
                        .delete(uiFileInfo.getContentType(),
                                EdsContentType.KEYSTORE.getStringValue().equals(uiFileInfo.getContentType())
                                        ? null : projectUuid,
                                uiFileInfo.getFilePath(), uiFileInfo.getFileName());
                log.info("File by path '{}' was deleted from external storage successfully. "
                                + "Prepare to notify others via topic...",
                        fileManagementService.getDirectoryPath(uiFileInfo.getContentType(), projectUuid,
                                uiFileInfo.getFilePath()).resolve(uiFileInfo.getFileName()));
                FileUploadHelper.checkStoredObjectIdAndSendMessageToExternalDataStorageUpdateTopic(null,
                        uiFileInfo.getFileName(), uiFileInfo.getFilePath(), uiFileInfo.getContentType(), projectUuid,
                        null, FileEventType.DELETE, executorToMessageBrokerSender, tenantId);
            }
            return new UIResult(true, "");
        } catch (Exception e) {
            return new UIResult(false, "An error occurred while deleting of file(s).");
        }
    }

    public UIResult getContent(EdsContentType contentType, String filePath, String fileName, UUID projectUuid) {
        try {
            String sanitizedFileName = FilenameUtils.getName(fileName);
            String sanitizedFilePath = FilenameUtils.normalize(filePath);
            Path path = fileManagementService
                    .getDirectoryPath(contentType.getStringValue(), projectUuid, sanitizedFilePath)
                    .resolve(sanitizedFileName);
            File srcFile = new File(path.toString());
            String content;

            String fileContentType = Files.probeContentType(path); //image/jpeg || image/png
            boolean isImageFile;
            if (Objects.isNull(fileContentType)) {
                Pattern pattern = Pattern.compile("\\.(jpe?g|gif|bmp|png|svg|tiff?)$");
                Matcher matcher = pattern.matcher(sanitizedFileName);
                isImageFile = matcher.find();
            } else {
                isImageFile = fileContentType.contains("image/");
            }

            if (isImageFile) {
                byte[] bytes = FileUtils.readFileToByteArray(srcFile);
                content = Base64.getEncoder().encodeToString(bytes);
            } else {
                content = FileUtils.readFileToString(srcFile);
            }
            UIResult uiResult = new UIResult();
            uiResult.setMessage(content);
            return uiResult;
        } catch (Exception e) {
            String error = "An error occurred while getting file contents. ";
            log.error(error, e);
            return new UIResult(false, error + e.getMessage());
        }
    }

    private void buildTree(TreeNode rootTreeNode, String filePath, String contentType, UUID projectUuid) {
        Path path = fileManagementService.getDirectoryPath(contentType, projectUuid, filePath);
        if (!new File(path.toString()).exists()) {
            return;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            for (Path pathObj : directoryStream) {
                BasicFileAttributes attributes = Files.readAttributes(pathObj, BasicFileAttributes.class);
                if (attributes.isDirectory()) {
                    TreeNode childTreeNode = new TreeNode();
                    childTreeNode.setText(pathObj.getFileName().toString());
                    childTreeNode.setFilePath(
                            Paths.get(rootTreeNode.getFilePath(), pathObj.getFileName().toString()).toString());
                    childTreeNode.setContentType(contentType);
                    rootTreeNode.getNodes().add(childTreeNode);
                    buildTree(childTreeNode, childTreeNode.getFilePath(), contentType, projectUuid);
                }
            }
        } catch (Exception e) {
            log.error("An error occurred while getting path for files from storage.", e);
        }
    }

    private Set<UIFileInfo> getFilesInfoFromExternalStorage(String filePath, String contentType, UUID projectUuid) {
        try {
            Set<FileInfo> filesInfo;
            if (EdsContentType.KEYSTORE.getStringValue().equals(contentType)) {
                filesInfo = externalDataManagementService.getExternalStorageService().getKeyStoreFileInfo();
            } else {
                Map<String, Object> map = new HashMap();
                map.put(EdsMetaInfo.FILE_PATH.getStringValue(),
                        StringUtils.isBlank(filePath)
                                ? filePath
                                : "/" + filePath.replaceAll("\\\\", "/"));
                map.put(EdsMetaInfo.CONTENT_TYPE.getStringValue(), contentType);
                map.put(EdsMetaInfo.PROJECT_UUID.getStringValue(), projectUuid);
                filesInfo = externalDataManagementService.getExternalStorageService()
                        .getFilesInfoByMetadataMapParams(map);
            }
            return filesInfo.stream()
                    .map(fileInfo -> new UIFileInfo(fileInfo.getObjectId().toString(), fileInfo.getFileName(),
                            fileInfo.getFilePath(), fileInfo.getFileLength(),
                            dateFormat.format(fileInfo.getUploadDate()), fileInfo.getContentType(),
                            fileInfo.getUserName())
                    ).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("An error occurred while getting files from external storage.", e);
            return Collections.emptySet();
        }
    }

    private Set<UIFileInfo> getFilesInfoFromCurrentService(String filePath, String contentType, UUID projectUuid) {
        Path folderPath = fileManagementService.getDirectoryPath(contentType, projectUuid, filePath);
        if (!new File(folderPath.toString()).exists()) {
            return Collections.emptySet();
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folderPath)) {
            Set<UIFileInfo> files = new HashSet<>();
            for (Path path : directoryStream) {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                if (!attributes.isDirectory()) {
                    UIFileInfo fileInfo = new UIFileInfo(ObjectId.get().toString(), path.getFileName().toString(),
                            filePath, attributes.size(),
                            dateFormat.format(new Date(attributes.lastModifiedTime().toMillis())), contentType,
                            "Undefined");
                    files.add(fileInfo);
                }
            }
            return files;
        } catch (IOException e) {
            log.error("An error occurred while getting files from local storage (path {}, contentType {}, project {})",
                    filePath, contentType, projectUuid, e);
            return Collections.emptySet();
        }
    }
}
