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

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.qubership.automation.itf.core.model.common.LabeledStorable;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.qubership.automation.itf.ui.util.UIHelper;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Schema(description = "NOTE. Property parent is hidden to prevent circular references.")
public class UIObject extends UIIdentifiedObject {

    @Schema(accessMode = READ_ONLY)
    private UIObject parent;
    private String description;
    private ImmutableList<String> labels;
    private String historyKey;
    private int version = -1;

    public UIObject() {
    }

    public UIObject(Storable storable) {
        defineObjectParam(storable);
    }

    public UIObject(Storable storable, boolean isFullWithParent) {
        defineObjectParam(storable, isFullWithParent);
    }

    public UIObject(UIObject uiObject) {
        setId(uiObject.getId());
        setName(uiObject.getName());
        if (uiObject.getParent() != null) {
            setParent(new UIObject(uiObject.getParent()));
        }
        setClassName(uiObject.getClassName());
        setVersion(uiObject.getVersion());
    }

    public UIObject getParent() {
        return parent;
    }

    public void setParent(UIObject parent) {
        this.parent = parent;
    }

    public void defineObjectParam(Storable storable) {
        defineObjectParam(storable, true);
    }

    public void defineObjectParam(Storable storable, boolean isFullWithParent) {
        setId(storable.getID().toString());
        setName(storable.getName());
        if (storable.getParent() != null) {
            if (isFullWithParent || !(storable.getParent() instanceof Folder)) {
                setParent(new UIObject(storable.getParent(), isFullWithParent));
            }
        }
        setClassName(storable.getClass().getName());
        setDescription(storable.getDescription());
        if (isFullWithParent && storable instanceof LabeledStorable) {
            LabeledStorable labeledStorable = (LabeledStorable) storable;
            setLabels(labeledStorable.getLabels());
        } else {
            setLabels(null);
        }
        setVersion(NumberUtils.toInt(String.valueOf(storable.getVersion()), -1));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ImmutableList<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = UIHelper.isNotNullCopyOfImmutableList(labels);
    }

    public void updateObject(Storable storable) {
        UIHelper.updateObject(this, storable);
    }

    @Override
    public String toString() {
        return String.format("Object '%s' with name '%s' and id '%s'", getClass().getSimpleName(), getName(),
                this.getId());
    }

    public void loadChildrenByClass(Class childClass, List<Storable> children) {
    }

    public String getHistoryKey() {
        return historyKey;
    }

    public void setHistoryKey(String historyKey) {
        this.historyKey = historyKey;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean calcIsVersionActual(Storable storable) {
        // Version by default is -1;
        // We compare current version with -1 to prevent cases when by some reasons
        // UIObject doesn't contain correct version. It can be related to some gaps
        // in Configurator-Executor integration. In this case we will update object and overwrite existed one
        // except of ignoring update. It is better than blocking update operation, otherwise user work can be blocked.
        if (getVersion() == -1) {
            log.warn("Storable version equals to '-1'. It means that UI doesn't have actual version number of object."
                    + " Please check integration between UI and backend for object {}", storable.getClass());
        }
        return storable.getVersion().equals(getVersion()) || getVersion() == -1;
    }
}
