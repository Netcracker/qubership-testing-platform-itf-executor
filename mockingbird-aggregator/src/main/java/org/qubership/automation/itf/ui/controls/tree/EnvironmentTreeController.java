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

package org.qubership.automation.itf.ui.controls.tree;

import java.math.BigInteger;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.exceptions.configuration.ConfigurationException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.FolderManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.EnvironmentObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.tree.UITreeData;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

@RestController
@Transactional(readOnly = true)
public class EnvironmentTreeController extends AbstractTreeBuilder<Environment, Folder> implements TreeController {

    @Override
    UITreeElement fillFolder(Storable nodeProvider) {
        Folder<Environment> folder = (Folder<Environment>) nodeProvider;
        UITreeElement currentFolder = new UITreeElement();
        configureObject(currentFolder, folder);
        addSubElements(currentFolder, folder.getSubFolders());
        addSubElements(currentFolder, folder.getObjects());
        return currentFolder;
    }

    @Override
    Class<Folder> getParentClass() {
        return Folder.class;
    }

    @Override
    Class<Environment> getChildClass() {
        return Environment.class;
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment/folder", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Environment Tree under Folder id {{#id}} in the project {{#projectUuid}}")
    public UITreeData getTree(@RequestParam(required = false) String id,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        return getTree(id);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment/tree/node", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Environment Tree for current id {{#id}} or parent id {{#parentId}} in the project"
            + " {{#projectUuid}}")
    public UITreeData getTreeFromNode(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String parentId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (StringUtils.isEmpty(id)) {
            return getTree(parentId);
        } else {
            UITreeData uiTreeData = buildTree(getEnvironmentOrFolder(id));
            addInboundStateToTree(uiTreeData.getTreeData());
            return uiTreeData;
        }
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment/tree/node/name", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Environment Tree for node by name {{#value}} and parent id {{#parentId}} in the "
            + "project {{#projectId}}/{{#projectUuid}}")
    public UITreeData getTreeFromNodeByName(
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String[] filters,
            @RequestParam(required = false) String parentId,
            @RequestParam("projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (StringUtils.isBlank(value)) {
            return getTree(parentId);
        }
        Collection<Storable> nodes = Sets.newHashSetWithExpectedSize(5);
        for (String filter : filters) {
            switch (filter) {
                case "NAME":
                    nodes.addAll(getEnvAndFolderByName(value, projectId));
                    break;
                case "LABEL":   // Labels are not used for environments. This functionality is turned off on the UI
                default:
                    break;
            }
        }
        UITreeData uiTreeData = buildTreeByFilter(nodes);
        addInboundStateToTree(uiTreeData.getTreeData());
        return uiTreeData;
    }

    private UITreeData getTree(String id) {
        Folder<Environment> folder = getFolderByIdOrRootIfNull(id);
        UITreeData treeData = new UITreeData();
        UITreeElement currentFolder = fillFolder(folder);
        addInboundStateToTree(currentFolder.getChildren());
        treeData.setTreeData(currentFolder.getChildren());
        return treeData;
    }

    private Collection<Storable> getEnvAndFolderByName(String name, BigInteger projectId) {
        Collection<Storable> nodes = Sets.newHashSetWithExpectedSize(5);
        nodes.addAll(CoreObjectManager.getInstance().getSpecialManager(Environment.class,
                EnvironmentObjectManager.class).getByPieceOfNameAndProjectId(name, projectId));
        nodes.addAll(CoreObjectManager.getInstance().getSpecialManager(Folder.class, FolderManager.class)
                .findFolderByPieceOfName("EnvFolder", name, projectId));
        return nodes;
    }

    private Storable getEnvironmentOrFolder(String id) {
        Storable node = CoreObjectManager.getInstance().getManager(Environment.class).getById(id);
        if (node == null) {
            node = CoreObjectManager.getInstance().getManager(Folder.class).getById(id);
        }
        if (node == null) {
            throw new ObjectNotFoundException("Environment or Environment folder", id, null, null);
        }
        return node;
    }

    @Nonnull
    private Folder<Environment> getFolderByIdOrRootIfNull(@Nullable String id) {
        if (StringUtils.isBlank(id)) {
            throw new ConfigurationException("folder id cannot be null");
        }
        return CoreObjectManager.getInstance().getManager(Folder.class).getById(id);
    }

    private UITreeElement addInboundStateToElement(UITreeElement element,
                                                   ObjectManager<Environment> environmentManager) {
        Environment currentEnvironment = environmentManager.getById(element.getId());
        if (currentEnvironment.getEnvironmentState() != null) {
            element.setInboundState(currentEnvironment.getEnvironmentState().toString());
        }
        return element;
    }

    private void addInboundStateToTree(Collection<UITreeElement> treeElements) {
        ObjectManager<Environment> environmentManager = CoreObjectManager.getInstance().getManager(Environment.class);
        for (UITreeElement element : treeElements) {
            if (!element.getIsFolder()) {
                addInboundStateToElement(element, environmentManager);
            } else {
                Collection<UITreeElement> children = element.getChildren();
                for (UITreeElement child : children) {
                    // No recursion is needed because UI requests relate direct children of a folder only
                    if (!child.getIsFolder()) {
                        addInboundStateToElement(child, environmentManager);
                    }
                }
            }
        }
    }
}
