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
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.util.eds.model.UIFileInfo;
import org.qubership.automation.itf.core.util.eds.service.EdsContentType;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.file.TreeNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileManagerController {

    private final FileManagerService fileManagerService;

    public FileManagerController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    /**
     * Get paths to files.
     *
     * @param projectUuid project UUID
     * @return the response entity {@link TreeNode}
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/files/path")
    public TreeNode[] getPathTreeNode(@RequestParam UUID projectUuid) {
        return fileManagerService.getPathTreeNode(projectUuid);
    }

    /**
     * Get files by path.
     *
     * @param filePath    path to file
     * @param contentType {@link EdsContentType} type content for file
     * @param projectUuid project UUID
     * @return the response entity {@link UIFileInfo}
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/files/byPath")
    public Set<UIFileInfo> getFilesByPath(@RequestParam String filePath,
                                          @RequestParam EdsContentType contentType,
                                          @RequestParam UUID projectUuid) {
        return fileManagerService.getFilesByPath(filePath, contentType, projectUuid);
    }

    /**
     * Delete file's.
     *
     * @param filesInfo   list with type {@link UIFileInfo} where contains file info
     * @param projectUuid project UUID
     * @return the response entity {@link UIResult}
     * @throws IOException I/O exception has occurred
     */
    @PreAuthorize("@entityAccess.isSupport() or @entityAccess.isAdmin()")
    @DeleteMapping(value = "/files/remove")
    public UIResult remove(@RequestBody List<UIFileInfo> filesInfo, @RequestParam UUID projectUuid,
                           @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) throws IOException {
        return fileManagerService.remove(filesInfo, projectUuid, tenantId);
    }

    /**
     * Get content from file.
     *
     * @param contentType {@link EdsContentType} type content for file
     * @param filePath    path to file
     * @param fileName    file name
     * @param projectUuid project UUID
     * @return the response entity {@link UIResult}
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\") and "
            + "!T(org.qubership.automation.itf.core.util.eds.service.EdsContentType).KEYSTORE.equals(#contentType)")
    @GetMapping("/file/content")
    public UIResult getContent(@RequestParam EdsContentType contentType, @RequestParam String filePath,
                               @RequestParam String fileName, @RequestParam UUID projectUuid) {
        return fileManagerService.getContent(contentType, filePath, fileName, projectUuid);
    }
}
