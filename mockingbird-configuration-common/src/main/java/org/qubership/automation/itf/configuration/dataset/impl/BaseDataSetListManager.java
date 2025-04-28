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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Triple;
import org.qubership.automation.itf.configuration.dataset.impl.excel.ExcelDataSetListRepository;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSetListRepository;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.IDataSetListManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.constants.Match;
import org.qubership.automation.itf.core.util.copier.StorableCopier;
import org.qubership.automation.itf.core.util.exception.CopyException;
import org.qubership.automation.itf.core.util.feign.http.HttpClientReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
public class BaseDataSetListManager implements IDataSetListManager, ApplicationListener<HttpClientReadyEvent> {

    public static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDataSetListManager.class);
    private final BigInteger folderId = new BigInteger("7");
    private Collection<DataSetListRepository> repos;
    private final Folder<DataSetListsSource> folder = new Folder<DataSetListsSource>(DataSetListsSource.class) {
        @Override
        public List<DataSetListsSource> getObjects() {
            return ImmutableList.copyOf(BaseDataSetListManager.this.getAllSources());
        }

        @Override
        protected ObjectManager getManager() {
            throw new IllegalStateException("dataset folder is not managed");
        }

        @Override
        public Object getID() {
            return folderId;
        }
    };
    private final ExcelDataSetListRepository excelRepo = new ExcelDataSetListRepository(folder);
    private Optional<RemoteDataSetListRepository> remoteRepo = Optional.empty();

    @Override
    public void onApplicationEvent(HttpClientReadyEvent event) {
        ImmutableList.Builder<DataSetListRepository> b = ImmutableList.builder();
        b.add(excelRepo);
        RemoteDataSetListRepository repo = new RemoteDataSetListRepository(folder);
        this.remoteRepo = Optional.of(repo);
        b.add(repo);
        this.repos = b.build();
    }

    @Override
    public Collection<? extends DataSetListsSource> getAllSources() {
        return repos.stream()
                .flatMap(repo -> Optional.ofNullable(repo.getAllSources())
                        .map(Collection::stream).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends DataSetListsSource> getAllSources(Object projectId) {
        return repos.stream().flatMap(repo -> Optional.ofNullable(repo.getAllSources(projectId))
                .map(Collection::stream).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public DataSetListsSource getSourceByNatureId(Object id, Object projectId) {
        return repos.stream()
                .map(repo -> repo.getSourceByNatureId(id, projectId))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Loads all DSLs from all DSLSources.
     *
     * @deprecated it's too complex to load all datasets the same time. use {@link #getAllSources()} instead
     */
    @Deprecated
    @Override
    public Collection<? extends DataSetList> getAll() {
        LOGGER.warn("Stop using deprecated method please",
                new Exception("It's too complex to load all datasets the same time. See stacktrace attached"));
        return repos.stream().map(DataSetListRepository::getAll).filter(Objects::nonNull)
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<? extends DataSetList> getByNatureId(@Nonnull Object id, Object projectId) {
        ArrayList<DataSetList> ds = new ArrayList<>();
        if (isUuid(id)) {
            ds.add(remoteRepo.map(repo -> repo.getByNatureId(id, projectId)).orElse(null));
        } else {
            ds.add(excelRepo.getByNatureId(id, projectId));
        }
        return ds;
    }

    private boolean isUuid(Object id) {
        if (Objects.isNull(id)) {
            return false;
        }
        String val = id.toString();
        String[] split = val.split("_");
        if (split.length != 2) {
            return false;
        }
        return split[0].matches(UUID_PATTERN);
    }

    @Override
    public List<IDataSet> getDataSetsWithLabel(DataSetList list, String label, Object projectId) {
        return repos.stream()
                .flatMap(repo -> Optional.ofNullable(repo.getDataSetsWithLabel(list, label, projectId))
                        .map(Collection::stream).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends DataSetList> getAllByParentId(@Nonnull Object id) {
        DataSetListsSource source = getSourceByNatureId(id, null);
        return source == null
                ? null
                : source.getDataSetLists();
    }
    //region Common

    /**
     * Loads all DSLs from all DSLSources.
     *
     * @deprecated it's too complex to load all datasets the same time. use {@link #getAllSources()} instead
     */
    @Deprecated
    @Override
    public Collection<? extends DataSetList> getByName(String name) {
        LOGGER.warn("Stop using deprecated method please",
                new Exception("It's too complex to load all datasets the same time to find by name. "
                        + "(!)Name is not unique identifier. See stacktrace attached"));
        return repos.stream().map(repo -> repo.getByName(name)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public Folder<DataSetListsSource> getFolder() {
        return folder;
    }

    @Override
    public DataSetList getById(@Nonnull Object id) {
        return getByNatureId(id, null).get(0);
    }

    public DataSetList getById(@Nonnull Object id, Object projectId) {
        return getByNatureId(id, projectId).get(0);
    }

    @Override
    public DataSetListsSource getSourceById(Object id, Object projectId) {
        return getSourceByNatureId(id, projectId);
    }

    @Override
    public Collection<? extends DataSetList> getByParentAndName(Storable parent, String name) {
        return Collections.singleton(DataSetListsSource.class.cast(parent).getDataSetList(name));
    }
    //endregion

    @Override
    public Storable copy(Storable dst, Storable obj, String projectId, String sessionId) throws CopyException {
        return new StorableCopier(sessionId).copy(obj, dst, projectId, "copy");
    }

    //region Not implemented
    @Override
    public Collection<? extends DataSetList> getByPieceOfName(String pieceOfName) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Collection<DataSetList> getAllByParentName(String name) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Collection<? extends DataSetList> getByProperties(BigInteger projectId,
                                                             Triple<String, Match, ?>[] properties) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public Collection<UsageInfo> remove(Storable object, boolean force) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public DataSetList create(Storable parent, String type, Map parameters) {
        throw new RuntimeException("can not create");
    }

    @Override
    public DataSetList create(Storable storable, String s, String s1) {
        return null;
    }

    @Override
    public DataSetList create(Storable storable, String s, String s1, String s2) {
        return null;
    }


    @Override
    public DataSetList create(Storable parent, String name, String type, String description, List<String> labels) {
        return null;
    }

    @Override
    public DataSetList create() {
        throw new RuntimeException("can not create");
    }

    @Override
    public DataSetList create(Storable storable) {
        return null;
    }

    @Override
    public DataSetList create(Storable storable, String s) {
        return null;
    }

    @Override
    public void move(Storable dst, Storable obj, String sessionId) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Nullable
    @Override
    public Collection<UsageInfo> findUsages(Storable storable) {
        return null;
    }

    @Override
    public Map<String, List<BigInteger>> findImportantChildren(Storable storable) {
        return null;
    }

    @Override
    public String acceptsTo(Storable storable) {
        return null;
    }

    @Override
    public void additionalMoveActions(Storable object, String sessionId) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public void onCreate(DataSetList object) {
    }

    @Override
    public void onUpdate(DataSetList object) {
    }

    @Override
    public void setReplicationRole(String roleName) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public void onRemove(DataSetList object) {
    }

    @Override
    public void store(Storable object) {
    }

    @Override
    public void replicate(Storable object) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean contains(Storable object) {
        return false;
    }

    @Override
    public void update(Storable object) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void flush() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void evict(Storable object) {
        throw new NotImplementedException("Not implemented");
    }

    //endregion
}
