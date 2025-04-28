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

package org.qubership.automation.itf.integration.bv.engine;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.qubership.atp.bv.dto.CopyWithNameRequestDto;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.engine.EngineAfterIntegration;
import org.qubership.automation.itf.core.util.engine.EngineControlIntegration;
import org.qubership.automation.itf.core.util.engine.EngineOnStepIntegration;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.core.util.feign.http.HttpClientFactory;
import org.qubership.automation.itf.core.util.logger.ItfLogger;
import org.qubership.automation.itf.integration.bv.BvEndpoints;
import org.qubership.automation.itf.integration.bv.BvIntegrationProperties;
import org.qubership.automation.itf.integration.bv.messages.request.RequestData;
import org.qubership.automation.itf.integration.bv.messages.request.quickCompare.QuickCompareRequest;
import org.qubership.automation.itf.integration.bv.messages.response.BvResponseData;
import org.qubership.automation.itf.integration.bv.messages.response.HttpBvResponse;
import org.qubership.automation.itf.integration.bv.messages.response.quickCompare.QuickCompareResponse;
import org.qubership.automation.itf.integration.bv.messages.response.quickCompare.Step;
import org.qubership.automation.itf.integration.bv.utils.BvHelper;
import org.qubership.automation.itf.integration.bv.utils.BvRequestBuilder;
import org.qubership.automation.itf.integration.bv.utils.BvResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@UserName("Bulk Validator Integration")
public class BvEngineIntegration implements EngineAfterIntegration, EngineControlIntegration, EngineOnStepIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BvEngineIntegration.class);
    private static final Logger ITF_LOGGER = ItfLogger.getLogger(BvEngineIntegration.class);
    public static Gson GSON_INSTANCE = new GsonBuilder().disableHtmlEscaping().create();

    @Parameter(shortName = "bv.testcase.type", longName = "Test Case Type", description = "BulkValidator TestCase Type")
    private String testCaseType;
    @Parameter(shortName = "bv.source.name", longName = "Source Name", description = "BulkValidator Source Name")
    private String sourceName;
    @Parameter(shortName = "bv.use.report.link",
            longName = "Use report link as validation parameter (URLtoISL)",
            description = "")
    private Boolean useReportLink;
    @Parameter(shortName = "bv.report.link.name",
            longName = "Report link name (URLtoISL)",
            description = "Integration Sessions Log (as given in Environment)")
    private String reportLink;
    @Parameter(shortName = "bv.input.params.names",
            longName = "Context keys to use as input parameters (Template)",
            description = "bvName1=$tc.Keys.CRM\nbvName2=$tc.Keys.processId")
    private List<String> inputKey;
    @Parameter(shortName = "bv.valid.params.names",
            longName = "Context keys to use as validation parameters (Template)",
            description = "bvName1=$tc.Keys.CRM\nbvName2=$tc.Keys.processId")
    private List<String> validKey;
    @Parameter(shortName = "bv.conf.path",
            longName = "Bulk Validator configuration path / Environment Extra Settings name",
            description = "bv_env_name/bv_system_name/bv_connection_name or Extra_Settings_Name")
    private String groupName;

    @Override
    public void executeAfter(CallChainInstance chainInstance, IntegrationConfig integrationConf)
            throws EngineIntegrationException {
        BvInstanceExtension extension = chainInstance.getExtension(BvInstanceExtension.class);
        if (extension == null) {
            LOGGER.warn("BVInstanceExtension is not available for call chain '{}'", chainInstance);
            return;
        }
        if (!extension.validate) {
            LOGGER.warn("BVInstanceExtension: testcase validation is turned OFF for call chain '{}'", chainInstance);
            return;
        }
        String action = extension.bvAction;
        if (!(BvIntegrationProperties.READ_COMPARE.equals(action)
                || BvIntegrationProperties.CREATE_NEW_TESTRUN.equals(action))) {
            throw new EngineIntegrationException("BulkValidator actions in project settings are not correct or empty");
        }
        BvInstance bvInstance = new BvInstance((CallChain) chainInstance.getStepContainer());
        bvInstance.getContext().setTC(chainInstance.getContext().tc());
        RequestData requestData = null;
        String endpoint = null;
        UUID projectUuid = chainInstance.getContext().tc().getProjectUuid();
        switch (action) {
            case BvIntegrationProperties.READ_COMPARE:
                requestData = BvRequestBuilder.buildReadCompareRequest(integrationConf, chainInstance, bvInstance);
                endpoint = String.format(BvEndpoints.READ_COMPARE_ENDPOINT, projectUuid);
                break;
            case BvIntegrationProperties.CREATE_NEW_TESTRUN:
                requestData = BvRequestBuilder.buildCreateNewTRRequest(integrationConf, chainInstance, bvInstance);
                endpoint = String.format(BvEndpoints.CREATING_NEW_TR_ENDPOINT, projectUuid);
                break;
            default:
        }
        bvInstance.setStatus(Status.IN_PROGRESS);
        bvInstance.setStartTime(new Date());
        String request = GSON_INSTANCE.toJson(requestData);

        String response;
        if (BvIntegrationProperties.READ_COMPARE.equals(action)) {
            ResponseEntity<String> responseEntity
                    = HttpClientFactory.getBvApiResourceFeignClient().readAndCompare(projectUuid, request);
            response = sendRequest("Executing after: {}", endpoint, request,
                    responseEntity.getBody());
        } else {
            ResponseEntity<Object> responseEntity
                    = HttpClientFactory.getBvPublicApiResourceFeignClient().createTr(projectUuid, request);
            response = sendRequest("Executing after: {}", endpoint, request,
                    GSON_INSTANCE.toJson(responseEntity.getBody()));
        }

        String trId = BvResponseProcessor.processResponse(Objects.requireNonNull(response), action, chainInstance,
                bvInstance, integrationConf);
        if (BvIntegrationProperties.CREATE_NEW_TESTRUN.equals(action) && trId != null) {
            requestData = BvRequestBuilder.buildCompareRequest(trId, bvInstance);
            request = GSON_INSTANCE.toJson(requestData);
            endpoint = String.format(BvEndpoints.COMPARE_ENDPOINT, projectUuid);

            ResponseEntity<String> responseEntity
                    = HttpClientFactory.getBvApiResourceFeignClient().compare(projectUuid, request);
            response = sendRequest("Executing after with " + action + ": {}", endpoint, request,
                    responseEntity.getBody());
            BvResponseProcessor.processResponse(response, BvIntegrationProperties.COMPARE, chainInstance, bvInstance,
                    integrationConf);
        }
    }

    @Override
    public void create(CallChain callChain, IntegrationConfig integrationConf, Map<String, String> properties,
                       BigInteger projectId) throws EngineIntegrationException {
        RequestData requestData = BvRequestBuilder.buildCreateRequestData(callChain, integrationConf, properties);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String request = GSON_INSTANCE.toJson(requestData);

        String endpoint = String.format(BvEndpoints.CREATE_TC_ENDPOINT, projectUuid);
        ResponseEntity<String> responseEntity
                = HttpClientFactory.getBvTestCaseResourceFeignClient().create(projectUuid, request);
        String response = sendRequest("Creation: {}", endpoint, request, responseEntity.getBody());
        try {
            JSONObject jsResponse = (JSONObject) new JSONParser().parse(response);
            callChain.getBvCases().put(properties.get("dataset.name"), jsResponse.get("tcId").toString());
        } catch (ParseException ex) {
            throw new EngineIntegrationException("Error while parsing response", ex);
        }
    }

    @Override
    public void create(Situation situation, IntegrationConfig integrationConf, BigInteger projectId)
            throws EngineIntegrationException {
        RequestData requestData = BvRequestBuilder.buildCreateRequestData(situation, integrationConf);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String request = GSON_INSTANCE.toJson(requestData);

        String endpoint = String.format(BvEndpoints.CREATE_TC_ENDPOINT, projectUuid);
        ResponseEntity<String> responseEntity
                = HttpClientFactory.getBvTestCaseResourceFeignClient().create(projectUuid, request);
        String response = sendRequest("Creation: {}", endpoint, request, responseEntity.getBody());
        try {
            JSONObject jsResponse = (JSONObject) new JSONParser().parse(response);
            situation.setBvTestcase(jsResponse.get("tcId").toString());
        } catch (ParseException ex) {
            throw new EngineIntegrationException("Error while parsing response", ex);
        }
    }

    @Override
    public void delete(CallChain callChain, IntegrationConfig integrationConf, Map<String, String> properties,
                       BigInteger projectId) {
        RequestData requestData = BvRequestBuilder.buildSimpleRequestData(callChain, properties);
        String request = GSON_INSTANCE.toJson(requestData);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String endpoint = String.format(BvEndpoints.REMOVE_TC_ENDPOINT, projectUuid);

        ResponseEntity<String> response
                = HttpClientFactory.getBvTestCaseResourceFeignClient().remove(projectUuid, request);
        sendRequest("Deleting: {}", endpoint, request, response.getBody());
    }

    @Override
    public void delete(Situation situation, IntegrationConfig integrationConf, BigInteger projectId) {
        RequestData requestData = new RequestData(situation.getBvTestcase());
        String request = GSON_INSTANCE.toJson(requestData);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String endpoint = String.format(BvEndpoints.REMOVE_TC_ENDPOINT, projectUuid);

        ResponseEntity<String> response
                = HttpClientFactory.getBvTestCaseResourceFeignClient().remove(projectUuid, request);
        sendRequest("Deleting: {}", endpoint, request, response.getBody());
    }

    @Override
    public void configure(CallChain callChain, IntegrationConfig integrationConf, Map<String, String> properties,
                          BigInteger projectId) {
        RequestData requestData = BvRequestBuilder.buildReadRequestData(callChain, integrationConf, properties);
        String request = GSON_INSTANCE.toJson(requestData);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String endpoint = String.format(BvEndpoints.READ_ENDPOINT, projectUuid);

        ResponseEntity<String> responseEntity
                = HttpClientFactory.getBvApiResourceFeignClient().read(projectUuid, request);
        String response = sendRequest("Configuring: {}", endpoint, request, responseEntity.getBody());

        BvResponseData responseData = BvResponseProcessor.parseResponse(response);
        if (BvIntegrationProperties.BV_RESPONSE_STATUS_OK.equals(responseData.getStatusCode())) {
            LOGGER.info(responseData.getStatusMessage());
        } else {
            String errorMessage = "Response from BulkValidator contains error(s)! Check URLtoISL, existence of the "
                    + "testCase in the BulkValidator by reference and check config BV Integration. Status "
                    + "Message: \n\r";
            throw new EngineIntegrationException(errorMessage + responseData.getStatusMessage());
        }
    }

    @Override
    public boolean isExist(Storable storable, IntegrationConfig integrationConf, Map<String, String> properties,
                           BigInteger projectId) throws EngineIntegrationException {
        RequestData requestData = BvRequestBuilder.buildSimpleRequestData(storable, properties);
        String request = GSON_INSTANCE.toJson(requestData);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String endpoint = String.format(BvEndpoints.IS_EXIST_ENDPOINT, projectUuid);

        ResponseEntity<String> responseEntity
                = HttpClientFactory.getBvTestCaseResourceFeignClient().getTestCaseStatus(projectUuid, request);
        String response = sendRequest("isExist: {}", endpoint, request, responseEntity.getBody());

        String status = BvResponseProcessor.parseResponse(response).getTcStatus().getStatus();
        return !"TEST_CASE_DOESN'T_EXIST.".equals(status);
    }

    @Override
    public String copyWithName(IntegrationConfig integrationConf, String newName, String sourceTcId,
                               BigInteger projectId) throws EngineIntegrationException {
        CopyWithNameRequestDto request = BvRequestBuilder.buildCopyRequestData(newName, sourceTcId);
        String requestString = GSON_INSTANCE.toJson(request);
        UUID projectUuid = UUID.fromString(BvHelper.getProjectUUID(projectId));
        String endpoint = String.format(BvEndpoints.COPY_TC_WITH_NAME_ENDPOINT, projectUuid);

        ResponseEntity<String> responseEntity
                = HttpClientFactory.getBvTestCaseResourceFeignClient().copyWithName(projectUuid, request);
        return sendRequest("copyWithName: {}", endpoint, requestString, responseEntity.getBody());
    }

    private String sendRequest(String requestAction, String endpoint, String content, String response)
            throws EngineIntegrationException {
        try {
            ITF_LOGGER.debug("Request to BulkValidator:\n{}", content);
            HttpBvResponse httpBVResponse = new HttpBvResponse(200, true, response);
            if (!httpBVResponse.isSuccess()) {
                String error = "BulkValidator returned an error response:\n HTTP response code: "
                        + httpBVResponse.getHttpResponseCode()
                        + ((httpBVResponse.getBvStatusCode() != -1)
                        ? ",\n Status code: " + httpBVResponse.getBvStatusCode() + ",\n Status message: "
                        + httpBVResponse.getBvStatusMessage()
                        : ",\n Response body: " + httpBVResponse.getHttpResponseBody());
                ITF_LOGGER.debug(error);
                throw new ClientProtocolException(error);
            }
            ITF_LOGGER.debug("Response from BulkValidator:\n{}", response);
            return response;
        } catch (Exception ex) {
            String message = String.format("Request to BV failed: endpoint=%s\n%s", endpoint, ex);
            ITF_LOGGER.debug(message);
            throw new EngineIntegrationException(message);
        }
    }

    @Override
    public boolean executeOnStep(SituationInstance instance,
                                 SpContext spContext,
                                 AbstractContainerInstance initiator,
                                 IntegrationConfig integrationConf) {
        return executeOnStep(instance, spContext, initiator, integrationConf, instance.getSituationById());
    }

    @Override
    public boolean executeOnStep(SituationInstance instance,
                                 SpContext spContext,
                                 AbstractContainerInstance initiator,
                                 IntegrationConfig integrationConf,
                                 Situation situation) {
        if (!checkExtension(situation, initiator)) {
            return true;
        }
        boolean situationTestcase = true;
        String tcId = situation.getBvTestcase();
        if (StringUtils.isBlank(tcId) && initiator instanceof CallChainInstance) {
            tcId = BvRequestBuilder.getBVCaseId((CallChainInstance) initiator);
            situationTestcase = false;
        }
        if (StringUtils.isBlank(tcId)) {
            throw new EngineIntegrationException("BulkValidator Testcase ID isn't specified! Validation at the "
                    + "situation '" + instance.getName() + "' is failed.");
        }

        String request;
        String response;
        QuickCompareRequest quickCompareRequest;
        UUID projectUuid = initiator.getContext().tc().getProjectUuid();
        if (situationTestcase) {
            request = "{\"testCasesIds\": [\"" + tcId + "\"] }";
            String endpoint = String.format(BvEndpoints.GET_TESTCASE_PARAMETERS_ENDPOINT, projectUuid);
            ResponseEntity<String> responseEntity
                    = HttpClientFactory.getBvTestCaseResourceFeignClient().getParameters(projectUuid, request);
            response = sendRequest("Getting testcase vobjects: {}", endpoint, request, responseEntity.getBody());

            /*  This is new request and response; format of response differs from all used before
             *   but we are interested in only few properties of response objects
             * */
            quickCompareRequest = BvRequestBuilder.buildQuickCompareRequest(instance, situation, spContext,
                    BvResponseProcessor.parseTestcaseVobjectsResponse(response), tcId);
        } else {
            quickCompareRequest = BvRequestBuilder.buildQuickCompareRequest(instance.getName(),
                    spContext.getIncomingMessage(), tcId);
        }
        request = GSON_INSTANCE.toJson(quickCompareRequest);
        String endpoint = String.format(BvEndpoints.VALIDATE_MESSAGE_ENDPOINT, projectUuid);
        ResponseEntity<Object> responseEntity
                = HttpClientFactory.getBvPublicApiResourceFeignClient().quickCompare(projectUuid, request);
        response = sendRequest("Executing message validation: {}", endpoint, request,
                GSON_INSTANCE.toJson(responseEntity.getBody()));

        QuickCompareResponse compareResult = BvResponseProcessor.processQuickCompareResponse(response);
        boolean result = "IDENTICAL".equals(compareResult.getCompareResult());
        String hlTable = printHlResult(compareResult.getSteps(), "");
        if (compareResult.getSteps() != null && compareResult.getSteps().size() > 0) {
            hlTable = "<div class=\"highlighter-container\">" + hlTable + "</div>";
        }
        if (!result) {
            initiator.setErrorName("Situation '" + instance.getName() + "': incoming message validation is failed "
                    + "(Compare Result is '" + compareResult.getCompareResult() + "')");
            initiator.setErrorMessage(hlTable);
        }
        initiator.getContext().tc().put("saved.summaryValidationResult", compareResult.getCompareResult());
        spContext.put("bvResultForRam2", response);
        spContext.setValidationResults(hlTable);
        return result;
    }

    private boolean checkExtension(Situation situation, AbstractContainerInstance initiator) {
        if (initiator instanceof CallChainInstance) {
            BvInstanceExtension extension = initiator.getExtension(BvInstanceExtension.class);
            if (extension == null) {
                LOGGER.debug("BVInstanceExtension is not available for call chain '{}'", initiator);
                return false;
            }
            if (!extension.validateMessages) {
                LOGGER.debug("BVInstanceExtension: messages validation is turned OFF for call chain '{}'", initiator);
                return false;
            }
            return true;
        } else {
            return situation.getBooleanValidateIncoming();
        }
    }

    private String printHlResult(List<Step> steps, String parentName) {
        StringBuilder sb = new StringBuilder();
        if (steps != null && !steps.isEmpty()) {
            for (Step step : steps) {
                String statusColor = compareResultToLabelColor(step.getCompareResult());
                String displayedDescription = StringUtils.isBlank(step.getDescription())
                        ? ""
                        : "<br>&nbsp;<b>Description: </b>" + step.getDescription();

                sb.append("<div class=\"panel panel-").append(statusColor).append("\">")
                        .append("<div class=\"panel" + "-heading\">").append(parentName).append(step.getStepName())
                        .append(" <span data-block-id=\"pc-highlight-block\" class=\"").append(step.getCompareResult())
                        .append("\">")
                        .append(StringUtils.upperCase(step.getCompareResult())).append("</span>")
                        .append(displayedDescription).append("</div>");

                sb.append("<div class=\"panel-body\">").append("<table border=\"1\"><tr><th width=\"50%\">ER</th><th "
                                + "width=\"50%\">AR</th></tr><tr style=\"vertical-align: top;\"><td width=\"50%\">")
                        .append(step.getHighlightedEr()).append("</td><td width=\"50%\">")
                        .append(step.getHighlightedAr()).append("</td></tr></table></div>");
                sb.append("</div>");
            }
        }
        return sb.toString();
    }

    private String compareResultToLabelColor(String compareResult) {
        if (StringUtils.isBlank(compareResult)) {
            return "default";
        }
        switch (compareResult.toLowerCase()) {
            case "identical":
            case "passed":
            case "success":
                return "success";
            case "similar":
                return "warning";
            default:
                return "danger";
        }
    }
}
