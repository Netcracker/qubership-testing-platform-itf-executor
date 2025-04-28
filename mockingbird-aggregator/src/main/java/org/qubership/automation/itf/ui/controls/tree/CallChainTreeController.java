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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.NotValidValueException;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.FolderManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.LabeledObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.CallChainObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerConstants;
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
public class CallChainTreeController extends AbstractTreeBuilder<CallChain, Folder> implements TreeController {

    /**
     * @param id - this parameter is not required, it can be null or folder Id
     * @return - it depends from Id, in case Id null or empty - return root folder of call chains
     *     in other case, find folder by Id and return childs of it.
     */
    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/folder", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get CallChain Tree under Folder id {{#id}} in the project {{#projectUuid}}")
    public UITreeData getTree(@RequestParam(required = false) String id,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        return getTree(id);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/tree/node", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get CallChain Tree for current id {{#id}} or parent id {{#parentId}} in the project "
            + "{{#projectUuid}}")
    public UITreeData getTreeFromNode(@RequestParam(required = false) String id,
                                      @RequestParam(required = false) String parentId,
                                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        return StringUtils.isBlank(id) ? getTree(parentId) : buildTree(getCallChainOrFolder(id));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/tree/node/name", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get CallChain Tree for node by name {{#value}} and parent id {{#parentId}} in the "
            + "project {{#projectId}}/{{#projectUuid}}")
    public UITreeData getTreeFromNodeByName(@RequestParam(required = false) String value,
                                            @RequestParam(required = false) String[] filters,
                                            @RequestParam(required = false) String parentId,
                                            @RequestParam("projectId") BigInteger projectId,
                                            @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (StringUtils.isBlank(value)) {
            return getTree(parentId);
        }
        Collection<Storable> nodes =
                Sets.newHashSetWithExpectedSize(ControllerConstants.DEFAULT_OBJECTS_COUNT_FOUND_BY_NAME.getIntValue());
        for (String filter : filters) {
            switch (filter) {
                case "NAME":
                    nodes.addAll(getCallChainAndFolderByName(value, projectId));
                    break;
                case "LABEL":
                    //Get all call chains and folders
                    nodes.addAll(getAllLabeledByPieceOfName(value.toLowerCase(),
                            getCallChainAndFolderByName("", projectId)));
                    break;
                default:
                    break;
            }
        }
        return buildTreeByFilter(nodes);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/label", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all CallChain Labels in the project {{#projectId}}/{{#projectUuid}}")
    public Set<String> gelLabels(
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam("projectId") BigInteger projectId) {
        return CoreObjectManager.getInstance().getSpecialManager(CallChain.class,
                LabeledObjectManager.class).getAllLabels(projectId);
    }

    //FIXME SZ: Copy paste, just extract this method to parent class and use generic!
    @Override
    UITreeElement fillFolder(Storable storable) {
        Folder<CallChain> folder = (Folder<CallChain>) storable;
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
    Class<CallChain> getChildClass() {
        return CallChain.class;
    }

    private UITreeData getTree(String id) {
        Folder<CallChain> folder = getFolderByIdOrRootIfNull(id);
        UITreeData treeData = new UITreeData();
        UITreeElement currentFolder = fillFolder(folder);
        treeData.setTreeData(currentFolder.getChildren());
        return treeData;
    }

    /**
     * Returns the root folder from stub project if {@code id} is null or empty
     * or returns {@link Folder} by this {@code id}.
     *
     * @param id - From UI we always get id as String
     * @return - root folder or folder found by id, see description
     */
    @Nonnull
    private Folder<CallChain> getFolderByIdOrRootIfNull(@Nullable String id) {
        if (StringUtils.isBlank(id)) {
            /*
                Multi-project support: id must be non-empty because folders are under projects
             */
            throw new NotValidValueException("Folder id cannot be null");
        }
        return CoreObjectManager.getInstance().getManager(Folder.class).getById(id);
    }

    private Storable getCallChainOrFolder(@RequestParam String id) {
        Storable node;
        node = CoreObjectManager.getInstance().getManager(CallChain.class).getById(id);
        if (node == null) {
            node = CoreObjectManager.getInstance().getManager(Folder.class).getById(id);
        }
        if (node == null) {
            throw new ObjectNotFoundException("Call chain or Call Chain folder", id, null, null);
        }
        return node;
    }

    private Collection<Storable> getCallChainAndFolderByName(String name, BigInteger projectId) {
        Collection<Storable> nodes =
                Sets.newHashSetWithExpectedSize(ControllerConstants.DEFAULT_OBJECTS_COUNT_FOUND_BY_NAME.getIntValue());
        nodes.addAll(CoreObjectManager.getInstance().getSpecialManager(CallChain.class,
                CallChainObjectManager.class).getByPieceOfNameAndProjectId(name, projectId));
        nodes.addAll(CoreObjectManager.getInstance().getSpecialManager(Folder.class, FolderManager.class)
                .findFolderByPieceOfName("ChainFolder", name, projectId));
        return nodes;
    }
}
