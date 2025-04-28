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

package org.qubership.automation.itf.ui.controls.execute;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION_DEFAULT_VALUE;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByParameterAndProjectIdManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.ExtensionManager;
import org.qubership.automation.itf.execution.data.CallchainExecutionData;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.executor.service.TCContextService;
import org.qubership.automation.itf.integration.bv.utils.BvHelper;
import org.qubership.automation.itf.report.extension.TCContextRamExtension;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.ExecutionRequest;
import org.qubership.automation.itf.ui.messages.objects.UIDataSet;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParameter;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParametersGroup;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tags({
        @Tag(name = SwaggerConstants.EXECUTOR_COMMAND_API,
                description = SwaggerConstants.EXECUTOR_COMMAND_API_DESCR)
})
@RequiredArgsConstructor
@Slf4j
public class ExecutorController extends ControllerHelper {

    private static final String CALLCHAIN_ID_OR_NAME_MUST_BE_SET =
            "One (not both) of callchain 'id' or 'name' must be specified";
    private static final String INVALID_RAM_SETTINGS = "'external.app.name' and ('testRunId' or 'ram2TestRunId') "
            + "work together: you must specify or not specify both of them";
    private static final String PROJECT_UUID_MUST_BE_SET = "'projectId' property must be specified";
    private static final String ENVIRONMENT_NAME_MUST_BE_SET = "'environment' property must be specified";
    private final Gson gson = new GsonBuilder().setExclusionStrategies(new DatasetContentExclusionStrategy()).create();

    private final ProjectSettingsService projectSettingsService;

    private static void processValidationError(Errors errors, String errorCategory, String errorMessage) {
        log.error("Invalid request to executor: {}", errorMessage);
        errors.reject(errorCategory, errorMessage);
    }

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(new Validator() {
            @Override
            public boolean supports(Class<?> clazz) {
                return ExecutionRequest.class.equals(clazz);
            }

            @Override
            public void validate(Object target, Errors errors) {
                ExecutionRequest toValidate = (ExecutionRequest) target;
                boolean nameIsEmpty = Strings.isNullOrEmpty(toValidate.getStarterChainName());
                boolean idIsEmpty = Strings.isNullOrEmpty(toValidate.getStarterChainId());
                if (nameIsEmpty == idIsEmpty) {
                    processValidationError(errors, "lackOfId", CALLCHAIN_ID_OR_NAME_MUST_BE_SET);
                }
                boolean exAppNameIsEmpty = Strings.isNullOrEmpty(toValidate.getExternalAppName());
                boolean testRunIdIsEmpty = (toValidate.getTestRunId() == null)
                        && (Strings.isNullOrEmpty(toValidate.getRam2TestRunId()));
                if (exAppNameIsEmpty != testRunIdIsEmpty) {
                    processValidationError(errors, "unexpectedSettings", INVALID_RAM_SETTINGS);
                }
                boolean projectIdIsEmpty = Strings.isNullOrEmpty(toValidate.getProjectId());
                if (projectIdIsEmpty) {
                    processValidationError(errors, "unexpectedSettings", PROJECT_UUID_MUST_BE_SET);
                }
                boolean environmentIsEmpty = Strings.isNullOrEmpty(toValidate.getEnvironment());
                if (environmentIsEmpty) {
                    processValidationError(errors, "unexpectedSettings", ENVIRONMENT_NAME_MUST_BE_SET);
                }
                if (errors.hasErrors()) {
                    log.error("Request isn't executed due to above errors, error code 400 'Bad request' is "
                            + "returned instead.\nRequest body: {}", gson.toJson(target));
                }
            }
        });
    }

    /**
     * This method for execute CallChain. You must prepare JSON for request.
     * Example for request body:
     * {
     * "dataset":"dataset name",
     * "environment":"environment name",
     * "name":"call chain name",
     * "id":"call chain id",
     * "external.app.name":"requester app name, mandatory for external run",
     * "testRunId":"some BigInteger value, mandatory for external run",
     * "externalSectionId":"some BigInteger value, not necessary",
     * "ram.er.name":"ramErName",
     * "ram.suite.name":"suite",
     * "ram.mail.list":"ramMailList",
     * "atp.project.name":"ramPrj",
     * "dataSetList":{
     *                "group1/entity1":{"key1":"value1"},
     *                "group2/entity2":{"key1":"value1"}
     *               }
     * }
     *
     * @param properties if dataSetList is specified, dataset name is not used.
     */
    @Transactional
    @RequestMapping(value = "/executor/execute", method = RequestMethod.POST)
    @Operation(summary = "Execute", description = "Execute callchain", tags = {SwaggerConstants.EXECUTOR_COMMAND_API})
    @AuditAction(auditAction = "Execute CallChain with properties")
    public String execute(@RequestBody @Valid ExecutionRequest properties) throws Exception {
        return ExecutionServices.getCallChainExecutorService()
                .executeInstance(doTestCase(properties))
                .getContext().tc().getID().toString();
    }

    private CallChainInstance doTestCase(final ExecutionRequest properties) throws Exception {
        return TxExecutor.execute(() -> {
            UUID projectUuid = UUID.fromString(properties.getProjectId());
            BigInteger projectId = CoreObjectManager.getInstance()
                    .getSpecialManager(StubProject.class, SearchManager.class)
                    .getEntityInternalIdByUuid(projectUuid);
            if (projectId == null) {
                throw new ObjectNotFoundException("Project", properties.getProjectId(), null, "execute Test Case");
            }
            SearchByParameterAndProjectIdManager<CallChain> man = CoreObjectManager.getInstance()
                    .getSpecialManager(CallChain.class, SearchByParameterAndProjectIdManager.class);
            String callChainName = properties.getStarterChainName();
            CallChain chain;
            if (callChainName != null) {
                Collection<? extends CallChain> chains = man.getByNameAndProjectId(callChainName, projectId);
                chain = chains != null && !chains.isEmpty()
                        ? chains.iterator().next()
                        : null;
            } else {
                chain = man.getById(properties.getStarterChainId());
            }
            ControllerHelper.throwExceptionIfNull(chain, callChainName, properties.getStarterChainId(), CallChain.class,
                    "get CallChain by id or name");
            CallChainInstance instance = createTestCaseInstance(properties.getDataset(), properties.getEnvironment(),
                    chain, properties.getDataSetList(), properties.getDataSetMap(),
                    properties.getMergeDatasetWithContext(), projectId, projectUuid);
            instance.getContext().setProjectUuid(projectUuid);
            TCContextRamExtension extension = new TCContextRamExtension();
            configureExtension(projectId, properties, extension);
            TcContext tcContext = instance.getContext().tc();
            tcContext.setStartedFrom(extension.getStartedFrom());
            tcContext.setAndCalculateNeedToReportToItf();
            tcContext.setPartNum(TCContextService.getCurrentPartitionNumberByProject(tcContext.getProjectUuid()));
            ExtensionManager.getInstance().extend(tcContext, extension);
            CallchainExecutionData data = new CallchainExecutionData(chain, properties.getEnvironment(),
                    tcContext.getEnvironmentId().toString(), properties.getDataset());
            data.setProjectUuid(projectUuid);
            data.setNeedToLogInATP(Boolean.TRUE);
            tcContext.put("DATASET_NAME", data.getDatasetName());
            if (properties.getValidateAtEnd()) {
                String propValue = projectSettingsService.get(projectId, BV_DEFAULT_ACTION,
                        BV_DEFAULT_ACTION_DEFAULT_VALUE);
                BvHelper.addOnCaseFinishValidation(instance, chain, properties.getDataset(), true, propValue);
                data.setRunBvCase(properties.getValidateAtEnd());
                data.setBvAction(propValue);
            }
            if (properties.getValidateOnSituation()) {
                BvHelper.addMessageOnStepValidation(instance);
                data.setValidateMessageOnStep(properties.getValidateOnSituation());
            }
            instance.setCallchainExecutionData(gson.toJson(data));
            return instance;
        });
    }

    private void configureExtension(
            @Nonnull BigInteger projectId,
            @Nonnull ExecutionRequest properties,
            @Nonnull TCContextRamExtension extension) {
        extension.setErName(properties.getRamExecRequestName());
        extension.setSuiteName(properties.getRamSuite());
        extension.setMailList(properties.getRamMailList());
        extension.setProjectName(properties.getRamProject());
        extension.setStartedFrom(StartedFrom.EXECUTOR);
        String ram2TestRunId = properties.getRam2TestRunId();
        log.debug("Configure extension testRunId for RAM2: {}", ram2TestRunId);
        if (!Strings.isNullOrEmpty(ram2TestRunId)) {
            TestRunContext runContext = TestRunContextHolder.getContext(properties.getRam2TestRunId());
            String sectionId = properties.getRam2TestRunId();
            runContext.setTestRunId(sectionId);
            LogRecord section = new LogRecord();
            section.setUuid(UUID.fromString(properties.getRam2SectionId() == null
                    ? runContext.getAtpCompaund().getSectionId()
                    : properties.getRam2SectionId()));
            section.setMessage("");
            section.setTestRunId(UUID.fromString(sectionId));
            section.setName(properties.getRam2SectionName());
            section.setTestingStatus(null);
            section.setType(TypeAction.ITF);
            section.setStartDate(new Timestamp(System.currentTimeMillis()));
            runContext.addSection(section);
            AtpCompaund compaund = new AtpCompaund();
            compaund.setSectionName(properties.getRam2SectionName());
            compaund.setSectionId(properties.getRam2SectionId());
            runContext.setAtpCompaund(compaund);
            extension.setRunContext(runContext);
            extension.setExternalRun(true);
            extension.setStartedFrom(StartedFrom.RAM2);
        }
    }

    /*  It's permitted to execute a callchain via REST API by means of a minimal json request like:
     *   {"environment": "ITF Environment name", "name": "ITF callchain name"}
     *   It's described here: "REST API for NTT/Executor integration"
     *
     *   So, both dataSetList and datasetName can be empty
     * */
    // TODO: dataSetList and datasetMap must be combined into one parameter; one- or two- levels strict limitation
    //  must be changed
    // TODO: datasetName processing must be improved for values like "Visibility area|DSL name|DS name"
    private CallChainInstance createTestCaseInstance(
            @Nullable String datasetName,
            String environmentName,
            @Nonnull CallChain callChain,
            @Nullable Map<String, Map<String, String>> dataSetList,
            @Nullable Map<String, String> datasetMap,
            Boolean mergeDatasetWithContext,
            BigInteger projectId,
            UUID projectUuid)
            throws Exception {
        Collection<? extends Environment> environments = CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, SearchByParameterAndProjectIdManager.class)
                .getByNameAndProjectId(environmentName, projectId);
        Environment environment = environments != null && !environments.isEmpty()
                ? environments.iterator().next() : null;
        ControllerHelper.throwExceptionIfNull(environment,
                StringUtils.isBlank(environmentName) ? "(name is null or empty)" : environmentName,
                null, Environment.class, "get Environment by name");
        JsonContext variablesJson = null;
        IDataSet dataSet = null;
        boolean contextEmpty = true;
        if (datasetMap != null) {
            variablesJson = new JsonContext();
            variablesJson.putAll(datasetMap);
            contextEmpty = datasetMap.isEmpty();
        }
        if (contextEmpty && dataSetList != null) {
            variablesJson = new JsonContext();
            variablesJson.merge(dataSetList);
            contextEmpty = dataSetList.isEmpty();
        }
        if ((contextEmpty || mergeDatasetWithContext) && !StringUtils.isBlank(datasetName)) {
            dataSet = ExecutorControllerHelper.findDataSetByName(datasetName, callChain, projectId);
        }
        return ExecutionServices.getCallChainExecutorService()
                .prepare(projectId, projectUuid, callChain, null, environment, dataSet, variablesJson, true);
    }

    @Nonnull
    private UIDataSet toUIDataSet(@Nonnull Map<String, Map<String, String>> dataSetList) {
        UIDataSet dataSet = new UIDataSet();
        dataSet.setDataSetParametersGroup(dataSetList.entrySet().stream()
                .map(new Function<Map.Entry<String, Map<String, String>>, UIDataSetParametersGroup>() {
                    @Nullable
                    @Override
                    public UIDataSetParametersGroup apply(@Nonnull Map.Entry<String, Map<String, String>> input) {
                        UIDataSetParametersGroup result = new UIDataSetParametersGroup();
                        result.setName(input.getKey());
                        result.setDataSetParameter(input.getValue().entrySet().stream()
                                .map(new Function<Map.Entry<String, String>, UIDataSetParameter>() {
                                    @Nullable
                                    @Override
                                    public UIDataSetParameter apply(@Nonnull Map.Entry<String, String> input) {
                                        return new UIDataSetParameter(input.getKey(), input.getValue());
                                    }
                                }).collect(Collectors.toSet()));
                        return result;
                    }
                }).collect(Collectors.toSet()));
        return dataSet;
    }

//    private String getReportUrl(BigInteger projectId, BigInteger testRunId) {
//        try {
//            URL url = new URL(projectSettingsService.get(projectId, ATP_WSDL_PATH, ATP_WSDL_PATH_DEFAULT_VALUE));
//            return url.getPort() != -1
//                    ? String.format("%s://%s:%d/common/uobject" + ".jsp?tab=_Test+Run+Tree+View" + "&object=%s",
//                    url.getProtocol(), url.getHost(), url.getPort(), testRunId)
//                    : String.format("%s" + "://%s/common/uobject.jsp?tab=_Test+Run+Tree+View&object=%s",
//                    url.getProtocol(), url.getHost(), testRunId);
//        } catch (MalformedURLException e) {
//            return null;
//        }
//    }

    public class DatasetContentExclusionStrategy implements ExclusionStrategy {

        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("dataSetList");
        }
    }
}
