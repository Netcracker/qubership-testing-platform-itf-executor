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

package org.qubership.automation.itf.ui.messages.objects.environment;

import java.util.List;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;

import com.google.common.collect.Lists;

public class UIEnvironmentFolder extends UIObject {

    private UIWrapper<List<UIEnvironment>> environments;

    public UIEnvironmentFolder(Storable storable) {
        super(storable);
    }

    public UIEnvironmentFolder() {
    }

    public UIWrapper<List<UIEnvironment>> getEnvironments() {
        return environments;
    }

    public void setEnvironments(UIWrapper<List<UIEnvironment>> environments) {
        this.environments = environments;
    }

    @Override
    public void loadChildrenByClass(Class childClass, List<Storable> children) {
        List<UIEnvironment> uiEnvironments = Lists.newArrayList();
        for (Storable child : children) {
            //should be changed in future. Need to add just UIEnvironment, other data should be taken by separate
            // request
            uiEnvironments.add(new UIEnvironment((Environment) child));
        }
        setEnvironments(new UIWrapper<>(uiEnvironments));
    }
}
