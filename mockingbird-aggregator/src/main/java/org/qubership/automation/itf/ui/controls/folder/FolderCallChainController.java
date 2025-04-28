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

package org.qubership.automation.itf.ui.controls.folder;

import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Transactional(readOnly = true)
@RestController
public class FolderCallChainController extends AbstractFolderController {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/callchain/folder", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create CallChain Folder under Folder id {{#parentId}} in the project {{#projectUuid}}")
    public UIObject createFolder(@RequestParam(value = "id", defaultValue = "0") String parentId,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.create(parentId, "New Folder", CallChain.class.getSimpleName());
    }
}
