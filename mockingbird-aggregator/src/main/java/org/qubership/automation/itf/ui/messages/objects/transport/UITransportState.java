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

package org.qubership.automation.itf.ui.messages.objects.transport;

import java.util.List;
import java.util.Map;

import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;

import com.google.common.collect.Lists;

public class UITransportState {

    private String typeName;
    private String userName;
    private String state;
    private int usageInfo;
    private List<UIProperty> properties;

    public UITransportState() {
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<UIProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<UIProperty> properties) {
        this.properties = properties;
    }

    public int getUsageInfo() {
        return usageInfo;
    }

    public void setUsageInfo(int usageInfo) {
        this.usageInfo = usageInfo;
    }

    public void defineProperties(Map<String, PropertyDescriptor> properties) {
        if (properties != null) {
            this.properties = Lists.newArrayListWithCapacity(properties.size());
            for (PropertyDescriptor descriptor : properties.values()) {
                this.properties.add(new UIProperty(descriptor));
            }
        }
    }
}
