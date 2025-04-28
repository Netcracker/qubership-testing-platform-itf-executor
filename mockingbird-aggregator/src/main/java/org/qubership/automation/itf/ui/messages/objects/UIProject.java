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

import java.util.Set;

import org.qubership.automation.itf.core.model.common.Storable;

import com.google.common.collect.ImmutableSet;

public class UIProject extends UIObject {

    private ImmutableSet<UIObject> systems;
    private ImmutableSet<UIObject> starters;
    private ImmutableSet<UIObject> environments;

    public UIProject(Storable storable) {
        super(storable);
    }

    public ImmutableSet<UIObject> getSystems() {
        return systems;
    }

    public void setSystems(Set<UIObject> systems) {
        this.systems = ImmutableSet.copyOf(systems);
    }

    public ImmutableSet<UIObject> getStarters() {
        return starters;
    }

    public void setStarters(Set<UIObject> starters) {
        this.starters = ImmutableSet.copyOf(starters);
    }

    public ImmutableSet<UIObject> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<UIObject> environments) {
        this.environments = ImmutableSet.copyOf(environments);
    }
}
