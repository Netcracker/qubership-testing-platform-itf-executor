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

package org.qubership.automation.itf.ui.messages.objects.parents;

import java.util.List;

import com.google.common.collect.Lists;

public class UIIdentifiedObjectList {

    private List<UIIdentifiedObject> objects = Lists.newArrayList();

    public List<UIIdentifiedObject> getObjects() {
        return objects;
    }

    public void setObjects(List<UIIdentifiedObject> objects) {
        this.objects = objects;
    }
}
