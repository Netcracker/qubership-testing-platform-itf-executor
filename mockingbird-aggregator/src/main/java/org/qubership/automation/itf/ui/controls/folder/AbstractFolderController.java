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

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.messages.UITreeElementList;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.util.UIHelper;

public abstract class AbstractFolderController extends AbstractController<UITreeElement, Folder> {

    protected void recalculateTreePath(UITreeElement parent, UITreeElementList children) {
        if (children.getObjects() != null) {
            for (UITreeElement treeElement : children.getObjects()) {
                parent.addChild(treeElement);
                if (treeElement.getIsFolder()) {
                    Folder nestedFolder =
                            CoreObjectManager.getInstance().getManager(Folder.class).getById(treeElement.getId());
                    recalculateTreePathsForFolder(treeElement, nestedFolder);
                }
            }
        }
    }

    protected void recalculateTreePathsForFolder(UITreeElement uiFolder, Folder folder) {
        recalculateTreePath(uiFolder, UIHelper.getTreeElementList(folder.getObjects()));
        recalculateTreePath(uiFolder, UIHelper.getTreeElementList(folder.getSubFolders()));
    }

    @Override
    protected Class<Folder> _getGenericUClass() {
        return Folder.class;
    }

    @Override
    protected UITreeElement _newInstanceTClass(Folder object) {
        return new UITreeElement(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        return CoreObjectManager.getInstance().getManager(Folder.class).getById(parentId);
    }
}
