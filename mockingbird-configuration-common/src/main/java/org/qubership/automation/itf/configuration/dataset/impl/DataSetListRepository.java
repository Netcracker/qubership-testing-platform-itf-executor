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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;

public interface DataSetListRepository {

    @Nullable
    @Deprecated
    Collection<DataSetList> getAll();

    @Nullable
    Collection<DataSetListsSource> getAllSources();

    @Nullable
    Collection<DataSetListsSource> getAllSources(Object projectId);

    @Nullable
    DataSetListsSource getSourceByNatureId(@Nonnull Object id, Object projectId);

    @Nullable
    DataSetList getByNatureId(@Nonnull Object id, Object projectId);

    @Nullable
    @Deprecated
    Collection<DataSetList> getByName(String name);

    @Nullable
    Set<IDataSet> getDataSetsWithLabel(@Nonnull DataSetList list, String label, @Nonnull Object projectId);

    @Nonnull
    Set<DataSetList> getDataSetLists(@Nonnull DataSetListsSource source);
}
