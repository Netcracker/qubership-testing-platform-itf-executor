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

package org.qubership.automation.itf.ui.services.javers.history;

import static org.qubership.automation.itf.ui.services.javers.history.HistoryEntityHelper.ENTITIES_MODEL_MAPPER;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.javers.core.Changes;
import org.javers.core.ChangesByCommit;
import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.SnapshotType;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.json.simple.JSONObject;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.javers.history.HistoryCallChain;
import org.qubership.automation.itf.core.model.javers.history.HistoryIntegrationConfig;
import org.qubership.automation.itf.core.model.javers.history.HistoryIntegrationStep;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperation;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryOperationTemplate;
import org.qubership.automation.itf.core.model.javers.history.HistorySituation;
import org.qubership.automation.itf.core.model.javers.history.HistorySituationEventTrigger;
import org.qubership.automation.itf.core.model.javers.history.HistoryStep;
import org.qubership.automation.itf.core.model.javers.history.HistoryStubProject;
import org.qubership.automation.itf.core.model.javers.history.HistorySystemTemplate;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.ui.services.javers.history.model.HistoryCompareEntity;
import org.qubership.automation.itf.ui.services.javers.history.model.HistoryItem;
import org.qubership.automation.itf.ui.services.javers.history.model.HistoryItemResponse;
import org.qubership.automation.itf.ui.services.javers.history.model.PageInfo;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryRetrieveService {

    private static final String CHILD_ACTIONS_PROPERTY = "childrenOperations";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm:ss")
            .withLocale(Locale.US);
    private static final Map<String, Set<String>> toSkip =
            new HashMap<String, Set<String>>() {
                {
                    put(HistorySituation.class.getName(), new HashSet<String>() {{
                        add("integrationStep.name");
                    }});
                }
            };
    private static final Set<String> childrenToIncludeInParentChanges =
            new HashSet<String>() {
                {
                    add(HistoryIntegrationStep.class.getName());
                }

            };
    private final Javers javers;
    private ObjectMapper objectMapper = new ObjectMapper();

    public HistoryItemResponse getAllHistory(BigInteger objectId,
                                             Class<? extends Storable> itemType,
                                             Integer offset,
                                             Integer limit) {
        try {
            HistoryEntityHelper.isSupportEntityByType(itemType);
            Class historyEntityClass = HistoryEntityHelper.getHistoryEntityClass(itemType);

            List<CdoSnapshot> snapshots =
                    javers.findSnapshots(getSnapshotsByLimit(objectId, historyEntityClass, offset, limit));

            boolean containsTerminatedSnapshot
                    = snapshots.stream().anyMatch(cdoSnapshot -> SnapshotType.TERMINAL.equals(cdoSnapshot.getType()));
            if (containsTerminatedSnapshot) {
                HistoryItemResponse response = new HistoryItemResponse();
                response.setHistoryItems(Collections.EMPTY_LIST);
                response.setPageInfo(getPageInfo(offset, limit, 0));
                return response;
            }

            JqlQuery query = getChangesByIdPaginationQuery(objectId, historyEntityClass, snapshots.stream()
                    .map(snapshot -> snapshot.getCommitId().valueAsNumber()).collect(Collectors.toList()));

            Changes changes = javers.findChanges(query);
            List<ChangesByCommit> changesByCommits = changes.groupByCommit();

            List<HistoryItem> historyItemList = changesByCommits
                    .stream()
                    .map(changesByCommit -> createHistoryItem(changesByCommit, snapshots, itemType))
                    .collect(Collectors.toList());

            List<Long> historyItemVersionsWithChanges = historyItemList.stream()
                    .map(historyItem -> Long.valueOf(historyItem.getVersion()))
                    .collect(Collectors.toList());

            List<CdoSnapshot> snapshotsWithNoChanges = snapshots.stream()
                    .filter(cdoSnapshot -> !historyItemVersionsWithChanges.contains(cdoSnapshot.getVersion()))
                    .collect(Collectors.toList());

            List<HistoryItem> simpleHistoryItemList = snapshotsWithNoChanges
                    .stream()
                    .map(cdoSnapshot -> createSimpleHistoryItem(cdoSnapshot, historyEntityClass))
                    .collect(Collectors.toList());

            historyItemList.addAll(simpleHistoryItemList);
            historyItemList.sort(Comparator.comparingInt(HistoryItem::getVersion).reversed());

            HistoryItemResponse response = new HistoryItemResponse();
            response.setHistoryItems(historyItemList);
            response.setPageInfo(getPageInfo(objectId, historyEntityClass, offset, limit));
            return response;
        } catch (Exception e) {
            if (e instanceof HistoryRetrieveException) {
                throw e;
            } else {
                throw new HistoryRetrieveException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(),
                        String.valueOf(objectId), itemType.getName());
            }
        }
    }

    private HistoryItem createSimpleHistoryItem(CdoSnapshot cdoSnapshot,
                                                Class entityClass) {
        HistoryItem historyItem = new HistoryItem();
        CommitMetadata commit = cdoSnapshot.getCommitMetadata();
        historyItem.setVersion(Integer.parseInt(String.valueOf(cdoSnapshot.getVersion())));
        historyItem.setType(entityClass.getSimpleName());
        historyItem.setModifiedBy(StringUtils.defaultIfEmpty(commit.getAuthor(), StringUtils.EMPTY));
        historyItem.setModifiedWhen(commit.getCommitDate().atOffset(ZoneOffset.UTC).format(dateTimeFormatter));
        historyItem.setChanged(Collections.EMPTY_LIST);
        return historyItem;
    }

    private HistoryItem createHistoryItem(ChangesByCommit changesByCommit,
                                          List<CdoSnapshot> snapshots,
                                          Class entityClass) {
        HistoryItem historyItem = new HistoryItem();

        CommitMetadata commit = changesByCommit.getCommit();
        historyItem.setVersion(getVersionByCommitId(snapshots, commit.getId()));
        historyItem.setType(entityClass.getSimpleName());
        historyItem.setModifiedBy(StringUtils.defaultIfEmpty(commit.getAuthor(), StringUtils.EMPTY));
        historyItem.setModifiedWhen(commit.getCommitDate().atOffset(ZoneOffset.UTC).format(dateTimeFormatter));

        Map<Boolean, List<Change>> partitions = changesByCommit.get()
                .stream()
                .filter(change -> change instanceof PropertyChange)
                .collect(Collectors.partitioningBy(change ->
                        CHILD_ACTIONS_PROPERTY.equals(((PropertyChange) change).getPropertyName())));
        historyItem.setChanged(
                calculateCommonChanges(partitions.get(false), ENTITIES_MODEL_MAPPER.get(entityClass.getName())));
        return historyItem;
    }

    private PageInfo getPageInfo(BigInteger objectId,
                                 Class historyEntityClass,
                                 Integer offset,
                                 Integer limit) {
        return getPageInfo(offset, limit, getCountByObjectId(objectId, historyEntityClass));
    }

    private PageInfo getPageInfo(Integer offset,
                                 Integer limit,
                                 Integer itemsTotalCount) {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setOffset(offset);
        pageInfo.setLimit(limit);
        pageInfo.setItemsTotalCount(itemsTotalCount);
        return pageInfo;
    }

    private Integer getCountByObjectId(BigInteger objectId, Class historyEntityClass) {
        List<CdoSnapshot> cdoSnapshots = javers.findSnapshots(getChangesByIdQuery(objectId, historyEntityClass));
        return cdoSnapshots.size();
    }

    private JqlQuery getChangesByIdQuery(BigInteger objectId, Class historyEntityClass) {
        return QueryBuilder.byInstanceId(objectId, historyEntityClass)
                .limit(Integer.MAX_VALUE)
                .withNewObjectChanges()
                .build();
    }

    private List<String> calculateCommonChanges(List<Change> changes, Class entityClass) {
        return changes
                .stream()
                .filter(change ->
                        !(toSkip.get(entityClass.getTypeName()) != null
                                && change instanceof ValueChange
                                && toSkip.get(entityClass.getTypeName())
                                .contains(((ValueChange) change).getPropertyNameWithPath())))
                .map(change -> {
                    if (!entityClass.getTypeName().equals(change.getAffectedGlobalId().getTypeName())
                            && change instanceof ValueChange
                            && !childrenToIncludeInParentChanges.contains(change.getAffectedGlobalId().getTypeName())) {
                        String propertyNameWithPath = ((ValueChange) change).getPropertyNameWithPath();
                        return propertyNameWithPath.split("/")[0].split("\\.")[0];
                    } else {
                        return ((PropertyChange) change).getPropertyName();
                    }
                })
                .distinct()
                .collect(Collectors.toList());
    }

    protected Integer getVersionByCommitId(List<CdoSnapshot> snapshots, CommitId id) {
        Integer version = null;
        Optional<CdoSnapshot> snapshot = snapshots
                .stream()
                .filter(cdoSnapshot -> cdoSnapshot.getCommitId().equals(id))
                .findFirst();

        if (snapshot.isPresent()) {
            version = Long.valueOf(snapshot.get().getVersion()).intValue();
        }

        return version;
    }

    protected JqlQuery getChangesByIdPaginationQuery(BigInteger objectId, Class entityClass,
                                                     Collection<BigDecimal> commitIds) {
        return QueryBuilder.byInstanceId(objectId, entityClass)
                .withNewObjectChanges()
                .withChildValueObjects()
                .withCommitIds(commitIds)
                .build();
    }

    protected JqlQuery getSnapshotsByLimit(BigInteger objectId, Class entityClass, Integer offset, Integer limit) {
        return QueryBuilder.byInstanceId(objectId, entityClass)
                .withScopeDeepPlus()
                .skip(offset)
                .limit(limit)
                .build();
    }

    public List<HistoryCompareEntity> getEntitiesByVersions(BigInteger objectId,
                                                            Class<? extends Storable> itemType,
                                                            List<Long> versions) {
        try {
            HistoryEntityHelper.isSupportEntityByType(itemType);
            Class historyEntityClass = HistoryEntityHelper.getHistoryEntityClass(itemType);
            return versions.stream()
                    .map(version -> getEntityByVersion(objectId, historyEntityClass, version))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            if (e instanceof HistoryRetrieveException) {
                throw e;
            } else {
                throw new HistoryRetrieveException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(),
                        String.valueOf(objectId), itemType.getName());
            }
        }
    }

    private HistoryCompareEntity getEntityByVersion(BigInteger objectId, Class entityClass, Long version) {
        Optional<Shadow<Object>> entity = getShadow(objectId, entityClass, version);
        if (entity.isPresent()) {
            return buildCompareEntity(objectId, entityClass, entity.get(), version);
        } else {
            throw new EntityNotFoundException(String.format("Failed to found shadow with id:%s, class:%s",
                    objectId, entityClass));
        }
    }

    private Optional<Shadow<Object>> getShadow(BigInteger objectId, Class entityClass, Long version) {
        JqlQuery query = QueryBuilder.byInstanceId(objectId, entityClass)
                .withVersion(version)
                .withScopeDeepPlus()
                .build();
        List<CdoSnapshot> snapshots = javers.findSnapshots(query);

        QueryBuilder queryBuilder = QueryBuilder.byInstanceId(objectId, entityClass)
                .withVersion(version).withScopeDeepPlus();
        if (Objects.nonNull(snapshots) && !snapshots.isEmpty()) {
            queryBuilder.withCommitId(snapshots.get(0).getCommitId());
        }
        List<Shadow<Object>> shadows = javers.findShadows(queryBuilder.build());
        return shadows.stream().findFirst();
    }

    private HistoryCompareEntity buildCompareEntity(BigInteger objectId, Class entityClass,
                                                    Shadow<Object> entity, Long revision) {
        Object object = entity.get();
        if (Objects.isNull(object)) {
            throw new EntityNotFoundException(String.format("Shadow object is null with id:%s." + objectId));
        }
        HistoryCompareEntity historyCompareEntity = new HistoryCompareEntity();
        historyCompareEntity.setRevision(revision.toString());
        Map<String, Object> compareEntityAsMap = convertObjectToMap(object);
        setCommonFields(objectId, entityClass, compareEntityAsMap, entity.getCommitMetadata());
        performInnerProperty(compareEntityAsMap, entityClass);
        historyCompareEntity.setCompareEntity(compareEntityAsMap);
        return historyCompareEntity;
    }

    private void performInnerProperty(Map<String, Object> propertyMap, Class entityClass) {
        if (propertyMap.containsKey("id")) {
            propertyMap.put("id", propertyMap.get("id").toString());
        }

        if (entityClass.isAssignableFrom(HistoryStep.class)) {
            String type = propertyMap.get("type").toString();
            if (SituationStep.TYPE.equals(type)) {
                removeKeyInMap(propertyMap, "chainId");
            } else if (EmbeddedStep.TYPE.equals(type)) {
                removeKeyInMap(propertyMap, "situationId");
            }
            performConditionParameters(propertyMap);

        } else if (entityClass.isAssignableFrom(HistoryCallChain.class)) {
            ArrayList steps = (ArrayList) propertyMap.getOrDefault("steps", Collections.EMPTY_LIST);
            if (!steps.isEmpty()) {
                Map newSteps = new LinkedHashMap();
                for (int i = 0; i < steps.size(); i++) {
                    Map<String, Object> stepAsMap = convertObjectToMap(steps.get(i));

                    JSONObject stepAsJson = new JSONObject();
                    stepAsJson.put("id", stepAsMap.get("id").toString());
                    stepAsJson.put("type", stepAsMap.get("type").toString());

                    String key = "step#" + (i + 1);
                    newSteps.put(key, stepAsJson);
                }
                propertyMap.put("steps", newSteps);
            }

        } else if (entityClass.isAssignableFrom(HistoryStubProject.class)) {
            performMapKey(propertyMap, "storableProp");

        } else if (entityClass.isAssignableFrom(HistoryIntegrationConfig.class)) {
            performMapKey(propertyMap, "configuration");
            removeKeyInMap(propertyMap, "typeName");

        } else if (entityClass.isAssignableFrom(HistoryOperationTemplate.class)
                || entityClass.isAssignableFrom(HistorySystemTemplate.class)) {
            ArrayList transportProperties = (ArrayList) propertyMap.get("transportProperties");

            if (!transportProperties.isEmpty()) {
                Map newTransportProperties = new LinkedHashMap();
                for (int i = 0; i < transportProperties.size(); i++) {
                    Map<String, Object> transportPropertiesAsMap = (Map<String, Object>) transportProperties.get(i);

                    removeKeyInMap(transportPropertiesAsMap, "description");
                    removeKeyInMap(transportPropertiesAsMap, "name");

                    String typeName = transportPropertiesAsMap.get("typeName").toString();
                    String[] splitTypeName = typeName.split("\\.");
                    String key = splitTypeName[splitTypeName.length - 1];
                    newTransportProperties.put(key, transportPropertiesAsMap);
                }
                propertyMap.put("transportProperties", newTransportProperties);
            }
        } else if (entityClass.isAssignableFrom(HistoryOperation.class)) {

            ArrayList situations = (ArrayList) propertyMap.getOrDefault("situations", Collections.EMPTY_LIST);
            if (!situations.isEmpty()) {
                Map newSituations = new LinkedHashMap();
                for (int i = 0; i < situations.size(); i++) {

                    Map<String, Object> situationAsMap = (Map<String, Object>) situations.get(i);
                    String key = "situation#" + (i + 1) + "[" + situationAsMap.get("id").toString() + "]";

                    JSONObject situationAsJson = new JSONObject();
                    situationAsJson.put("id", situationAsMap.get("id").toString());
                    situationAsJson.put("name", situationAsMap.get("name").toString());
                    newSituations.put(key, situationAsJson);
                }
                propertyMap.put("situations", newSituations);
            }
            removeKeyInMap(propertyMap, "situationEventTriggers");
            removeKeyInMap(propertyMap, "operationEventTriggers");

        } else if (entityClass.isAssignableFrom(HistorySituation.class)) {
            removeKeyInMap(propertyMap, "situationEventTriggers");
            removeKeyInMap(propertyMap, "operationEventTriggers");

        } else if (entityClass.isAssignableFrom(HistorySituationEventTrigger.class)
                || entityClass.isAssignableFrom(HistoryOperationEventTrigger.class)) {
            performConditionParameters(propertyMap);
        }
    }

    private void performConditionParameters(Map<String, Object> propertyMap) {
        ArrayList conditionParameters = (ArrayList) propertyMap
                .getOrDefault("conditionParameters", Collections.EMPTY_LIST);
        if (!conditionParameters.isEmpty()) {
            Map newConditionParameters = new LinkedHashMap();
            for (int i = 0; i < conditionParameters.size(); i++) {
                Object conditionParameter = conditionParameters.get(i);
                Map<String, Object> conditionParameterAsMap = convertObjectToMap(conditionParameter);
                String key = "condition#" + (i + 1);
                newConditionParameters.put(key, conditionParameterAsMap);
            }
            propertyMap.put("conditionParameters", newConditionParameters);
        }
    }

    private void setCommonFields(BigInteger objectId, Class entityClass, Map<String, Object> destination,
                                 CommitMetadata commitMetadata) {
        destination.put("modifiedBy", commitMetadata.getAuthor());
        destination.put("modifiedWhen",
                commitMetadata.getCommitDate().atOffset(ZoneOffset.UTC).format(dateTimeFormatter));

        List<CdoSnapshot> snapshots = javers.findSnapshots(QueryBuilder
                .byInstanceId(objectId, entityClass)
                .withSnapshotType(SnapshotType.INITIAL)
                .build());

        if (Objects.nonNull(snapshots) && !snapshots.isEmpty()) {
            CommitMetadata initialCommitMetadata = snapshots.get(0).getCommitMetadata();
            destination.put("createdBy",
                    StringUtils.defaultIfEmpty(initialCommitMetadata.getAuthor(), StringUtils.EMPTY));
            destination.put("createdWhen",
                    initialCommitMetadata.getCommitDate().atOffset(ZoneOffset.UTC).format(dateTimeFormatter));
        }
    }

    private Map<String, Object> convertObjectToMap(Object object) {
        return objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
    }

    private void removeKeyInMap(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            map.remove(key);
        }
    }

    private void performMapKey(Map<String, Object> propertyMap, String propertyKey) {
        if (propertyMap.containsKey(propertyKey)) {
            Map<String, String> map = (Map) propertyMap.get(propertyKey);
            Map<String, String> newMap = new HashMap<>();
            for (String key : map.keySet()) {
                newMap.put(toCamelCaseFormat(key), map.get(key));
            }
            propertyMap.put(propertyKey, newMap);
        }
    }

    private String toCamelCaseFormat(String propertyName) {
        return CaseUtils.toCamelCase(propertyName, false, '.');
    }
}
