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

package org.qubership.automation.itf.ui.controls.service.integration;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.START_TRANSPORT_TRIGGERS_AT_STARTUP;
import static org.qubership.automation.itf.core.util.converter.IdConverter.toBigInt;
import static org.qubership.automation.itf.ui.controls.integration.InboundTransportClassToTypeMapping.getTransportType;
import static org.qubership.automation.itf.ui.controls.util.MultiTenantProjectCollector.ADDITIONAL_CLUSTERS;
import static org.qubership.automation.itf.ui.controls.util.MultiTenantProjectCollector.DEFAULT_CLUSTER;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.TriggerConfigurationObjectManager;
import org.qubership.automation.itf.core.model.communication.EnvironmentSample;
import org.qubership.automation.itf.core.model.communication.TriggerSample;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.TriggerConfiguration;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.converter.PropertiesConverter;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.ui.controls.integration.Result;
import org.qubership.automation.itf.ui.controls.util.MultiTenantProjectCollector;
import org.qubership.automation.itf.ui.messages.objects.integration.stubs.UIUpdateTriggerStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItfStubsRequestsService {

    private final MultiTenantProjectCollector multiTenantProjectCollector;
    private final ProjectSettingsService projectSettingsService;
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    private static ConnectionProperties getTriggerConnectionProperties(TriggerConfiguration trigger) {
        ConnectionProperties triggerConnectionProperties = new ConnectionProperties(new HashMap<>());
        try {
            triggerConnectionProperties = PropertiesConverter.convert(
                    trigger.getTypeName(),
                    trigger.getParent().getReferencedConfiguration().getConfiguration(),
                    trigger.getParent().getConfiguration(),
                    trigger.getConfiguration());
        } catch (TransportException e) {
            log.error("Error while computing connection properties for trigger {}. "
                    + "Empty or partial connection properties are returned!", trigger.getName(), e);
        }
        return triggerConnectionProperties;
    }

    public TriggerSample createTriggerSample(TriggerConfiguration trigger) {
        return createTriggerSample(trigger, getProjectUuidForServer(trigger.getParent().getParent()));
    }

    @Transactional(readOnly = true)
    public TriggerSample createTriggerSample(TriggerConfiguration trigger, UUID projectUuid) {
        TriggerSample triggerSample = new TriggerSample();
        triggerSample.setTriggerId((BigInteger) trigger.getID());
        triggerSample.setTriggerName(trigger.getName());
        triggerSample.setTriggerTypeName(trigger.getTypeName());
        triggerSample.setTriggerState(trigger.getState());
        triggerSample.setTransportName(trigger.getParent().getReferencedConfiguration().getName());
        triggerSample.setServerName(trigger.getParent().getParent().getName());
        triggerSample.setTriggerProperties(getTriggerConnectionProperties(trigger));
        triggerSample.setTransportType(getTransportType(trigger.getTypeName()));
        triggerSample.setProjectUuid(projectUuid);
        triggerSample.setProjectId(trigger.getParent().getParent().getProjectId());
        return triggerSample;
    }

    private UUID getProjectUuidForServer(Server server) {
        return CoreObjectManager.getInstance()
                .getManager(StubProject.class)
                .getById(server.getProjectId())
                .getUuid();
    }

    /**
     * Get {@link TriggerSample} triggers if start.transport.triggers.at.startup is enabled in project settings
     * and trigger has 'ACTIVE' state.
     * This method using when atp-itf-stubs service is starting via feign request.
     *
     * @return {@link TriggerSample} triggers that should be activated at start of atp-itf-stubs.
     */
    public List<TriggerSample> getAllActiveTriggers() {
        long startTime = java.lang.System.nanoTime();
        log.info("Getting transport triggers for activation...");
        //TODO: replace to get projects with enabled property only (not all)
        Map<String, Collection<StubProject>> projects = multiTenantProjectCollector.collectProjects();
        Map<BigInteger, UUID> projectUuids = new HashMap<>();
        //TODO fix it after collectProjects
        Map<String, List<StubProject>> projectsWithEnabledProperty =
                getProjectsWithEnabledStartTriggersAtStartup(projects, projectUuids);
        List<TriggerSample> triggers = collectActiveTriggers(projectsWithEnabledProperty, projectUuids);
        long elapsedTime = java.lang.System.nanoTime() - startTime;
        log.info("Getting transport triggers for activation is completed, triggers count {}, elapsed time {} (s)",
                Objects.requireNonNull(triggers).size(), String.format("%.3f", (double) elapsedTime / 1000000000.0));
        return triggers;
    }

    public List<TriggerSample> getAllTriggersByProject(UUID projectUuid) {
        Function<StubProject, ArrayList<TriggerSample>> function
                = (proj) -> (ArrayList<TriggerSample>) getAndCreateTriggerSimpleObjects(proj);
        return getTriggersByProject(projectUuid, "all", function);
    }

    public List<TriggerSample> getAllActiveAndErrorTriggersByProject(UUID projectUuid) {
        Function<StubProject, ArrayList<TriggerSample>> function
                = (proj) -> (ArrayList<TriggerSample>) getActiveAndErrorAndCreateTriggerSimpleObjects(proj);
        return getTriggersByProject(projectUuid, "active/error", function);
    }

    @SuppressWarnings("unchecked")
    public List<TriggerSample> getTriggersByProject(UUID projectUuid, String actionName,
                                                    Function<StubProject, ArrayList<TriggerSample>> function) {
        long startTime = java.lang.System.nanoTime();
        log.info("Getting {} triggers of {} project...", actionName, projectUuid);
        StubProject project = (StubProject) CoreObjectManager.getInstance()
                .getSpecialManager(StubProject.class, SearchManager.class).getByUuid(projectUuid);
        List<TriggerSample> triggers = collectTriggersByProject(Objects.requireNonNull(project), function);
        long elapsedTime = java.lang.System.nanoTime() - startTime;
        log.info("Getting {} triggers of {} project is completed: triggers count {}, elapsed time {} (s)",
                actionName, projectUuid, Objects.requireNonNull(triggers).size(),
                String.format("%.3f", (double) elapsedTime / 1000000000.0));
        return triggers;
    }

    public List<EnvironmentSample> getTriggersByEnvFolder(BigInteger envFolderId) {
        EnvFolder envFolder = CoreObjectManager.getInstance().getManager(EnvFolder.class).getById(envFolderId);
        if (Objects.isNull(envFolder)) {
            log.debug("There is no environment folder with id={}. Null will be returned.", envFolderId);
            return Collections.emptyList();
        }
        Collection<? extends Environment> environments
                = CoreObjectManager.getInstance().getManager(Environment.class).getAllByParentId(envFolderId);
        List<EnvironmentSample> environmentSamples = new ArrayList<>();
        UUID projectUuid = envFolder.getProject().getUuid();
        environments.forEach(environment ->
                environmentSamples.add(getEnvironmentSampleObject(environment, projectUuid)));
        return environmentSamples;
    }

    public EnvironmentSample getTriggersByEnvironment(BigInteger environmentId) {
        Environment environment = CoreObjectManager.getInstance().getManager(Environment.class).getById(environmentId);
        if (environment == null) {
            log.debug("There is no environment with id={}. Null is returned.", environmentId);
            return null;
        }
        UUID projectUuid = CoreObjectManager.getInstance().getManager(StubProject.class)
                .getById(environment.getProjectId()).getUuid();
        return getEnvironmentSampleObject(environment, projectUuid);
    }

    public TriggerSample getTriggerById(BigInteger id) {
        TriggerConfiguration trigger =
                CoreObjectManager.getInstance().getManager(TriggerConfiguration.class).getById(id);
        return trigger == null ? null : createTriggerSample(trigger);
    }

    public Result updateTriggerStatus(UIUpdateTriggerStatus uiUpdateTriggerStatus) {
        BigInteger triggerId = uiUpdateTriggerStatus.getId();
        TriggerConfiguration triggerConfiguration =
                CoreObjectManager.getInstance().getManager(TriggerConfiguration.class).getById(triggerId);
        if (triggerConfiguration == null) {
            String errorMsg = String.format("Trigger [id=%s] doesn't exist.", triggerId);
            log.debug(errorMsg);
            return new Result(false, errorMsg);
        }
        TriggerState triggerState = TriggerState.fromString(uiUpdateTriggerStatus.getStatus());
        if (triggerState != null) {
            triggerConfiguration.setActivationErrorMessage(uiUpdateTriggerStatus.getDescription());
            triggerConfiguration.setState(triggerState);
            triggerConfiguration.store();
            String statusChangeMessage = String.format("Status of trigger [id=%s] is changed to %s.", triggerId,
                    triggerState);
            log.debug(statusChangeMessage);
            return new Result(!(TriggerState.ERROR.equals(triggerState)), statusChangeMessage);
        } else {
            String errorMsg = String.format("Status can't be found by value=%s. Trigger [id=%s] is not updated",
                    uiUpdateTriggerStatus.getStatus(), triggerId);
            log.debug(errorMsg);
            return new Result(false, errorMsg);
        }
    }

    private List<TriggerSample> collectTriggersByProject(StubProject project,
                                                         Function<StubProject, ArrayList<TriggerSample>> function) {
        List<TriggerSample> triggers = new ArrayList<>();
        if (multiTenancyEnabled) {
            TenantContext.setTenantInfo(project.getUuid().toString());
            triggers.addAll(function.apply(project));
            TenantContext.setDefaultTenantInfo();
        } else {
            triggers.addAll(function.apply(project));
        }
        return triggers;
    }

    private List<TriggerSample> collectActiveTriggers(Map<String, List<StubProject>> projects,
                                                      Map<BigInteger, UUID> projectUuids) {
        List<TriggerSample> activeTriggers = new ArrayList<>();
        if (multiTenancyEnabled) {
            log.info("getAllActiveTriggers: additional cluster(s) are processed...");
            collectActiveTriggers(projects.get(ADDITIONAL_CLUSTERS), activeTriggers, projectUuids, false);
            TenantContext.setDefaultTenantInfo();
        }
        log.info("getAllActiveTriggers: default cluster is processed...");
        collectActiveTriggers(projects.get(DEFAULT_CLUSTER), activeTriggers, projectUuids, true);
        return activeTriggers;
    }

    private void collectActiveTriggers(List<StubProject> projects, List<TriggerSample> to,
                                       Map<BigInteger, UUID> projectUuids, boolean isDefault) {
        projects.forEach(project -> {
            log.info("getAllActiveTriggers, project {} is processed...", project.getUuid());
            if (multiTenancyEnabled && !isDefault) {
                TenantContext.setTenantInfo(projectUuids.get((BigInteger) project.getID()).toString());
            }
            to.addAll(getActiveAndCreateTriggerSamples((BigInteger) project.getID(), project.getUuid()));
            log.info("getAllActiveTriggers, project {} is added: triggers total {}", project.getUuid(), to.size());
        });
    }

    protected List<TriggerSample> getActiveAndCreateTriggerSamples(BigInteger projectId, UUID projectUuid) {
        try {
            return TxExecutor.execute(() -> {
                Collection<TriggerConfiguration> triggers = CoreObjectManager.getInstance()
                        .getSpecialManager(TriggerConfiguration.class, TriggerConfigurationObjectManager.class)
                        .getAllActiveTriggersByProjectId(projectId);
                return triggers.stream().map(trigger -> {
                            try {
                                return createTriggerSample(trigger, projectUuid);
                            } catch (Exception ex) {
                                log.warn("getAllActiveTriggers: trigger {} processing is failed", trigger.getID(), ex);
                                return null;
                            }
                        })
                        .collect(Collectors.toList());
            }, TxExecutor.readOnlyTransaction());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<TriggerSample> getAndCreateTriggerSimpleObjects(StubProject project) {
        try {
            return TxExecutor.execute(() -> {
                BigInteger projectId = (BigInteger) project.getID();
                Collection<TriggerConfiguration> triggers = CoreObjectManager.getInstance()
                        .getSpecialManager(TriggerConfiguration.class, TriggerConfigurationObjectManager.class)
                        .getAllTriggersByProjectId(projectId);
                return triggers.parallelStream().map(trigger -> {
                            try {
                                return createTriggerSample(trigger, project.getUuid());
                            } catch (Exception ex) {
                                throw new RuntimeException(
                                        String.format("Error while getting all triggers for activation: trigger %s",
                                                trigger.getID()), ex);
                            }
                        })
                        .collect(Collectors.toList());
            }, TxExecutor.readOnlyTransaction());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<TriggerSample> getActiveAndErrorAndCreateTriggerSimpleObjects(StubProject project) {
        try {
            return TxExecutor.execute(() -> {
                BigInteger projectId = (BigInteger) project.getID();
                Collection<TriggerConfiguration> triggers = CoreObjectManager.getInstance()
                        .getSpecialManager(TriggerConfiguration.class, TriggerConfigurationObjectManager.class)
                        .getAllActiveAndErrorTriggersByProjectId(projectId);
                return triggers.parallelStream().map(trigger -> {
                            try {
                                return createTriggerSample(trigger, project.getUuid());
                            } catch (Exception ex) {
                                throw new RuntimeException(
                                        String.format("Error while getting all active and error triggers: trigger %s",
                                                trigger.getID()), ex);
                            }
                        })
                        .collect(Collectors.toList());
            }, TxExecutor.readOnlyTransaction());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected EnvironmentSample getEnvironmentSampleObject(Environment environment, UUID projectUuid) {
        EnvironmentSample environmentSample = new EnvironmentSample();
        environmentSample.setEnvId((BigInteger) environment.getID());
        environmentSample.setTurnedOn(environment.getEnvironmentState().isOn());
        environmentSample.setTriggerSamples(new ArrayList<>());
        for (Map.Entry<System, Server> entry : environment.getInbound().entrySet()) {
            if (entry.getValue() != null) {
                for (InboundTransportConfiguration configuration : entry.getValue().getInbounds(entry.getKey())) {
                    if (!configuration.getTriggerConfigurations().isEmpty()) {
                        for (TriggerConfiguration triggerConfiguration : configuration.getTriggerConfigurations()) {
                            environmentSample.getTriggerSamples().add(
                                    createTriggerSample(triggerConfiguration, projectUuid));
                        }
                    }
                }
            }
        }
        return environmentSample;
    }

    private Map<String, List<StubProject>> getProjectsWithEnabledStartTriggersAtStartup(Map<String,
            Collection<StubProject>> projects, Map<BigInteger, UUID> projectUuids) {
        Map<String, List<StubProject>> activeProjects = new HashMap<>();
        log.info("getAllActiveTriggers: Collect projects to activate triggers...");
        if (multiTenancyEnabled) {
            collectProjectsInfoIfNeedToActivateTriggers(projects.get(ADDITIONAL_CLUSTERS), activeProjects,
                    ADDITIONAL_CLUSTERS, false, projectUuids);
            log.info("getAllActiveTriggers, additional clusters processing: projects count {}",
                    activeProjects.get(ADDITIONAL_CLUSTERS).size());
            TenantContext.setDefaultTenantInfo();
        }
        collectProjectsInfoIfNeedToActivateTriggers(projects.get(DEFAULT_CLUSTER), activeProjects,
                DEFAULT_CLUSTER, true, projectUuids);
        log.info("getAllActiveTriggers, default cluster processing: projects count {}",
                activeProjects.get(DEFAULT_CLUSTER).size());
        return activeProjects;
    }

    private void collectProjectsInfoIfNeedToActivateTriggers(Collection<StubProject> projects, Map<String,
            List<StubProject>> to, String key, boolean isDefault, Map<BigInteger, UUID> projectUuids) {
        to.put(key, collectProjectsInfoIfNeedToActivateTriggers(projects, isDefault, projectUuids));
    }

    private List<StubProject> collectProjectsInfoIfNeedToActivateTriggers(Collection<StubProject> projects,
                                                                          boolean isDefaultCluster,
                                                                          Map<BigInteger, UUID> projectUuids) {
        List<StubProject> enabled = new ArrayList<>();
        for (StubProject project : projects) {
            if (!isDefaultCluster) {
                TenantContext.setTenantInfo(project.getUuid().toString());
            }
            if (isNeedToActivateTriggersAtStartUp(project)) {
                enabled.add(project);
                projectUuids.put((BigInteger) project.getID(), project.getUuid());
            }
        }
        log.info("Projects count with start.transport.triggers.at.startup = true for {} cluster: {}",
                isDefaultCluster ? DEFAULT_CLUSTER : ADDITIONAL_CLUSTERS, enabled.size());
        return enabled;
    }

    private boolean isNeedToActivateTriggersAtStartUp(StubProject project) {
        return projectSettingsService.getBoolean(toBigInt(project.getID()), START_TRANSPORT_TRIGGERS_AT_STARTUP);
    }
}
