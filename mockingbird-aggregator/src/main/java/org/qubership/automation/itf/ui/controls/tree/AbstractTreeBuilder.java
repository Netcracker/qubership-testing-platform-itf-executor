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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.math.NumberUtils;
import org.qubership.automation.itf.core.model.common.LabeledStorable;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.ui.controls.util.ControllerConstants;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.tree.UITreeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class AbstractTreeBuilder<C extends Storable, P extends Storable> extends ControllerHelper
        implements TreeBuilder {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractTreeBuilder.class);
    private static final Predicate<Map.Entry<Object, UITreeElement>> PREDICATE = entry -> !entry.getValue().isRoot();

    public UITreeData buildTree(Storable node) {
        UITreeElement currentItem = null;
        UITreeData treeData = new UITreeData();
        Storable parentFolder = null;
        if (getParentClass().isAssignableFrom(node.getClass())) {
            parentFolder = node;
        } else if (getChildClass().isAssignableFrom(node.getClass())) {
            parentFolder = getParentFolder(node);
        }
        while (parentFolder != null) {
            UITreeElement treeElement = fillFolder(parentFolder);
            if (currentItem != null) {
                treeElement.getChildren().remove(currentItem);
                treeElement.getChildren().add(currentItem);
            }
            parentFolder = getParentFolder(parentFolder);
            currentItem = treeElement;
        }
        if (currentItem != null) {
            treeData.setTreeData(currentItem.getChildren());
        }
        return treeData;
    }

    public UITreeData buildTreeByFilter(Collection<Storable> storables) {
        Map<Object, UITreeElement> uiTreeElementMap = Maps.newHashMap();
        UITreeData treeData = new UITreeData();
        storables.forEach(storable -> {
            UITreeElement elem = new UITreeElement(storable);
            if (getParentClass().isAssignableFrom(storable.getClass())) {
                elem.setIsFolder(true);
            } else if (getChildClass().isAssignableFrom(storable.getClass())) {
                elem.setIsFolder(false);
            }

            if (storable instanceof EciConfigurable) {
                elem.setEcId(((EciConfigurable) storable).getEcId());
            }
            putUITreeElementAndParents(uiTreeElementMap, elem);
        });
        return buildTreeStructure(uiTreeElementMap, treeData);
    }

    public UITreeData buildTreeById(UIObject uiObject) {
        Map<Object, UITreeElement> uiTreeElementMap = Maps.newHashMap();
        UITreeData treeData = new UITreeData();
        putUITreeElementAndParents(uiTreeElementMap, new UITreeElement(uiObject, false));
        return buildTreeStructure(uiTreeElementMap, treeData);
    }

    public UITreeData buildTreeStructure(Map<Object, UITreeElement> uiTreeElementMap, UITreeData treeData) {
        for (Map.Entry<Object, UITreeElement> treeElementEntry : uiTreeElementMap.entrySet()) {
            UITreeElement value = treeElementEntry.getValue();
            if (Objects.isNull(value.getParent())) {
                value.setRoot(true);
            } else {
                uiTreeElementMap.get(value.getParent().getId()).addChild(value);
            }
            value.setExpanded(true);
            value.setParent(null);
            value.setFolder(null);
        }
        uiTreeElementMap.entrySet().removeIf(PREDICATE);
        if (uiTreeElementMap.isEmpty()) {
            return treeData;
        }
        treeData.setTreeData(uiTreeElementMap.entrySet().stream().findFirst().get().getValue().getChildren());
        return treeData;
    }

    @Nullable
    private Storable getParentFolder(Storable node) {
        Storable parent = node.getParent();
        return parent;
    }

    abstract UITreeElement fillFolder(Storable nodeProvider);

    abstract Class<P> getParentClass();

    abstract Class<C> getChildClass();

    public void configureObject(UITreeElement treeElement, Storable storable) {
        if (storable == null) {
            return;
        }
        treeElement.setId(storable.getID().toString());
        treeElement.setName(storable.getName());
        treeElement.setDescription(storable.getDescription());
        if (storable.getParent() != null) {
            treeElement.setParent(new UIObject(storable.getParent()));
        }
        if (storable instanceof LabeledStorable) {
            treeElement.setLabels(((LabeledStorable) storable).getLabels());
        }
        if (storable instanceof EciConfigurable) {
            treeElement.setEcId(((EciConfigurable) storable).getEcId());
        }
        treeElement.setClassName(storable.getClass().getName());
        treeElement.setIsFolder(getParentClass().isAssignableFrom(storable.getClass()));
        treeElement.setVersion(NumberUtils.toInt(String.valueOf(storable.getVersion()), -1));
    }

    public <T extends Storable> void addSubElements(UITreeElement currentFolder, Collection<T> objects) {
        for (T storable : objects) {
            UITreeElement object = new UITreeElement();
            configureObject(object, storable);
            if (getParentClass().isAssignableFrom(storable.getClass())) {
                object.setIsFolder(true);
            }
            if (!currentFolder.getChildren().contains(object)) {
                currentFolder.addChild(object);
            }
        }
    }

    private void putUITreeElementAndParents(Map<Object, UITreeElement> uiTreeElementMap, UITreeElement uiTreeElement) {
        uiTreeElementMap.put(uiTreeElement.getId(), uiTreeElement);
        if (uiTreeElement.getParent() != null) {
            putUITreeElementAndParents(uiTreeElementMap, new UITreeElement(uiTreeElement.getParent(), true));
        }
    }

    public Collection<Storable> getAllLabeledByPieceOfName(String value, Collection<Storable> objects) {
        Collection<Storable> nodes =
                Sets.newHashSetWithExpectedSize(ControllerConstants.DEFAULT_OBJECTS_COUNT_FOUND_BY_NAME.getIntValue());
        for (Storable labeledObj : objects) {
            if (findLabeledByPieceOfName(labeledObj, value)) {
                nodes.add(labeledObj);
            }
        }
        return nodes;
    }

    public HashSet<String> getAllObjectsLabels(Collection<Storable> objects) {
        HashSet<String> labelsSet =
                Sets.newHashSetWithExpectedSize(ControllerConstants.DEFAULT_LABELS_COUNT.getIntValue());
        for (Storable labeledObj : objects) {
            if (labeledObj instanceof LabeledStorable) {
                List<String> labels = ((LabeledStorable) labeledObj).getLabels();
                if (labels != null) {
                    labelsSet.addAll(labels);
                }
            }
        }
        return labelsSet;
    }

    // labelName should be .toLowerCase() while invoking this function
    public Boolean findLabeledByPieceOfName(Storable labeledObject, String labelName) {
        List<String> labels = null;
        if (labeledObject instanceof LabeledStorable) {
            labels = ((LabeledStorable) labeledObject).getLabels();
            if (labels != null) {
                for (String label : labels) {
                    if (label.toLowerCase().contains(labelName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
