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

package org.qubership.automation.itf.ui.messages;

import java.util.Collection;

import org.qubership.automation.itf.ui.messages.objects.UITypedObject;

import com.google.common.collect.Sets;

public class UITypeList {

    private Collection<UITypedObject> types;

    public Collection<UITypedObject> getTypes() {
        return types;
    }

    public void setTypes(Collection<UITypedObject> types) {
        this.types = types;
    }

    public void defineTypes(Collection<UITypedObject> types) {
        if (this.types == null) {
            this.types = Sets.newHashSetWithExpectedSize(types.size());
        }
        this.types.addAll(types);
    }
}
