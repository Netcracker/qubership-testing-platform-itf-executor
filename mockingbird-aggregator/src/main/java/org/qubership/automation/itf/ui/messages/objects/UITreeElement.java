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

package org.qubership.automation.itf.ui.messages.objects;

import java.util.Collection;
import java.util.List;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.ui.messages.objects.integration.ec.UIECIConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class UITreeElement extends UIECIConfiguration {

    private UIObject folder;
    private boolean isFolder;
    private boolean isRoot;
    private boolean expanded;
    private String inboundState;
    private Collection<UITreeElement> children = Sets.newHashSetWithExpectedSize(50);

    public UITreeElement() {
    }

    public UITreeElement(Storable storable) {
        super(storable);
        /*if (storable.getFolder() != null) {
            setFolder(new UIObject(storable.getFolder()));
        } else*/
        {
            if (getParent() != null) {
                setFolder(getParent());
            }
        }
        if (storable instanceof Folder) {
            setIsFolder(true);
        }
    }

    public UITreeElement(UIObject uiObject, boolean isFolder) {
        super(uiObject);
        if (getParent() != null) {
            setFolder(getParent());
        }
        setIsFolder(isFolder);
    }

    public UITreeElement(UITreeElement uiObject) {
        super(uiObject);
        if (uiObject.getFolder() != null) {
            setFolder(new UIObject(uiObject.getFolder()));
        } else {
            if (getParent() != null) {
                setFolder(getParent());
            }
        }
        if (uiObject.getIsFolder()) {
            setIsFolder(true);
        }
    }

    public UITreeElement(UITreeElement uiObject, Collection<UITreeElement> children, Class<? extends Storable> clazz) {
        setName(uiObject.getName());
        setId(uiObject.getId());
        if (!clazz.getName().equals(uiObject.getClassName())) {
            setIsFolder(true);
        }
        setDescription(uiObject.getDescription());
        setLabels(uiObject.getLabels());
        setClassName(uiObject.getClassName());
        addListChildrenIfExists(children, clazz);
        setVersion(uiObject.getVersion());
    }

    @Override
    public void loadChildrenByClass(Class childClass, List<Storable> children) {
        List<UITreeElement> childrenByClass = Lists.newArrayList();
        for (UITreeElement child : this.children) {
            if (child.getClassName().equals(childClass.getName())) {
                childrenByClass.add(child);
            }
        }
        if (!childrenByClass.isEmpty()) {
            children.retainAll(childrenByClass);
        }
        for (Storable child : children) {
            addChild(new UITreeElement(child));
        }
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public boolean getIsFolder() {
        return isFolder;
    }

    public void setIsFolder(boolean folder) {
        isFolder = folder;
    }

    public UIObject getFolder() {
        return folder;
    }

    public void setFolder(UIObject folder) {
        this.folder = folder;
    }

    public boolean getExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void addChild(UITreeElement object) {
        children.add(object);
    }

    public Collection<UITreeElement> getChildren() {
        return children;
    }

    public void setChildren(Collection<UITreeElement> children) {
        this.children = children;
    }

    public void addListChildrenIfExists(Collection<UITreeElement> objects, Class<? extends Storable> clazz) {
        for (UITreeElement object : objects) {
            if (object.getFolder() != null) {
                if (getId().equals(object.getFolder().getId())) {
                    setParent(null);
                    object.setIsFolder(true);
                    addChild(new UITreeElement(object, objects, clazz));
                }
            }
        }
    }

    public String getInboundState() {
        return inboundState;
    }

    public void setInboundState(String inboundState) {
        this.inboundState = inboundState;
    }

    @Override
    public int hashCode() {
        return getId() == null ? super.hashCode() : getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (obj == this || obj.hashCode() == this.hashCode());
    }
}
