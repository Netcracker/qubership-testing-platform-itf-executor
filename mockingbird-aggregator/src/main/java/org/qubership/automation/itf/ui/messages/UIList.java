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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;

public interface UIList<T extends UIIdentifiedObject> {

    @Nullable
    //TODO SZ: it's worst practise return the null instead of Collections.emptyList() or another collection;
    Collection<T> getObjects();

    void setObjects(@Nullable Collection<T> objects);

    void defineObjects(@Nonnull Collection<T> objects);

    void addObject(@Nonnull T object);
}
