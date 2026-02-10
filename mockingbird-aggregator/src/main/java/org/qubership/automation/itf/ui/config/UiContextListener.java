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

package org.qubership.automation.itf.ui.config;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.DATA_SET_SERVICE_DS_FORMAT;
import static org.qubership.automation.itf.ui.controls.util.MultiTenantProjectCollector.ADDITIONAL_CLUSTERS;
import static org.qubership.automation.itf.ui.controls.util.MultiTenantProjectCollector.DEFAULT_CLUSTER;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.catalogue.openapi.dto.ProjectDto;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.multitenancy.core.context.TenantContext;
import org.qubership.automation.diameter.connection.ConnectionFactory;
import org.qubership.automation.diameter.connection.DiameterConnection;
import org.qubership.automation.itf.core.execution.DaemonThreadPoolFactory;
import org.qubership.automation.itf.core.execution.ExecutorServiceProviderFactory;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvironmentManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.UpgradeHistoryObjectManager;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.versions.UpgradeHistory;
import org.qubership.automation.itf.core.report.impl.TemplateBasedLinkCollector;
import org.qubership.automation.itf.core.util.DiameterConnectionInfoProvider;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.eds.ExternalDataManagementService;
import org.qubership.automation.itf.core.util.eds.model.FileInfo;
import org.qubership.automation.itf.core.util.holder.ActiveInterceptorHolder;
import org.qubership.automation.itf.core.util.holder.InterceptorHolder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.core.util.transport.service.report.ReportAdapterStorage;
import org.qubership.automation.itf.executor.cache.CacheCleanerService;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.qubership.automation.itf.integration.catalogue.CatalogueProjectFeignClient;
import org.qubership.automation.itf.ui.controls.util.MultiTenantProjectCollector;
import org.qubership.automation.itf.ui.util.EventTriggerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.AbstractJmsListeningContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UiContextListener {

    private final ApplicationContext myContext;
    private final ReportLinkCollector reportLinkCollector;
    private final ExternalDataManagementService externalDataManagementService;
    private final CacheCleanerService cacheCleanerService;
    private final ContextTransport contextTransport = new ContextTransport();
    private final CatalogueProjectFeignClient catalogueProjectFeignClient;
    private final LockManager lockManager;
    private final MultiTenantProjectCollector multiTenantProjectCollector;
    private final ProjectSettingsService projectSettingsService;
    //private final UnusedConfigurationsCleanerService unusedConfigurationsCleanerService;
    private HazelcastInstance hazelcastInstance;
    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    @Autowired(required = false)
    public void setHazelcastInstance(@Qualifier("hazelcastCacheInstance") HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    private void registerReportAdapters() {
        ReportAdapterStorage.getInstance().init();
    }

    @Transactional
    public void contextInitialized() {
        try {
            registerLinkCollectors();
            lockManager.executeWithLock("Init: upgradeHistory", this::upgradeHistory);
            Map<String, Collection<StubProject>> projects = multiTenantProjectCollector.collectProjects();
            refreshProjectSettingsAndInitCache(projects);
            activateEventTriggers(projects);
            lockManager.executeWithLock("Init: updateInitialEnvState", this::updateInitialEnvState);
            contextTransport.init();
            cacheCleanerService.startWorker();
            loadDataFromExternalStorage(projects);
        } catch (Exception e) {
            log.error("Error initialing object manager or modules", e);
        }
        try {
            registerInterceptors();
        } catch (Exception e) {
            log.error("Failed registering Interceptors", e);
        }
        try {
            registerReportAdapters();
        } catch (Exception e) {
            log.error("Failed registering Report Adapters", e);
        }
        try {
            startingJmsListenerContainer();
        } catch (Exception e) {
            log.error("Failed starting of Jms Listeners", e);
        }
        log.info("ITF-Executor service initialization completed");
        //unusedConfigurationsCleanerService.startWorker(); // Do not start it. Cleaner should be rewritten.
    }

    public void closingAllDiameterConnections() {
        for (Map.Entry<Object, DiameterConnection> connectionEntry : ConnectionFactory.getAll().asMap().entrySet()) {
            DiameterConnection connection = connectionEntry.getValue();
            try {
                if (connection.isOpen()) {
                    connection.close();
                }
                String connectionId = (String) connectionEntry.getKey();
                DiameterConnectionInfoProvider.getDiameterConnectionInfoCacheService().remove(connectionId);
            } catch (Exception e) {
                log.warn("Error closing diameter connection {}", connection.getChannel(), e);
            }
        }
    }

    @Transactional
    public void contextDestroyed() {
        log.info("ITF graceful shutdown is started...");
        ExecutorServiceProviderFactory.get().shutdown();
        ReportAdapterStorage.getInstance().terminateAll();
        DaemonThreadPoolFactory.getInstance().shutdown();
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
        closingAllDiameterConnections();
        contextTransport.destroyed();
    }

    @EventListener
    public void init(ContextRefreshedEvent event) {
        if (event.getSource().equals(myContext)) {
            contextInitialized();
        }
    }

    @EventListener
    public void beforeCloseContext(ContextClosedEvent event) {
        if (event.getSource().equals(myContext)) {
            stopContexts();
            contextDestroyed();
        }
    }

    private void stopContexts() {
        TCContextService tcContextService = ExecutionServices.getTCContextService();
        List<TcContext> tcContexts = tcContextService.getTcContextCacheService()
                .getAllTcContexts(Config.getConfig().getRunningHostname());
        for (TcContext context : tcContexts) {
            tcContextService.stop(context);
        }
    }

    private void startingJmsListenerContainer() {
        log.info("Getting JmsListenerContainerFactories...");
        DefaultJmsListenerContainerFactory factory = (DefaultJmsListenerContainerFactory)
                myContext.getBean("stubDefaultJmsListenerQueueContainerFactory");
        factory.setAutoStartup(true);
        factory = (DefaultJmsListenerContainerFactory)
                myContext.getBean("defaultJmsListenerTopicContainerFactory");
        factory.setAutoStartup(true);
        log.info("JmsListenerContainerFactories: setAutoStartup is set to true.");
        log.info("Getting JmsListenerEndpointRegistry...");
        JmsListenerEndpointRegistry jmsListenerEndpointRegistry
                = myContext.getBean(JmsListenerEndpointRegistry.class);
        log.info("Setting 'autoStartup' to true for all JMS Listeners...");
        jmsListenerEndpointRegistry.getListenerContainers().stream().forEach(messageListenerContainer ->
                ((AbstractJmsListeningContainer) messageListenerContainer).setAutoStartup(true)
        );
        log.info("All JMS Listeners are ready. Starting jmsListenerEndpointRegistry...");
        jmsListenerEndpointRegistry.start();
        log.info("JmsListenerEndpointRegistry is started.");
    }

    private void registerLinkCollectors() {
        reportLinkCollector.registerCollectors(
                Collections.singletonList(TemplateBasedLinkCollector.class.getCanonicalName()));
    }

    private void upgradeHistory() {
        if (multiTenancyEnabled) {
            Collection<String> clusters = TenantContext.getTenantIds(true);
            for (String cluster : clusters) {
                TenantContext.setTenantInfo(cluster);
                doUpgradeHistory();
            }
            TenantContext.setDefaultTenantInfo();
        }
        doUpgradeHistory();
    }

    private void doUpgradeHistory() {
        UpgradeHistory upgradeHistories = CoreObjectManager.getInstance()
                .getSpecialManager(UpgradeHistory.class, UpgradeHistoryObjectManager.class).findLastVersion();
        String currentBuildVersion = getCurrentBuildVersion().replaceFirst("application.version=", "");
        if (upgradeHistories == null || !currentBuildVersion.contains(upgradeHistories.getName())) {
            UpgradeHistory lastVersion = CoreObjectManager.getInstance().getManager(UpgradeHistory.class).create();
            lastVersion.setUpgradeDatetime(Timestamp.valueOf(LocalDateTime.now()));
            lastVersion.setName(currentBuildVersion);
            lastVersion.store();
        }
    }

    protected String getCurrentBuildVersion() {
        try {
            FileInputStream inFile = new FileInputStream("./buildVersion.properties");
            byte[] str = new byte[inFile.available()];
            //noinspection ResultOfMethodCallIgnored
            inFile.read(str);
            return new String(str);
        } catch (Exception e) {
            log.error("Error while getting current build version", e);
            return null;
        }
    }

    private void activateEventTriggers(Map<String, Collection<StubProject>> allProjects) {
        log.info("Activate event triggers...");
        ExecutorService threadPool = DaemonThreadPoolFactory.cachedThreadPool(10, "EventTriggersActivationThread - ");
        Queue<Future<Boolean>> queue = new ConcurrentLinkedQueue<>();
        if (multiTenancyEnabled) {
            activateEventTriggers(allProjects.get(ADDITIONAL_CLUSTERS), false, threadPool, queue);
            TenantContext.setDefaultTenantInfo();
        }
        activateEventTriggers(allProjects.get(DEFAULT_CLUSTER), true, threadPool, queue);
        log.info("All event triggers are sent to activation, waiting to finish...");
        while (!queue.isEmpty()) {
            Future<Boolean> future = queue.peek();
            if (future != null && (future.isCancelled() || future.isDone())) {
                queue.remove(future);
                continue;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(400);
            } catch (InterruptedException ex) {
                // Do nothing
            }
        }
        try {
            threadPool.shutdown();
            //noinspection ResultOfMethodCallIgnored
            threadPool.awaitTermination(4000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Do nothing
        } finally {
            List<Runnable> pending = threadPool.shutdownNow();
            log.info("Activation of event triggers is completed. Pending (aborted) tasks: {}", pending.size());
        }
    }

    private void activateEventTriggers(Collection<? extends StubProject> projects, boolean isDefaultCluster,
                                       ExecutorService threadPool, Queue<Future<Boolean>> queue) {
        for (StubProject project : projects) {
            Future<Boolean> future = threadPool.submit(() -> {
                try {
                    if (isDefaultCluster) {
                        TenantContext.setDefaultTenantInfo();
                    } else {
                        TenantContext.setTenantInfo(project.getUuid().toString());
                    }
                    log.info("Project {} / {}, '{}' is processed...",
                            project.getID(), project.getUuid(), project.getName());
                    EventTriggerHelper.activateEventTriggers((BigInteger) project.getID());
                    return true;
                } catch (Exception ex) {
                    log.warn("Exception while activation of event triggers, project {} / {}: {}",
                            project.getID(), project.getUuid(), ex.getMessage());
                    return false;
                }
            });
            queue.add(future);
        }
    }

    private void refreshProjectSettingsAndInitCache(Map<String, Collection<StubProject>> projects) {
        Map<UUID, ProjectDto> projectsFromCatalogue = getProjectsFromCatalogue();
        boolean invalidDataFromCatalogue = Objects.isNull(projectsFromCatalogue);
        if (multiTenancyEnabled) {
            log.info("Filling project setting cache & refreshing from ATP Catalogue "
                    + "for ADDITIONAL clusters is started...");
            List<StubProject> additionalClustersProjects = new ArrayList<>(projects.get(ADDITIONAL_CLUSTERS));
            additionalClustersProjects.sort(Comparator.comparing(proj -> proj.getID().toString()));
            long processingTime = refreshProjectSettingsAndInitCache(additionalClustersProjects, projectsFromCatalogue,
                    invalidDataFromCatalogue);
            log.info("Filling project setting cache & refreshing from ATP Catalogue "
                            + "for ADDITIONAL clusters is finished. It takes {}",
                    String.format("%.3f", (double) processingTime / 1000000000.0));
            TenantContext.setDefaultTenantInfo();
        }
        List<StubProject> defaultClustersProjects = new ArrayList<>(projects.get(DEFAULT_CLUSTER));
        defaultClustersProjects.sort(Comparator.comparing(proj -> proj.getID().toString()));
        log.info("Filling project setting cache & refreshing from ATP Catalogue "
                + "for DEFAULT cluster is started...");
        long processingTime = refreshProjectSettingsAndInitCache(defaultClustersProjects, projectsFromCatalogue,
                invalidDataFromCatalogue);
        log.info("Filling project setting cache & refreshing from ATP Catalogue "
                        + "for DEFAULT cluster is finished. It takes {}",
                String.format("%.3f", (double) processingTime / 1000000000.0));
    }

    private long refreshProjectSettingsAndInitCache(List<StubProject> itfProjects,
                                                    Map<UUID, ProjectDto> projectsFromCatalogue,
                                                    boolean invalidDataFromCatalogue) {
        long processTime = 0;
        for (StubProject itfProject : itfProjects) {
            TenantContext.setTenantInfo(itfProject.getUuid().toString());
            long startTime = System.nanoTime();
            projectSettingsService.initCache(itfProject);
            refreshProjectSettings(itfProject, projectsFromCatalogue, invalidDataFromCatalogue);
            processTime = processTime + (System.nanoTime() - startTime);
        }
        return processTime;
    }

    private void refreshProjectSettings(StubProject itfProject, Map<UUID, ProjectDto> projectsFromCatalogue,
                                        boolean invalidDataFromCatalogue) {
        if (invalidDataFromCatalogue) {
            return;
        }
        try {
            log.info("Project {} / {}, '{}' is processed...", itfProject.getID(), itfProject.getUuid(),
                    itfProject.getName());
            refreshProjectSettingsFromCatalogue(itfProject, projectsFromCatalogue);
        } catch (Exception ex) {
            log.warn("Exception while refreshing project settings from atp-catalogue, project {} / {}: {}",
                    itfProject.getID(), itfProject.getUuid(), ex.getMessage());
        }
    }

    protected void refreshProjectSettingsFromCatalogue(StubProject project, Map<UUID, ProjectDto> projects) {
        if (Objects.isNull(project.getUuid())) {
            log.warn("Project {}, '{}': UUID is not set, refreshing from catalogue can't be performed.",
                    project.getID(), project.getName());
            return;
        }
        if (!projects.containsKey(project.getUuid())) {
            log.warn("Project {} / {}, '{}': There is no such project UUID in the atp-catalogue!",
                    project.getID(), project.getUuid(), project.getName());
            return;
        }
        ProjectDto projectDto = projects.get(project.getUuid());
        String newDatasetFormat = projectDto.getDatasetFormat().toString();
        if (StringUtils.isBlank(newDatasetFormat)) {
            log.error("'datasetFormat' property is missed or invalid. Response: {}", projectDto);
            return;
        }
        BigInteger projectId = (BigInteger) project.getID();
        String currentDatasetFormat = projectSettingsService.get(projectId,
                DATA_SET_SERVICE_DS_FORMAT);
        if (newDatasetFormat.equals(currentDatasetFormat)) {
            log.info("Old and New DatasetFormat settings are the same: {}", newDatasetFormat);
            return;
        }
        projectSettingsService.update(projectId, DATA_SET_SERVICE_DS_FORMAT, newDatasetFormat, true);
        log.info("DatasetFormat setting is changed to {}", newDatasetFormat);
    }

    private void loadDataFromExternalStorage(Map<String, Collection<StubProject>> projects) throws IOException {
        log.info("Loading files from external storage...");
        List<StubProject> projectsList = projects.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        for (StubProject proj : projectsList) {
            try {
                log.info("Project {} / {}, '{}' is processed...", proj.getID(), proj.getUuid(), proj.getName());
                Set<FileInfo> files = externalDataManagementService.getExternalStorageService()
                        .getFilesInfoByProject(proj.getUuid());
                externalDataManagementService.getFileManagementService().save(files);
                log.info("Project {} / {}, '{}' - {} files are loaded.", proj.getID(), proj.getUuid(), proj.getName(),
                        files.size());
            } catch (Exception ex) {
                log.warn("Exception while loading files for project {} / {}: {}",
                        proj.getID(), proj.getUuid(), ex.getMessage());
            }
        }
        externalDataManagementService.getFileManagementService()
                .save(externalDataManagementService.getExternalStorageService().getKeyStoreFileInfo());
        log.info("Loading files from external storage is completed.");
    }

    private void updateInitialEnvState() {
        if (multiTenancyEnabled) {
            Collection<String> tenantIdsPerCluster = TenantContext.getTenantIds(true);
            for (String tenantId : tenantIdsPerCluster) {
                TenantContext.setTenantInfo(tenantId);
                doUpdateInitialEnvState();
            }
            TenantContext.setDefaultTenantInfo();
        }
        doUpdateInitialEnvState();
    }

    private void doUpdateInitialEnvState() {
        CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, EnvironmentManager.class).updateInitialEnvState();
    }

    private void registerInterceptors() {
        log.info("Registration of interceptors' modules is started...");
        InterceptorHolder.getInstance();
        log.info("Registration of interceptors' modules is completed.");
        if (multiTenancyEnabled) {
            Collection<String> tenantIdsPerCluster = TenantContext.getTenantIds(true);
            for (String tenantId : tenantIdsPerCluster) {
                TenantContext.setTenantInfo(tenantId);
                doRegisterActiveInterceptors(tenantId);
            }
            TenantContext.setDefaultTenantInfo();
        }
        doRegisterActiveInterceptors("default");
    }

    private void doRegisterActiveInterceptors(String tenantId) {
        log.info("Registration of active interceptors is started for '{}' tenant...", tenantId);
        ActiveInterceptorHolder.getInstance().fillActiveInterceptorHolder();
        log.info("Registration of active interceptors is completed for '{}' tenant.", tenantId);
    }

    @Nullable
    private Map<UUID, ProjectDto> getProjectsFromCatalogue() {
        try {
            Set<ProjectDto> projectsFromCatalogue = catalogueProjectFeignClient.getAll().getBody();
            return Objects.nonNull(projectsFromCatalogue)
                    ? projectsFromCatalogue.stream().collect(Collectors.toMap(ProjectDto::getUuid, Function.identity()))
                    : null;
        } catch (Exception ex) {
            log.error("Exception while getting projects list from atp-catalogue or collect its to map. Refresh "
                    + "projects settings will be skipped.", ex);
            return null;
        }
    }
}
