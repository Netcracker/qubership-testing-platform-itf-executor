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

package org.qubership.automation.itf.ui.messages.objects.template;

import java.util.List;

import org.qubership.automation.itf.ui.messages.objects.parents.UINamedObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;

public class UITransportPropsForTemplate extends UINamedObject {

    private String className;
    private String displayName;
    private List<UIProperty> transportProperties;

    public UITransportPropsForTemplate() {
    }

    public UITransportPropsForTemplate(String className) {
        this.setName(className.replaceFirst("(\\S+?)(\\w+$)", "$2"));
        this.className = className;
    }

    public UITransportPropsForTemplate(String className, List<UIProperty> transportProperties) {
        this(className);
        this.transportProperties = transportProperties;
    }

    public UITransportPropsForTemplate(String className, String displayName, List<UIProperty> transportProperties) {
        this(className);
        this.displayName = displayName;
        this.transportProperties = transportProperties;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<UIProperty> getTransportProperties() {
        return transportProperties;
    }

    public void setTransportProperties(List<UIProperty> transportProperties) {
        this.transportProperties = transportProperties;
    }
}
