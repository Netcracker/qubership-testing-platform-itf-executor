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

package org.qubership.automation.itf.ui.messages.tree;

import java.util.Collection;
import java.util.List;

import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class UITreeData {

    private List<UIProperty> properties = Lists.newArrayList();
    private String type;
    private Collection<UITreeElement> treeData = Sets.newHashSet();

    public Collection<UITreeElement> getTreeData() {
        return treeData;
    }

    public void setTreeData(Collection<UITreeElement> treeData) {
        this.treeData = treeData;
    }

    public List<UIProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<UIProperty> properties) {
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
