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

import java.util.Collection;

import org.qubership.automation.itf.core.model.common.LabeledStorable;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.tree.UITreeData;

public interface TreeBuilder {

    UITreeData buildTree(Storable node);

    //To avoid building parent path... it was madness, because parent path was greater than an object...
    default void configureObject(UITreeElement treeElement, Storable storable) {
        if (storable == null) {
            return;
        }
        treeElement.setId(storable.getID().toString());
        treeElement.setName(storable.getName());
        treeElement.setDescription(storable.getDescription());
        if (storable instanceof LabeledStorable) {
            treeElement.setLabels(((LabeledStorable) storable).getLabels());
        }
        treeElement.setClassName(storable.getClass().getName());
        treeElement.setIsFolder(storable instanceof Folder);
    }


    /**
     * This method will convert all {@code objects} to UITreeElement and add it to {@code currentFolder}.
     *
     * @param <T>           - all types which extends Storable
     * @param currentFolder - the folder where will be added converted {@code objects}
     * @param objects       - objects to convert and add to UITreeElement
     */
    default <T extends Storable> void addSubElements(UITreeElement currentFolder, Collection<T> objects) {
        for (T storable : objects) {
            UITreeElement object = new UITreeElement();
            configureObject(object, storable);
            if (storable instanceof Folder) {
                object.setIsFolder(true);
            }
            if (!currentFolder.getChildren().contains(object)) {
                currentFolder.addChild(object);
            }
        }
    }
}
