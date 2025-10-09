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

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.server.ServerHB;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.project.ProjectSettings;
import org.qubership.automation.itf.core.util.descriptor.Extractor;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.PropertyProvider;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.executor.transports.classloader.TransportClassLoader;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtraObjectsCollector {

    private final TemplateExtractor templateExtractor;
    private final ProjectSettingsService projectSettingsService;

    private final Map<String, BiFunction<Storable, Map<String, Set<Object>>, Map<Object, Storable>>> functionsMap
            = new HashMap<>();

    {
        functionsMap.put(CallChain.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForCallChain((CallChain) obj, checkedObjectsMap));
        functionsMap.put(System.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForSystem((System) obj, checkedObjectsMap));
        functionsMap.put(Environment.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForEnvironment((Environment) obj, checkedObjectsMap));
        functionsMap.put(ChainFolder.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForFolder((ChainFolder) obj));
        functionsMap.put(EnvFolder.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForFolder((EnvFolder) obj));
        functionsMap.put(SystemFolder.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForFolder((SystemFolder) obj));
        functionsMap.put(ServerHB.class.getName(),
                (obj, checkedObjectsMap) -> getExtraObjectsForServer((ServerHB) obj, checkedObjectsMap));
        functionsMap.put(StubProject.class.getName(),
                (obj, checkedObjectsMap) -> getProjectSettingsFromProject((StubProject) obj));
        functionsMap.put(IntegrationConfig.class.getName(),
                (obj, checkedObjectsMap) -> getIntegrationConfigsForProject((IntegrationConfig) obj));
    }

    /**
     * Collect extra objects for export.
     *
     * @param storable processed object
     * @return set extra objects (storable)
     */
    public Map<Object, Storable> collect(Storable storable, Map<String, Set<Object>> checkedObjectsMap) {
        String storableName = storable.getClass().getSimpleName();
        log.info("Collecting of extra objects for type {} is started...", storableName);
        BiFunction<Storable, Map<String, Set<Object>>, Map<Object, Storable>> function
                = functionsMap.get(storable.getClass().getName());
        Objects.requireNonNull(function,
                String.format("Function to collect extra objects for object [%s] was not found.", storableName));
        Map<Object, Storable> extraObjects = function.apply(storable, checkedObjectsMap);
        log.info("Collecting of extra objects for type {} is completed.", storableName);
        return extraObjects;
    }

    private Map<Object, Storable> getExtraObjectsForCallChain(CallChain callChain,
                                                              Map<String, Set<Object>> checkedObjectsMap) {
        Map<Object, Storable> extraObjects = new HashMap<>();
        if (!checkedObjectsMap.get(CallChain.class.getSimpleName()).contains(callChain.getID())) {
            log.info("List of checked callchain ids doesn't contain id {}. Callchain is checked...", callChain.getID());
            checkedObjectsMap.get(CallChain.class.getSimpleName()).add(callChain.getID());
            extraObjects.put(callChain.getID(), callChain);
            for (Step step : callChain.getSteps()) {
                if (step != null) {
                    if (step instanceof SituationStep) {
                        if (((SituationStep) step).getSituation() != null) {
                            extraObjects.putAll(getExtraObjectsForSystem(
                                    ((SituationStep) step).getSituation().getParent().getParent(), checkedObjectsMap));
                            collectSystemsFromSituations(extraObjects,
                                    ((SituationStep) step).getEndSituations(), checkedObjectsMap);
                            collectSystemsFromSituations(extraObjects,
                                    ((SituationStep) step).getExceptionalSituations(), checkedObjectsMap);
                        }
                    } else if (step instanceof EmbeddedStep) {
                        CallChain nested = ((EmbeddedStep) step).getChain();
                        if (nested != null) {
                            extraObjects.putAll(getExtraObjectsForCallChain(nested, checkedObjectsMap));
                        }
                    }
                }
            }
        } else {
            log.info("List of checked callchains ids already contains id {}. Callchain is skipped.", callChain.getID());
        }
        return extraObjects;
    }

    private void collectSystemsFromSituations(Map<Object, Storable> extraObjects,
                                              Set<Situation> situations,
                                              Map<String, Set<Object>> checkedObjectsMap) {
        for (Situation endSituation : situations) {
            extraObjects.putAll(getExtraObjectsForSystem(endSituation.getParent().getParent(), checkedObjectsMap));
        }
    }

    private Map<Object, Storable> getExtraObjectsForSystem(System system, Map<String, Set<Object>> checkedObjectsMap) {
        Map<Object, Storable> extraObjects = Maps.newHashMap();
        if (!checkedObjectsMap.get(System.class.getSimpleName()).contains(system.getID())) {
            log.info("List of checked systems ids doesn't contain id {}. System is checked...", system.getID());
            checkedObjectsMap.get(System.class.getSimpleName()).add(system.getID());
            extraObjects.put(system.getID(), system);
            collectSystemsThroughSituationEventTriggersAndReceivers(system, extraObjects, checkedObjectsMap);
        } else {
            log.info("List of checked systems ids already contains id {}. System is skipped.", system.getID());
        }
        return extraObjects;
    }

    private Map<Object, Storable> getExtraObjectsForFolder(Folder<?> folder) {
        Map<Object, Storable> extraObjects = Maps.newHashMap();
        folder.getObjects().forEach(storable -> extraObjects.put(storable.getID(), storable));
        folder.getSubFolders().forEach(subFolder -> extraObjects.putAll(getExtraObjectsForFolder(subFolder)));
        return extraObjects;
    }

    private Map<Object, Storable> getProjectSettingsFromProject(StubProject project) {
        Map<Object, Storable> extraObjects = Maps.newHashMap();
        Object projectId = project.getID();
        ProjectSettings settings = new ProjectSettings(projectSettingsService.getAll(projectId));
        settings.setID(projectId);
        extraObjects.put(projectId, settings);
        return extraObjects;
    }

    private Map<Object, Storable> getIntegrationConfigsForProject(IntegrationConfig integrationConfig) {
        Map<Object, Storable> extraObjects = Maps.newHashMap();
        extraObjects.put(integrationConfig.getID(), integrationConfig);
        return extraObjects;
    }

    private void collectSystemsThroughSituationEventTriggersAndReceivers(System system,
                                                                         Map<Object, Storable> extraObjects,
                                                                         Map<String, Set<Object>> checkedObjectsMap) {
        if (Objects.isNull(system.getParent())) {
            log.error("Extra objects search is skipped for System '{}' [id={}], because its parent is null",
                    system.getName(), system.getID());
            return;
        }
        BigInteger projectId = system.getParent().getProjectId();
        collectSystemsFromLoadPartInTemplates(system, extraObjects, checkedObjectsMap, projectId);
        collectSystemsFromDiameterTransportFields(system, extraObjects, checkedObjectsMap);
        for (Operation operation : system.getOperations()) {
            collectSystemsFromLoadPartInTemplates(operation, extraObjects, checkedObjectsMap, projectId);
            for (Situation situation : operation.getSituations()) {
                if (Objects.nonNull(situation.getIntegrationStep())
                        && situation.getIntegrationStep().getReceiver() != null) {
                    extraObjects.putAll(getExtraObjectsForSystem(situation.getIntegrationStep().getReceiver(),
                            checkedObjectsMap));
                }
                for (SituationEventTrigger situationEventTrigger : situation.getSituationEventTriggers()) {
                    if (Objects.nonNull(situationEventTrigger.getSituation())) {
                        extraObjects.putAll(getExtraObjectsForSystem(
                                situationEventTrigger.getSituation().getParent().getParent(), checkedObjectsMap));
                    }
                }
            }
        }
    }

    private Map<Object, Storable> getExtraObjectsForEnvironment(Environment environment,
                                                                Map<String, Set<Object>> checkedObjectsMap) {
        Map<Object, Storable> extraObjects = new HashMap<>();
        collectObjectsFromMapConfiguration(extraObjects, environment.getInbound(), checkedObjectsMap);
        collectObjectsFromMapConfiguration(extraObjects, environment.getOutbound(), checkedObjectsMap);
        collectSystemsFromExtraSettings(environment, extraObjects, checkedObjectsMap);
        return extraObjects;
    }

    private void collectObjectsFromMapConfiguration(Map<Object, Storable> extraObjects,
                                                    Map<System, Server> mapConfiguration,
                                                    Map<String, Set<Object>> checkedObjectsMap) {
        for (Map.Entry<System, Server> map : mapConfiguration.entrySet()) {
            extraObjects.putAll(getExtraObjectsForSystem(map.getKey(), checkedObjectsMap));
            extraObjects.putAll(getExtraObjectsForServer(map.getValue(), checkedObjectsMap));
        }
    }

    private Map<Object, Storable> getExtraObjectsForServer(Server server, Map<String, Set<Object>> checkedObjectsMap) {
        Map<Object, Storable> extraObjects = new HashMap<>();
        if (!checkedObjectsMap.get(ServerHB.class.getSimpleName()).contains(server.getID())) {
            log.info("List of checked servers ids doesn't contain id {}. Server is checked...", server.getID());
            checkedObjectsMap.get(ServerHB.class.getSimpleName()).add(server.getID());
            extraObjects.put(server.getID(), server);
            // Next 2 rows (collectObjectsFromInboundTransportConfig, collectObjectsFromOutboundTransportConfig)
            // are commented; to be deleted soon.
            /*
            collectObjectsFromInboundTransportConfig(extraObjects, server.getInbounds(), checkedObjectsMap);
            collectObjectsFromOutboundTransportConfig(extraObjects, server.getOutbounds(), checkedObjectsMap);
            */
        } else {
            log.info("List of checked servers ids already contains id {}. Server is skipped.", server.getID());
        }
        return extraObjects;
    }

    private void collectObjectsFromInboundTransportConfig(Map<Object, Storable> extraObjects,
                                                          Collection<InboundTransportConfiguration> inboundTransportConfig,
                                                          Map<String, Set<Object>> checkedObjectsMap) {
        for (InboundTransportConfiguration inboundTransport : inboundTransportConfig) {
            extraObjects.putAll(getExtraObjectsForSystem(inboundTransport.getReferencedConfiguration().getParent(),
                    checkedObjectsMap));
        }
    }

    private void collectObjectsFromOutboundTransportConfig(Map<Object, Storable> extraObjects,
                                                           Collection<OutboundTransportConfiguration> outboundTransportConfig,
                                                           Map<String, Set<Object>> checkedObjectsMap) {
        for (OutboundTransportConfiguration outboundTransport : outboundTransportConfig) {
            extraObjects.putAll(getExtraObjectsForSystem(outboundTransport.getSystem(), checkedObjectsMap));
        }
    }

    private void collectSystemsFromExtraSettings(Environment environment, Map<Object, Storable> extraObjects,
                                                 Map<String, Set<Object>> checkedObjectsMap) {
        Map<Object, System> extraSystems = environment.getReportCollectors().stream()
                .map(configuration -> configuration.getConfiguration().get("system"))
                .filter(id -> !id.isEmpty() && !checkedObjectsMap.get(System.class.getSimpleName()).contains(id))
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> CoreObjectManager.getInstance().getManager(System.class).getById(id),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
        extraSystems.values().forEach(system ->
                extraObjects.putAll(getExtraObjectsForSystem(system, checkedObjectsMap)));
        extraObjects.putAll(extraSystems);
    }

    /**
     * Collect parents of storable to parent storage.
     *
     * @param source    storable object.
     * @param collectTo storage.
     */
    public void collectParents(Storable source, ConcurrentHashMap<Object, Pair<Folder<?>, Integer>> collectTo) {
        Storable current = source.getParent();
        while (Objects.nonNull(current) && Objects.nonNull(current.getParent())) {
            if (current instanceof Folder) {
                collectTo.put(current.getID(),
                        new ImmutablePair<>(simpleFolder((Folder<?>) current), ((Folder<?>) current).hierarchyLevel()));
                current = current.getParent();
            } else {
                log.warn("Parent of {} is not Folder but {}", current, current.getParent().getClass());
                break;
            }
        }
    }

    private <T extends Folder<?>> T simpleFolder(T source) {
        try {
            T simpleFolder = (T) source.getClass().getConstructor().newInstance();
            simpleFolder.setProject(source.getProject());
            simpleFolder.setTypeName(source.getTypeName());
            simpleFolder.setParent(source.getParent());
            simpleFolder.setID(source.getID());
            simpleFolder.setName(source.getName());
            simpleFolder.setVersion(source.getVersion());
            simpleFolder.setLabels(source.getLabels());
            simpleFolder.setPrefix(source.getPrefix());
            simpleFolder.setDescription(source.getDescription());
            simpleFolder.setStorableProp(source.getStorableProp());
            return simpleFolder;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new RuntimeException("Folder instantiation exception", e);
        }
    }

    private Set<System> extractSystemFromLoadPart(String content, BigInteger projectId) {
        Set<String> loadPartTemplateIdentifiers = templateExtractor.findLoadPartTemplates(content);
        if (Objects.isNull(loadPartTemplateIdentifiers) || loadPartTemplateIdentifiers.isEmpty()) {
            return new HashSet<>();
        }
        Set<System> systems = new HashSet<>();
        for (String templateIdentifier : loadPartTemplateIdentifiers) {
            Template<? extends TemplateProvider> templateObject = templateExtractor.getTemplateObject(
                    templateIdentifier, projectId);
            if (Objects.nonNull(templateObject)) {
                System system = getSystem(templateObject);
                systems.add(system);
            }
        }
        return systems;
    }

    private System getSystem(Template<? extends TemplateProvider> template) {
        return template instanceof SystemTemplate
                ? (System) template.getParent()
                : (System) template.getParent().getParent();
    }

    private void collectSystemsFromDiameterTransportFields(System system,
                                                           Map<Object, Storable> extraObjects,
                                                           Map<String, Set<Object>> checkedObjectsMap) {
        for (TransportConfiguration transport : system.getTransports()) {
            if (!"org.qubership.automation.itf.transport.diameter.outbound.DiameterOutbound".equals(
                    transport.getTypeName())) {
                continue;
            }
            BigInteger projectId = system.getParent().getProjectId();
            List<PropertyDescriptor> transportPropertiesDescriptor = getTransportPropertyDescriptor(transport);
            if (transportPropertiesDescriptor.isEmpty()) {
                continue;
            }
            for (PropertyDescriptor property : transportPropertiesDescriptor) {
                if (!property.loadTemplate()) {
                    continue;
                }
                String shortName = property.getShortName();
                String propertyValue = transport.get(shortName);
                if (StringUtils.isNotEmpty(propertyValue)) {
                    Template<? extends TemplateProvider> templateObject =
                            templateExtractor.getTemplateObject(propertyValue, projectId);
                    if (Objects.nonNull(templateObject)) {
                        if (!checkedObjectsMap.get(Template.class.getSimpleName()).contains(templateObject.getID())) {
                            log.info("Diameter - List of checked templates doesn't contain template with id {}. "
                                    + "It will be added and checked.", templateObject.getID());
                            checkedObjectsMap.get(Template.class.getSimpleName()).add(templateObject.getID());
                            extraObjects.putAll(getExtraObjectsForSystem(getSystem(templateObject), checkedObjectsMap));
                        } else {
                            log.info("Diameter - List of checked templates already contains template with id {}. "
                                    + "Checking of template is skipped.", templateObject.getID());
                        }
                    }
                }
            }
        }
    }

    private List<PropertyDescriptor> getTransportPropertyDescriptor(TransportConfiguration transport) {
        try {
            ClassLoader classLoader = TransportClassLoader.getInstance()
                    .getClassLoaderHolder().get(transport.getTypeName())
                    .loadClass(transport.getTypeName()).getClassLoader();
            return Extractor.extractProperties(
                    (PropertyProvider) Class.forName(transport.getTypeName(), false, classLoader)
                            .newInstance()
            );
        } catch (Exception e) {
            log.error("Can't define transport properties '{}' while objects export", transport.getTypeName(), e);
        }
        return new ArrayList<>();
    }

    private void collectSystemsFromLoadPartInTemplates(TemplateProvider templateProvider,
                                                       Map<Object, Storable> extraObjects,
                                                       Map<String, Set<Object>> checkedObjectsMap,
                                                       BigInteger projectId) {
        for (Template template : templateProvider.returnTemplates()) {
            if (!checkedObjectsMap.get(Template.class.getSimpleName()).contains(template.getID())) {
                log.info("List of checked templates doesn't contain template with id {}. "
                        + "It will be added and checked.", template.getID());
                checkedObjectsMap.get(Template.class.getSimpleName()).add(template.getID());
                Set<System> systems = extractSystemFromLoadPart(template.getText(), projectId);
                for (System extractedSystem : systems) {
                    extraObjects.putAll(getExtraObjectsForSystem(extractedSystem, checkedObjectsMap));
                }
            } else {
                log.info("List of checked templates already contains template with id {}. "
                        + "Checking of template is skipped.", template.getID());
            }
        }
    }
}
