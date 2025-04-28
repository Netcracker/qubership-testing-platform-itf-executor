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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;
import org.qubership.atp.ei.node.ExportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.ServerHB;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.project.ProjectSettings;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItfExportExecutor implements ExportExecutor {

    private final ObjectSaverToDiskService objectSaverToDiskService;
    private final ExtraObjectsCollector extraObjectsCollector;
    private final LinkedList<String> typesForExportInCorrectImportOrder = new LinkedList<>(Arrays.asList(
            ProjectSettings.class.getName(),
            IntegrationConfig.class.getName(),
            System.class.getName(),
            ServerHB.class.getName(),
            CallChain.class.getName(),
            Environment.class.getName()
    ));
    @Value("${spring.application.name}")
    private String implementationName;

    @Override
    @Transactional(readOnly = true)
    public void exportToFolder(ExportImportData exportImportData, Path path) {
        log.info("Export started...");
        Map<String, Set<Object>> checkedObjectsMap = getCheckedObjectsMap();
        ConcurrentHashMap<Object, Storable> storablesForExport = new ConcurrentHashMap<>();
        extractFoldersFromExportImportData(exportImportData).stream().forEach(exportObject ->
                addObjectToStorablesForExport(exportObject, storablesForExport, checkedObjectsMap, true));

        extractChildren(storablesForExport.values()).stream().forEach(storableObject ->
                addObjectToStorablesForExport(storableObject, storablesForExport, checkedObjectsMap, true));

        extractChildrenStorablesFromExportImportData(exportImportData).stream().forEach(exportObject ->
                addObjectToStorablesForExport(exportObject, storablesForExport, checkedObjectsMap, false));
        ConcurrentHashMap<Object, Pair<Folder<?>, Integer>> parentsMap = new ConcurrentHashMap<>();
        storablesForExport.values().stream()
                .forEach(storable -> extraObjectsCollector.collectParents(storable, parentsMap));
        parentsMap.values().forEach(parentObject ->
                saveToDisk(parentObject.getKey(), parentPath(path, parentObject.getValue()))
        );
        applyTcFilterForServer(checkedObjectsMap, storablesForExport);
        storablesForExport.values().forEach(storable -> saveToDisk(storable, otherObjectsPath(path, storable)));
        cleanCheckedObjectsMap(checkedObjectsMap);
        log.info("Export completed.");
    }

    private List<Storable> extractChildren(Collection<Storable> storables) {
        List<Storable> childrenList = new ArrayList<>();
        for (Storable storable : storables) {
            if (storable instanceof CallChain || storable instanceof System || storable instanceof Environment) {
                childrenList.add(storable);
            }
        }
        return childrenList;
    }

    /**
     * This method filters Transport Configurations (Outbound and Inbound) for server objects.
     * (discards unnecessary server object configurations)
     * We don't need outbound transport configurations in the export file that contains references
     * to systems (OTC has a SystemId) and those systems are not exported in the current export session.
     * If we don't apply this filter we will get many unnecessary systems in export file while export environment(s).
     * The same way, we don't need inbound transport configurations in the export file that contains references
     * to transport under systems (ITC has a .getReferencedConfiguration() link to transport under a System)
     * and those systems are not exported in the current export session.
     * If we don't apply this filter we will get many unnecessary systems in export file while export environment(s).
     *
     * @param checkedObjectsMap        - map that contains object ids by entity type (System, CallChain, ServerHB...),
     *                                 that were collected by ITF export process.
     * @param storableObjectsForExport - storable objects for export.
     */
    private void applyTcFilterForServer(Map<String, Set<Object>> checkedObjectsMap,
                                        ConcurrentHashMap<Object, Storable> storableObjectsForExport) {
        log.info("Applying OutboundTransportConfiguration filter for Server objects is started...");
        Set<Object> systemIdsForExport = checkedObjectsMap.get(System.class.getSimpleName());
        Object[] serverIdsForExport = checkedObjectsMap.get(ServerHB.class.getSimpleName()).toArray();
        applyTcFilter(storableObjectsForExport, serverIdsForExport, systemIdsForExport);
        log.info("Applying OutboundTransportConfiguration filter for Server objects is completed.");
    }

    private void applyTcFilter(ConcurrentHashMap<Object, Storable> storableObjectsForExport,
                               Object[] serverIdsForExport, Set<Object> systemIdsForExport) {
        for (Object serverId : serverIdsForExport) {
            ServerHB exportedServer = (ServerHB) storableObjectsForExport.get(serverId);
            Collection<OutboundTransportConfiguration> outbounds = exportedServer.getOutbounds();
            if (!(Objects.isNull(outbounds) || outbounds.isEmpty())) {
                Set<OutboundTransportConfiguration> filteredOutbounds =
                        collectOtcByExportedSystems(outbounds, systemIdsForExport);
                exportedServer.fillOutbounds(filteredOutbounds);
            }
            Collection<InboundTransportConfiguration> inbounds = exportedServer.getInbounds();
            if (Objects.isNull(inbounds) || inbounds.isEmpty()) {
                continue;
            }
            Set<InboundTransportConfiguration> filteredInbounds =
                    collectItcByExportedSystems(inbounds, systemIdsForExport);
            exportedServer.fillInbounds(filteredInbounds);
        }
    }

    private Set<OutboundTransportConfiguration> collectOtcByExportedSystems(
            Collection<OutboundTransportConfiguration> outbounds,
            Set<Object> systemIdsForExport) {
        Set<OutboundTransportConfiguration> filteredOutbounds = new HashSet<>();
        for (OutboundTransportConfiguration otc : outbounds) {
            System systemUnderOtc = otc.getSystem();
            if (Objects.isNull(systemUnderOtc)) {
                continue;
            }
            Object systemIdUnderOtc = systemUnderOtc.getID();
            if (systemIdsForExport.contains(systemIdUnderOtc)) {
                filteredOutbounds.add(otc);
            }
        }
        return filteredOutbounds;
    }

    private Set<InboundTransportConfiguration> collectItcByExportedSystems(
            Collection<InboundTransportConfiguration> inbounds,
            Set<Object> systemIdsForExport) {
        Set<InboundTransportConfiguration> filteredInbounds = new HashSet<>();
        for (InboundTransportConfiguration itc : inbounds) {
            System systemUnderItc = itc.getReferencedConfiguration().getParent();
            if (Objects.isNull(systemUnderItc)) {
                continue;
            }
            Object systemIdUnderItc = systemUnderItc.getID();
            if (systemIdsForExport.contains(systemIdUnderItc)) {
                filteredInbounds.add(itc);
            }
        }
        return filteredInbounds;
    }

    private Path parentPath(Path initialPath, int order) {
        return initialPath.resolve(
                Paths.get(StringUtils.leftPad(String.valueOf(order), 4, '0') + "_Parent"));
    }

    private Path otherObjectsPath(Path initialPath, Storable storable) {
        return initialPath.resolve(
                Paths.get("1"
                                + StringUtils.leftPad(String.valueOf(
                                typesForExportInCorrectImportOrder.indexOf(storable.getClass().getName()) + 1
                        ), 3, '0')
                                + "_"
                                + storable.getClass().getSimpleName()
                ));
    }

    private void addObjectToStorablesForExport(Pair<String, String> objectForExportInfo,
                                               ConcurrentHashMap<Object, Storable> storablesForExport,
                                               Map<String, Set<Object>> checkedObjectsMap,
                                               boolean onlyExtra) {
        TxExecutor.executeVoid(() -> {
            try {
                Storable storable = CoreObjectManager.getInstance()
                        .getManager(Class.forName(objectForExportInfo.getKey()).asSubclass(Storable.class))
                        .getById(objectForExportInfo.getValue());
                if (Objects.nonNull(storable)) {
                    storable = (Storable) Hibernate.unproxy(storable);
                    log.info("Storable name: {}, class {}, id: {}",
                            storable.getName(), storable.getClass(), storable.getID());
                    if (!onlyExtra) {
                        storablesForExport.put(storable.getID(), storable);
                    }
                    storablesForExport.putAll(extraObjectsCollector.collect(storable, checkedObjectsMap));
                } else {
                    log.error("Object is excluded from export, because was not found. Object of class '{}', id={}",
                            objectForExportInfo.getKey(), objectForExportInfo.getValue());
                }
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }, TxExecutor.readOnlyTransaction());
    }

    private void addObjectToStorablesForExport(Storable storable,
                                               ConcurrentHashMap<Object, Storable> storablesForExport,
                                               Map<String, Set<Object>> checkedObjectsMap,
                                               boolean onlyExtra) {
        TxExecutor.executeVoid(() -> {
            log.info("[#2] Storable name: {}, class {}, id: {}",
                    storable.getName(), storable.getClass(), storable.getID());
            if (!onlyExtra) {
                storablesForExport.put(storable.getID(), storable);
            }
            storablesForExport.putAll(extraObjectsCollector.collect(storable, checkedObjectsMap));
        }, TxExecutor.readOnlyTransaction());
    }

    private void saveToDisk(Storable exportObject, Path path) {
        objectSaverToDiskService.exportAtpEntity(
                UUID.nameUUIDFromBytes(exportObject.getID().toString().getBytes(StandardCharsets.UTF_8)),
                exportObject, path);
    }

    private List<Pair<String, String>> extractChildrenStorablesFromExportImportData(ExportImportData exportImportData) {
        List<Pair<String, String>> processed = new ArrayList<>();
        Map<String, Set<String>> exportScopeEntities = exportImportData.getExportScope().getEntities();
        processed.addAll(transformSetToListOfPairs(CallChain.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_CALL_CHAINS.getValue(),
                        new HashSet<>())));
        processed.addAll(transformSetToListOfPairs(System.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_SYSTEMS.getValue(), new HashSet<>())));
        processed.addAll(transformSetToListOfPairs(Environment.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_ENVIRONMENTS.getValue(),
                        new HashSet<>())));
        processed.addAll(transformSetToListOfPairs(IntegrationConfig.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_INTEGRATION_CONFIGS.getValue(),
                        new HashSet<>())));
        processed.addAll(transformSetToListOfPairs(StubProject.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_PROJECT_SETTINGS.getValue(),
                        new HashSet<>())));
        return processed;
    }

    private List<Pair<String, String>> extractFoldersFromExportImportData(ExportImportData exportImportData) {
        List<Pair<String, String>> processed = new ArrayList<>();
        Map<String, Set<String>> exportScopeEntities = exportImportData.getExportScope().getEntities();
        processed.addAll(transformSetToListOfPairs(ChainFolder.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_CALL_CHAIN_FOLDERS.getValue(),
                        new HashSet<>())));
        processed.addAll(transformSetToListOfPairs(SystemFolder.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_SYSTEM_FOLDERS.getValue(),
                        new HashSet<>())));
        processed.addAll(transformSetToListOfPairs(EnvFolder.class.getName(),
                exportScopeEntities.getOrDefault(ServiceScopeEntities.ENTITY_ITF_ENVIRONMENT_FOLDERS.getValue(),
                        new HashSet<>())));
        return processed;
    }

    private List<Pair<String, String>> transformSetToListOfPairs(String className, Set<String> objects) {
        List<Pair<String, String>> collected = new ArrayList<>();
        objects.forEach(s -> collected.add(new ImmutablePair<>(className, s)));
        return collected;
    }

    @Override
    public String getExportImplementationName() {
        return implementationName;
    }

    private Map<String, Set<Object>> getCheckedObjectsMap() {
        Map<String, Set<Object>> checkedObjectsMap = new HashMap<>();
        checkedObjectsMap.put(Template.class.getSimpleName(), new HashSet<>());
        checkedObjectsMap.put(System.class.getSimpleName(), new HashSet<>());
        checkedObjectsMap.put(CallChain.class.getSimpleName(), new HashSet<>());
        checkedObjectsMap.put(ServerHB.class.getSimpleName(), new HashSet<>());
        return checkedObjectsMap;
    }

    private void cleanCheckedObjectsMap(Map<String, Set<Object>> checkedObjectsMap) {
        checkedObjectsMap.forEach((key, value) -> value.clear());
    }
}
