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
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ByProject;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.objects.template.UITemplate;
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
public class TemplateTreeController extends AbstractTreeBuilder<Template, TemplateProvider> implements TreeController {

    public UITreeData getTree(String id, UUID projectUuid) {
        return getTree(id, System.class.getName(), projectUuid);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/folder", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Template Tree under Folder id {{#id}} in the project {{#projectUuid}}")
    public UITreeData getTree(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String className,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        UITreeData treeData = new UITreeData();
        UITreeElement currentFolder;
        if (StringUtils.isBlank(className) && !StringUtils.isBlank(id)) {
            try {
                return buildTreeById(new UITemplate(TemplateHelper.getById(id)));
            } catch (Exception e) {
                LOGGER.error("Error occurred while was getting template with id + " + id);
                id = ""; // Further behaviour is the same as no <id> is provided. Default tree is returned. May be we
                // should throw an exception instead
            }
        }
        if (StringUtils.isBlank(id)) {
            currentFolder = new UITreeElement();
            BigInteger projectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class,
                            SearchManager.class)
                    .getEntityInternalIdByUuid(projectUuid);
            CoreObjectManager.getInstance()
                    .getSpecialManager(System.class, SystemObjectManager.class).getByProjectId(projectId).forEach(
                            templateProvider -> {
                                UITreeElement element = new UITreeElement();
                                configureObject(element, templateProvider);
                                currentFolder.addChild(element);
                            }
                    );
        } else {
            currentFolder = fillFolder(getTemplateProvider(id, className));
        }
        if (currentFolder.getId() == null) {
            fillSystemFolders(currentFolder, CoreObjectManager.getInstance().getManager(Folder.class).getById(id));
        }
        treeData.setTreeData(currentFolder.getChildren());
        return treeData;
    }

    private void fillSystemFolders(UITreeElement parentUiElement, Folder<System> systemFolder) {
        systemFolder.getObjects().forEach(templateProvider -> {
            UITreeElement element = new UITreeElement();
            configureObject(element, templateProvider);
            parentUiElement.addChild(element);
        });
        systemFolder.getSubFolders().forEach(systemSubfolder -> fillSystemFolders(parentUiElement, systemSubfolder));
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/tree/node", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Template Tree for current id {{#id}} or parent id {{#parentId}} in the project "
            + "{{#projectUuid}}")
    public UITreeData getTreeFromNode(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String parentId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Storable node;
        if (StringUtils.isBlank(id)) {
            return getTree(parentId, projectUuid);
        } else {
            UITreeData uiTreeData = getTree(parentId, projectUuid);
            node = getTemplateOrFolder(id);
            if (node instanceof System) {
                // Expand system node, so add the system children (operations and templates) to tree
                replaceAndExpandSystem(uiTreeData, (System) node);
            } else if (node instanceof Operation) {
                // Expand system node (parent of the operation),
                // so add the system children (operations and templates) to tree.
                // And, expand operation node.
                replaceAndExpandSystemExpandOperation(uiTreeData,
                        (System) node.getParent(),
                        (Operation) node);
            } else {
                if (node.getParent() instanceof System) {
                    // Expand system node, so add the system children (operations and templates) to tree
                    replaceAndExpandSystem(uiTreeData, (System) node.getParent());
                } else {
                    // Expand system node, so add the system children (operations and templates) to tree
                    // And, expand operation node - for the parent of the Template
                    replaceAndExpandSystemExpandOperation(uiTreeData,
                            (System) node.getParent().getParent(),
                            (Operation) node.getParent());
                }
            }
            return uiTreeData;
        }
    }

    private void replaceAndExpandSystem(UITreeData uiTreeData, System system) {
        UITreeElement uiTreeElementSystem = new UITreeElement(system);
        uiTreeElementSystem.setFolder(null);
        uiTreeElementSystem.setIsFolder(true);
        uiTreeElementSystem.setExpanded(true);
        for (Operation oper : system.getOperations()) {
            UITreeElement uiTreeElementOperation = new UITreeElement(oper);
            uiTreeElementOperation.setFolder(null);
            uiTreeElementOperation.setIsFolder(true);
            uiTreeElementSystem.getChildren().add(uiTreeElementOperation);
        }
        for (Template template : system.returnTemplates()) {
            UITreeElement uiTreeElementTemplate = new UITreeElement(template);
            uiTreeElementSystem.getChildren().add(uiTreeElementTemplate);
        }
        uiTreeData.getTreeData().remove(uiTreeElementSystem);
        uiTreeData.getTreeData().add(uiTreeElementSystem);
    }

    private void replaceAndExpandSystemExpandOperation(UITreeData uiTreeData, System system, Operation operation) {
        UITreeElement uiTreeElementSystem = new UITreeElement(system);
        uiTreeElementSystem.setFolder(null);
        uiTreeElementSystem.setIsFolder(true);
        uiTreeElementSystem.setExpanded(true);
        for (Operation oper : system.getOperations()) {
            UITreeElement uiTreeElementOperation = new UITreeElement(oper);
            uiTreeElementOperation.setFolder(null);
            uiTreeElementOperation.setIsFolder(true);
            if (oper.equals(operation)) {
                uiTreeElementOperation.setExpanded(true);
                for (Template template : oper.returnTemplates()) {
                    UITreeElement uiTreeElementTemplate = new UITreeElement(template);
                    uiTreeElementOperation.getChildren().add(uiTreeElementTemplate);
                }
            }
            uiTreeElementSystem.getChildren().add(uiTreeElementOperation);
        }
        for (Template template : system.returnTemplates()) {
            UITreeElement uiTreeElementTemplate = new UITreeElement(template);
            uiTreeElementSystem.getChildren().add(uiTreeElementTemplate);
        }
        uiTreeData.getTreeData().remove(uiTreeElementSystem);
        uiTreeData.getTreeData().add(uiTreeElementSystem);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/tree/node/name", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Template Tree for node by name {{#value}} and parent id {{#parentId}} in the "
            + "project {{#projectId}}/{{#projectUuid}}")
    public UITreeData getTreeFromNodeByName(
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String[] filters,
            @RequestParam(required = false) String parentId,
            @RequestParam("projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (StringUtils.isBlank(value)) {
            return getTree(parentId, projectUuid);
        }
        Collection<Storable> nodes = Sets.newHashSetWithExpectedSize(5);
        for (String filter : filters) {
            switch (filter) {
                case "NAME": {
                    nodes.addAll(getAllNodesByName(value, projectId));
                }
                break;
                case "LABEL": {
                    //Get all templates and folders
                    nodes.addAll(getAllLabeledByPieceOfName(value.toLowerCase(), getAllNodesByName("", projectId)));
                }
                break;
                default:
                    break;
            }
        }
        return buildTreeByFilter(nodes);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TEMPLATE.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/template/label", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Template Labels in the project {{#projectId}}/{{#projectUuid}}")
    public Set<String> gelLabels(
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam("projectId") BigInteger projectId) {
        return TemplateHelper.getAllLabels(projectId);
    }

    @Override
    UITreeElement fillFolder(Storable nodeProvider) {
        UITreeElement currentFolder = new UITreeElement();
        if (nodeProvider instanceof TemplateProvider) {
            TemplateProvider templateProvider = (TemplateProvider) nodeProvider;
            configureObject(currentFolder, templateProvider);
            if (templateProvider instanceof System) {
                addSubElements(currentFolder, ((System) templateProvider).getOperations());
            }
            addSubElements(currentFolder, templateProvider.returnTemplates());
        } else if (nodeProvider instanceof SystemFolder) {
            SystemFolder systemFolder = (SystemFolder) nodeProvider;
            configureObject(currentFolder, systemFolder);
            addSubElements(currentFolder, systemFolder.getObjects());
            addSubElements(currentFolder, systemFolder.getSubFolders());
        }
        return currentFolder;
    }

    @Override
    Class<TemplateProvider> getParentClass() {
        return TemplateProvider.class;
    }

    @Override
    Class<Template> getChildClass() {
        return Template.class;
    }

    private Storable getTemplateOrFolder(@RequestParam String id) {
        Storable node;
        node = TemplateHelper.getById(id);
        if (node == null) {
            node = CoreObjectManager.getInstance().getManager(System.class).getById(id);
        }
        if (node == null) {
            node = CoreObjectManager.getInstance().getManager(Operation.class).getById(id);
        }
        if (node == null) {
            throw new ObjectNotFoundException("Template or its parent (System/Operation)", id, null, null);
        }
        return node;
    }

    private Collection<Storable> getAllNodesByName(@RequestParam String name, BigInteger projectId) {
        Collection<Storable> nodes = Sets.newHashSetWithExpectedSize(5);
        nodes.addAll(CoreObjectManager.getInstance()
                .getSpecialManager(Operation.class, ByProject.class).getByPieceOfNameAndProject(name, projectId));
        nodes.addAll(TemplateHelper.getByPieceOfNameAndProject(name, projectId));
        nodes.addAll(CoreObjectManager.getInstance()
                .getSpecialManager(System.class, SystemObjectManager.class)
                .getByPieceOfNameAndProject(name, projectId));
        return nodes;
    }

    private TemplateProvider getTemplateProvider(@RequestParam(required = false) String id, String className) {
        Class<? extends TemplateProvider> clazz;
        clazz = (System.class.getName().equalsIgnoreCase(className))
                ? System.class
                : Operation.class;
        return CoreObjectManager.getInstance().getManager(clazz).getById(id);
    }
}
