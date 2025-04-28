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

package org.qubership.automation.itf.configuration.dataset.impl;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.model.common.Identified;
import org.qubership.automation.itf.core.model.common.Named;

public class Identifier<T> implements Identified<T>, Named {

    private final T id;
    private final String name;

    public Identifier(@Nonnull T id, @Nonnull String name) {
        this.id = id;
        this.name = name;
    }

    @Nonnull
    @Override
    public T getID() {
        return id;
    }

    @Override
    public void setID(T id) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }
}
