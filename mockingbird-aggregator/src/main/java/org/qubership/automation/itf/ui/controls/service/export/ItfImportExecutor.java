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

package org.qubership.automation.itf.ui.controls.service.export;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.qubership.atp.ei.node.ImportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SituationEventTriggerObjectManager;
import org.qubership.automation.itf.core.model.common.Identified;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.EventTriggerBriefInfo;
import org.qubership.automation.itf.core.model.communication.StubUser;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.folder.ServerFolder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.server.ServerHB;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.project.ProjectSettings;
import org.qubership.automation.itf.core.util.Pair;
import org.qubership.automation.itf.core.util.constants.EiConstants;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.vavr.Function3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItfImportExecutor implements ImportExecutor {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private final ProjectSettingsService projectSettingsService;
    @PersistenceContext
    protected EntityManager entityManager;
    Map<String, Class> map = Maps.newLinkedHashMapWithExpectedSize(7);
    Map<String, Function3<ConcurrentHashMap<Object, ? extends Storable>, StubProject,
            Map<BigInteger, BigInteger>, Void>> beforeImportFunctionMap = new HashMap<>();
    private ItfReplicationService itfReplicationService;
    @Value("${ei.waiting.project.creation.timeout}")
    private int EI_WAITING_PROJECT_CREATION_TIMEOUT;
    @Value("${ei.waiting.project.creation.attempt.count}")
    private int EI_WAITING_PROJECT_CREATION_ATTEMPT_COUNT;

    {
        map.put(ProjectSettings.class.getSimpleName(), ProjectSettings.class);
        map.put(IntegrationConfig.class.getSimpleName(), IntegrationConfig.class);
        map.put(SystemFolder.class.getSimpleName(), SystemFolder.class);
        map.put(ChainFolder.class.getSimpleName(), ChainFolder.class);
        map.put(EnvFolder.class.getSimpleName(), EnvFolder.class);
        map.put(System.class.getSimpleName(), System.class);
        map.put(CallChain.class.getSimpleName(), CallChain.class);
        map.put(Environment.class.getSimpleName(), Environment.class);
        map.put(ServerHB.class.getSimpleName(), ServerHB.class);
    }

    {
        beforeImportFunctionMap.put(System.class.getSimpleName(),
                (input, project, replacementMap)
                        -> invalidateSystemReferences((ConcurrentHashMap<Object, System>) input));
        beforeImportFunctionMap.put(CallChain.class.getSimpleName(),
                (input, project, replacementMap)
                        -> invalidateCallchainReferences((ConcurrentHashMap<Object, CallChain>) input, replacementMap));
        beforeImportFunctionMap.put(ServerHB.class.getSimpleName(),
                (input, project, replacementMap)
                        -> invalidateServerReferences((ConcurrentHashMap<Object, ServerHB>) input, replacementMap));
        beforeImportFunctionMap.put(Environment.class.getSimpleName(),
                (input, project, replacementMap)
                        -> invalidateEnvironmentReferences((ConcurrentHashMap<Object, Environment>) input,
                        replacementMap));
    }

    @Autowired
    public void setItfReplicationService(ItfReplicationService itfReplicationService) {
        this.itfReplicationService = itfReplicationService;
    }

    @Transactional
    @Override
    public void importData(ExportImportData exportImportData, Path path) {
        Timestamp timestampMainImportStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("Main import process start. Start Time: {}", timestampMainImportStart);
        ObjectManager<StubProject> manager = CoreObjectManager.getInstance().getManager(StubProject.class);

        UUID importedProjectId = exportImportData.getImportedProjectId();
        UUID destinationProjectUuid = exportImportData.getReplacementMap().get(importedProjectId);
        UUID resultProjectUuid = destinationProjectUuid != null ? destinationProjectUuid : importedProjectId;

        BigInteger projectId = getProjectIdByUuid(resultProjectUuid);
        StubProject targetProject = manager.getById(projectId);
        List<Pair<Path, String>> foldersForImport = prepareListOfFoldersForImport(path);
        Map<BigInteger, BigInteger> replacementMap = Maps.newHashMap();
        Map<String, Boolean> postImportActions = new HashMap<>();
        List<BigInteger> systemIds = new ArrayList<>();
        TxExecutor.executeVoid(() -> {
            try {
                manager.setReplicationRole("replica");
                for (Pair<Path, String> importFolder : foldersForImport) {
                    log.warn("entityType: {}, exportImportData: {}", importFolder.getValue(), exportImportData);
                    importObjects(importFolder.getValue(), exportImportData, importFolder.getKey(), targetProject,
                            replacementMap, systemIds);

                    Timestamp timestampFlushStart = new Timestamp(java.lang.System.currentTimeMillis());
                    log.info("Start flush for entities: {}. Start time: {}",
                            importFolder.getValue(),
                            timestampFlushStart);
                    try {
                        ((Session) this.entityManager.getDelegate()).flush();
                    } catch (Exception ex) {
                        log.error("Error while import: ", ex);
                    }
                    Timestamp timestampFlushFinish = new Timestamp(java.lang.System.currentTimeMillis());
                    log.info("Finish flush for entities: {}. Finish time: {}, Duration: {}",
                            importFolder.getValue(),
                            timestampFlushFinish,
                            getTimestampDifference(timestampFlushStart, timestampFlushFinish));

                    if (EiConstants.SYSTEM.equals(importFolder.getValue())) {
                        postImportActions.put("shouldActivateEventTriggers", true);
                    }
                }
            } catch (Exception e) {
                log.error("Error while import: ", e);
                throw new IllegalArgumentException(e);
            } finally {
                manager.setReplicationRole("origin");
            }
        }, TxExecutor.defaultWritableTransaction());
        Timestamp timestampMainImportFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("Main import process finish. Finish Time: {}, Duration: {}",
                timestampMainImportFinish,
                getTimestampDifference(timestampMainImportStart, timestampMainImportFinish));

        if (postImportActions.getOrDefault("shouldActivateEventTriggers", false)) {
            prepareAndSendSyncEventTriggersRequest(systemIds, resultProjectUuid);
        }
    }

    private void prepareAndSendSyncEventTriggersRequest(List<BigInteger> systemIds, UUID projectUuid) {
        log.info("Activate Event Triggers (make and send SyncActivationRequest)...");
        TxExecutor.executeVoid(() -> {
            try {
                List<EventTriggerBriefInfo> triggersToDeactivate = new ArrayList<>();
                List<EventTriggerBriefInfo> triggersToReactivate = new ArrayList<>();
                SituationEventTriggerObjectManager objectManager = CoreObjectManager.getInstance()
                        .getSpecialManager(SituationEventTrigger.class, SituationEventTriggerObjectManager.class);
                for (BigInteger id : systemIds) {
                    Map<String, List<EventTriggerBriefInfo>> map = objectManager.getTriggersBriefInfoBySystem(id);
                    triggersToReactivate.addAll(map.get("ToReactivate"));
                    triggersToDeactivate.addAll(map.get("ToDeactivate"));
                    log.info("Getting info for system {} is done.", id);
                }
                EventTriggerSyncActivationRequest eventTriggerSyncActivationRequest =
                        new EventTriggerSyncActivationRequest(triggersToDeactivate, triggersToReactivate,
                                new StubUser(""), UUID.randomUUID().toString());
                executorToMessageBrokerSender.sendMessage(eventTriggerSyncActivationRequest,
                        "message-broker.configurator-executor-event-triggers.topic",
                        "topic",
                        projectUuid.toString());
            } catch (Exception e) {
                log.error("Error while making/sending of SyncActivationRequest: ", e);
                throw new IllegalArgumentException(e);
            }
        }, TxExecutor.readOnlyTransaction());
        log.info("Activate Event Triggers (make and send SyncActivationRequest) is done.");
    }

    @Override
    public ValidationResult preValidateData(ExportImportData exportImportData, Path path) throws Exception {
        return null;
    }

    @Override
    public ValidationResult validateData(ExportImportData exportImportData, Path path) throws Exception {
        return null;
    }

    private String getTimestampDifference(Timestamp first, Timestamp second) {
        long minutes = (second.getTime() - first.getTime()) / (60 * 1000);
        if (minutes == 0) {
            return ((second.getTime() - first.getTime()) / (1000)) + " sec.";
        } else {
            return minutes + " min.";
        }
    }

    /**
     * Import object.
     *
     * @param entityType       - ITF entity for import.
     * @param exportImportData import data from catalogue.
     * @param path             to import object
     * @param project          project.
     * @throws ClassNotFoundException when storable parent class not found.
     */
    public void importObjects(String entityType, ExportImportData exportImportData, Path path,
                              StubProject project, Map<BigInteger, BigInteger> replacementMap,
                              List<BigInteger> systemIds)
            throws Exception {
        Timestamp timestampImportObjectsStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("Start import process for: {}. Start time: {}", entityType, timestampImportObjectsStart);

        Timestamp timestampGetStorablesFilesStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- Get storablesFiles - start. Start time: {}", timestampGetStorablesFilesStart);
        Map<UUID, Path> storablesFiles = objectLoaderFromDiskService.getListOfObjects(path, map.get(entityType));
        Timestamp timestampGetStorablesFilesFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- Get storablesFiles - finish. Finish time: {}, Duration: {}",
                timestampGetStorablesFilesFinish,
                getTimestampDifference(timestampGetStorablesFilesStart, timestampGetStorablesFilesFinish));

        ConcurrentHashMap<Object, Storable> storablesForImport = new ConcurrentHashMap<>();

        Timestamp timestampLoadFilesStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- loadFileAsObjectWithReplacementMap - start. Start time: {}", timestampLoadFilesStart);
        for (Map.Entry<UUID, Path> entry : storablesFiles.entrySet()) {
            Path filePath = entry.getValue();
            Storable storable = (Storable) objectLoaderFromDiskService.loadFileAsObjectWithReplacementMapThrowException(
                    filePath, map.get(entityType), exportImportData.getReplacementMap(), false);
            storablesForImport.put(storable.getID(), storable);
        }
        Timestamp timestampLoadFilesFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- loadFileAsObjectWithReplacementMap - finish. Finish time: {}, Duration: {}",
                timestampLoadFilesFinish,
                getTimestampDifference(timestampLoadFilesStart, timestampLoadFilesFinish));

        Timestamp timestampInvalidateStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- invalidateBeforeImport - start. Start time: {}", timestampInvalidateStart);
        invalidateBeforeImport(entityType, storablesForImport, project, replacementMap);
        Timestamp timestampInvalidateFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- invalidateBeforeImport - finish. Finish time: {}, Duration: {}",
                timestampInvalidateFinish,
                getTimestampDifference(timestampInvalidateStart, timestampInvalidateFinish));

        boolean internalProjectAnother = isInternalProjectAnother((BigInteger) project.getID(),
                storablesForImport.values());
        boolean catalogueProjectAnother = isCatalogueProjectAnotherOrNew(exportImportData);
        if (internalProjectAnother || catalogueProjectAnother) {
            Timestamp timestampPerformActionsForImportIntoAnotherProjectStart =
                    new Timestamp(java.lang.System.currentTimeMillis());
            log.info("-- performActionsForImportIntoAnotherProject - start. Start time: {}",
                    timestampPerformActionsForImportIntoAnotherProjectStart);
            performActionsForImportIntoAnotherProject(storablesForImport.values(), replacementMap,
                    (BigInteger) project.getID(), project.getUuid(), internalProjectAnother, catalogueProjectAnother);
            Timestamp timestampPerformActionsForImportIntoAnotherProjectFinish =
                    new Timestamp(java.lang.System.currentTimeMillis());
            log.info("-- performActionsForImportIntoAnotherProject - finish. Finish time: {}, Duration: {}",
                    timestampPerformActionsForImportIntoAnotherProjectFinish,
                    getTimestampDifference(timestampPerformActionsForImportIntoAnotherProjectStart,
                            timestampPerformActionsForImportIntoAnotherProjectFinish));
        }

        Timestamp timestampconfigAndSaveStorablesStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- configAndSaveStorables - start. Start time: {}", timestampconfigAndSaveStorablesStart);
        configAndSaveStorables(storablesForImport.values(), project);
        Timestamp timestampconfigAndSaveStorablesFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("-- configAndSaveStorables - finish. Finish time: {}, Duration: {}",
                timestampconfigAndSaveStorablesFinish,
                getTimestampDifference(timestampconfigAndSaveStorablesStart, timestampconfigAndSaveStorablesFinish));

        for (Storable storable : storablesForImport.values()) {
            if (storable instanceof System) {
                systemIds.add((BigInteger) storable.getID());
            }
        }

        Timestamp timestampImportObjectsFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("Finish import process for: {}. Finish time: {}, Duration: {}",
                entityType,
                timestampImportObjectsFinish,
                getTimestampDifference(timestampImportObjectsStart, timestampImportObjectsFinish));
    }

    private Storable getRoot(StubProject project, String entityType) throws ClassNotFoundException {
        Storable root = null;
        Pair<BigInteger, String> rootWithClass = defineParametersForParentInCaseOfTreeObjects(project, entityType);
        if (rootWithClass.getKey() != null && rootWithClass.getValue() != null) {
            root = CoreObjectManager.getInstance()
                    .getManager(Class.forName(rootWithClass.getValue()).asSubclass(Storable.class))
                    .getById(rootWithClass.getKey());
        }
        return root;
    }

    private void invalidateBeforeImport(String entityType,
                                        ConcurrentHashMap<Object, Storable> storablesForImport,
                                        StubProject project,
                                        Map<BigInteger, BigInteger> replacementMap) throws ClassNotFoundException {
        Function3<ConcurrentHashMap<Object, ? extends Storable>, StubProject, Map<BigInteger, BigInteger>, Void>
                beforeImportConsumer = beforeImportFunctionMap.get(entityType);
        if (Objects.nonNull(beforeImportConsumer)) {
            beforeImportConsumer.apply(storablesForImport, project, replacementMap);
        }
        Storable root = getRoot(project, entityType);
        ObjectManager<Folder> folderManager = CoreObjectManager.getInstance().getManager(Folder.class);
        storablesForImport.values().stream()
                .filter(storable -> !(storable instanceof ProjectSettings))
                .forEach(storable -> {
                            if (storable instanceof IntegrationConfig) {
                                storable.setParent(project);
                            } else if (Objects.isNull(storable.getParent())) {
                                storable.setParent(root);
                            } else {
                                BigInteger serializedParentId = (BigInteger) storable.getParent().getID();
                                BigInteger replacedParentId = replacementMap.get(serializedParentId);
                                storable.setParent(replacedParentId != null
                                        ? folderManager.getById(replacedParentId)
                                        : folderManager.getById(serializedParentId));
                            }
                        }
                );
    }

    private Void invalidateSystemReferences(ConcurrentHashMap<Object, System> systems) {
        ConcurrentHashMap<Object, Situation> situations = systems.values().parallelStream()
                .flatMap(system -> system.getOperations().stream())
                .flatMap(operation -> operation.getSituations().stream()).collect(
                        Collectors.toMap(Situation::getID, Function.identity(), (existing, replacement) -> existing,
                                ConcurrentHashMap::new));
        List<IntegrationStep> integrationSteps = situations.values().parallelStream()
                .flatMap(situation -> situation.getSteps().stream())
                .filter(step -> step instanceof IntegrationStep && Objects.nonNull(
                        ((IntegrationStep) step).getReceiver())).map(IntegrationStep.class::cast)
                .collect(Collectors.toList());
        //invalidate reference to System as receiver
        integrationSteps.parallelStream().forEach(step -> step.setReceiver(systems.get(step.getReceiver().getID())));

        ConcurrentHashMap<SituationEventTrigger, Object> situationEventTriggersRefersToSituation = situations.values()
                .stream().flatMap(situation -> situation.getSituationEventTriggers().stream())
                .filter(situationEventTrigger -> Objects.nonNull(situationEventTrigger.getSituation())).collect(
                        Collectors.toMap(trigger -> trigger, trigger -> trigger.getSituation().getID(),
                                (existing, replacement) -> existing, ConcurrentHashMap::new));
        //invalidate reference to Situation from SituationEventTrigger
        situationEventTriggersRefersToSituation.entrySet().stream()
                .forEach(entry -> entry.getKey().setSituation(situations.get(entry.getValue())));

        return null;
    }

    private Void invalidateServerReferences(ConcurrentHashMap<Object, ServerHB> servers,
                                            Map<BigInteger, BigInteger> replacementMap) {
        ObjectManager<System> systemObjectManager = CoreObjectManager.getInstance().getManager(System.class);
        ObjectManager<TransportConfiguration> transportObjectManager =
                CoreObjectManager.getInstance().getManager(TransportConfiguration.class);
        servers.values().forEach(server -> {
            server.getOutbounds().forEach(outbound -> {
                BigInteger deserializedSystemId = (BigInteger) outbound.getSystem().getID();
                BigInteger replacedSystemId = replacementMap.get(deserializedSystemId);
                outbound.setSystem(replacedSystemId != null ? systemObjectManager.getById(replacedSystemId) :
                        systemObjectManager.getById(deserializedSystemId));
            });
            server.getInbounds().forEach(inbound -> {
                BigInteger deserializedReferencedConfigurationId =
                        (BigInteger) inbound.getReferencedConfiguration().getID();
                BigInteger replacedReferencedConfigurationId =
                        replacementMap.get(deserializedReferencedConfigurationId);
                inbound.setReferencedConfiguration(replacedReferencedConfigurationId != null
                        ? transportObjectManager.getById(replacedReferencedConfigurationId)
                        : transportObjectManager.getById(deserializedReferencedConfigurationId));
            });
        });
        return null;
    }

    private Void invalidateEnvironmentReferences(ConcurrentHashMap<Object, Environment> environments, Map<BigInteger,
            BigInteger> replacementMap) {
        environments.values().forEach(environment -> {
            environment.fillOutbound(getEnvironmentsSystemServerPairs(environment.getOutbound(), replacementMap));
            environment.fillInbound(getEnvironmentsSystemServerPairs(environment.getInbound(), replacementMap));
        });

        return null;
    }

    private Map<System, Server> getEnvironmentsSystemServerPairs(Map<System, Server> pairs, Map<BigInteger,
            BigInteger> replacementMap) {
        Map<System, Server> pairsWithReplacedIds = Maps.newHashMap();
        ObjectManager<System> systemManager = CoreObjectManager.getInstance().getManager(System.class);
        ObjectManager<Server> serverManager = CoreObjectManager.getInstance().getManager(Server.class);
        pairs.forEach((key, value) -> {
                    BigInteger replacedSystemId = replacementMap.get((BigInteger) key.getID());
                    BigInteger replacedServerId = replacementMap.get((BigInteger) value.getID());
                    pairsWithReplacedIds.put(
                            replacedSystemId != null ? systemManager.getById(replacedSystemId) :
                                    systemManager.getById(key.getID()),
                            replacedServerId != null ? serverManager.getById(replacedServerId) :
                                    serverManager.getById(value.getID())
                    );
                }
        );
        return pairsWithReplacedIds;
    }

    private Void invalidateCallchainReferences(ConcurrentHashMap<Object, CallChain> callChains, Map<BigInteger,
            BigInteger> replacementMap) {
        ConcurrentHashMap<Object, Step> embeddedSteps = callChains.values().parallelStream()
                .flatMap(entry -> entry.getSteps().stream()).filter(step -> step instanceof EmbeddedStep).collect(
                        Collectors.toMap(Identified::getID, Function.identity(), (existing, replacement) -> existing,
                                ConcurrentHashMap::new));
        embeddedSteps.forEach((key, value) -> {
            EmbeddedStep embeddedStep = (EmbeddedStep) value;
            if (embeddedStep.getChain() != null) {
                embeddedStep.setChain(callChains.get(embeddedStep.getChain().getID()));
            }
        });
        ObjectManager<Situation> situationManager = CoreObjectManager.getInstance().getManager(Situation.class);
        callChains.values().stream()
                .flatMap(entry -> entry.getSteps().stream()).filter(step -> step instanceof SituationStep)
                .map(SituationStep.class::cast).collect(Collectors.toList()).stream().forEach(step -> {
                    step.setSituation(getSituationForSituationStep(step.getSituation(), replacementMap,
                            situationManager));
                    step.setEndSituations(getSituationsForSituationStep(step.getEndSituations(), replacementMap,
                            situationManager));
                    step.setExceptionalSituations(getSituationsForSituationStep(step.getExceptionalSituations(),
                            replacementMap, situationManager));
                });
        return null;
    }

    private Set<Situation> getSituationsForSituationStep(Set<Situation> deserializedSituations, Map<BigInteger,
            BigInteger> replacementMap, ObjectManager<Situation> situationManager) {
        Set<Situation> situations = Sets.newHashSet();
        deserializedSituations.stream().forEach(situation -> {
            situations.add(getSituationForSituationStep(situation, replacementMap, situationManager));
        });
        return situations;
    }

    private Situation getSituationForSituationStep(Situation deserializedSituation,
                                                   Map<BigInteger, BigInteger> replacementMap,
                                                   ObjectManager<Situation> situationManager) {
        if (deserializedSituation != null) {
            BigInteger deserializedSituationId = replacementMap.get((BigInteger) deserializedSituation.getID());
            return deserializedSituationId != null
                    ? situationManager.getById(deserializedSituationId)
                    : situationManager.getById(deserializedSituation.getID());
        }
        return null;
    }

    private List<Pair<Path, String>> prepareListOfFoldersForImport(Path workDir) {
        List<Pair<Path, String>> filesForImport = new LinkedList<>();
        Arrays.stream(Objects.requireNonNull(workDir.toFile().listFiles())).filter(File::isDirectory).sorted().forEach(
                folder -> Arrays.stream(folder.listFiles()).filter(File::isDirectory).forEach(
                        nestedFolder -> filesForImport.add(new Pair<>(folder.toPath(), nestedFolder.getName()))));
        return filesForImport;
    }

    public Pair<BigInteger, String> defineParametersForParentInCaseOfTreeObjects(StubProject project,
                                                                                 String entityType) {
        Pair<BigInteger, String> result;
        switch (entityType) {
            case EiConstants.SYSTEM:
            case EiConstants.SYSTEM_FOLDER:
                result = new Pair<>((BigInteger) project.getSystems().getID(), SystemFolder.class.getName());
                break;
            case EiConstants.CALLCHAIN:
            case EiConstants.CALLCHAIN_FOLDER:
                result = new Pair<>((BigInteger) project.getCallchains().getID(), ChainFolder.class.getName());
                break;
            case EiConstants.ENVIRONMENT:
            case EiConstants.ENVIRONMENT_FOLDER:
                result = new Pair<>((BigInteger) project.getEnvironments().getID(), EnvFolder.class.getName());
                break;
            case EiConstants.SERVER:
            case EiConstants.SERVER_FOLDER:
                result = new Pair<>((BigInteger) project.getServers().getID(), ServerFolder.class.getName());
                break;
            default:
                result = new Pair<>(null, null);
        }
        return result;
    }

    public void performActionsForImportIntoAnotherProject(Collection<Storable> storables,
                                                          Map<BigInteger, BigInteger> replacementMap,
                                                          BigInteger projectId,
                                                          UUID projectUuid,
                                                          boolean needToUpdateProjectId,
                                                          boolean needToGenerateNewId) {
        storables.stream().filter(storable -> !(storable instanceof ProjectSettings))
                .forEach(storable -> storable.performActionsForImportIntoAnotherProject(
                                replacementMap, projectId, projectUuid, needToUpdateProjectId, needToGenerateNewId
                        )
                );
    }

    public void configAndSaveStorables(Collection<Storable> storables, StubProject project) {
        storables.forEach(storable -> configAndSaveStorable(null, storable, project));
    }

    public void configAndSaveStorable(Storable newParent, Storable storable, StubProject project) {
        if (storable == null) {
            log.debug("Storable is null");
            return;
        }
        Timestamp timestampConfigAndSaveStorableStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info("Start configAndSaveStorable for entity (ID: {}, Type: {}, Name: {}). Start time: {}",
                storable.getID(), storable.getClass().getSimpleName(), storable.getName(),
                timestampConfigAndSaveStorableStart);

        if (storable instanceof ProjectSettings) {
            log.info("Importing project settings...");
            Map<String, String> projectSettings = ((ProjectSettings) storable).getConfiguration();
            project.setStorableProp(projectSettings);
            project.store();
            projectSettingsService.fillCache(project, projectSettings);
            log.info("Project settings are imported successfully.");
            return;
        }
        checkAndRemoveOldIntegrationConfig(storable, project);
        if (storable instanceof System) {
            processTemplatesTransportConfigurations((System) storable);
        }
        log.info("Storable name: {} id: {} importing...", storable.getName(), storable.getID());
        Timestamp timestampReplicateStart = new Timestamp(java.lang.System.currentTimeMillis());
        log.info(" Start replication for entity (ID: {}, Type: {}, Name: {}). Start time: {}",
                storable.getID(), storable.getClass().getSimpleName(), storable.getName(), timestampReplicateStart);
        itfReplicationService.replicateStorableWithHistory(storable);
        Timestamp timestampReplicateFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info(" Finish replication for entity (ID: {}, Type: {}, Name: {}). Finish time: {}, Duration: {}",
                storable.getID(), storable.getClass().getSimpleName(), storable.getName(),
                timestampReplicateFinish,
                getTimestampDifference(timestampReplicateStart, timestampReplicateFinish));

        Timestamp timestampConfigAndSaveStorableFinish = new Timestamp(java.lang.System.currentTimeMillis());
        log.info(" Finish configAndSaveStorable for entity (ID: {}, Type: {}, Name: {}). Finish time: {}, Duration: {}",
                storable.getID(), storable.getClass().getSimpleName(), storable.getName(),
                timestampConfigAndSaveStorableFinish,
                getTimestampDifference(timestampConfigAndSaveStorableStart, timestampConfigAndSaveStorableFinish));
        log.info("Storable name: {} id: {} is imported.", storable.getName(), storable.getID());
    }

    private void processTemplatesTransportConfigurations(System system) {
        log.info("System: {} - Templates Transport Configurations processing is started...", system);
        system.getSystemTemplates().stream()
                .forEach(systemTemplate -> processTemplateTransportConfiguration(systemTemplate, true));
        log.info("System: {} - System templates transport configurations are processed", system);
        system.getOperations().stream()
                .forEach(operation -> operation.getOperationTemplates().stream()
                        .forEach(operationTemplate -> processTemplateTransportConfiguration(operationTemplate, false)));
        log.info("System: {} - Operation templates transport configurations are processed", system);
    }

    private void processTemplateTransportConfiguration(Template<? extends TemplateProvider> template,
                                                       boolean isSystemTemplate) {
        Collection<OutboundTemplateTransportConfiguration> transportProperties = template.getTransportProperties();
        if (transportProperties.isEmpty()) {
            return;
        }
        CoreObjectManagerService coreObjectManagerService = CoreObjectManager.getInstance();
        if (isSystemTemplate) {
            ObjectManager<SystemTemplate> manager = coreObjectManagerService.getManager(SystemTemplate.class);
            getAndCheckTemplateTransportConfiguration(manager.getById(template.getID()), transportProperties);
        } else {
            ObjectManager<OperationTemplate> manager = coreObjectManagerService.getManager(OperationTemplate.class);
            getAndCheckTemplateTransportConfiguration(manager.getById(template.getID()), transportProperties);
        }
    }

    private void getAndCheckTemplateTransportConfiguration(
            Template<? extends TemplateProvider> templateFromDB,
            Collection<OutboundTemplateTransportConfiguration> transportProperties) {
        if (Objects.nonNull(templateFromDB) && !templateFromDB.getTransportProperties().isEmpty()) {
            for (OutboundTemplateTransportConfiguration transportConfiguration : transportProperties) {
                OutboundTemplateTransportConfiguration transportConfigurationFromDB
                        = templateFromDB.getTransportProperties(transportConfiguration.getTypeName());
                if (Objects.nonNull(transportConfigurationFromDB)) {
                    transportConfiguration.setID(transportConfigurationFromDB.getID());
                }
            }
        }
    }

    private void checkAndRemoveOldIntegrationConfig(Storable storable, StubProject project) {
        if (storable instanceof IntegrationConfig) {
            IntegrationConfig currentIntegrationConfig = (IntegrationConfig) storable;
            log.info("Start checking if integration config '{}' exists...", currentIntegrationConfig.getTypeName());
            Set<IntegrationConfig> integrationConfs = project.getIntegrationConfs();
            Iterator<IntegrationConfig> iter = integrationConfs.iterator();

            while (iter.hasNext()) {
                IntegrationConfig integrationConfig = iter.next();
                if (currentIntegrationConfig.getTypeName().equals(integrationConfig.getTypeName())
                        && !currentIntegrationConfig.getID().equals(integrationConfig.getID())) {
                    integrationConfs.remove(integrationConfig);
                    integrationConfig.remove();
                    project.store();
                    log.info("Old integration config '{}' is removed.", currentIntegrationConfig.getTypeName());
                    break;
                }
            }
            log.info("Finish checking if integration config '{}' exists.", currentIntegrationConfig.getTypeName());
        }
    }

    private BigInteger getProjectIdByUuid(UUID projectUuid) {
        SearchManager specialManager = CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class);
        BigInteger projectId = specialManager.getEntityInternalIdByUuid(projectUuid);
        if (projectId == null) {
            log.info("Project with uuid = {} doesn't exist. Wait the project creation...", projectUuid);
            int attempt = 0;
            while (attempt < EI_WAITING_PROJECT_CREATION_ATTEMPT_COUNT) {
                attempt++;
                try {
                    Thread.sleep(EI_WAITING_PROJECT_CREATION_TIMEOUT);
                    projectId = specialManager.getEntityInternalIdByUuid(projectUuid);
                    if (projectId != null) {
                        log.info("Project with id = {}, uuid = {} is created, attempt# {}. Continue import...",
                                projectId, projectUuid, attempt);
                        break;
                    } else {
                        log.info("Project with uuid = {} isn't created yet, attempt# {}. Continue waiting...",
                                projectUuid, attempt);
                    }
                } catch (InterruptedException exc) {
                    throw new RuntimeException(String.format("Failed while waiting the creation of the project "
                            + "(uuid=%s) during the import!", projectUuid), exc);
                }
            }
            if (projectId == null) {
                throw new IllegalArgumentException(
                        String.format("Project with uuid=%s wasn't created, import process can't be started!",
                                projectUuid));
            }
        }
        return projectId;
    }

    private boolean isInternalProjectAnother(BigInteger targetProjectId, Collection<Storable> storables) {
        Storable storableForImport = storables.stream().filter(storable -> !(storable instanceof ProjectSettings))
                .findFirst().orElse(null);
        if (storableForImport != null) {
            return !targetProjectId.equals(storableForImport.getProjectId());
        }
        return true;
    }

    private boolean isCatalogueProjectAnotherOrNew(ExportImportData exportImportData) {
        return exportImportData.isInterProjectImport() || exportImportData.isImportFirstTime();
    }
}
