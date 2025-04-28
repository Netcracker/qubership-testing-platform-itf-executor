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

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.ATP_LOGGER_URL;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.bv.dto.CopyWithNameRequestDto;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.exception.EngineIntegrationException;
import org.qubership.automation.itf.integration.bv.BvIntegrationProperties;
import org.qubership.automation.itf.integration.bv.engine.BvInstance;
import org.qubership.automation.itf.integration.bv.messages.request.BvReadMode;
import org.qubership.automation.itf.integration.bv.messages.request.BvReadType;
import org.qubership.automation.itf.integration.bv.messages.request.BvSource;
import org.qubership.automation.itf.integration.bv.messages.request.Label;
import org.qubership.automation.itf.integration.bv.messages.request.ReportData;
import org.qubership.automation.itf.integration.bv.messages.request.RequestData;
import org.qubership.automation.itf.integration.bv.messages.request.Servers;
import org.qubership.automation.itf.integration.bv.messages.request.TestCase;
import org.qubership.automation.itf.integration.bv.messages.request.ValidationObject;
import org.qubership.automation.itf.integration.bv.messages.request.quickCompare.QuickCompareRequest;
import org.qubership.automation.itf.integration.bv.messages.request.quickCompare.ValidationParameter;
import org.qubership.automation.itf.integration.bv.messages.response.TestcaseVobjectsResponse;
import org.qubership.automation.itf.integration.bv.messages.response.Vobject;
import org.qubership.automation.itf.report.extension.TCContextRamExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class BvRequestBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BvRequestBuilder.class);
    private static final Pattern SERVER_REGEXP = Pattern.compile("//([^/]*)/?");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Splitter KEY_VALUE_MAP_SPLITTER = Splitter.on('\n').omitEmptyStrings().trimResults();

    /**
     * Produce Read-and-Compare request to BulkValidator.
     */
    public static RequestData buildReadCompareRequest(IntegrationConfig integrationConf,
                                                      CallChainInstance callchainInstance, BvInstance bvInstance) {
        RequestData requestData = new RequestData();
        Map<String, String> inputParameters = collectInputParams(integrationConf, callchainInstance);
        if (Boolean.parseBoolean(integrationConf.get(BvIntegrationProperties.REPORT_LINK_SWITCHER))) {
            Map<String, String> reportLinks = callchainInstance.getContext().tc().getReportLinks();
            inputParameters.put("URLtoISL", reportLinks.get(integrationConf.get(BvIntegrationProperties.URL_TO_ISL)));
        }
        String bvCaseId = getBVCaseId(callchainInstance);
        TestCase testCase = createTestCase(bvCaseId, inputParameters);
        requestData.setTestCases(new TestCase[]{testCase});
        String confFromIntegrationTab = integrationConf.get(BvIntegrationProperties.BV_CONF_PATH);
        String bvConfPath = BvHelper.buildBVConfPath(confFromIntegrationTab, callchainInstance);
        Servers servers = new Servers(bvConfPath,
                new String[]{integrationConf.get(BvIntegrationProperties.BV_SOURCE).trim()});
        requestData.setServers(new Servers[]{servers});
        requestData.setReadMode(BvReadMode.AR);
        requestData.setReadType(BvReadType.READ);
        setAtpReportRequestSettingsIfNotNull(requestData, bvInstance);
        return requestData;
    }

    /**
     * Produce Compare request to BulkValidator.
     */
    public static RequestData buildCompareRequest(String trId, BvInstance bvInstance) {
        RequestData requestData = new RequestData();
        TestCase testCase = createTestCaseForCompareRequest(trId);
        requestData.setTestCases(new TestCase[]{testCase});
        setAtpReportRequestSettingsIfNotNull(requestData, bvInstance);
        return requestData;
    }

    /**
     * Produce 'Create New Test Run' request to BulkValidator.
     */
    public static RequestData buildCreateNewTRRequest(
            IntegrationConfig integrationConf,
            CallChainInstance callChainInstance,
            BvInstance bvInstance) {
        RequestData requestData = new RequestData();
        String bvCaseId = getBVCaseId(callChainInstance);
        requestData.setTcId(bvCaseId);
        Map<String, String> inputParameters = collectInputParams(integrationConf, callChainInstance);
        if (!inputParameters.isEmpty()) {
            requestData.setInputParameters(inputParameters);
        }
        requestData.setValidationObjects(collectValidationObjects(integrationConf, callChainInstance));
        setAtpReportRequestSettingsIfNotNull(requestData, bvInstance);
        return requestData;
    }

    /**
     * Produce 'Create Test Case' request to BulkValidator.
     * Test case configuration is based on callChain, integrationConf and properties parameters.
     */
    public static RequestData buildCreateRequestData(
            CallChain callChain,
            IntegrationConfig integrationConf,
            Map<String, String> properties) {
        BvSource source = new BvSource();
        source.setSourceName(integrationConf.get(BvIntegrationProperties.BV_SOURCE).trim());
        List<Label> labels = new ArrayList<>();
        for (String chainLabel : callChain.getLabels()) {
            Label label = new Label();
            label.setName(chainLabel);
            labels.add(label);
        }
        return fillRequestData(callChain.getName() + " " + properties.get("dataset.name"),
                integrationConf.get(BvIntegrationProperties.BV_TEST_CASE_TYPE), new BvSource[]{source}, labels);
    }

    /**
     * Produce 'Create Test Case' request to BulkValidator.
     * Test case configuration is based on situation and integrationConf parameters.
     */
    public static RequestData buildCreateRequestData(Situation situation, IntegrationConfig integrationConf) {
        BvSource source = new BvSource();
        source.setSourceName(integrationConf.get(BvIntegrationProperties.BV_SOURCE).trim());
        ArrayList<Label> labels = new ArrayList<>();
        return fillRequestData(situation.getParent().getName() + " / " + situation.getName(),
                integrationConf.get(BvIntegrationProperties.BV_TEST_CASE_TYPE), new BvSource[]{source}, labels);
    }

    /**
     * Produce 'Copy-with-name' request to BulkValidator.
     */
    public static CopyWithNameRequestDto buildCopyRequestData(String newName, String sourceTcId) {
        CopyWithNameRequestDto copyWithNameRequest = new CopyWithNameRequestDto();
        copyWithNameRequest.setNewName(newName);
        copyWithNameRequest.setSourceTcId(sourceTcId);
        return copyWithNameRequest;
    }

    private static RequestData fillRequestData(String name, String type, BvSource[] sources, List<Label> labels) {
        RequestData requestData = new RequestData();
        requestData.setName(name);
        requestData.setType(type);
        requestData.setSources(sources);
        requestData.setLabels(labels);
        return requestData;
    }

    /**
     * Build a request to BulkValidator.
     */
    public static RequestData buildSimpleRequestData(Storable storable, Map<String, String> properties) {
        if (storable instanceof CallChain) {
            return new RequestData(((CallChain) storable).getBvCases().get(properties.get("dataset.name")));
        } else {
            return new RequestData(((Situation) storable).getBvTestcase());
        }
    }

    /**
     * Produce 'Read' request to BulkValidator.
     */
    public static RequestData buildReadRequestData(
            CallChain callChain,
            IntegrationConfig integrationConf,
            Map<String, String> properties) {
        Map<String, String> inputParameters = new HashMap<>();
        inputParameters.put("URLtoISL", properties.get("islLink"));
        String bvCaseId = callChain.getBvCases().get(properties.get("dataset.name"));
        TestCase testCase = createTestCase(bvCaseId, inputParameters);
        Servers server = new Servers(getServerName(properties.get("islLink")),
                new String[]{integrationConf.get(BvIntegrationProperties.BV_SOURCE).trim()});
        RequestData requestData = new RequestData();
        requestData.setReadMode(BvReadMode.ER);
        requestData.setReadType(BvReadType.CLEAR_AND_REWRITE);
        requestData.setTestCases(new TestCase[]{testCase});
        requestData.setServers(new Servers[]{server});
        return requestData;
    }

    /**
     * Produce 'Quick Compare' request to BulkValidator.
     */
    public static QuickCompareRequest buildQuickCompareRequest(
            String situationName,
            Message message,
            String bvCaseId) {
        QuickCompareRequest requestData = new QuickCompareRequest();
        requestData.setTcId(bvCaseId);
        List<ValidationParameter> children = new ArrayList<>();
        ValidationParameter childObject = new ValidationParameter();
        childObject.setName("incoming");
        childObject.setAr(message.getText());
        children.add(childObject);
        ValidationParameter parentObject = new ValidationParameter();
        parentObject.setName(situationName);
        parentObject.setChildren(children);
        List<ValidationParameter> validationObjects = new ArrayList<>();
        validationObjects.add(parentObject);
        requestData.setValidationObjects(validationObjects);
        return requestData;
    }

    /**
     * Produce 'Quick Compare' request to BulkValidator.
     */
    public static QuickCompareRequest buildQuickCompareRequest(
            SituationInstance instance,
            Situation situation,
            SpContext sp,
            TestcaseVobjectsResponse testcaseVobjectsResponse,
            String bvCaseId) {
        if (testcaseVobjectsResponse.getVObjects() == null || testcaseVobjectsResponse.getVObjects().isEmpty()) {
            throw new EngineIntegrationException("There is no validation parameters in BV testcase, id=" + bvCaseId);
        }
        QuickCompareRequest requestData = new QuickCompareRequest();
        requestData.setTcId(bvCaseId);
        requestData.setLoadHighlight(true);
        TcContext tc = instance.getContext().getTC();
        JsonContext saved = new JsonContext();
        try {
            Object savedObj = tc.get("saved");
            if (savedObj instanceof Map) {
                saved.putAll((Map) savedObj);
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred during unpacking tc.saved: [" + tc.get("saved").toString() + "]: ", e);
        }
        List<ValidationParameter> validationObjects = new ArrayList<>();
        for (Vobject obj : testcaseVobjectsResponse.getVObjects()) {
            ValidationParameter parentObject = new ValidationParameter();
            parentObject.setName(obj.getName());
            if (obj.getChilds().isEmpty()) {
                // This vobject must be validated. If there are children - only children must be validated
                parentObject.setAr(getValueFromContext(saved, sp, obj.getName()));
                parentObject.setValue(TemplateEngineFactory.get().process(situation,
                        obj.getDecodedValue(), instance.getContext(), "ER value of '" + obj.getName() + "'"));
            } else {
                List<ValidationParameter> children = new ArrayList<>();
                for (Vobject child : obj.getChilds()) {
                    ValidationParameter childObject = new ValidationParameter();
                    childObject.setName(child.getName());
                    childObject.setAr(getValueFromContext(saved, sp, child.getName()));
                    childObject.setValue(TemplateEngineFactory.get().process(situation,
                            child.getDecodedValue(), instance.getContext(), "ER value of '" + child.getName() + "'"));
                    children.add(childObject);
                }
                parentObject.setChildren(children);
            }
            validationObjects.add(parentObject);
        }
        requestData.setValidationObjects(validationObjects);
        return requestData;
    }

    private static String getValueFromContext(JsonContext savedMap, SpContext sp, String varName) {
        if (varName.equals("incoming")) {
            return getMessageBody(sp.getIncomingMessage());
        } else if (varName.equals("outgoing")) {
            return getMessageBody(sp.getOutgoingMessage());
        } else {
            if (sp.containsKey(varName)) {
                return value2String(sp.get(varName));
            }
            if (savedMap != null) {
                if (savedMap.containsKey(varName)) {
                    return value2String(savedMap.get(varName));
                }
            }
        }
        return "";
    }

    private static String value2String(Object obj) {
        try {
            if (obj == null || obj.toString().isEmpty()) {
                return "";
            } else if (obj instanceof ArrayList) {
                List objList = (ArrayList) obj;
                if (!objList.isEmpty()) {
                    try {
                        List<Object> newobjs = new ArrayList<>();
                        for (Object o : objList) {
                            if (o instanceof String) {
                                newobjs.add(mapper.readTree(o.toString()));
                            } else {
                                newobjs.add(o);
                            }
                        }
                        return mapper.writeValueAsString(newobjs);
                    } catch (Exception ex1) {
                        return mapper.writeValueAsString(obj);
                    }
                } else {
                    return "[]";
                }
            } else if (obj instanceof Map) {
                return mapper.writeValueAsString(obj);
            } else if (obj instanceof String) {
                String sobj = obj.toString();
                if (sobj.startsWith("{") || sobj.startsWith("[")) {
                    JsonNode jn = mapper.readTree(sobj);
                    return mapper.writeValueAsString(jn);
                } else {
                    return sobj;
                }
            } else {
                return obj.toString();
            }
        } catch (Exception ex) {
            return obj.toString();
        }
    }

    private static String getMessageBody(Message message) {
        return (message == null) ? "" : message.getText();
    }

    /**
     * Prepare an object containing settings to log execution results to ATP/ATP2.
     * Then the object will be sent as a part of request to BulkValidator,
     * and BulkValidator will log to ATP/ATP2 using these settings (under test run).
     * Point to discuss (Aleksandr Kapustin, 2018-04-06):
     * - May be, we should create new section here. And report BVIntegration exceptions to it too.
     *
     * @param bvInstance BvInstance extension containing test run info to log to RAM2.
     * @return ReportData object with required info.
     */
    private static ReportData getAtpReportRequestSettings(BvInstance bvInstance) {
        ReportData report = new ReportData();
        TCContextRamExtension ramExtension = bvInstance.getContext().tc().getExtension(TCContextRamExtension.class);
        if (ramExtension != null) {
            BigInteger runId = ramExtension.getRunId();
            TestRunContext ram2Context = ramExtension.getRunContext();
            if (runId == null && ram2Context == null) {
                return null; // BV will not report to ATP because ITF couldn't send all required parameters 
            }
            report.setBuilder("ATP_RAM_REPORT");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("atpAdapterMode", "ntt");
            if (runId != null) {
                parameters.put("atpTestRunId", runId.toString());
                BigInteger sectionId = ramExtension.getSectionId();
                if (sectionId != null) {
                    parameters.put("atpSectionId", sectionId.toString());
                    LOGGER.debug("BV_REQUEST: ATP_SECTION_ID: {}", sectionId);
                }
            }
            String reportUrl = ramExtension.getReportUrl();
            if (!StringUtils.isEmpty(reportUrl)) {
                String[] tokens = reportUrl.split("/");
                parameters.put("atpRamUrl", tokens[0] + "//" + tokens[2]);
            }
            LOGGER.debug("BV_REQUEST: Ram2Context:{}", ram2Context != null);
            if (ram2Context != null) {
                parameters.put("ram2TestRunId", ram2Context.getTestRunId());
                LOGGER.debug("BV_REQUEST: ram2TestRunId:{}", ram2Context.getTestRunId());
                String ram2SectionId = ram2Context.getCurrentSectionId();
                String ram2SectionName = "ITF Integration";
                AtpCompaund compaund = ram2Context.getAtpCompaund();
                if (compaund != null) {
                    ram2SectionId = compaund.getSectionId();
                    ram2SectionName = compaund.getSectionName();
                }
                parameters.put("ram2SectionId", ram2SectionId);
                parameters.put("ram2SectionName", ram2SectionName);
                parameters.put("atpRamLoggerUrl", System.getProperty(ATP_LOGGER_URL));
                if (StartedFrom.RAM2.equals(ramExtension.getStartedFrom())) {
                    parameters.put("atpReportTo", "ATP RAM 2");
                }
                LOGGER.debug("BV_REQUEST: {}, {}", ram2SectionId, ram2SectionName);
            }
            parameters.put("atpAdapterMode", "ntt");
            parameters.put("atpCustomer", "DT");
            parameters.put("atpProject", "Demo");
            report.setParameters(parameters);
            return report;
        }
        return null;
    }

    private static void setAtpReportRequestSettingsIfNotNull(RequestData requestData, BvInstance bvInstance) {
        ReportData reportData = getAtpReportRequestSettings(bvInstance);
        if (reportData != null) {
            requestData.setReport(reportData);
        }
    }

    private static TestCase createTestCase(String bvCaseId, Map<String, String> inputParameters) {
        TestCase testCase = new TestCase();
        testCase.setTcId(bvCaseId);
        if (!inputParameters.isEmpty()) {
            testCase.setInputParameters(inputParameters);
        }
        return testCase;
    }

    private static TestCase createTestCaseForCompareRequest(String trId) {
        TestCase testCases = new TestCase();
        testCases.setTrId(trId);
        return testCases;
    }

    private static Map<String, String> collectInputParams(IntegrationConfig integrationConf,
                                                          CallChainInstance callChainInstance) {
        Map<String, String> inputParameters = new HashMap<>();
        String inputContextKeys = integrationConf.get(BvIntegrationProperties.INPUT_PARAMS_CONTEXT_KEYS);
        if (!Strings.isNullOrEmpty(inputContextKeys)) {
            Iterable<String> keys = KEY_VALUE_MAP_SPLITTER.split(inputContextKeys);
            for (String key : keys) {
                String[] pair = key.split("=", 2);
                if (pair.length < 2) {
                    LOGGER.warn("BV Testcase Input parameters - incorrect entry format of '{}'", key);
                    continue;
                }
                String bvKeyValue = TemplateEngineFactory.get().process(callChainInstance.getStepContainer(),
                        pair[1], callChainInstance.getContext(), "BV Testcase Input parameter '" + pair[0] + "'");
                inputParameters.put(pair[0], bvKeyValue);
            }
        }
        return inputParameters;
    }

    private static List<ValidationObject> collectValidationObjects(IntegrationConfig integrationConf,
                                                                   CallChainInstance callChainInstance) {
        String validateContextKeys = integrationConf.get(BvIntegrationProperties.VALIDATE_PARAMS_CONTEXT_KEYS);
        if (Strings.isNullOrEmpty(validateContextKeys)) {
            throw new EngineIntegrationException("Validation parameters for BulkValidator are not specified");
        }
        Iterable<String> validateKeys = KEY_VALUE_MAP_SPLITTER.split(validateContextKeys);
        List<ValidationObject> validationObjects = new ArrayList<>();
        for (String key : validateKeys) {
            String[] pair = key.split("=", 2);
            if (pair.length < 2) {
                LOGGER.warn("BV Testcase Validation parameters - incorrect entry format of '{}'", key);
                continue;
            }
            String bvKeyVal = TemplateEngineFactory.get().process(callChainInstance.getStepContainer(),
                    pair[1], callChainInstance.getContext(), "BV Testcase Validation parameter '" + pair[0] + "'");
            validationObjects.add(new ValidationObject(pair[0], bvKeyVal, "XML"));
        }
        return validationObjects;
    }

    public static String getBVCaseId(CallChainInstance callChainInstance) {
        CallChain callchain = (CallChain) callChainInstance.getSource();
        return callchain.getBvCases().get(callChainInstance.getDatasetName());
    }

    private static String getServerName(String serverUrl) {
        try {
            Matcher m = SERVER_REGEXP.matcher(serverUrl);
            m.find();
            return m.group(1);
        } catch (Exception e) {
            throw new EngineIntegrationException("Error while building server name, URL is not correct: "
                    + ((StringUtils.isBlank(serverUrl)) ? "null or empty" : serverUrl) + "!", e);
        }
    }
}
