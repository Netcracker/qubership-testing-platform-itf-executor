//package org.qubership.automation.itf.monitoring;
//
//import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_RUNNING_SCHEDULED_TASKS;
//import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.SCHEDULED_CLEANUP_DELAY_MINUTES;
//import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.SCHEDULED_CLEANUP_ENABLED;
//import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.SCHEDULED_CLEANUP_HOURS_TO_DELETE;
//import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.SCHEDULED_CLEANUP_INITIAL_DELAY_MINUTES;
//import static org.mockito.Matchers.any;
//import static org.mockito.Matchers.eq;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.math.BigInteger;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import com.hazelcast.config.MapConfig;
//import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.test.TestHazelcastInstanceFactory;
//import org.qubership.automation.itf.core.hibernate.ManagerFactory;
//import org.qubership.automation.itf.core.hibernate.spring.managers.StubProjectObjectManager;
//import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
//import org.qubership.automation.itf.core.model.jpa.project.StubProject;
//import org.qubership.automation.itf.core.util.config.Config;
//import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
//import org.qubership.automation.itf.executor.cache.service.RunningScheduledTasksCacheService;
//import org.qubership.automation.itf.monitoring.tasks.ScheduledGlobalTaskKey;
//import org.qubership.automation.itf.monitoring.tasks.ScheduledTaskSettings;
//import org.qubership.automation.itf.monitoring.tasks.ScheduledTaskType;
//import org.qubership.automation.itf.monitoring.tasks.service.DefaultScheduledGlobalTaskService;
//import org.qubership.automation.itf.monitoring.tasks.service.DefaultScheduledProjectTaskService;
//import org.qubership.automation.itf.monitoring.tasks.service.ScheduledTaskService;
//
//public class TCMonitoringWorkerTest {
//
//    private GlobalTaskService globalTaskService;
//    private ProjectTaskService projectTaskService;
//    private HazelcastInstance hazelcastClient;
//    private RunningScheduledTasksCacheService runningScheduledTasksCacheService;
//    private CountDownLatch countDownLatchForWait = new CountDownLatch(1);
//    private TestHazelcastInstanceFactory testHazelcastFactory = new TestHazelcastInstanceFactory();
//
//    @Mock
//    private ScheduledExecutorService scheduledExecutorService;
//
//    @Before
//    public void initMocks() {
//        MockitoAnnotations.initMocks(this);
//        globalTaskService = new GlobalTaskService(scheduledExecutorService, runningScheduledTasksCacheService);
//        projectTaskService = new ProjectTaskService(scheduledExecutorService, runningScheduledTasksCacheService);
//        hazelcastClient = testHazelcastFactory.newHazelcastInstance();
//        runningScheduledTasksCacheService = new RunningScheduledTasksCacheService(hazelcastClient);
//        hazelcastClient.getConfig().addMapConfig(new MapConfig().setName(ATP_ITF_RUNNING_SCHEDULED_TASKS));
//    }
//
//    @Test
//    public void startWorker_twoProjectTaskAndOneGlobalTask_allTasksWereScheduled() {
//        TCMonitoringWorker tcMonitoringWorker =
//                initTCMonitoringWorkerWithScheduledTaskServices(globalTaskService, projectTaskService);
//
//        BigInteger projectId1 = BigInteger.valueOf(123456789);
//        BigInteger projectId2 = BigInteger.valueOf(987654321);
//        configureObjectManagerForProjects(projectId1, projectId2);
//        long initialDelay1 = 5;
//        long initialDelay2 = 15;
//        long delay1 = 10;
//        long delay2 = 20;
//        long hoursToDelete = 1;
//        long globalTaskDelay = 30;
//        updateProjectSchedulingSettings(projectId1, true, initialDelay1, delay1, hoursToDelete);
//        updateProjectSchedulingSettings(projectId2, true, initialDelay2, delay2, hoursToDelete);
//
//        tcMonitoringWorker.startWorker();
//
//        verify(scheduledExecutorService).scheduleWithFixedDelay(any(), eq(globalTaskDelay), eq(globalTaskDelay),
//        any());
//        verify(scheduledExecutorService).scheduleWithFixedDelay(any(), eq(initialDelay1), eq(delay1), any());
//        verify(scheduledExecutorService).scheduleWithFixedDelay(any(), eq(initialDelay2), eq(delay2), any());
//    }
//
//    @Test
//    public void startWorker_twoProjectTasks_oneOfTasksWithDisabledScheduling_onlyOneTasksWereScheduled() {
//        TCMonitoringWorker tcMonitoringWorker = initTCMonitoringWorkerWithScheduledTaskServices(projectTaskService);
//
//        BigInteger projectId1 = BigInteger.valueOf(123456789);
//        BigInteger projectId2 = BigInteger.valueOf(987654321);
//        configureObjectManagerForProjects(projectId1, projectId2);
//        long initialDelay1 = 5;
//        long initialDelay2 = 15;
//        long delay1 = 10;
//        long delay2 = 20;
//        long hoursToDelete = 1;
//        updateProjectSchedulingSettings(projectId1, true, initialDelay1, delay1, hoursToDelete);
//        updateProjectSchedulingSettings(projectId2, false, initialDelay2, delay2, hoursToDelete);
//
//        tcMonitoringWorker.startWorker();
//
//        verify(scheduledExecutorService).scheduleWithFixedDelay(any(), eq(initialDelay1), eq(delay1), any());
//        verify(scheduledExecutorService, never()).scheduleWithFixedDelay(any(), eq(initialDelay2), eq(delay2), any());
//    }
//
//    @Test
//    public void startWorker_twoGlobalTaskWithTimeIntersection_bothTasksWereScheduledAndOnlyOneTaskWasExecuted()
//            throws InterruptedException {
//        // Mock for global task 1
//        ScheduledExecutorService scheduledExecutorService1 = Executors.newSingleThreadScheduledExecutor();
//        GlobalTaskService globalTaskService1 = spy(new GlobalTaskService(
//                scheduledExecutorService1, runningScheduledTasksCacheService));
//        when(globalTaskService1.createScheduledTaskSettings()).thenReturn(
//                getScheduledTaskSettingsWithDelay(1, 0, TimeUnit.SECONDS));
//
//        // Mock for global task 2
//        ScheduledExecutorService scheduledExecutorService2 = Executors.newSingleThreadScheduledExecutor();
//        GlobalTaskService globalTaskService2 = spy(new GlobalTaskService(
//                scheduledExecutorService2, runningScheduledTasksCacheService));
//        when(globalTaskService2.createScheduledTaskSettings()).thenReturn(
//                getScheduledTaskSettingsWithDelay(1, 1, TimeUnit.SECONDS));
//
//        TCMonitoringWorker tcMonitoringWorker =
//                initTCMonitoringWorkerWithScheduledTaskServices(globalTaskService1, globalTaskService2);
//
//        tcMonitoringWorker.startWorker();
//
//        countDownLatchForWait.await(3, TimeUnit.SECONDS);
//
//        verify(globalTaskService1).runTask();
//        verify(globalTaskService2, never()).runTask();
//    }
//
//    @Test
//    public void startWorker_twoProjectTaskWithTimeIntersection_bothTasksWereScheduledAndOnlyOneTaskWasExecuted()
//            throws InterruptedException {
//        BigInteger projectId = BigInteger.valueOf(123456789);
//
//        // Mock for global task 1
//        ScheduledExecutorService scheduledExecutorService1 = Executors.newSingleThreadScheduledExecutor();
//        ProjectTaskService projectTaskService1 = spy(new ProjectTaskService(
//                scheduledExecutorService1, runningScheduledTasksCacheService));
//        when(projectTaskService1.createScheduledTaskSettingsForProject(projectId)).thenReturn(
//                getScheduledTaskSettingsWithDelay(1, 0, TimeUnit.SECONDS));
//
//        // Mock for global task 2
//        ScheduledExecutorService scheduledExecutorService2 = Executors.newSingleThreadScheduledExecutor();
//        ProjectTaskService projectTaskService2 = spy(new ProjectTaskService(
//                scheduledExecutorService2, runningScheduledTasksCacheService));
//        when(projectTaskService2.createScheduledTaskSettingsForProject(projectId)).thenReturn(
//                getScheduledTaskSettingsWithDelay(1, 1, TimeUnit.SECONDS));
//
//        TCMonitoringWorker tcMonitoringWorker =
//                initTCMonitoringWorkerWithScheduledTaskServices(projectTaskService1, projectTaskService2);
//
//        configureObjectManagerForProjects(projectId);
//        long initialDelay = 5;
//        long delay = 10;
//        long hoursToDelete = 1;
//        updateProjectSchedulingSettings(projectId, true, initialDelay, delay, hoursToDelete);
//
//        tcMonitoringWorker.startWorker();
//
//        countDownLatchForWait.await(3, TimeUnit.SECONDS);
//
//        verify(projectTaskService1).runTaskForProject(projectId);
//        verify(projectTaskService2, never()).runTaskForProject(projectId);
//    }
//
//    @Test
//    public void startWorker_oneGlobalTask_runningTaskWasCorrectlyEvictedFromCache() throws InterruptedException {
//        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
//        GlobalTaskService globalTaskService = spy(new GlobalTaskService(
//                scheduledExecutorService, runningScheduledTasksCacheService));
//        when(globalTaskService.createScheduledTaskSettings()).thenReturn(
//                getScheduledTaskSettingsWithDelay(3600, 0, TimeUnit.SECONDS));
//        doAnswer(invocation -> {
//            Thread.sleep(500);
//            return null;
//        }).when(globalTaskService).runTask();
//        TCMonitoringWorker tcMonitoringWorker = initTCMonitoringWorkerWithScheduledTaskServices(globalTaskService);
//
//        tcMonitoringWorker.startWorker();
//
//        countDownLatchForWait.await(3, TimeUnit.SECONDS);
//
//        verify(globalTaskService).runTask();
//        String removedKey = new ScheduledGlobalTaskKey(globalTaskService.getTaskType()).getKeyRepresentation();
//        Assert.assertFalse(hazelcastClient.getMap(ATP_ITF_RUNNING_SCHEDULED_TASKS).containsKey(removedKey));
//    }
//
//    @Test
//    public void stop_scheduledExecutorCorrectlyShutdowns() {
//        TCMonitoringWorker tcMonitoringWorker =
//                initTCMonitoringWorkerWithScheduledTaskServices(globalTaskService);
//
//        tcMonitoringWorker.stop();
//
//        verify(scheduledExecutorService).shutdown();
//    }
//
//    private TCMonitoringWorker initTCMonitoringWorkerWithScheduledTaskServices(ScheduledTaskService... services) {
//        return new TCMonitoringWorker(scheduledExecutorService, Arrays.asList(services));
//    }
//
//    private void configureObjectManagerForProjects(BigInteger... projectIds) {
//        ManagerFactory managerFactory = mock(ManagerFactory.class);
//        ObjectManager<StubProject> projectManager = mock(StubProjectObjectManager.class);
//        List<StubProject> projects = Arrays.stream(projectIds)
//                .map(projectId -> {
//                    StubProject project = new StubProject();
//                    project.setID(projectId);
//                    project.setName("TestProject" + projectId);
//                    return project;
//                }).collect(Collectors.toList());
//        when(projectManager.getAll()).thenAnswer(invocation -> projects);
//        when(managerFactory.getManager(StubProject.class)).thenReturn(projectManager);
//        CoreObjectManager.setManagerFactory(managerFactory);
//    }
//
//    private void updateProjectSchedulingSettings(BigInteger projectId, boolean isSchedulingEnabled,
//                                                 long initialDelay, long delay, long hoursToDelete) {
//        Map<String, String> projectSettings = new HashMap<>();
//        projectSettings.put(SCHEDULED_CLEANUP_ENABLED, String.valueOf(isSchedulingEnabled));
//        projectSettings.put(SCHEDULED_CLEANUP_DELAY_MINUTES, String.valueOf(delay));
//        projectSettings.put(SCHEDULED_CLEANUP_INITIAL_DELAY_MINUTES, String.valueOf(initialDelay));
//        projectSettings.put(SCHEDULED_CLEANUP_HOURS_TO_DELETE, String.valueOf(hoursToDelete));
//    }
//
//    private ScheduledTaskSettings getScheduledTaskSettingsWithDelay(long delay, long initialDelay, TimeUnit
//    timeUnit) {
//        ScheduledTaskSettings scheduledTaskSettings = new ScheduledTaskSettings();
//        scheduledTaskSettings.setSchedulingPossible(true);
//        scheduledTaskSettings.setDelay(delay);
//        scheduledTaskSettings.setInitialDelay(initialDelay);
//        scheduledTaskSettings.setDelayTimeUnit(timeUnit);
//        return scheduledTaskSettings;
//    }
//
//    private class ProjectTaskService extends DefaultScheduledProjectTaskService {
//
//        protected ProjectTaskService(ScheduledExecutorService scheduledExecutor,
//                                     RunningScheduledTasksCacheService runningScheduledTasksCacheService) {
//            super(scheduledExecutor, runningScheduledTasksCacheService);
//        }
//
//        @Override
//        protected void runTaskForProject(BigInteger projectId) {
//            try {
//                Thread.sleep(500000000);
//            } catch (InterruptedException e) {
//                // only for test use case
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        protected ScheduledTaskSettings createScheduledTaskSettingsForProject(BigInteger projectId) {
//            ScheduledTaskSettings scheduledTaskSettings = new ScheduledTaskSettings();
//            scheduledTaskSettings.setSchedulingPossible(isScheduledCleanupEnabled(projectId));
//            scheduledTaskSettings.setDelay(Config.getConfig().getInt(projectId, SCHEDULED_CLEANUP_DELAY_MINUTES));
//            scheduledTaskSettings.setInitialDelay(Config.getConfig()
//                    .getInt(projectId, SCHEDULED_CLEANUP_INITIAL_DELAY_MINUTES));
//            scheduledTaskSettings.setDelayTimeUnit(TimeUnit.MINUTES);
//            return scheduledTaskSettings;
//        }
//
//        @Override
//        public ScheduledTaskType getTaskType() {
//            return ScheduledTaskType.DEFAULT_PROJECT_TASK;
//        }
//
//        private boolean isScheduledCleanupEnabled(BigInteger projectId) {
//            return Boolean.parseBoolean(Config.getConfig().getString(projectId, SCHEDULED_CLEANUP_ENABLED));
//        }
//    }
//
//    private class GlobalTaskService extends DefaultScheduledGlobalTaskService {
//
//        protected GlobalTaskService(ScheduledExecutorService scheduledExecutor,
//                                    RunningScheduledTasksCacheService runningScheduledTasksCacheService) {
//            super(scheduledExecutor, runningScheduledTasksCacheService);
//        }
//
//        @Override
//        protected void runTask() {
//            try {
//                Thread.sleep(500000000);
//            } catch (InterruptedException e) {
//                // only for test use case
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        protected ScheduledTaskSettings createScheduledTaskSettings() {
//            ScheduledTaskSettings scheduledTaskSettings = new ScheduledTaskSettings();
//            scheduledTaskSettings.setSchedulingPossible(true);
//            scheduledTaskSettings.setDelay(30L);
//            scheduledTaskSettings.setInitialDelay(30L);
//            scheduledTaskSettings.setDelayTimeUnit(TimeUnit.SECONDS);
//            return scheduledTaskSettings;
//        }
//
//        @Override
//        public ScheduledTaskType getTaskType() {
//            return ScheduledTaskType.DEFAULT_GLOBAL_TASK;
//        }
//    }
//}
