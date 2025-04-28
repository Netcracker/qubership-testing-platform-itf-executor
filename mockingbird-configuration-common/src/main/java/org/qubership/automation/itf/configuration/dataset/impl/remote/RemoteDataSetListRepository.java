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

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.DATA_SET_SERVICE_DS_FORMAT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.DATA_SET_SERVICE_DS_FORMAT_DEFAULT_VALUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.datasets.dto.DataSetDto;
import org.qubership.atp.datasets.dto.DataSetListCreatedModifiedViewDto;
import org.qubership.atp.datasets.dto.DataSetTreeDto;
import org.qubership.atp.datasets.dto.VisibilityAreaFlatModelDto;
import org.qubership.automation.configuration.dataset.excel.impl.Utils;
import org.qubership.automation.itf.configuration.dataset.impl.DataSetListRepository;
import org.qubership.automation.itf.configuration.utils.JSONContextUtils;
import org.qubership.automation.itf.core.model.common.Named;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.constants.DatasetFormat;
import org.qubership.automation.itf.core.util.feign.http.HttpClientFactory;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteDataSetListRepository implements DataSetListRepository {

    private static final char DS_LIST_NATURAL_ID_SEPARATOR = '_';
    private static final Pattern UUID_KEY_PATTERN = Pattern.compile("^[0-9a-f-]{36}\\Z");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VALUE_KEY = "value";
    private static final String TYPE_KEY = "type";
    private static final String PARAMETERS_KEY = "parameters";
    private static final String GROUPS_KEY = "groups";
    private final Folder<DataSetListsSource> folder;

    public RemoteDataSetListRepository(Folder<DataSetListsSource> folder) {
        this.folder = folder;
    }

    @Nonnull
    private static Stream<JsonNode> array(@Nullable JsonNode jsonNode) {
        return (jsonNode == null) ? Stream.empty() : stream(jsonNode.elements());
    }

    @Nonnull
    private static ObjectNode object(@Nonnull JsonNode jsonNode) throws IOException {
        if (!jsonNode.isObject()) {
            throw new IOException(String.format("Expected json object, got [%s]", jsonNode.getNodeType().name()));
        }
        return (ObjectNode) jsonNode;
    }

    @Nonnull
    private static <T> Stream<T> stream(@Nonnull Iterator<T> iter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false);
    }

    private static String makeErrorMessage(String message, String endpoint, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        String serviceRoute = ApplicationConfig.env.getProperty("feign.atp.datasets.url")
                + "/" + ApplicationConfig.env.getProperty("feign.atp.datasets.route");
        return message + "\nURL: " + serviceRoute + ", endpoint: " + endpoint + ", duration: " + duration + "(ms)";
    }

    private boolean isValidUuid(@Nonnull String id) {
        Matcher matcher = UUID_KEY_PATTERN.matcher(id);
        return (matcher.matches()) && id.split("-").length == 5;
    }

    @Nullable
    @Deprecated
    @Override
    public Collection<DataSetList> getAll() {
        return null;//will not implement deprecated method
    }

    @Nullable
    @Override
    public Collection<DataSetListsSource> getAllSources() {
        long startTime = System.currentTimeMillis();
        String endpoint = "/va";
        try {
            ResponseEntity<List<VisibilityAreaFlatModelDto>> response
                    = HttpClientFactory.getDatasetsVisibilityAreaFeignClient().getVisibilityAreas();
            if (!response.hasBody()) {
                throw new IOException(String.format("Response body is null for '%s', http status %s.",
                        endpoint, response.getStatusCode()));
            }
            return Objects.requireNonNull(response.getBody()).stream()
                    .map(obj -> createSource(obj.getId(), obj.getName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(makeErrorMessage("DSS is not available. Remote datasets can not be loaded.",
                    endpoint, startTime));
            return null;
        }
    }

    @Nullable
    @Override
    public Collection<DataSetListsSource> getAllSources(Object projectUuid) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/va";
        try {
            ResponseEntity<List<VisibilityAreaFlatModelDto>> response
                    = HttpClientFactory.getDatasetsVisibilityAreaFeignClient().getVisibilityAreas();
            if (!response.hasBody()) {
                throw new IOException(String.format("Response body is null for '%s', http status %s.",
                        endpoint, response.getStatusCode()));
            }
            return Objects.requireNonNull(response.getBody()).stream()
                    .filter(obj -> projectUuid.equals(obj.getId()))
                    .map(obj -> createSource(obj.getId(), obj.getName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(makeErrorMessage("DSS is not available. Remote datasets can not be loaded.",
                    endpoint, startTime));
            return null;
        }
    }

    @Nullable
    @Override
    public DataSetListsSource getSourceByNatureId(@Nonnull Object id, Object projectId) {
        String uuidStr = id.toString();
        if (!isValidUuid(uuidStr)) {
            return null;
        }
        //need source name. will load all sources for now
        return Optional.ofNullable(getAllSources()).flatMap(sources -> sources.stream()
                        .filter(source -> uuidStr.equals(source.getNaturalId()))
                        .findFirst())
                .orElse(null);
    }

    @Override
    public DataSetList getByNatureId(@Nonnull Object id, Object projectId) {
        String naturalId = id.toString();
        int separatorIndex = naturalId.indexOf(DS_LIST_NATURAL_ID_SEPARATOR);
        if (separatorIndex == -1) {
            return null;
        }
        String dslId = naturalId.substring(separatorIndex + 1);
        if (!isValidUuid(dslId)) {
            return null;
        }
        String sourceId = naturalId.substring(0, separatorIndex);
        DataSetListsSource source = getSourceByNatureId(sourceId, null);
        if (source == null) {
            return null;
        }
        return getDataSetLists(source).stream()
                .filter(dsl -> naturalId.equals(dsl.getNaturalId()))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    @Deprecated
    @Override
    public Collection<DataSetList> getByName(String name) {
        return null;//will not implement deprecated method
    }

    @Nonnull
    @Override
    public Set<DataSetList> getDataSetLists(@Nonnull DataSetListsSource source) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/dsl/va/".concat(source.getID().toString());
        try {
            UUID vaId = UUID.fromString(source.getID().toString());
            ResponseEntity<List<DataSetListCreatedModifiedViewDto>> response
                    = HttpClientFactory.getDatasetsDatasetListFeignClient().getDataSetListsByVaId(vaId, null);
            if (!response.hasBody()) {
                throw new IOException(String.format("Response body is null for '%s', http status %s.",
                        endpoint, response.getStatusCode()));
            }
            return Objects.requireNonNull(response.getBody()).stream()
                    .map(obj -> create(source, obj.getId(), obj.getName()))
                    .collect(Collectors.toSet());
        } catch (IOException | IllegalArgumentException e) {
            String errorMessage = makeErrorMessage("Can not get all DSLs for Visibility Area",
                    endpoint, startTime);
            log.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    @Nonnull
    Set<IDataSet> getDataSets(@Nonnull DataSetList list, Object projectId) {
        return getDataSetsWithLabel(list, "", projectId);
    }

    @Nonnull
    @Override
    public Set<IDataSet> getDataSetsWithLabel(@Nonnull DataSetList list, String label, @Nonnull Object projectId) {
        long startTime = System.currentTimeMillis();
        String dslId = list.getNaturalId().toString().split("_")[1];
        String endpoint = StringUtils.isNotBlank(label) ? String.format("/dsl/%s/ds?label=%s", dslId, label) :
                String.format("/dsl/%s/ds", dslId);
        try {
            UUID dataSetListId = UUID.fromString(dslId);
            ResponseEntity<List<DataSetDto>> response;
            if (StringUtils.isNotBlank(label)) {
                response = HttpClientFactory.getDatasetsDatasetListFeignClient()
                        .getDataSets(dataSetListId, null, label);
            } else {
                response = HttpClientFactory.getDatasetsDatasetListFeignClient()
                        .getDataSets(dataSetListId, null, null);
            }
            if (!response.hasBody()) {
                throw new IOException(String.format("Response body is null for '%s', http status %s.",
                        endpoint, response.getStatusCode()));
            }
            return Objects.requireNonNull(response.getBody()).stream()
                    .map(dataSetDto -> {
                        String name = dataSetDto.getName();
                        String id = dataSetDto.getId().toString();
                        Supplier<JsonContext> jsonContextSup = new DataSetValuesSup(id, projectId);
                        return new RemoteDataSet(name, Utils.memoize(jsonContextSup), id, new ArrayList<>());
                    }).collect(Collectors.toSet());
        } catch (IOException | IllegalArgumentException e) {
            String errorMessage = makeErrorMessage("Can not get list of Datasets for DSL id: '" + dslId + "'"
                    + (StringUtils.isNotBlank(label) ? " with label: '" + label + "'" : ""), endpoint, startTime);
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    @Nonnull
    Set<String> getVariables(@Nonnull DataSetList list) {
        long startTime = System.currentTimeMillis();
        String dslId = list.getNaturalId().toString().split("_")[1];
        String endpoint = String.format("/attribute/dsl/%s/itf", dslId);
        try {
            UUID vaId = UUID.fromString(dslId);
            ResponseEntity<Object> responseEntity
                    = HttpClientFactory.getDatasetsAttributeFeignClient().getAttributesInItfFormat(vaId);
            if (!responseEntity.hasBody()) {
                throw new IOException(String.format("Response body is null for '%s', http status %s.",
                        endpoint, responseEntity.getStatusCode()));
            }
            return (Set<String>) ((ArrayList) Objects.requireNonNull(responseEntity.getBody()))
                    .stream().collect(Collectors.toSet());
        } catch (IOException | IllegalArgumentException e) {
            String errorMessage = String.format("Can not get variables for DSL id:%s", dslId);
            log.error(makeErrorMessage(errorMessage, endpoint, startTime), e);
            return Collections.emptySet();
        }
    }

    @Nonnull
    Set<String> getDataSetListsNames(@Nonnull DataSetListsSource source) {
        return getDataSetLists(source).stream().map(Named::getName).collect(Collectors.toSet());
    }

    DataSetList getDataSetList(@Nonnull DataSetListsSource source, @Nonnull String dataSetListName) {
        return getDataSetLists(source).stream()
                .filter(dsl -> dataSetListName.equals(dsl.getName())).findFirst().orElse(null);
    }

    IDataSet getDataSet(@Nonnull DataSetList list, @Nonnull String name, @Nonnull Object projectId) {
        Set<IDataSet> dataSets = getDataSets(list, projectId);
        if (!dataSets.isEmpty()) {
            Optional<IDataSet> opt = dataSets.stream().filter(ds -> name.equals(ds.getName())).findFirst();
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    IDataSet getDataSetById(@Nonnull DataSetList list, @Nonnull String id, @Nonnull Object projectId) {
        Set<IDataSet> dataSets = getDataSets(list, projectId);
        if (!dataSets.isEmpty()) {
            Optional<IDataSet> opt = dataSets.stream().filter(ds -> id.equals(ds.getIdDs())).findFirst();
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    @Nonnull
    JsonContext getDataSetContextById(@Nonnull String id, @Nonnull Object projectId) {
        return DataSetValuesSup.get(id, projectId);
    }

    @Nonnull
    private DataSetListsSource createSource(@Nonnull UUID id, @Nonnull String name) {
        return new RemoteDataSetListsSource(this, folder, id, name);
    }

    @Nonnull
    private DataSetList create(@Nonnull DataSetListsSource source, @Nonnull UUID id, @Nonnull String name) {
        String idStr = source.getNaturalId().toString() + DS_LIST_NATURAL_ID_SEPARATOR + id;
        return new RemoteDataSetList(this, source, idStr, name);
    }

    private static class DataSetValuesSup implements Supplier<JsonContext> {

        private static final Gson GSON = new GsonBuilder().create();
        private static final Map<String, String> DATASET_FORMATS;

        static {
            DATASET_FORMATS = new HashMap<>();
            DATASET_FORMATS.put(DatasetFormat.ITF.toString(), "itf");
            DATASET_FORMATS.put(DatasetFormat.DEFAULT.toString(), "atp");
            DATASET_FORMATS.put(DatasetFormat.OBJECT.toString(), "atp/object");
            DATASET_FORMATS.put(DatasetFormat.OBJECTEXTENDED.toString(), "atp/objectExtended");
            DATASET_FORMATS.put(DatasetFormat.OPTIMIZED.toString(), "atp/optimized");
        }

        @Getter
        private final String dsId;
        private final Object projectId;

        private DataSetValuesSup(@Nonnull String dsId, @Nonnull Object projectId) {
            this.dsId = dsId;
            this.projectId = projectId;
        }

        private static JsonContext getItf(String dsId) {
            long startTime = System.currentTimeMillis();
            String endpoint = String.format("/ds/%s/itf", dsId);
            try {
                UUID dataSetId = UUID.fromString(dsId);
                ResponseEntity<String> response
                        = HttpClientFactory.getDatasetsDatasetFeignClient().getItfContext(dataSetId);
                if (!response.hasBody()) {
                    throw new IOException(String.format("Response body is null for '%s', http status %s.",
                            endpoint, response.getStatusCode()));
                }
                return GSON.fromJson(response.getBody(), JsonContext.class);
            } catch (IOException | IllegalArgumentException e) {
                String errorMessage = makeErrorMessage(
                        String.format("Can not get contents of Dataset with id:%s", dsId), endpoint, startTime);
                throw new RuntimeException(errorMessage, e);
            }
        }

        public static JsonContext getAtp(String dsId, String datasetFormat) {
            long startTime = System.currentTimeMillis();
            String endpoint = String.format("/ds/%s/%s?evaluate=true", dsId, datasetFormat);
            try {
                ResponseEntity<DataSetTreeDto> response = null;
                UUID dataSetId = UUID.fromString(dsId);
                switch (datasetFormat) {
                    case ("atp"): {
                        response = HttpClientFactory.getDatasetsDatasetFeignClient()
                                .getAtpContextFull(dataSetId, "true", StringUtils.EMPTY);
                        break;
                    }
                    case ("atp/object"): {
                        response = HttpClientFactory.getDatasetsDatasetFeignClient()
                                .getAtpContextObject(dataSetId, "true", StringUtils.EMPTY);
                        break;
                    }
                    case ("atp/objectExtended"): {
                        response = HttpClientFactory.getDatasetsDatasetFeignClient()
                                .getAtpContextObjectExtended(dataSetId, "true", StringUtils.EMPTY);
                        break;
                    }
                    case ("atp/optimized"): {
                        response = HttpClientFactory.getDatasetsDatasetFeignClient()
                                .getAtpContextOptimized(dataSetId, "true", StringUtils.EMPTY);
                        break;
                    }
                    default: {
                    }
                }
                if (Objects.nonNull(response) && !response.hasBody()) {
                    throw new IOException(String.format("Response body is null for '%s', http status %s.",
                            endpoint, response.getStatusCode()));
                }
                ObjectNode objectNode = MAPPER.convertValue(Objects.requireNonNull(response).getBody(),
                        ObjectNode.class);
                return JSONContextUtils.convert(atp2itf(objectNode, dsId), MAPPER);
            } catch (IOException | IllegalArgumentException e) {
                String errorMessage = makeErrorMessage(
                        String.format("Can not get contents of Dataset with id:%s", dsId), endpoint, startTime);
                throw new RuntimeException(errorMessage, e);
            }
        }

        private static ObjectNode atp2itf(ObjectNode source, String dsId) {
            ObjectNode destination = MAPPER.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> jsonField = fields.next();
                processNode(destination, jsonField.getValue(), jsonField.getKey());
            }
            return destination;
        }

        private static void processNode(ObjectNode destination, JsonNode value, String key) {
            if (value == null) {
                return;
            }
            switch (key) {
                case PARAMETERS_KEY:
                    Iterator<Map.Entry<String, JsonNode>> params = value.fields();
                    while (params.hasNext()) {
                        Map.Entry<String, JsonNode> jsonField = params.next();
                        JsonNode jsonFieldValue = jsonField.getValue();
                        if ("FILE".equals(jsonFieldValue.get(TYPE_KEY).textValue())
                                && jsonFieldValue.get(VALUE_KEY).textValue() != null) {
                            String newValue = "/attachment/"
                                    + UUID.fromString(jsonFieldValue.get(VALUE_KEY).textValue().substring(0, 36));
                            destination.put(jsonField.getKey(), newValue);
                        } else {
                            destination.put(jsonField.getKey(), jsonFieldValue.get(VALUE_KEY).textValue() != null
                                    ? jsonFieldValue.get(VALUE_KEY).textValue() : "");
                        }
                    }
                    break;
                case GROUPS_KEY:
                    Iterator<Map.Entry<String, JsonNode>> groups = value.fields();
                    while (groups.hasNext()) {
                        Map.Entry<String, JsonNode> jsonField = groups.next();
                        ObjectNode grpObj = destination.putObject(jsonField.getKey());
                        processNode(grpObj, jsonField.getValue().get(PARAMETERS_KEY), PARAMETERS_KEY);
                        processNode(grpObj, jsonField.getValue().get(GROUPS_KEY), GROUPS_KEY);
                        if (grpObj.isEmpty()) {
                            destination.remove(jsonField.getKey());
                            destination.put(jsonField.getKey(), "");
                        }
                    }
                    break;
                default:
            }
        }

        public static JsonContext get(String dsId, Object projectId) {
            String datasetFormat = CoreServices.getProjectSettingsService().get(projectId,
                    DATA_SET_SERVICE_DS_FORMAT, DATA_SET_SERVICE_DS_FORMAT_DEFAULT_VALUE);
            return ("Itf".equals(datasetFormat)) ? getItf(dsId) : getAtp(dsId, DATASET_FORMATS.get(datasetFormat));
        }

        @Override
        public JsonContext get() {
            return get(this.dsId, this.projectId);
        }
    }
}
