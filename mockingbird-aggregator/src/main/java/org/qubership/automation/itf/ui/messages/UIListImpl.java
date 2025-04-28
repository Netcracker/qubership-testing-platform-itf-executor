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
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;

import com.google.common.collect.Sets;

public class UIListImpl<T extends UIIdentifiedObject> implements UIList<T> {

    private LinkedHashSet<T> objects;

    public UIListImpl() {
    }

    @Nullable
    @Override
    public Collection<T> getObjects() {
        return objects;
    }

    @Override
    public void setObjects(@Nullable Collection<T> objects) {
        if (objects == null) {
            this.objects = null;
        } else {
            this.objects = new LinkedHashSet<>(objects);
        }
    }

    @Override
    public void defineObjects(@Nonnull Collection<T> objects) {
        if (this.objects == null) {
            this.objects = Sets.newLinkedHashSet();
        }
        this.objects.addAll(objects);
    }

    @Override
    public void addObject(@Nonnull T object) {
        defineObjects(Collections.singleton(object));
    }
}
