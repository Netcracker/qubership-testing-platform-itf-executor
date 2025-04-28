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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.usermodel.Cell;
import org.qubership.automation.configuration.dataset.excel.builder.DataSetBuilder;
import org.qubership.automation.configuration.dataset.excel.core.DSList;
import org.qubership.automation.configuration.dataset.excel.core.DSLists;
import org.qubership.automation.configuration.dataset.excel.core.Named;
import org.qubership.automation.configuration.dataset.excel.core.ParamsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.core.ReevaluateFormulas;
import org.qubership.automation.configuration.dataset.excel.core.VarsEntryConverter;
import org.qubership.automation.configuration.dataset.excel.impl.DSCell;
import org.qubership.automation.configuration.dataset.excel.tracker.base.AbstractTracker;
import org.qubership.automation.configuration.dataset.excel.tracker.base.Resource;
import org.qubership.automation.itf.configuration.dataset.impl.DataSetListRepository;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.startup.StartupErrorCollector;
import org.qubership.automation.itf.core.util.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ExcelDataSetListRepository implements DataSetListRepository {

    public static final char DS_LIST_NATURAL_ID_SEPARATOR = File.separatorChar;
    public static final String DS_FILE_EXTENSION = ".xlsx";
    public static final Joiner COMMA_JOINER = Joiner.on(',');
    public static final Path DS_DIR = Paths.get(Config.getConfig().getString("local.storage.directory"), "dataset");
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelDataSetListRepository.class);
    /**
     * returns pair of entity and parameter strings. all is not null or empty
     */
    private static final ParamsEntryConverter<String> PARAM_CONV = new ParamsEntryConverter<String>() {
        @Nullable
        @Override
        public String doParamsEntry(@Nullable DSCell entity, @Nonnull DSCell parameter) {
            if (entity == null) {
                logWarn("Group", null);
                return null;
            }
            String entityStr = entity.getStringValue();
            if (entityStr.isEmpty()) {
                logWarn("Group", entity.getCell());
                return null;
            }
            String parameterStr = parameter.getStringValue();
            if (parameterStr.isEmpty()) {
                logWarn("Param for group " + entityStr, parameter.getCell());
                return null;
            }
            return entityStr + "." + parameterStr;
        }
    };
    private static final Function<Iterator<String>, Set<String>> PARAMS_CONV = Sets::newHashSet;
    private static final VarsEntryConverter<String, Triple<String, String, String>> VAR_CONV = (entity, param,
                                                                                                convertedParam,
                                                                                                value) -> {
        //null | empty entities or parameters are filtered
        return Triple.of(entity.getStringValue(), param.getStringValue(), value.getStringValue());
    };
    private static final Function<Iterator<Triple<String, String, String>>,
            Collection<Triple<String, String, String>>> VARS_CONV = Lists::newArrayList;
    private final AbstractTracker<String, Set<String>, Collection<Triple<String, String, String>>> tracker;
    private final Folder<DataSetListsSource> folder;

    public ExcelDataSetListRepository(@Nonnull Folder<DataSetListsSource> folder) {
        tracker = new AbstractTracker<String, Set<String>, Collection<Triple<String, String, String>>>(DS_DIR, 10000L, true) {
            @Nonnull
            @Override
            protected DSLists<String, Set<String>, Collection<Triple<String, String, String>>> build(
                    @Nonnull DataSetBuilder builder) {
                return builder
                        .forAllSheets()
                        .forAllDataSets()
                        .customParams(PARAM_CONV, PARAMS_CONV)
                        .customVars(VAR_CONV, VARS_CONV, ReevaluateFormulas.IN_CONVERTER)
                        .build();
            }
        };
        this.folder = folder;
    }

    @Nonnull
    private static DataSetListsSource getSource(@Nonnull DataSetList dsList) {
        DataSetListsSource result = dsList.getParent();
        Preconditions.checkNotNull(result, "DataSetList with id {%s} and name {%s} has no parent", dsList.getID(),
                dsList.getName());
        return result;
    }

    private static String getPathName(@Nonnull DataSetList dsList) {
        return getSource(dsList).getName();
    }

    private static void logWarn(@Nonnull String targetName, @Nullable Cell cell) {
        if (cell != null) {
            LOGGER.debug("{} is empty. Sheet '{}' at [{};{}]", targetName, cell.getSheet().getSheetName(),
                    cell.getRowIndex(), cell.getColumnIndex());
        } else {
            LOGGER.debug("{} has empty cell", targetName);
        }
    }

    private static void logListNamesDuplication(@Nonnull DataSetList used, @Nonnull Stream<DataSetList> unused) {
        String sheetName = used.getName();
        String unusedFileNames = COMMA_JOINER.join(unused.map(ExcelDataSetListRepository::getPathName).iterator());
        String allFileNames = COMMA_JOINER.join(Lists.newArrayList(getPathName(used), unusedFileNames));//for case
        // when joiner joins not on commas
        String errorMessage = String.format("Duplication of Data Set Sheet found. Sheet name '%s', Files '%s'. "
                        + "(!) You would be not able to use datasets from file(s) '%s' at sheet '%s' (!)", sheetName,
                allFileNames, unusedFileNames, sheetName);
        logDuplication(errorMessage);
    }

    private static void logDuplication(String errorMessage) {
        LOGGER.error(errorMessage);
        StartupErrorCollector.getInstance().addError("Datasets reading", errorMessage, null);
    }

    @Nullable
    @Deprecated
    @Override
    public Collection<DataSetList> getAll() {
        Collection<DataSetListsSource> allSources = getAllSources(null);
        if (allSources == null || allSources.isEmpty()) {
            return null;
        }
        return allSources
                .stream()
                .flatMap(dataSetListsSource -> dataSetListsSource.getDataSetLists().stream())
                .collect(Collectors.toSet());
    }

    @Nullable
    @Override
    public Collection<DataSetListsSource> getAllSources() {
        return getAllSources(null);
    }

    @Nullable
    @Override
    public Collection<DataSetListsSource> getAllSources(Object projectUuid) {
        Path projectDir = DS_DIR.resolve(String.valueOf(projectUuid));
        if (Files.notExists(projectDir)) {
            LOGGER.info("Directory '{}' for project Excel Data Set does not exist.", projectDir);
            return null;
        }
        try (Stream<Path> paths = Files.walk(projectDir)) {
            List<File> files = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(DS_FILE_EXTENSION))
                    .collect(Collectors.toList());
            if (files.size() == 0) {
                return null;
            }
            return files
                    .stream()
                    .map(file -> this.createSource(projectDir.relativize(file.toPath()), projectUuid))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            LOGGER.debug("Error while getting the excel dataset-files");
            return null;
        }
    }

    @Nullable
    @Override
    public Set<IDataSet> getDataSetsWithLabel(@Nonnull DataSetList list, String label, @Nonnull Object projectUuid) {
        return null;
    }

    @Nullable
    @Override
    public DataSetListsSource getSourceByNatureId(@Nonnull Object id, Object projectUuid) {
        Collection<DataSetListsSource> allSources = getAllSources(projectUuid);
        if (allSources == null || allSources.isEmpty()) {
            return null;
        }
        return allSources
                .stream()
                .filter(dataSetListsSource -> Objects.equals(dataSetListsSource.getNaturalId(), id))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return proxy for accessing to datasets based on provided id or null if id is not valid
     */
    @Override
    public DataSetList getByNatureId(@Nonnull Object id, @Nonnull Object projectUuid) {
        String naturalId = FilenameUtils.normalize(Objects.toString(id));
        int separatorIndex = naturalId.indexOf(DS_FILE_EXTENSION + DS_LIST_NATURAL_ID_SEPARATOR);
        if (separatorIndex == -1) {
            return null;
        }
        String filePath = naturalId.substring(0, separatorIndex + DS_FILE_EXTENSION.length());
        String sheetName = naturalId.substring(separatorIndex + DS_FILE_EXTENSION.length() + 1);
        try {
            DataSetListsSource source = createSource(Paths.get(filePath), projectUuid);
            return create(source, sheetName);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    @Nullable
    @Deprecated
    @Override
    public Collection<DataSetList> getByName(String name) {
        Collection<DataSetListsSource> allSources = getAllSources();
        if (allSources == null || allSources.isEmpty()) {
            return null;
        }
        Set<DataSetList> found = allSources
                .stream()
                .flatMap(dataSetListsSource -> dataSetListsSource.getDataSetLists().stream())
                .filter(dataSetList -> Objects.equals(dataSetList.getName(), name))
                .collect(Collectors.toSet());
        if (found.isEmpty()) {
            return null;
        } else if (found.size() == 1) {
            return found;
        } else {
            DataSetList used = found.iterator().next();
            found.remove(used);//it's contains only unused items now
            logListNamesDuplication(used, found.stream());
            return Collections.singleton(used);
        }
    }

    @Nonnull
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public Set<DataSetList> getDataSetLists(@Nonnull DataSetListsSource source) {
        return resource(source).getResource().get().keySet()
                .stream().map(dsName -> create(source, dsName)).collect(Collectors.toSet());
    }

    @Nonnull
    Set<IDataSet> getDataSets(@Nonnull DataSetList list) {
        return getDsList(list).getDataSets().stream().map(ExcelDataSet::new).collect(Collectors.toSet());
    }

    @Nonnull
    Set<String> getVariables(@Nonnull DataSetList list) {
        return getDsList(list).getParameters();
    }

    @Nonnull
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    Set<String> getDataSetListsNames(@Nonnull DataSetListsSource source) {
        return resource(source).getResource().get().keySet();
    }

    @Nonnull
    DataSetList getDataSetList(@Nonnull DataSetListsSource source, @Nonnull String dataSetListName) {
        return create(source, dataSetListName);
    }

    @Nullable
    IDataSet getDataSet(@Nonnull DataSetList list, @Nonnull String name) {
        return getDataSets(list).stream().filter(ds -> name.equals(ds.getName())).findAny().orElse(null);
    }

    /**
     * converts path to string and returns it's wrapper which acts like a proxy for {@link DataSetList}.
     * guarantees that path can be constructed back in {@link #resource(DataSetListsSource)}
     *
     * @param path to .xlsx file. should be relative to {@link #DS_DIR}
     */
    @Nonnull
    private DataSetListsSource createSource(@Nonnull Path path, @Nonnull Object projectUuid) {
        String relativePath = path.toString();
        return new ExcelDataSetListsSource(this, folder, relativePath, relativePath, projectUuid);
    }

    @Nonnull
    private DataSetList create(@Nonnull DataSetListsSource source, @Nonnull String name) {
        String naturalId = source.getName() + DS_LIST_NATURAL_ID_SEPARATOR + name;
        return new ExcelDataSetList(this, source, naturalId, name);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Nonnull
    private DSList<String, Set<String>, Collection<Triple<String, String, String>>> getDsList(
            @Nonnull DataSetList instance) {
        String dsName = instance.getName();
        Resource<Map<String, DSList<String, Set<String>, Collection<Triple<String, String, String>>>>> resource =
                resource(getSource(instance));
        DSList<String, Set<String>, Collection<Triple<String, String, String>>> list =
                resource.getResource().get().get(dsName);
        Preconditions.checkNotNull(list, "No ds list with name [%s] found in file [%s]. Available names are: [%s]",
                dsName, resource.getPath(), resource.getResource().get().keySet());
        return list;
    }

    /**
     * a bridge between {@link AbstractTracker} and ITF model
     * verifies that resource has value.
     *
     * @param source should contain path relative to {@link #DS_DIR} in it's {@link Named#getName()}
     */
    @Nonnull
    private Resource<Map<String, DSList<String, Set<String>, Collection<Triple<String, String, String>>>>> resource(
            @Nonnull DataSetListsSource source) {
        Path dsPath = DS_DIR.resolve(source.getProjectUuid().toString()).resolve(source.getName());
        Resource<Map<String, DSList<String, Set<String>, Collection<Triple<String, String, String>>>>> resource =
                tracker.getDataSet(dsPath);
        if (resource.getResource().isPresent()) {
            return resource;
        } else {
            throw new RuntimeException(String.format("Can not read ds list [%s] with status [%s]", resource.getPath(),
                    resource.getStatus()), resource.getLastException().orElse(null));
        }
    }
}
