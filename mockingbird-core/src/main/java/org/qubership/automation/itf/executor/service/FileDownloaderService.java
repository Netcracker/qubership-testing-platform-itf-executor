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

package org.qubership.automation.itf.executor.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.qubership.automation.itf.core.util.eds.service.EdsContentType;
import org.qubership.automation.itf.core.util.eds.service.FileManagementService;
import org.qubership.automation.itf.core.util.exception.ExportException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileDownloaderService {

    private final FileManagementService fileManagementService;
    @Value("${local.storage.directory}")
    private String rootFolder;

    public FileDownloaderService(FileManagementService fileManagementService) {
        this.fileManagementService = fileManagementService;
    }

    /**
     * Processing project files into zip file.
     *
     * @param typesToDownload types {@link EdsContentType} file to download
     * @param projectUuid     project UUID
     * @return the response entity {@link ResponseEntity}
     * @throws ExportException the export exception
     * @throws IOException     I/O exception has occurred
     */
    public ResponseEntity<InputStreamResource> zipProjectFiles(List<EdsContentType> typesToDownload, UUID projectUuid)
            throws IOException, ExportException {
        String rootDirectory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        Path rootPath = Paths.get(rootDirectory, "data_temp");
        if (!Files.exists(rootPath)) {
            Files.createDirectory(rootPath);
        }

        String sanitizedFileName = FilenameUtils.getName(
                String.format("project_files_%s_%s.zip", projectUuid,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_hh_mm_ss"))));
        FileOutputStream fileOutputStream = new FileOutputStream(rootPath.resolve(sanitizedFileName).toString());
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        int skippedTypes = 0;
        for (EdsContentType edsContentType : typesToDownload) {
            String type = edsContentType.getStringValue();
            String sourceDir = Paths.get(rootFolder, type, projectUuid.toString()).toString();
            if (EdsContentType.KEYSTORE.equals(edsContentType)) {
                sourceDir = Paths.get(rootFolder, type).toString();
            }
            File fileSource = new File(sourceDir);
            log.debug("Trying to get files of type: '{}', source dir: '{}'", type, sourceDir);
            File[] files = fileSource.listFiles();
            if (Objects.isNull(files)) {
                log.warn("There is no files of type: '{}'", type);
                skippedTypes++;
                continue;
            }
            addDirectory(zipOutputStream, fileSource);
        }
        if (skippedTypes == typesToDownload.size()) {
            throw new IOException("There is no files for selected types: " + typesToDownload);
        }
        zipOutputStream.finish();
        zipOutputStream.close();
        fileOutputStream.close();

        File file = new File(rootPath.resolve(sanitizedFileName).toString());
        Path path = Paths.get(file.getAbsolutePath());
        return downloadFile(sanitizedFileName, path);
    }

    /**
     * Processing project file.
     *
     * @param fileName    file name
     * @param filePath    path to file
     * @param contentType {@link EdsContentType} type content for file
     * @param projectUuid project UUID
     * @return the response entity {@link ResponseEntity}
     * @throws ExportException the export exception
     * @throws IOException     I/O exception has occurred
     */
    public ResponseEntity<InputStreamResource> projectFiles(String fileName, String filePath,
                                                            EdsContentType contentType, UUID projectUuid)
            throws IOException, ExportException {
        String sanitizedFileName = FilenameUtils.getName(fileName);
        String sanitizedFilePath = FilenameUtils.normalize(filePath);

        Path fullPathToFile = fileManagementService
                .getDirectoryPath(contentType.getStringValue(), projectUuid, sanitizedFilePath)
                .resolve(sanitizedFileName);
        File file = new File(fullPathToFile.toString());
        Path path = Paths.get(file.getAbsolutePath());
        return downloadFile(sanitizedFileName, path);
    }

    private void addDirectory(ZipOutputStream zipOutputStream, File fileSource) throws IOException {
        log.debug("Adding directory: '{}' ...", fileSource.getName());
        File[] files = fileSource.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (file.isDirectory()) {
                addDirectory(zipOutputStream, file);
                continue;
            }
            log.debug("Adding file :'{}' ...", file.getName());
            FileInputStream fileInputStream = new FileInputStream(file);
            zipOutputStream.putNextEntry(new ZipEntry(file.getPath()));
            byte[] buffer = new byte[4024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }
            fileInputStream.close();
            zipOutputStream.closeEntry();
        }
    }

    /**
     * Download file response entity.
     *
     * @param fileName the file name
     * @param archive  the archive
     * @return the response entity
     * @throws ExportException the export exception
     */
    public ResponseEntity<InputStreamResource> downloadFile(String fileName, Path archive)
            throws ExportException, IOException {
        FileInputStream inputStream = new FileInputStream(archive.toString());
        InputStreamResource streamResource = new InputStreamResource(inputStream);
        return ResponseEntity.ok().headers(createHeaderFor(fileName, archive)).body(streamResource);
    }

    /**
     * Create header for http headers.
     *
     * @param fileName the file name
     * @param archive  the archive
     * @return the http headers
     * @throws ExportException the export exception
     */
    private HttpHeaders createHeaderFor(String fileName, Path archive) throws ExportException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        try {
            headers.setContentLength(Files.size(archive));
        } catch (IOException e) {
            throw new ExportException(String.format("Cannot get size of file %s", archive), e);
        }
        headers.setAccessControlExposeHeaders(Arrays.asList("Content-Disposition"));
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
        return headers;
    }
}
