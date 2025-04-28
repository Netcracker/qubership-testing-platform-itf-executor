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
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.qubership.automation.itf.configuration.dataset.impl.DeprecatedStorable;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;

import com.google.common.base.Strings;

public class RemoteDataSetList extends DeprecatedStorable implements DataSetList {

    private final RemoteDataSetListRepository repo;
    private final DataSetListsSource parent;
    private final String name;
    private final String id;

    public RemoteDataSetList(
            @Nonnull RemoteDataSetListRepository repo,
            @Nonnull DataSetListsSource parent, @Nonnull String id, @Nonnull String name) {
        this.repo = repo;
        this.name = name;
        this.id = id;
        this.parent = parent;
    }

    @Override
    public DataSetListsSource getParent() {
        return parent;
    }

    @Override
    public Storable returnSimpleParent() {
        return null;
    }

    @Override
    public Object getNaturalId() {
        return getID();
    }

    @Override
    public void performPostImportActions(BigInteger projectId, BigInteger sessionId) {
    }

    @Override
    public void performActionsForImportIntoAnotherProject(Map<BigInteger, BigInteger> map,
                                                          BigInteger projectId, UUID projectUuid,
                                                          boolean needToUpdateProjectId, boolean needToGenerateNewId) {
    }

    @Override
    public void replicate() {
        throw new NotImplementedException("");
    }

    @Override
    public Map<String, String> getStorableProp() {
        throw new NotImplementedException("");
    }

    @Override
    public void setStorableProp(Map<String, String> properties) {
    }

    //TODO We don't need these methods
    @Override
    public Storable findRootObject(BigInteger bigInteger) {
        return null;
    }

    @Override
    public boolean contains() {
        return false;
    }

    @Override
    public void flush() {
    }

    @Override
    public void performPostImportActionsParent(BigInteger bigInteger, BigInteger bigInteger1) {
    }

    @Override
    public Storable getExtendsParameters() {
        return null;
    }

    @Override
    public void upStorableVersion() {
    }

    @Nonnull
    @Transient
    @Override
    public Set<IDataSet> getDataSets(Object projectId) {
        return repo().getDataSets(this, projectId);
    }

    @Nonnull
    @Transient
    @Override
    public Set<IDataSet> getDataSetsWithLabel(String label, Object projectId) {
        return repo().getDataSetsWithLabel(this, label, projectId);
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
        return repo().getDataSet(this, dataSetName, projectId);
    }

    @Nullable
    @Transient
    @Override
    public IDataSet getDataSetById(String dataSetId, Object projectId) {
        if (Strings.isNullOrEmpty(dataSetId)) {
            return null;
        }
        return repo().getDataSetById(this, dataSetId, projectId);
    }

    @Nullable
    @Transient
    @Override
    // ITF-CR-18; Alexander Kapustin, 2017-12-28, This method - may be - will be removed. I will re-think all around
    // it and make changes after ITF 4.2.15 release.
    public JsonContext getDataSetContextById(String dataSetId, @Nonnull Object projectId) {
        if (Strings.isNullOrEmpty(dataSetId)) {
            return null;
        }
        return repo().getDataSetContextById(dataSetId, projectId);
    }

    @Transient
    public RemoteDataSetListRepository repo() {
        return repo;
    }

    @Override
    public Object getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
