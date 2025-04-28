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

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.exception.CopyException;
import org.qubership.automation.itf.core.util.exception.StorageException;
import org.qubership.automation.itf.core.util.storage.StoreInformationDelegate;

/**
 * Seems like subclasses should not be storables at all.
 * This class is temporary solution.
 */
@Deprecated
public abstract class DeprecatedStorable implements Storable {

    @Override
    public Collection<UsageInfo> findUsages() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StoreInformationDelegate getStoreInformationDelegate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<UsageInfo> remove() throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store() throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVersion() {
        return null;
    }

    @Override
    public void setVersion(Object version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrefix() {
        return StringUtils.EMPTY;
    }

    @Override
    public void setPrefix(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void performPostCopyActions(boolean statusOff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Storable newParent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Storable copy(Storable newParent) throws CopyException {
        throw new CopyException(String.format("[%s] is not storable at all", this));
    }

    /**
     * Copy-paste from {@link AbstractStorable#equals(Object)}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(this.getClass().isInstance(o))) {
            return false;
        }
        if (getID() != null) {
            return Objects.equals(getID(), ((Storable) o).getID());
        } else {
            return false;
        }
    }

    /**
     * Copy-paste from {@link AbstractStorable#hashCode()}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getID());
    }

    /**
     * Copy-paste from {@link AbstractStorable#toString()}.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Storable parent = this.getParent();
        // Please note: similar methods are in the: AbstractStorable, DeprecatedStorable, JsonStorable
        // May be defense against infinite loop is necessary. And/or method duplicates should be removed
        while (parent != null) {
            builder.insert(0, '>').insert(0, parent.getName()).insert(0, ']').insert(0,
                    parent.getClass().getSimpleName()).insert(0, '[');
            parent = parent.getParent();
        }
        builder.append('\n')
                .append("Name: '").append(getName())
                .append('\'').append(", ID: '").append(getID()).append('\'');
        return builder.toString();
    }

    @Override
    public void setParent(Storable parent) {
        throw new UnsupportedOperationException("Designed as immutable instance");
    }

    @Override
    public void setNaturalId(Object id) {
        throw new UnsupportedOperationException("Designed as immutable instance");
    }

    @Override
    public void setID(Object id) {
        throw new UnsupportedOperationException("Designed as immutable instance");
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Designed as immutable instance");
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String description) {
        throw new UnsupportedOperationException("Designed as immutable instance");
    }
}
