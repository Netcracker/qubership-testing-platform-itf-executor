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

package org.qubership.automation.itf.ui.messages.objects.request.referenceregenerator;

import java.util.HashSet;
import java.util.Set;

import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;

public class UIReplacement extends UIIdentifiedObject {

    private Set<UIIdentifiedObject> compatibleSystems = new HashSet<>();
    private UIIdentifiedObject replacement = new UIIdentifiedObject();

    public UIReplacement() {
    }

    public UIReplacement(System system) {
        setId(system.getID().toString());
        setName(system.getName());
        this.replacement.setId(system.getID().toString());
        this.replacement.setName(system.getName());
        addCompatibleSystem(this.replacement);
    }

    public Set<UIIdentifiedObject> getCompatibleSystems() {
        return compatibleSystems;
    }

    public void setCompatibleSystems(Set<UIIdentifiedObject> compatibleSystems) {
        this.compatibleSystems = compatibleSystems;
    }

    public void addCompatibleSystem(UIIdentifiedObject compatibleSystem) {
        this.compatibleSystems.add(compatibleSystem);
    }

    public void addCompatibleSystem(System system) {
        UIIdentifiedObject compatibleSystem = new UIIdentifiedObject();
        compatibleSystem.setId(system.getID().toString());
        compatibleSystem.setName(system.getName());
        this.compatibleSystems.add(compatibleSystem);
    }

    @Override
    public boolean equals(Object o) {
        return this.getId().equals(((UIIdentifiedObject) o).getId());
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    public UIIdentifiedObject getReplacement() {
        return replacement;
    }

    public void setReplacement(UIIdentifiedObject replacement) {
        this.replacement = replacement;
    }
}
