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

package org.qubership.automation.itf.configuration.dataset.impl.remote;

import java.beans.Transient;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;

public class RemoteDataSetListsSource extends AbstractStorable implements DataSetListsSource {

    private final transient RemoteDataSetListRepository repo;
    private final UUID id;

    public RemoteDataSetListsSource(
            @Nonnull RemoteDataSetListRepository repo,
            @Nonnull Folder<DataSetListsSource> parent, @Nonnull UUID id, @Nonnull String name) {
        this.repo = repo;
        this.id = id;
        setID(id.toString());
        setName(name);
        setParent(parent);
        setNaturalId(id.toString());
    }

    @Nonnull
    @Override
    public Set<DataSetList> getDataSetLists() {
        return repo().getDataSetLists(this);
    }

    @Nonnull
    @Override
    public Set<String> getDataSetListsNames() {
        return repo().getDataSetListsNames(this);
    }

    @Nullable
    @Override
    public DataSetList getDataSetList(String dataSetListName) {
        return repo().getDataSetList(this, dataSetListName);
    }

    @Transient
    public RemoteDataSetListRepository repo() {
        return repo;
    }

    public UUID getId() {
        return id;
    }

    @Transient
    public String getSourceType() {
        return "Visibility Area";
    }

    @Override
    public Object getProjectUuid() {
        return null;
    }
}
