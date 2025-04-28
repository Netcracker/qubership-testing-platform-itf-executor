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

package org.qubership.automation.itf.configuration.dataset.impl.excel;

import java.beans.Transient;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;

import com.google.common.base.Strings;

public class ExcelDataSetList extends AbstractStorable implements DataSetList {

    private final ExcelDataSetListRepository repo;

    public ExcelDataSetList(
            @Nonnull ExcelDataSetListRepository repo,
            @Nonnull DataSetListsSource parent, @Nonnull String id, @Nonnull String name) {
        this.repo = repo;
        setParent(parent);
        setID(id);
        setNaturalId(id);
        setName(name);
    }

    @Override
    public DataSetListsSource getParent() {
        return (DataSetListsSource) super.getParent();
    }

    @Override
    public void setParent(Storable parent) {
        super.setParent(DataSetListsSource.class.cast(parent));
    }

    @Nonnull
    @Transient
    @Override
    public Set<IDataSet> getDataSets(Object projectId) {
        return repo().getDataSets(this);
    }

    @Nonnull
    @Transient
    @Override
    public Set<String> getVariables() {
        return repo().getVariables(this);
    }

    @Nullable
    @Transient
    @Override
    public IDataSet getDataSet(String dataSetName, Object projectId) {
        if (Strings.isNullOrEmpty(dataSetName)) {
            return null;
        }
        return repo().getDataSet(this, dataSetName);
    }

    @Nonnull
    @Override
    public Set<IDataSet> getDataSetsWithLabel(String label, Object projectId) {
        return null;
    }

    @Nullable
    @Transient
    @Override
    // Currently this is the same with getDataSet(). 
    public IDataSet getDataSetById(String dataSetId, Object projectId) {
        if (Strings.isNullOrEmpty(dataSetId)) {
            return null;
        }
        return repo().getDataSet(this, dataSetId);
    }

    @Nullable
    @Transient
    @Override
    // ITF-CR-18; Alexander Kapustin, 2017-12-28, This method - may be - will be removed. I will re-think all around
    // it and make changes after ITF 4.2.15 release.
    public JsonContext getDataSetContextById(String dataSetId, Object projectId) {
        return null; // Not supported yet. Should NOT be used
    }

    @Transient
    public ExcelDataSetListRepository repo() {
        return repo;
    }
}
