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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.util.eds.service.EdsContentType;
import org.qubership.automation.itf.core.util.exception.ExportException;
import org.qubership.automation.itf.executor.service.FileDownloaderService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class FileDownloadController {

    private final FileDownloaderService fileDownloaderService;

    public FileDownloadController(FileDownloaderService fileDownloaderService) {
        this.fileDownloaderService = fileDownloaderService;
    }

    /**
     * Download file response entity.
     *
     * @param typesToDownload {@link EdsContentType} types file to download
     * @param projectUuid     project UUID
     * @return the response entity {@link ResponseEntity}
     * @throws ExportException the export exception
     * @throws IOException     I/O exception has occurred
     */
    @PreAuthorize("(@entityAccess.checkAccess(#projectUuid, \"READ\") and !#typesToDownload.contains("
            + "T(org.qubership.automation.itf.core.util.eds.service.EdsContentType).KEYSTORE)) or"
            + " (@entityAccess.isSupport() or @entityAccess.isAdmin())")
    @GetMapping(value = "/project/files/download", produces = "application/zip")
    @AuditAction(auditAction = "Download files of {{#typesToDownload}} types from project {{#projectUuid}}")
    public ResponseEntity<InputStreamResource> downloadProjectFiles(@RequestParam List<EdsContentType> typesToDownload,
                                                                    @RequestParam UUID projectUuid)
            throws IOException, ExportException {
        if (typesToDownload.isEmpty()) {
            throw new IllegalArgumentException("Something went wrong: 'typesToDownload' parameter is empty...");
        }
        return fileDownloaderService.zipProjectFiles(typesToDownload, projectUuid);
    }

    /**
     * Download file response entity.
     *
     * @param fileName    file name
     * @param filePath    path to file
     * @param contentType {@link EdsContentType} type content for file
     * @param projectUuid project UUID
     * @return the response entity {@link ResponseEntity}
     * @throws ExportException the export exception
     * @throws IOException     I/O exception has occurred
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/project/file/download")
    public ResponseEntity<InputStreamResource> downloadProjectFile(@RequestParam String fileName,
                                                                   @RequestParam String filePath,
                                                                   @RequestParam EdsContentType contentType,
                                                                   @RequestParam UUID projectUuid)
            throws IOException, ExportException {
        return fileDownloaderService.projectFiles(fileName, filePath, contentType, projectUuid);
    }
}
