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

package org.qubership.automation.itf.integration.atp.exector;

import static org.qubership.automation.itf.Constants.ENV_INFO_KEY;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.environments.openapi.dto.EnvironmentFullVer1ViewDto;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.exception.ExtensionException;
import org.qubership.automation.itf.core.util.manager.ExtensionManager;
import org.qubership.automation.itf.core.util.manager.MonitorManager;
import org.qubership.automation.itf.execution.data.CallchainExecutionData;
import org.qubership.automation.itf.execution.manager.CallChainExecutorManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.qubership.automation.itf.integration.atp.util.CallchainRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.environments.AtpEnvironmentsEnvironmentFeignClient;
import org.qubership.automation.itf.report.extension.TCContextRamExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtpCallchainExecutor {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final CallChainExecutorManager callChainExecutorManager;
    private final AtpEnvironmentsEnvironmentFeignClient atpEnvironmentsEnvironmentFeignClient;
    private final ProjectSettingsService projectSettingsService;
    @Value("${executor.context.start.max.time}")
    private int contextStartMaxTimeMillis; // 40000 ms = 40 sec default
    @Value("${executor.context.from.atp.finish.max.time}")
    private int contextFromAtpFinishMaxTimeMillis; // 1800000 ms = 30 minutes default (set on the orch side)
    @Value("${executor.context.check.interval}")
    private int contextCheckIntervalMillis; // 10000 ms = 10 sec default

    /**
     * The method executes CallChain on the received data.
     *
     * @param callchainRunInfo object containing information about callChain run
     * @param testRunInfo      object containing information about test run
     * @return TcContext object containing information about the result of execution callChain
     */
    public TcContext execute(CallchainRunInfo callchainRunInfo, TestRunInfo testRunInfo) throws Exception {
        CallchainExecutionData data = new CallchainExecutionData(callchainRunInfo.getCallChain(),
                testRunInfo.getEnvironment(), callchainRunInfo.getDataset(), testRunInfo.getContextToMerge(),
                StringUtils.isNotEmpty(testRunInfo.getBvAction()), testRunInfo.getBvAction(),
                testRunInfo.getTcpDumpOptions(),
                testRunInfo.isValidateMessageOnStep(),
                testRunInfo.getProjectId(),
                testRunInfo.getProjectUuid());
        CallChainInstance instance = callChainExecutorManager.prepare(data, true);
        TcContext tcContext = instance.getContext().tc();
        ExtensionManager.getInstance().extend(tcContext, createTCContextExtension(testRunInfo));
        tcContext.setStartedFrom(testRunInfo.getStartedFrom());
        tcContext.setPartNum(TCContextService.getCurrentPartitionNumberByProject(testRunInfo.getProjectUuid()));
        //noinspection unchecked
        tcContext.putIfAbsent("DATASET_NAME", data.getDatasetName());
        Map<String, Map<?, ?>> simpleEnvironmentInfo = syncAtpEnvironmentInfo(testRunInfo.getAtpEnvironmentId());
        instance.getContext().put(ENV_INFO_KEY, simpleEnvironmentInfo);
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldThreadName + "/run-from-" + testRunInfo.getStartedFrom()
                + "/[" + tcContext.getID() + "]"); // to simplify blocked threads identification
        ExecutionServices.getCallChainExecutorService().executeInstance(instance);
        waitExecutionCompletion(instance, tcContext);
        Thread.currentThread().setName(oldThreadName);
        return tcContext;
    }

    private void waitExecutionCompletion(CallChainInstance instance, TcContext tcContext) throws InterruptedException {
        /*
            We should wait more accurately:
                - One should take into account, that:
                    - TcContext can be executed very quickly, before we even start waiting,
                    - There can be error during startup ==> TcContext won't be started,
                    - There can be long waiting in the queue before one of regular pool threads become free,
                    - There can be normal execution and completion ==> notification will be sent,
                    - There can be termination by timeout ==> notification will be sent too,
                    - There can be abnormal breaking of execution ==> we should not wait infinitely.
         */
        long elapsed = 0L;
        if (!tcContext.isFinished()) {
            Object syncObject = MonitorManager.getInstance().get(tcContext.getID().toString());
            synchronized (syncObject) {
                long safetyDelta = 5000L;
                while (!tcContext.isFinished()) {
                    syncObject.wait(contextCheckIntervalMillis);
                    log.debug("Execution from ATP2 loop: one more wait {} ms is completed", contextCheckIntervalMillis);
                    elapsed += contextCheckIntervalMillis;
                    /*
                        Break execution in case 'tc.timeout.fail' project setting is exceeded,
                        but normal termination by timeout was not processed properly due to some reason.
                        So, we should stop waiting after this time. And terminate execution too, if needed.
                     */
                    if (tcContext.getLastUpdateTime() > 0 && System.currentTimeMillis()
                            > (tcContext.getLastUpdateTime() + tcContext.getTimeToLive() + safetyDelta)) {
                        log.error("Execution from ATP2 is broken due to context fail timeout is exceeded, {} elapsed",
                                elapsed);
                        ExecutionServices.getCallChainExecutorService().reportErrorThenStop(instance,
                                tcContext,
                                "Too long execution of CallChain",
                                "Execution is terminated by timeout",
                                "Last event in the context was at "
                                        + SIMPLE_DATE_FORMAT.format(tcContext.getLastUpdateTime())
                                        + " but 'Context Timeout Fail' setting is " + tcContext.getTimeToLive(),
                                null);
                        break;
                    }
                    /*
                        It's sense-less to wait further, because execution is still not started (after max time)
                        So, we should stop waiting after this time. And terminate execution too, if needed.
                        ... The 1st limitation was, it seemed, too strict:
                        (tcContext.getLastUpdateTime() == 0 || Status.NOT_STARTED.equals(tcContext.getStatus()))
                        So, tcContext.getLastUpdateTime() == 0
                            ==> updateLastAccess() method was not invoked even once,
                            ==> no one situation was executed in the context.
                        So, it was correct checking, but, might be, too strict as for now.
                        So, I simplify it, and change it to:
                            Status.NOT_STARTED.equals(tcContext.getStatus())
                     */
                    if (elapsed >= contextStartMaxTimeMillis && Status.NOT_STARTED.equals(tcContext.getStatus())) {
                        log.error("Execution from ATP2 is broken due to still not started, {} elapsed", elapsed);
                        ExecutionServices.getCallChainExecutorService().reportErrorThenStop(instance, tcContext,
                                "Too long start of CallChain",
                                "Execution is not started in time",
                                "Context is not started yet, after " + elapsed + " ms",
                                null);
                        break;
                    }
                    /*
                        It's senseless to wait longer than How much time executor waits for action completion.
                        So, we should stop waiting after this time. And terminate execution too.
                     */
                    if (elapsed >= contextFromAtpFinishMaxTimeMillis) {
                        log.error("Execution from ATP2 is broken after {} millis, {} elapsed",
                                contextFromAtpFinishMaxTimeMillis, elapsed);
                        ExecutionServices.getCallChainExecutorService().reportErrorThenStop(instance,
                                tcContext,
                                "Too long execution of CallChain",
                                "Execution is still not completed",
                                "Context is not completed yet, after " + elapsed + " ms",
                                null);
                        break;
                    }
                }
            }
        }
        log.debug("Execution from ATP2 is finished, status: {}, elapsed: {}", tcContext.getStatus(), elapsed);
        tcContext.setNotified(true);
        CacheServices.getTcContextCacheService().evict(tcContext);
    }

    private TCContextRamExtension createTCContextExtension(TestRunInfo testRunInfo) throws ExtensionException {
        TCContextRamExtension extension = ExtensionManager.getInstance().createExtendable(TCContextRamExtension.class);
        extension.setRunId(testRunInfo.getTestRunId());
        extension.setProjectName(testRunInfo.getProject());
        extension.setSectionId(testRunInfo.getLogRecordId());
        extension.setExternalRun(true);
        extension.setRunContext(testRunInfo.getRamTestRunContext());
        extension.setStartedFrom(testRunInfo.getStartedFrom());
        if (testRunInfo.getTestRunId() != null && testRunInfo.getAtpRamUrl() != null) {
            extension.setReportUrl(testRunInfo.getAtpRamUrl() + (testRunInfo.getAtpRamUrl().endsWith("/") ? "" : "/")
                    + "common/uobject.jsp?object=" + testRunInfo.getTestRunId() + "&tab=_Test+Run+Tree+View");
        }
        extension.setExternalAppName("ATP server url: " + testRunInfo.getAtpRamUrl());
        return extension;
    }

    private Map<String, Map<?, ?>> syncAtpEnvironmentInfo(UUID id) {
        //noinspection unchecked,rawtypes
        Map<String, Map<?, ?>> simpleEnvironment = (Map) CacheServices.getEnvironmentCacheService().get(id);
        if (simpleEnvironment == null) {
            EnvironmentFullVer1ViewDto environment
                    = atpEnvironmentsEnvironmentFeignClient.getEnvironment(id, true).getBody();
            if (environment != null) {
                simpleEnvironment = simplifyEnvironmentInfo(environment);
                CacheServices.getEnvironmentCacheService()
                        .set(environment.getId(), simpleEnvironment);
            } else {
                throw new IllegalArgumentException("Cannot find ATP Environment by UUID " + id
                        + " in Environment Configuration Service.");
            }
        }
        return simpleEnvironment;
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Map<?, ?>> simplifyEnvironmentInfo(EnvironmentFullVer1ViewDto environment) {
        Map<String, Map<?, ?>> systemsMap = new HashMap<>();
        for (Object system : environment.getSystems()) {
            Map<String, Map<?, ?>> connectionsMap = new HashMap<>();
            String systemName = (String) ((Map) system).get("name");
            systemsMap.put(systemName, connectionsMap);
            for (Object connection : (ArrayList) ((Map) system).get("connections")) {
                String connectionName = (String) ((Map) connection).get("name");
                Map parameters = (Map) ((Map) connection).get("parameters");
                connectionsMap.put(connectionName, parameters);
            }
        }
        return systemsMap;
    }
}
