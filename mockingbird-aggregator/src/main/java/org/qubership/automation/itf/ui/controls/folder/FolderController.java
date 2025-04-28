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

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.IllegalClassException;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FolderController extends AbstractFolderController {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/folder", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Folder with id {{#uiObject.id}} in the project {{#projectUuid}}")
    public UITreeElement update(@RequestBody UITreeElement uiObject,
                                @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.updateUIObject(getManager(Folder.class).getById(uiObject.getId()), uiObject);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/folder/delete/data", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Check Folder contents before deleting it from project {{#projectUuid}}")
    public Map<String, Collection<UIObject>> getDataForDelete(
            @RequestBody Collection<UIObject> objectsToDelete,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws IllegalClassException {
        List<UIObject> nonEmptyFolders = new ArrayList<>();
        List<UIObject> emptyFolders = new ArrayList<>();
        List<UIObject> otherEntities = new ArrayList<>();
        for (UIObject object : objectsToDelete) {
            Class sourceClass = getSupportEntityClass(object.getClassName());
            Storable obj =
                    CoreObjectManager.getInstance().getManager(sourceClass).getById(object.getId());
            UIObject uiObject = new UIObject(obj);
            if (obj instanceof Folder && folderIsNotEmpty((Folder) obj)) {
                nonEmptyFolders.add(uiObject);
            } else if (obj instanceof Folder) {
                emptyFolders.add(uiObject);
            } else {
                otherEntities.add(uiObject);
            }
        }
        Map<String, Collection<UIObject>> result = new HashMap<>();
        result.put("non_empty_folder", nonEmptyFolders);
        result.put("empty_folder", emptyFolders);
        result.put("other_entities", otherEntities);
        return result;
    }

    private Class getSupportEntityClass(String className) {
        Class sourceClassName;
        if (ChainFolder.class.getCanonicalName().equals(className)) {
            sourceClassName = ChainFolder.class;
        } else if (EnvFolder.class.getCanonicalName().equals(className)) {
            sourceClassName = EnvFolder.class;
        } else if (CallChain.class.getCanonicalName().equals(className)) {
            sourceClassName = CallChain.class;
        } else if (Environment.class.getCanonicalName().equals(className)) {
            sourceClassName = Environment.class;
        } else {
            throw new IllegalClassException("Unexpected class: " + className + ". Class is not supported for delete.");
        }
        return sourceClassName;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/folder", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Folders from project {{#projectUuid}}")
    public void delete(@RequestBody Collection<UITreeElement> objectsToDelete,
                       @RequestParam(value = "projectUuid") UUID projectUuid) {
        super.delete(objectsToDelete);
    }

    @Override
    protected Folder _beforeUpdate(UITreeElement uiObject, Folder folder) {
        ControllerHelper.throwExceptionIfNull(folder, null, uiObject.getId(), Folder.class, "get Folder by id");
        folder.setDescription(uiObject.getDescription());
        folder.getLabels().clear();
        folder.getLabels().addAll(uiObject.getLabels());
        return super._beforeUpdate(uiObject, folder);
    }

    private boolean folderIsNotEmpty(Folder folder) {
        if (!folder.getObjects().isEmpty()) {
            return true;
        } else {
            for (Object sub : folder.getSubFolders()) {
                boolean result = folderIsNotEmpty((Folder) sub);
                if (result) {
                    return true;
                }
            }
        }
        return false;
    }
}
