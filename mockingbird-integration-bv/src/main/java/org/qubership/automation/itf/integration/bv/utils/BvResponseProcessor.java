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

package org.qubership.automation.itf.integration.bv.utils;

import static org.qubership.automation.itf.integration.bv.BvEndpoints.ENDPOINT_FOR_LINK_TO_TR;

import java.io.IOException;
import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.core.util.logger.ItfLogger;
import org.qubership.automation.itf.integration.bv.BvIntegrationProperties;
import org.qubership.automation.itf.integration.bv.engine.BvEngineIntegration;
import org.qubership.automation.itf.integration.bv.engine.BvInstance;
import org.qubership.automation.itf.integration.bv.messages.response.BvResponseData;
import org.qubership.automation.itf.integration.bv.messages.response.TestcaseVobjectsResponse;
import org.qubership.automation.itf.integration.bv.messages.response.quickCompare.QuickCompareResponse;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BvResponseProcessor {

    private static final Logger ITF_LOGGER = ItfLogger.getLogger(BvEngineIntegration.class);

    private static final ObjectMapper objectMapper;

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper = mapper;
    }

    public static String processResponse(String response, String bvAction, CallChainInstance callChainInstance,
                                         BvInstance bvInstance, IntegrationConfig integrationConfig) {
        BvResponseData responseData = parseResponse(response);
        BigInteger projectId = callChainInstance.getContext().tc().getProjectId();
        switch (bvAction) {
            case BvIntegrationProperties.READ_COMPARE:
                return processResponseReadCompare(responseData, callChainInstance, bvInstance, integrationConfig,
                        projectId);
            case BvIntegrationProperties.CREATE_NEW_TESTRUN:
                return processResponseCreateTr(responseData, callChainInstance, bvInstance, integrationConfig,
                        projectId);
            case BvIntegrationProperties.COMPARE:
                return processResponseCompare(responseData, callChainInstance, bvInstance, integrationConfig,
                        projectId);
            default:
                return null;
        }
    }

    private static String processResponseReadCompare(BvResponseData responseData, CallChainInstance chainInstance,
                                                     BvInstance bvInstance, IntegrationConfig integrationConf,
                                                     BigInteger projectId) {
        if (responseData.getStatusCode() != null) {
            String statusCode = responseData.getStatusCode();
            if (statusCode.equals(BvIntegrationProperties.BV_RESPONSE_STATUS_OK)) {
                String successMessage =
                        "Response from Bulk Validator to read and compare for test case " + responseData.getTcName()
                                + " : Actual results were successfully read!";
                String uuid = chainInstance.getContext().tc().getProjectUuid().toString(); // Old, already incorrect:
                // = BvHelper.getProjectUUID(projectId);
                String reportLink = BvHelper.normalizeUrl(ApplicationConfig.env.getProperty("atp.catalogue.url",
                                StringUtils.EMPTY),
                        String.format(ENDPOINT_FOR_LINK_TO_TR, uuid)) + responseData.getTrId();
                BvHelper.completeSuccessfully(chainInstance, bvInstance, reportLink, successMessage,
                        responseData.getCompareResult());
                changeTcContextStatus(chainInstance.getContext().tc(), responseData.getCompareResult());
                return responseData.getTrId();
            } else {
                String errorMessage = "Response returned an error from the Bulk Validator while read and compare! "
                        + "Status Message " + responseData.getStatusMessage();
                chainInstance.getContext().tc().setStatus(Status.FAILED);
                BvHelper.endWithFailed(errorMessage, bvInstance);
            }
        }
        return null;
    }

    private static String processResponseCompare(BvResponseData responseData, CallChainInstance chainInstance,
                                                 BvInstance bvInstance, IntegrationConfig integrationConf,
                                                 BigInteger projectId) {
        if (responseData.getStatusCode() != null) {
            String statusCode = responseData.getStatusCode();
            if (statusCode.equals(BvIntegrationProperties.BV_RESPONSE_STATUS_OK)) {
                String successMessage =
                        "Response from Bulk Validator to compare for TestRun " + responseData.getTrId()
                                + " : TestRun was successfully validated!";
                String uuid = chainInstance.getContext().tc().getProjectUuid().toString(); // Old, already incorrect:
                // = BvHelper.getProjectUUID(projectId);
                String reportLink = BvHelper.normalizeUrl(ApplicationConfig.env.getProperty("atp.catalogue.url",
                                StringUtils.EMPTY),
                        String.format(ENDPOINT_FOR_LINK_TO_TR, uuid)) + responseData.getTrId();
                BvHelper.completeSuccessfully(chainInstance, bvInstance, reportLink, successMessage,
                        responseData.getCompareResult());
                changeTcContextStatus(chainInstance.getContext().tc(), responseData.getCompareResult());
                return responseData.getTrId();
            } else {
                String errorMessage = "Response returned an error from the Bulk Validator while compare! Status "
                        + "Message " + responseData.getStatusMessage();
                chainInstance.getContext().tc().setStatus(Status.FAILED);
                BvHelper.endWithFailed(errorMessage, bvInstance);
            }
        }
        return null;
    }

    private static String processResponseCreateTr(BvResponseData responseData, CallChainInstance chainInstance,
                                                  BvInstance bvInstance, IntegrationConfig integrationConf,
                                                  BigInteger projectId) {
        if (responseData.getTrId() != null) {
            String successMessage =
                    "Response from Bulk Validator. Create new testrun for" + " test case " + responseData.getTcId()
                            + " : successfully!";
            String uuid = chainInstance.getContext().tc().getProjectUuid().toString(); // Old, already incorrect: =
            // BvHelper.getProjectUUID(projectId);
            String reportLink = BvHelper.normalizeUrl(ApplicationConfig.env.getProperty("atp.catalogue.url",
                            StringUtils.EMPTY),
                    String.format(ENDPOINT_FOR_LINK_TO_TR, uuid)) + responseData.getTrId();
            BvHelper.completeSuccessfully(chainInstance, bvInstance, reportLink, successMessage, "Not validated");
            return responseData.getTrId();
        } else {
            String errorMessage = "Response from Bulk Validator returned an error! New testrun not created. "
                    + "Status Message " + responseData.getStatusMessage();
            chainInstance.getContext().tc().setStatus(Status.FAILED);
            BvHelper.endWithFailed(errorMessage, bvInstance);
            return null;
        }
    }

    private static void changeTcContextStatus(TcContext tcContext, String compareResult) {
        Status calculatedValidationStatus = compareResult2contextStatus(compareResult);
        Status mergedStatus = mergeStatuses(tcContext.getStatus(), calculatedValidationStatus);
        if (!mergedStatus.equals(tcContext.getStatus())) {
            tcContext.setStatus(mergedStatus);
        }
    }

    private static Status compareResult2contextStatus(String compareResult) {
        if (compareResult == null) {
            return Status.PASSED;
        }
        switch (compareResult) {
            case "PASSED":
            case "SUCCESS":
            case "IDENTICAL":
            case "IGNORED":
            case "SKIPPED":
                return Status.PASSED; // There is no status for "IGNORED"/"SKIPPED" from BV
            case "SIMILAR":
                return Status.WARNING;
            default:
                return Status.FAILED;
        }
    }

    private static Status mergeStatuses(Status currentStatus, Status newStatus) {
        if (currentStatus.equals(Status.FAILED) || currentStatus.equals(Status.FAILED_BY_TIMEOUT)) {
            return currentStatus;
        } else {
            return newStatus;
        }
    }

    public static BvResponseData parseResponse(String inputResponse) {
        String response = checkResponse(inputResponse);
        try {
            return objectMapper.readValue(response, BvResponseData.class);
        } catch (IOException e) {
            ITF_LOGGER.debug("Parsing of response from Bulk Validator is completed with error:", e);
            throw new EngineIntegrationException(e);
        }
    }

    /*  In case response is JsonArray we parse response without brackets [] enclosing it!?
     *    I'm not sure that it's correct behaviour.
     *    It might be correct if JsonArray consists of the only element.
     *    TODO:  check all responses' types,
     *           identify the response which could be JsonArray,
     *           and check if such processing is correct
     * */
    private static String checkResponse(String inputResponse) {
        if (inputResponse.startsWith("[")) {
            return inputResponse.substring(1, inputResponse.length() - 1);
        } else {
            return inputResponse;
        }
    }

    public static QuickCompareResponse processQuickCompareResponse(String responseData) {
        QuickCompareResponse quickCompareResponse;
        try {
            quickCompareResponse = objectMapper.readValue(responseData, QuickCompareResponse.class);
        } catch (IOException e) {
            ITF_LOGGER.debug("Parsing of response from Bulk Validator is completed with error:", e);
            throw new EngineIntegrationException(e);
        }
        return quickCompareResponse;
    }

    public static TestcaseVobjectsResponse parseTestcaseVobjectsResponse(String inputResponse) {
        try {
            return BvEngineIntegration.GSON_INSTANCE.fromJson(inputResponse, TestcaseVobjectsResponse.class);
        } catch (Exception e) {
            ITF_LOGGER.debug("Parsing of response from Bulk Validator is completed with error:", e);
            throw new EngineIntegrationException(e);
        }
    }
}
