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

package org.qubership.automation.itf.executor.scheduled;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanTmpFileScheduler {

    @Value("${scheduled.cleanup.tempFiles.modifiedBefore.ms}")
    private int modifiedBeforeMs;

    /**
     * Job to remove tmp files (archives created for 'Download File' requests from UI).
     * Job is executed every hour (by default), and deletes all files older than 1 hour (by default).
     */
    @Scheduled(fixedRateString = "${scheduled.cleanup.tempFiles.fixedRate.ms}")
    public void cleanTempArchiveFiles() {
        log.debug("Scheduled Cleanup of Temp Files Job is started...");
        String rootDirectory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        Path tempPath = Paths.get(rootDirectory + File.separatorChar + "data_temp");

        final Long expirationDate = System.currentTimeMillis() - modifiedBeforeMs;
        if (Objects.isNull(tempPath) || !Files.exists(tempPath)) {
            log.warn("Temp directory {} doesn't exist, files will not be removed", tempPath);
            return;
        }
        List<String> deletedPaths = new ArrayList<>();
        removeFiles(tempPath, expirationDate, deletedPaths);
        log.debug("Files are removed: {}", deletedPaths);
    }

    /**
     * Temp file of export archive may be not removed after downloading.
     * Remove tmp files.
     */
    private void removeFiles(Path tmpdir, Long expirationDate, List<String> deletedPaths) {
        try {
            Files.walkFileTree(tmpdir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = String.valueOf(file.getFileName());
                    if (attrs.lastModifiedTime().toMillis() < expirationDate) {
                        deletedPaths.add(fileName);
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            log.error("Unable to remove archive files from temp directory {}", tmpdir, e);
        }
    }
}
