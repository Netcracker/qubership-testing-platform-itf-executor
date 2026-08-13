package org.qubership.automation.itf.integration.atp.exector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.environments.openapi.dto.EnvironmentFullVer1ViewDto;
import org.qubership.automation.itf.core.instance.testcase.chain.CallChainExecutorService;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.execution.data.CallchainExecutionData;
import org.qubership.automation.itf.execution.manager.CallChainExecutorManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.cache.service.impl.EnvironmentCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.TCContextCacheService;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.integration.atp.util.CallchainRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsEnvironmentFeignClient;
import org.qubership.automation.itf.report.extension.TCContextRamExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AtpCallchainExecutor Tests")
class AtpCallchainExecutorTest {

    @Mock
    private CallChainExecutorManager callChainExecutorManager;

    @Mock
    private AtpEnvironmentsEnvironmentFeignClient atpEnvironmentsEnvironmentFeignClient;

    @Mock
    private ProjectSettingsService projectSettingsService;

    @InjectMocks
    private AtpCallchainExecutor atpCallchainExecutor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(atpCallchainExecutor, "contextStartMaxTimeMillis", 40000);
        ReflectionTestUtils.setField(atpCallchainExecutor, "contextFromAtpFinishMaxTimeMillis", 1800000);
        ReflectionTestUtils.setField(atpCallchainExecutor, "contextCheckIntervalMillis", 10000);
    }

    // ==================== Tests for createTCContextExtension ====================

    @Test
    @DisplayName("Should successfully create TCContextRamExtension with all fields populated")
    void testCreateTCContextExtension_ShouldCreateExtensionWithAllFields() throws Exception {
        // Given
        BigInteger testRunId = BigInteger.valueOf(12345L);
        String atpRamUrl = "https://atp-ram.example.com";
        TestRunContext ramContext = mock(TestRunContext.class);

        TestRunInfo testRunInfo = mock(TestRunInfo.class);
        when(testRunInfo.getTestRunId()).thenReturn(testRunId);
        when(testRunInfo.getProject()).thenReturn("TestProject");
        when(testRunInfo.getLogRecordId()).thenReturn(BigInteger.valueOf(999L));
        when(testRunInfo.getRamTestRunContext()).thenReturn(ramContext);
        when(testRunInfo.getStartedFrom()).thenReturn(StartedFrom.RAM2);
        when(testRunInfo.getAtpRamUrl()).thenReturn(atpRamUrl);

        // When
        java.lang.reflect.Method method = AtpCallchainExecutor.class
                .getDeclaredMethod("createTCContextExtension", TestRunInfo.class);
        method.setAccessible(true);

        TCContextRamExtension extension = (TCContextRamExtension) method.invoke(
                atpCallchainExecutor, testRunInfo);

        // Then
        assertNotNull(extension);
        assertEquals(testRunId, extension.getRunId());
        assertEquals("TestProject", extension.getProjectName());
        assertEquals(BigInteger.valueOf(999L), extension.getSectionId());
        assertTrue(extension.getExternalRun());
        assertEquals(ramContext, extension.getRunContext());
        assertEquals(StartedFrom.RAM2, extension.getStartedFrom());
        assertTrue(extension.getReportUrl().contains(testRunId.toString()));
        assertTrue(extension.getReportUrl().contains("_Test+Run+Tree+View"));
        assertEquals("ATP server url: " + atpRamUrl, extension.getExternalAppName());
    }

    @Test
    @DisplayName("Should create TCContextRamExtension without report URL when testRunId is null")
    void testCreateTCContextExtension_WithNullTestRunId_ShouldNotSetReportUrl() throws Exception {
        // Given
        TestRunInfo testRunInfo = mock(TestRunInfo.class);
        when(testRunInfo.getTestRunId()).thenReturn(null);
        when(testRunInfo.getProject()).thenReturn("TestProject");
        when(testRunInfo.getLogRecordId()).thenReturn(BigInteger.valueOf(999L));
        when(testRunInfo.getRamTestRunContext()).thenReturn(mock(TestRunContext.class));
        when(testRunInfo.getStartedFrom()).thenReturn(StartedFrom.RAM2);
        when(testRunInfo.getAtpRamUrl()).thenReturn(null);

        // When
        java.lang.reflect.Method method = AtpCallchainExecutor.class
                .getDeclaredMethod("createTCContextExtension", TestRunInfo.class);
        method.setAccessible(true);

        TCContextRamExtension extension = (TCContextRamExtension) method.invoke(
                atpCallchainExecutor, testRunInfo);

        // Then
        assertNotNull(extension);
        assertNull(extension.getReportUrl());
        assertEquals("ATP server url: null", extension.getExternalAppName());
    }

    @Test
    @DisplayName("Should handle null RamTestRunContext gracefully")
    void testCreateTCContextExtension_WithNullRamTestRunContext_ShouldSetNull() throws Exception {
        // Given
        TestRunInfo testRunInfo = mock(TestRunInfo.class);
        when(testRunInfo.getTestRunId()).thenReturn(BigInteger.valueOf(12345L));
        when(testRunInfo.getProject()).thenReturn("TestProject");
        when(testRunInfo.getLogRecordId()).thenReturn(BigInteger.valueOf(999L));
        when(testRunInfo.getRamTestRunContext()).thenReturn(null);
        when(testRunInfo.getStartedFrom()).thenReturn(StartedFrom.RAM2);
        when(testRunInfo.getAtpRamUrl()).thenReturn("https://atp-ram.example.com");

        // When
        java.lang.reflect.Method method = AtpCallchainExecutor.class
                .getDeclaredMethod("createTCContextExtension", TestRunInfo.class);
        method.setAccessible(true);

        TCContextRamExtension extension = (TCContextRamExtension) method.invoke(
                atpCallchainExecutor, testRunInfo);

        // Then
        assertNotNull(extension);
        assertNull(extension.getRunContext());
    }

    // ==================== Tests for execute ====================

    @Test
    @DisplayName("Should execute callchain and return TcContext")
    void testExecute_ShouldExecuteSuccessfully() throws Exception {
        // Given
        CallChain callChain = mock(CallChain.class);
        CallchainRunInfo callchainRunInfo = new CallchainRunInfo(callChain, null);
        Environment environment = mock(Environment.class);

        TestRunInfo testRunInfo = mock(TestRunInfo.class);
        BigInteger testRunId = BigInteger.valueOf(12345L);
        when(testRunInfo.getTestRunId()).thenReturn(testRunId);
        when(testRunInfo.getProject()).thenReturn("TestProject");
        when(testRunInfo.getEnvironment()).thenReturn(environment);
        when(environment.getID()).thenReturn(BigInteger.valueOf(123));
        when(testRunInfo.getAtpEnvironmentId()).thenReturn(UUID.randomUUID());
        when(testRunInfo.getContextToMerge()).thenReturn(new JsonContext());
        when(testRunInfo.getProjectUuid()).thenReturn(UUID.randomUUID());
        when(testRunInfo.getStartedFrom()).thenReturn(StartedFrom.RAM2);
        when(testRunInfo.getLogRecordId()).thenReturn(BigInteger.valueOf(999L));
        when(testRunInfo.getRamTestRunContext()).thenReturn(mock(TestRunContext.class));
        when(testRunInfo.getAtpRamUrl()).thenReturn("https://atp-ram.example.com");

        CallChainInstance instance = mock(CallChainInstance.class);
        TcContext tcContext = mock(TcContext.class);
        when(tcContext.getID()).thenReturn(BigInteger.valueOf(123L));
        when(tcContext.isFinished()).thenReturn(true);
        when(tcContext.getStatus()).thenReturn(Status.PASSED);

        InstanceContext instanceContext = new InstanceContext();
        instanceContext.setTC(tcContext);
        when(instance.getContext()).thenReturn(instanceContext);

        when(callChainExecutorManager.prepare(any(CallchainExecutionData.class), eq(true)))
                .thenReturn(instance);

        ProjectSettingsService projectSettingsService = mock(ProjectSettingsService.class);
        when(projectSettingsService.get(any(), any())).thenReturn("20");
        when(projectSettingsService.get(any(), any(), any())).thenReturn("30");
        when(projectSettingsService.get(any(), eq(TCPDUMP_PACKET_COUNT_DEFAULT), any())).thenReturn("10");

        CallChainExecutorService executorService = mock(CallChainExecutorService.class);

        TCContextCacheService tcContextCacheService = mock(TCContextCacheService.class);

        Map<String, Map<String, String>> envMap = new HashMap<>();
        EnvironmentCacheService environmentCacheService = mock(EnvironmentCacheService.class);
        when(environmentCacheService.get(any())).thenReturn(envMap);

        try (var mockedServices = mockStatic(ExecutionServices.class);
             var mockedCoreServices = mockStatic(CoreServices.class);
             var mockedCacheServices = mockStatic(CacheServices.class)) {
            mockedServices.when(ExecutionServices::getCallChainExecutorService)
                    .thenReturn(executorService);
            mockedCoreServices.when(CoreServices::getProjectSettingsService)
                    .thenReturn(projectSettingsService);
            mockedCacheServices.when(CacheServices::getEnvironmentCacheService)
                    .thenReturn(environmentCacheService);
            mockedCacheServices.when(CacheServices::getTcContextCacheService)
                    .thenReturn(tcContextCacheService);

            // When
            TcContext result = atpCallchainExecutor.execute(callchainRunInfo, testRunInfo);

            // Then
            assertNotNull(result);
            assertEquals(tcContext, result);
            verify(callChainExecutorManager).prepare(any(CallchainExecutionData.class), eq(true));
            verify(executorService).executeInstance(instance);
        }
    }

    @Test
    @DisplayName("Should handle exception when callChainExecutorManager.prepare throws")
    void testExecute_WhenPrepareThrows_ShouldPropagateException() throws Exception {
        // Given
        CallChain callChain = mock(CallChain.class);
        CallchainRunInfo callchainRunInfo = new CallchainRunInfo(callChain, null);

        TestRunInfo testRunInfo = mock(TestRunInfo.class);
        when(testRunInfo.getAtpEnvironmentId()).thenReturn(UUID.randomUUID());

        when(callChainExecutorManager.prepare(any(CallchainExecutionData.class), eq(true)))
                .thenThrow(new RuntimeException("Prepare failed"));

        // When & Then
        assertThrows(Exception.class,
                () -> atpCallchainExecutor.execute(callchainRunInfo, testRunInfo));
    }

    // ==================== Tests for syncAtpEnvironmentInfo ====================

    @Test
    @DisplayName("Should sync ATP environment info from cache when available")
    void testSyncAtpEnvironmentInfo_FromCache_ShouldReturnCached() throws Exception {
        // Given
        UUID envId = UUID.randomUUID();
        Map<String, Map<?, ?>> cachedEnv = new HashMap<>();
        cachedEnv.put("system1", new HashMap<>());

        try (var mockedCache = mockStatic(CacheServices.class)) {
            var cacheService = mock(EnvironmentCacheService.class);
            mockedCache.when(CacheServices::getEnvironmentCacheService)
                    .thenReturn(cacheService);
            when(cacheService.get(envId)).thenReturn(cachedEnv);

            // When
            java.lang.reflect.Method method = AtpCallchainExecutor.class
                    .getDeclaredMethod("syncAtpEnvironmentInfo", UUID.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Map<?, ?>> result = (Map<String, Map<?, ?>>) method.invoke(
                    atpCallchainExecutor, envId);

            // Then
            assertSame(cachedEnv, result);
            verify(cacheService).get(envId);
            verify(atpEnvironmentsEnvironmentFeignClient, never()).getEnvironment(any(), anyBoolean());
        }
    }

    @Test
    @DisplayName("Should sync ATP environment info from remote when not in cache")
    void testSyncAtpEnvironmentInfo_FromRemote_ShouldFetchAndCache() throws Exception {
        // Given
        UUID envId = UUID.randomUUID();
        EnvironmentFullVer1ViewDto environment = mock(EnvironmentFullVer1ViewDto.class);
        when(environment.getId()).thenReturn(envId);

        var response = mock(ResponseEntity.class);
        when(response.getBody()).thenReturn(environment);
        when(atpEnvironmentsEnvironmentFeignClient.getEnvironment(eq(envId), eq(true)))
                .thenReturn(response);

        try (var mockedCache = mockStatic(CacheServices.class)) {
            var cacheService = mock(EnvironmentCacheService.class);
            mockedCache.when(CacheServices::getEnvironmentCacheService)
                    .thenReturn(cacheService);
            when(cacheService.get(envId)).thenReturn(null);

            // When
            java.lang.reflect.Method method = AtpCallchainExecutor.class
                    .getDeclaredMethod("syncAtpEnvironmentInfo", UUID.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Map<?, ?>> result = (Map<String, Map<?, ?>>) method.invoke(
                    atpCallchainExecutor, envId);

            // Then
            assertNotNull(result);
            verify(atpEnvironmentsEnvironmentFeignClient).getEnvironment(eq(envId), eq(true));
            verify(cacheService).set(eq(envId), any());
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when environment not found")
    void testSyncAtpEnvironmentInfo_WhenEnvironmentNotFound_ShouldThrow() throws Exception {
        // Given
        UUID envId = UUID.randomUUID();

        var response = mock(ResponseEntity.class);
        when(response.getBody()).thenReturn(null);
        when(atpEnvironmentsEnvironmentFeignClient.getEnvironment(eq(envId), eq(true)))
                .thenReturn(response);

        try (var mockedCache = mockStatic(CacheServices.class)) {
            var cacheService = mock(EnvironmentCacheService.class);
            mockedCache.when(CacheServices::getEnvironmentCacheService)
                    .thenReturn(cacheService);
            when(cacheService.get(envId)).thenReturn(null);

            // When & Then
            java.lang.reflect.Method method = AtpCallchainExecutor.class
                    .getDeclaredMethod("syncAtpEnvironmentInfo", UUID.class);
            method.setAccessible(true);

            InvocationTargetException exception = assertThrows(
                    InvocationTargetException.class,
                    () -> method.invoke(atpCallchainExecutor, envId)
            );

            // Check that cause is IllegalArgumentException
            Throwable cause = exception.getCause();
            assertInstanceOf(IllegalArgumentException.class, cause);
            assertTrue(cause.getMessage().contains("Cannot find ATP Environment"));
        }
    }

    // ==================== Integration Test ====================

    @Test
    @DisplayName("Integration: createTCContextExtension should work with real ExtensionManager (no CGLIB errors)")
    void testCreateTCContextExtension_WithRealExtensionManager_ShouldNotThrowCglibException() throws Exception {
        // Given
        TestRunInfo testRunInfo = mock(TestRunInfo.class);
        BigInteger testRunId = BigInteger.valueOf(99999L);
        when(testRunInfo.getTestRunId()).thenReturn(testRunId);
        when(testRunInfo.getProject()).thenReturn("IntegrationTest");
        when(testRunInfo.getLogRecordId()).thenReturn(BigInteger.valueOf(888L));
        when(testRunInfo.getRamTestRunContext()).thenReturn(mock(TestRunContext.class));
        when(testRunInfo.getStartedFrom()).thenReturn(StartedFrom.RAM2);
        when(testRunInfo.getAtpRamUrl()).thenReturn("https://integration-atp.example.com");

        // When
        java.lang.reflect.Method method = AtpCallchainExecutor.class
                .getDeclaredMethod("createTCContextExtension", TestRunInfo.class);
        method.setAccessible(true);

        // That invocation should be completed w/o ExceptionInInitializerError
        TCContextRamExtension extension = (TCContextRamExtension) method.invoke(
                atpCallchainExecutor, testRunInfo);

        // Then
        assertNotNull(extension);
        assertEquals(testRunId, extension.getRunId());
        assertEquals("IntegrationTest", extension.getProjectName());
        assertTrue(extension.getExternalRun());
    }
}