/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.executor.context;

import static org.qubership.automation.itf.core.util.constants.Status.IN_PROGRESS;
import static org.qubership.automation.itf.core.util.constants.Status.PASSED;
import static org.qubership.automation.itf.core.util.constants.Status.STOPPED;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.automation.itf.core.instance.situation.SituationExecutorService;
import org.qubership.automation.itf.core.instance.step.StepExecutorFactory;
import org.qubership.automation.itf.core.instance.testcase.chain.CallChainExecutorService;
import org.qubership.automation.itf.core.instance.testcase.execution.ExecutionProcessManagerService;
import org.qubership.automation.itf.core.metric.MetricsAggregateService;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.executor.cache.hazelcast.HazelcastAsyncExecutor;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.cache.service.impl.AwaitingContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.BoundContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.CallchainSubscriberCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.EnvironmentCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.PendingDataContextsCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.ResponseCacheService;
import org.qubership.automation.itf.executor.cache.service.impl.TCContextCacheService;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.hazelcast.core.HazelcastInstance;

@SpringJUnitConfig(classes = {
        TCContextServiceTest.TestConfig.class,
        TCContextService.class,
        CallChainExecutorService.class,
        SituationExecutorService.class,
        ExecutionProcessManagerService.class,
        ExecutionServices.class,
        CacheServices.class})
@TestPropertySource(properties = {"hazelcast.cache.enabled=false",
        "hazelcast.project-settings.cache.refill.time.seconds=3600",
        "exclude.registry.metrics.tags={defaultKey: {'defaultValue1','defaultValue2'}}",
        "atp.multi-tenancy.enabled=false"})
public class TCContextServiceTest {

    @Configuration
    static class TestConfig {

        @Bean
        @Primary
        public TCContextCacheService mockTCContextCacheService() {
            return Mockito.mock(TCContextCacheService.class);
        }

        // Other cache services mocks - start

        @Bean
        @Primary
        public BoundContextsCacheService mockBoundContextsCacheService() {
            return Mockito.mock(BoundContextsCacheService.class);
        }

        @Bean
        @Primary
        public PendingDataContextsCacheService mockPendingDataContextsCacheService() {
            return Mockito.mock(PendingDataContextsCacheService.class);
        }

        @Bean
        @Primary
        public AwaitingContextsCacheService mockAwaitingContextsCacheService() {
            return Mockito.mock(AwaitingContextsCacheService.class);
        }

        @Bean
        @Primary
        public CallchainSubscriberCacheService mockCallchainSubscriberCacheService() {
            return Mockito.mock(CallchainSubscriberCacheService.class);
        }

        @Bean
        @Primary
        public EnvironmentCacheService mockEnvironmentCacheService() {
            return Mockito.mock(EnvironmentCacheService.class);
        }

        @Bean
        @Primary
        public ResponseCacheService mockResponseCacheService() {
            return Mockito.mock(ResponseCacheService.class);
        }

        // Other cache services mocks - end

        @Bean
        @Qualifier("hazelcastClient")
        public HazelcastInstance mockHazelcastInstance() {
            return Mockito.mock(HazelcastInstance.class);
        }

        @Bean
        @Primary
        public ExecutorToMessageBrokerSender mockExecutorToMessageBrokerSender() {
            return Mockito.mock(ExecutorToMessageBrokerSender.class);
        }

        @Bean
        @Primary
        public ProjectSettingsService mockProjectSettingsService() {
            return Mockito.mock(ProjectSettingsService.class);
        }

        @Bean
        @Primary
        public EventBusProvider mockEventBusProvider() {
            return Mockito.mock(EventBusProvider.class);
        }

        @Bean
        @Primary
        public HazelcastAsyncExecutor mockHazelcastAsyncExecutor() {
            return Mockito.mock(HazelcastAsyncExecutor.class);
        }

        @Bean
        @Primary
        public ReportLinkCollector mockReportLinkCollector() {
            return Mockito.mock(ReportLinkCollector.class);
        }

        @Bean
        @Primary
        public MetricsAggregateService mockMetricsAggregateService() {
            return Mockito.mock(MetricsAggregateService.class);
        }

        @Bean
        @Primary
        public TemplateEngine mockTemplateEngine() {
            return Mockito.mock(TemplateEngine.class);
        }

        @Bean
        @Primary
        public StepExecutorFactory mockStepExecutorFactory() {
            return Mockito.mock(StepExecutorFactory.class);
        }

    }

    TcContext tcContext;

    @BeforeEach
    public void createTCContext() {
        tcContext = new TcContext();
        tcContext.setID(new BigInteger("11111111111111111111111111111111"));
    }

    @Test
    public void startTest() {
        ExecutionServices.getTCContextService().start(tcContext);
        Assertions.assertEquals(IN_PROGRESS, tcContext.getStatus());
    }

    @Test
    public void startWithWrongStatusTest() {
        ExecutionServices.getTCContextService().start(tcContext);
        tcContext.setStatus(PASSED);
        Assertions.assertNotEquals(IN_PROGRESS, tcContext.getStatus());
    }

    /*
        'stopTest' is disabled by KAG, because .stop() method works via sending special message over all
        executor nodes. This functionality is not initialized before this test, and its testing is out of
        the scope here.
     */
    @Disabled
    @Test
    public void stopTest() {
        ExecutionServices.getTCContextService().stop(tcContext);
        Assertions.assertEquals(STOPPED, tcContext.getStatus());
        Assertions.assertNotEquals("", tcContext.getEndTime().toString());
    }

    @Test
    public void finishTest() {
        configureObjectManager();
        tcContext.setStatus(IN_PROGRESS);
        ExecutionServices.getTCContextService().finish(tcContext);
        Assertions.assertEquals(PASSED, tcContext.getStatus());
        Assertions.assertNotEquals("", tcContext.getEndTime().toString());
    }

    private void configureObjectManager() {
//        ManagerFactory managerFactory = mock(ManagerFactory.class);
//        ObjectManager<TcContext> tcContextOM = mock(TcContextObjectManager.class);
//        when(managerFactory.getManager(TcContext.class)).thenReturn(tcContextOM);
//        CoreObjectManager.setManagerFactory(managerFactory);
    }

    @Test
    public void setMessageParameterTest() {
        Map<String, MessageParameter> messageParameters = createMessageParameters();
        ExecutionServices.getTCContextService().setMessageParameters(tcContext, messageParameters);
        Assertions.assertTrue(tcContext.containsKey("saved"));
        JsonContext saved = (JsonContext) tcContext.get("saved");
        Assertions.assertTrue(saved.containsKey("111"));
    }

    private Map<String, MessageParameter> createMessageParameters() {
        MessageParameter.Builder build = MessageParameter.build("111", new SystemParsingRule());
        MessageParameter messageParameter = build.get();
        messageParameter.setAutosave(true);
        List<String> stringList = new ArrayList<>();
        stringList.add("firstMessageParameter");
        messageParameter.setMultipleValue(stringList);
        messageParameter.setMultiple(true);
        Map<String, MessageParameter> map = new HashMap<>();
        map.put("parameters", messageParameter);
        return map;
    }
}
