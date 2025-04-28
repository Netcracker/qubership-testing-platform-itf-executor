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

package org.qubership.automation.itf.integration.atp;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.CALL_CHAIN_PARALLEL_RUNNING_THREAD_COUNT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.CALL_CHAIN_PARALLEL_RUNNING_THREAD_COUNT_DEFAULT_VALUE;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.CompoundLogRecordContainer;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.automation.itf.core.model.container.StepContainer;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.util.Version;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.integration.atp.action.ATPActionConstants;
import org.qubership.automation.itf.integration.atp.model.ContextEntity;
import org.qubership.automation.itf.integration.atp.model.CoolDownRequest;
import org.qubership.automation.itf.integration.atp.model.CoolDownResponse;
import org.qubership.automation.itf.integration.atp.model.ExecuteStepResponse;
import org.qubership.automation.itf.integration.atp.model.ExecutionStatus;
import org.qubership.automation.itf.integration.atp.model.GetAdditionStepInfoResponse;
import org.qubership.automation.itf.integration.atp.model.GetAdditionalStepInfoRequest;
import org.qubership.automation.itf.integration.atp.model.GetDefinitionsResponse;
import org.qubership.automation.itf.integration.atp.model.PingResponse;
import org.qubership.automation.itf.integration.atp.model.TestAutomationEngineFeature;
import org.qubership.automation.itf.integration.atp.model.WarmUpResponse;
import org.qubership.automation.itf.integration.atp.model.ram2.Ram2ExecuteStepRequest;
import org.qubership.automation.itf.integration.atp.model.ram2.Ram2ExecuteStepResponse;
import org.qubership.automation.itf.integration.atp.model.ram2.Ram2WarmUpRequest;
import org.qubership.automation.itf.integration.atp.util.AtpRunManager;
import org.qubership.automation.itf.integration.atp.util.CallchainRunInfo;
import org.qubership.automation.itf.integration.atp.util.CallchainRunner;
import org.qubership.automation.itf.integration.atp.util.TestRunInfo;
import org.qubership.automation.itf.integration.atp.util.TestRunInfoBuilder;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockingbirdEngineService {

    private static final Gson GSON = new GsonBuilder().create();

    @Getter
    private final ObjectFactory<CallchainRunner> factoryCallChainRunner;
    private final ProjectSettingsService projectSettingsService;

    private static void provideAtpParamsForWarmUp(Ram2WarmUpRequest ram2WarmUpRequest) {
        String testRunId = ram2WarmUpRequest.getRam2TestRunId();
        log.debug("Creating Test Run context for TR {} ER {}", testRunId, ram2WarmUpRequest.getExecutionRequestId());
        TestRunContext ram2Context = TestRunContextHolder.getContext(testRunId);
        ram2Context.setExecutionRequestId(ram2WarmUpRequest.getExecutionRequestId());
        ram2Context.setAtpExecutionRequestId(ram2WarmUpRequest.getExecutionRequestId());
        ram2Context.setProjectName(ram2WarmUpRequest.getProject());
        ram2Context.setProjectId(ram2WarmUpRequest.getProjectId());
        ram2Context.setTestPlanName(ram2WarmUpRequest.getTestPlanName());
        ram2Context.setTestPlanId(ram2WarmUpRequest.getTestPlanId());
        ram2Context.setTestCaseName(ram2WarmUpRequest.getTestCaseName());
        ram2Context.setExecutionRequestName(ram2WarmUpRequest.getExecutionRequestName());
        ram2Context.setTestRunName(ram2WarmUpRequest.getTestRunName());
        Report.startAtpRun(testRunId);
    }

    public PingResponse ping() {
        PingResponse response = new PingResponse();
        response.setUptime(AtpRunManager.getInstance().uptime());
        response.setAdditionalInfo(ATPActionConstants.MOCKINGBIRD_DESCRIPTION.stringValue());
        response.setAvailableActionsHash(AtpRunManager.getInstance().getAvailableActionsHash());
        response.setSupportedFeatures(Lists.newArrayList(TestAutomationEngineFeature.StepAdditionalInfo));
        response.setEngineVersion(Version.MOCKINGBIRD_VERSION);
        return response;
    }

    public GetDefinitionsResponse getDefinitions(BigInteger projectId) {
        GetDefinitionsResponse response = new GetDefinitionsResponse();
        response.setDefinitions(AtpRunManager.getInstance().getDefinitions(projectId));
        return response;
    }

    public GetAdditionStepInfoResponse getAdditionStepInfo(GetAdditionalStepInfoRequest request) {
        GetAdditionStepInfoResponse response = new GetAdditionStepInfoResponse();
        response.setStepsInfo(AtpRunManager.getInstance().getStepAdditionalInfo(request.getSteps()));
        return response;
    }

    public ExecuteStepResponse executeStep(Ram2ExecuteStepRequest request) {
        fillMdsFields(request);
        logRequestStage(request, true, "is received", null);
        TestRunContext testRunContext = provideAtpParamsToRam2Adapter(request);
        TestRunInfo testRunInfo;
        testRunInfo = new TestRunInfo();
        testRunInfo.setAtpRamUrl(request.getAtpRamUrl());
        testRunInfo.setTestRunName(request.getTestRunName());
        testRunInfo.setProject(request.getProjectName());
        testRunInfo.setStartedFrom(StartedFrom.RAM2);
        testRunInfo.setRam2TestRunId(request.getRam2TestRunId());
        testRunInfo.setRamTestRunContext(testRunContext);
        testRunInfo.fillParamsFromRequest(request);
        logRequestStage(request, false, "is pre-processed", null);
        // Attempt to bypass an exception:
        //  failed to lazily initialize a collection of role: org.qubership.automation.itf.core.testcase.callchain
        //  .CallChain.steps,
        //  could not initialize proxy - no Session
        AtomicBoolean isFailed = new AtomicBoolean(false);
        try {
            return TxExecutor.execute(() -> {
                try {
                    testRunInfo.build();
                    logRequestStage(request, false, "is ready to execute", null);
                    runCallChains(testRunInfo, isFailed);
                    logRequestStage(request, false, reportStageResults("is executed successfully", testRunInfo), null);
                    return createExecuteStepResponse(createContextEntity(testRunInfo), isFailed.get()
                            ? ExecutionStatus.Terminated : ExecutionStatus.Finished, isFailed.get());
                } catch (Exception e) {
                    // All errors while preparing of the testRunInfo are already reported at the moment
                    if (testRunInfo.isPrepared()) {
                        testRunInfo.reportError("Errors", "Error while running of callchain(s) from ATP",
                                StringUtils.EMPTY, e);
                        logRequestStage(request, false, reportStageResults("is executed with error(s)", testRunInfo),
                                e);
                    } else {
                        logRequestStage(request, false, " - error(s) while preparing to execute", e);
                    }
                    return createExecuteStepResponse(testRunInfo.getContextToReturn(), ExecutionStatus.Terminated,
                            true);
                } finally {
                    MDC.clear();
                }
            }, TxExecutor.readOnlyTransaction());
        } catch (Exception e) {
            logRequestStage(request, false, " - inner error(s) while preparing/executing", e);
            throw new IllegalStateException("Database/Jdbc exception while processing of the Execute Step Request", e);
        } finally {
            MDC.clear();
        }
    }

    private String reportStageResults(String stage, TestRunInfo testRunInfo) {
        if (testRunInfo == null) {
            return stage;
        }
        List<CallchainRunInfo> callchainRunInfoList = testRunInfo.getCallchainsToExecute();
        if (callchainRunInfoList == null || callchainRunInfoList.isEmpty()) {
            return stage + "\nNo callchains were executed!";
        }
        StringBuilder sb = new StringBuilder(stage + "\nDetailed results are:");
        for (CallchainRunInfo runInfo : callchainRunInfoList) {
            CallChain callChain = runInfo.getCallChain();
            if (callChain != null) {
                sb.append("\n  Callchain: [").append(callChain.getID()).append("] '").append(callChain.getName()).append("'");
            }
            TcContext tcContext = runInfo.getTcContext();
            if (tcContext != null) {
                sb.append("\n    TC Context: [").append(tcContext.getID()).append("] status '").append(tcContext.getStatus()).append("'");
            }
        }
        return sb.append("\n").toString();
    }

    private void logRequestStage(Ram2ExecuteStepRequest request, boolean first, String stage, Exception ex) {
        if (first) {
            /*I guess direct call of #getExecutionActions is huge performance issue*/
            ParamSupplier paramSupplier = new ParamSupplier() {
                @Override
                public String toString() {
                    return getExecutionActions(request);
                }
            };
            log.debug("Execute step from RAM2: RunId: {}, with params: {}",
                    request.getRam2TestRunId(), paramSupplier);
        }
        if (ex == null) {
            log.info("ER from RAM2 (project: {}, RunId: {}, Test Run Name: {}) {}",
                    request.getProjectName(),
                    request.getRam2TestRunId(),
                    request.getTestRunName(),
                    stage);
        } else {
            log.error("ER from RAM2 (project: {}, RunId: {}, Test Run Name: {}) {}, {}",
                    request.getProjectName(),
                    request.getRam2TestRunId(),
                    request.getTestRunName(),
                    stage, ex.getMessage());
        }
    }

    private TestRunContext provideAtpParamsToRam2Adapter(Ram2ExecuteStepRequest request) {
        TestRunContext ram2Context;
        ram2Context = TestRunContextHolder.getContext(request.getRam2TestRunId());
        ram2Context.setTestRunName(request.getTestRunName());
        ram2Context.setProjectName(request.getProjectName());
        ram2Context.setTestPlanName(request.getTestPlanName());
        ram2Context.setTestSuiteName(request.getTestSuiteName());
        ram2Context.setTestCaseName(request.getTestCaseName());
        ram2Context.setExecutionRequestName(request.getExecutionRequestName());
        ram2Context.setTestRunName(request.getTestRunName());
        ram2Context.setAtpExecutionRequestId(request.getExecutionRequestId());
        ram2Context.setCompoundAndUpdateCompoundStatuses(request.getRamSection());
        writeParentSections(ram2Context);
        return ram2Context;
    }

    private void writeParentSections(TestRunContext context) {
        AtpCompaund compound = context.getAtpCompaund();
        log.info("Write parent sections for Test Run {} from compounds {}.", context.getTestRunId(), compound);
        if (compound != null) {
            Stack<AtpCompaund> compounds = new Stack<>();
            fillStepStack(compounds, compound);
            while (!compounds.isEmpty()) {
                AtpCompaund atpCompaund = compounds.pop();
                if (Strings.isNullOrEmpty(atpCompaund.getSectionId())) {
                    continue;
                }
                UUID id = UUID.fromString(atpCompaund.getSectionId());
                UUID parentId = Objects.isNull(atpCompaund.getParentSection())
                        ? null : UUID.fromString(atpCompaund.getParentSection().getSectionId());
                TestingStatuses statuses = Objects.isNull(atpCompaund.getTestingStatuses())
                        ? TestingStatuses.UNKNOWN : atpCompaund.getTestingStatuses();
                CompoundLogRecordContainer logRecord = new CompoundLogRecordContainer();
                logRecord.setName(atpCompaund.getSectionName());
                logRecord.setUuid(id);
                logRecord.setParentRecordId(parentId);
                logRecord.setTestingStatus(statuses);
                logRecord.setStartDate(atpCompaund.getStartDate());
                logRecord.setLastInSection(atpCompaund.isLastInSection());
                if (compounds.isEmpty()) {
                    logRecord.setStep(true);
                }
                context.addSection(logRecord);
            }
        }
    }

    private void fillStepStack(Stack<AtpCompaund> stack, AtpCompaund compaund) {
        if (!Strings.isNullOrEmpty(compaund.getSectionId())) {
            stack.push(compaund);
        }
        if (compaund.getParentSection() != null && !Strings.isNullOrEmpty(compaund.getParentSection().getSectionId())) {
            fillStepStack(stack, compaund.getParentSection());
        }
    }

    private String getExecutionActions(Ram2ExecuteStepRequest request) {
        StringBuilder builder = new StringBuilder();
        try {
            request.getScope().getSteps().forEach(step -> step.getActions()
                    .forEach(action -> {
                        builder.append('{')
                                .append(action.getName()).append(':').append(action.getId())
                                .append(',').append('[');
                        action.getParameters().forEach(parameter -> builder.append('{')
                                .append(parameter.getId())
                                .append(':').append(parameter.getValue())
                                .append('}')
                                .append(',')
                        );
                        builder.append(']').append('}');
                    }));
        } catch (Exception e) {
            builder.append("Error occurred while processing step execution request: ").append(e.getMessage());
        }
        return builder.toString();
    }

    public WarmUpResponse warmUp(Ram2WarmUpRequest request) {
        provideAtpParamsForWarmUp(request);
        return new WarmUpResponse();
    }

    public CoolDownResponse coolDown(CoolDownRequest request) {
        AtpRunManager.getInstance().remove(request.getTestRunId());
        return new CoolDownResponse();
    }

    private void runCallChains(TestRunInfo testRunInfo, AtomicBoolean isFailed) {
        int size = testRunInfo.getCallchainsToExecute().size();
        if (size > 1) {
            runCallChainsInParallelMode(testRunInfo, isFailed);
        } else if (size == 1) {
            runSingleCallChain(testRunInfo, isFailed);
        }
    }

    private void runCallChainsInParallelMode(TestRunInfo testRunInfo, AtomicBoolean isFailed) {
        int poolSize = projectSettingsService.getInt(testRunInfo.getProjectId(),
                CALL_CHAIN_PARALLEL_RUNNING_THREAD_COUNT,
                Integer.parseInt(CALL_CHAIN_PARALLEL_RUNNING_THREAD_COUNT_DEFAULT_VALUE));
        if (poolSize <= 0) {
            log.error(String.format("Count of the threads for running the callchains in parallel mode cannot be "
                            + "equal to 0 or less. %s as default value will be used.",
                    CALL_CHAIN_PARALLEL_RUNNING_THREAD_COUNT_DEFAULT_VALUE));
            poolSize = Integer.parseInt(CALL_CHAIN_PARALLEL_RUNNING_THREAD_COUNT_DEFAULT_VALUE);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        for (CallchainRunInfo callchainRunInfo : testRunInfo.getCallchainsToExecute()) {
            CallchainRunner callchainRunner = factoryCallChainRunner.getObject();
            callchainRunner.fillRunInfo(callchainRunInfo, testRunInfo, isFailed);
            executorService.execute(callchainRunner);
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Execution of threadPool has been interrupted: ", e);
        }
    }

    private void runSingleCallChain(TestRunInfo testRunInfo, AtomicBoolean isFailed) {
        CallchainRunner callchainRunner = factoryCallChainRunner.getObject();
        callchainRunner.fillRunInfo(testRunInfo.getCallchainsToExecute().get(0),
                testRunInfo, isFailed);
        callchainRunner.run();
    }

    private ExecuteStepResponse createExecuteStepResponse(ContextEntity entity,
                                                          ExecutionStatus status,
                                                          boolean isFailed) {
        Ram2ExecuteStepResponse response = new Ram2ExecuteStepResponse();
        response.setContext(entity);
        response.setStatus(status);
        response.setTestingStatus((isFailed) ? TestingStatuses.FAILED : TestingStatuses.PASSED);
        return response;
    }

    private ContextEntity createContextEntity(TestRunInfo testRunInfo) {
        ContextEntity contextEntity = new ContextEntity();
        List<TcContext> tcContexts = getTcContextsFromExecution(testRunInfo);
        if (tcContexts.size() == 1) {
            TcContext context = tcContexts.get(0);
            if (context == null) {
                contextEntity.setJsonString(new JsonFlattener(StringUtils.EMPTY).withFlattenMode(FlattenMode.NORMAL).flatten());
            } else {
                String jsonString = context.toJSONString();
                contextEntity.setJsonString(new JsonFlattener(jsonString).withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten());
            }
        } else if (tcContexts.size() > 1) {
            contextEntity.setJsonString(createTCContextsJson(tcContexts));
        }
        return contextEntity;
    }

    private List<TcContext> getTcContextsFromExecution(TestRunInfo testRunInfo) {
        List<TcContext> tcContextList = Lists.newArrayList();
        for (CallchainRunInfo callchainRunInfo : testRunInfo.getCallchainsToExecute()) {
            tcContextList.add(callchainRunInfo.getTcContext());
        }
        return tcContextList;
    }

    private String createTCContextsJson(List<TcContext> tcContexts) {
        StringBuilder mainSb = new StringBuilder("{").append("\"tcContexts\"").append(":").append("[");
        Iterator<TcContext> iterator = tcContexts.iterator();
        while (iterator.hasNext()) {
            TcContext tcContext = iterator.next();
            StepContainer stepContainer = tcContext.getInitiator().getStepContainer();
            StringBuilder contextSb = new StringBuilder("{")
                    .append(createJsonPair("id", stepContainer.getID().toString(), false)).append(",")
                    .append(createJsonPair("name", stepContainer.getName(), false)).append(",")
                    .append(createJsonPair("context", GSON.toJson(tcContext, TcContext.class), true))
                    .append("}");
            if (iterator.hasNext()) contextSb.append(",");
            mainSb.append(contextSb);
        }
        mainSb.append("]").append("}");
        return mainSb.toString();
    }

    private String createJsonPair(String key, String value, boolean valueInJsonFormat) {
        String formattedValue = valueInJsonFormat ? value : "\"" + value + "\"";
        return "\"" + key + "\"" + ":" + formattedValue;
    }

    private void fillMdsFields(Ram2ExecuteStepRequest request) {
        try {
            String testRunId = request.getRam2TestRunId();
            MdcUtils.put(MdcField.TEST_RUN_ID.toString(), testRunId);
            MdcUtils.put(MdcField.EXECUTION_REQUEST_ID.toString(), request.getExecutionRequestId());
            String projectUuid =
                    TestRunInfoBuilder.getParameterValueFromContextEntity(request.getContext(), "projectId");
            if (StringUtils.isEmpty(projectUuid)) {
                String error = "Project UUID from ATP-request is empty! ITF-project can not be found!";
                log.error(error);
                MDC.clear();
                throw new IllegalArgumentException(error);
            }
            MdcUtils.put(MdcField.PROJECT_ID.toString(), projectUuid);
        } catch (Exception e) {
            String error = "Can't get projectUUID / testRunId / executionRequestId from ATP request";
            log.error(error);
            MDC.clear();
            throw new IllegalArgumentException(error, e);
        }
    }

    interface ParamSupplier {

        @Override
        String toString();
    }
}
