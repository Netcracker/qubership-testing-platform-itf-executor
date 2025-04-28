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

package org.qubership.automation.itf.integration.atp.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvironmentManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByParameterAndProjectIdManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.ServerObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemObjectManager;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.constants.StartedFrom;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.service.report.Report;
import org.qubership.automation.itf.integration.atp.action.ATPActionFactory;
import org.qubership.automation.itf.integration.atp.action.model.impl.AbstractAtpAction;
import org.qubership.automation.itf.integration.atp.model.ActionEntity;
import org.qubership.automation.itf.integration.atp.model.ConfigurationEntity;
import org.qubership.automation.itf.integration.atp.model.ContextEntity;
import org.qubership.automation.itf.integration.atp.model.DataSetEntity;
import org.qubership.automation.itf.integration.atp.model.DataSetItem;
import org.qubership.automation.itf.integration.atp.model.ResourceAttribute;
import org.qubership.automation.itf.integration.atp.model.ScopeEntity;
import org.qubership.automation.itf.integration.atp.model.StepEntity;
import org.qubership.automation.itf.integration.atp.model.TestAutomationResource;
import org.qubership.automation.itf.integration.atp.model.ram2.Ram2ExecuteStepRequest;
import org.qubership.automation.itf.report.extension.InstanceRamExtension;
import org.qubership.automation.itf.report.extension.TCContextRamExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;

public class TestRunInfo {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestRunInfo.class);
    private static final String INBOUND = "inbound";
    private static final String OUTBOUND = "outbound";
    private String project;
    private BigInteger testRunId;
    private String ram2TestRunId;
    private String testRunName;
    private DataSetEntity dataSet;
    private String atpRamUrl;
    private ContextEntity context;
    private JsonContext contextToMerge;
    private JSONObject atpContext;
    private BigInteger logRecordId;
    private ScopeEntity scope;
    private ConfigurationEntity configuration;
    private List<CallchainRunInfo> callchainsToExecute = new ArrayList<>();
    private Environment environment;
    private Map<String, Set<System>> systems = new HashMap<>();
    private Map<String, String> tcpDumpOptions = null;
    private String bvAction = null;
    private TestRunContext ramTestRunContext;
    private UUID atpEnvironmentId;

    private boolean validateMessageOnStep = false;
    private boolean prepared = false;

    private StartedFrom startedFrom;

    private BigInteger projectId;

    private UUID projectUuid;

    public BigInteger getTestRunId() {
        return testRunId;
    }

    public void setTestRunId(BigInteger testRunId) {
        this.testRunId = testRunId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getTestRunName() {
        return testRunName;
    }

    public void setTestRunName(String testRunName) {
        this.testRunName = testRunName;
    }

    public DataSetEntity getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSetEntity dataSet) {
        this.dataSet = dataSet;
    }

    public String getAtpRamUrl() {
        return atpRamUrl;
    }

    public void setAtpRamUrl(String atpRamUrl) {
        this.atpRamUrl = atpRamUrl;
    }

    public ContextEntity getContext() {
        return context;
    }

    public void setContext(ContextEntity context) {
        this.context = context;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public StartedFrom getStartedFrom() {
        return startedFrom;
    }

    public void setStartedFrom(StartedFrom startedFrom) {
        this.startedFrom = startedFrom;
    }

    public String getRam2TestRunId() {
        return ram2TestRunId;
    }

    public void setRam2TestRunId(String ram2TestRunId) {
        this.ram2TestRunId = ram2TestRunId;
    }

    public JsonContext getContextToMerge() {
        JsonContext context = new JsonContext();
        context.putAll(contextToMerge);
        return context;
    }

    public void fillParamsFromRequest(Ram2ExecuteStepRequest request) {
        setContext(request.getContext());
        setProjectInfo(request.getContext());
        setLogRecordId(request.getLogRecordId());
        setScope(request.getScope());
        setConfiguration(request.getConfiguration());
        setProject(request.getProjectName());
        setAtpRamUrl(request.getAtpRamUrl());
        setAtpEnvironmentId(request.getContext());
    }

    public void build() {
        prepareContext();
        LOGGER.debug("{} - Context is prepared", erInfo());
        prepareScope();
        LOGGER.debug("{} - Scope is prepared", erInfo());
        prepareEnvironment();
        LOGGER.debug("{} - Environment is prepared", erInfo());
        prepared = true;
    }

    public AbstractInstance createInstanceForReport() {
        TCContextRamExtension tcContextRamExtension = new TCContextRamExtension();
        tcContextRamExtension.setAsync(false);
        tcContextRamExtension.setRunId(getTestRunId());
        tcContextRamExtension.setRunContext(ramTestRunContext);
        TcContext tc = new TcContext();
        if (ramTestRunContext.getAtpCompaund() != null) {
            tc.setID(ramTestRunContext.getAtpCompaund().getSectionId());
        }
        tc.setStartedFrom(startedFrom);
        tc.setProjectId(projectId);
        tc.setProjectUuid(projectUuid);
        tc.setNeedToReportToAtp(true);
        tc.extend(tcContextRamExtension);
        InstanceRamExtension extension = new InstanceRamExtension();
        extension.setSectionId(getLogRecordId());
        CallChainInstance instance = new CallChainInstance();
        instance.setName("Errors");
        instance.setID(BigInteger.valueOf(1000 + (int) (Math.random() * (999001))));
        instance.getContext().setTC(tc);
        instance.extend(extension);
        tc.setInitiator(instance);
        return instance;
    }

    private void prepareScope() {
        for (StepEntity entity : scope.getSteps()) {
            for (ActionEntity actionEntity : entity.getActions()) {
                boolean actionWasIdentified = false;
                for (AbstractAtpAction atpAction : ATPActionFactory.getActions()) {
                    Pattern atpActionPattern = Pattern.compile(atpAction.getTemplate());
                    Matcher matcher = atpActionPattern.matcher(actionEntity.getName());
                    if (matcher.find()) {
                        if (matcher.groupCount() >= 1) {
                            atpAction.setTestRunInfoParams(this, matcher);
                            actionWasIdentified = true;
                            break;
                        }
                    }
                }
                // Previous variant (actionName == callChainName). We should be able to process it too.
                if (!actionWasIdentified) {
                    String callChainName = actionEntity.getName();
                    if (StringUtils.isBlank(callChainName)) {
                        /*  Empty or null name of callChain - it's a tester's mistake.
                            May be, we should throw an exception here.
                         */
                        continue;
                    }
                    CallChain callChain =
                            Iterables.getFirst(((SearchByParameterAndProjectIdManager<CallChain>)
                                    CoreObjectManager.getInstance()
                                            .getSpecialManager(CallChain.class,
                                                    SearchByParameterAndProjectIdManager.class))
                                    .getByNameAndProjectId(callChainName, getProjectId()), null);
                    if (callChain != null) {
                        callchainsToExecute.add(new CallchainRunInfo(callChain, null));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void prepareContext() {
        contextToMerge = new JsonContext();
        JSONObject dataSetObject = new JSONObject();
        if (dataSet != null) {
            for (DataSetItem dataSetItem : dataSet.getVariables()) {
                String dataSetItemName = dataSetItem.getName();
                if (itemNameNotValid(dataSetItemName)) {
                    continue;
                }
                String dataSetItemValue = dataSetItem.getValue();
                if (dataSetItemName.contains(".")) {
                    dataSetObject = parseItem(dataSetItemName, dataSetItemValue, dataSetObject);
                } else {
                    dataSetObject.put(dataSetItemName, dataSetItemValue);
                }
            }
            contextToMerge.merge(dataSetObject);
            contextToMerge.put("dataset", dataSetObject);
        }
        try {
            atpContext = (JSONObject) new JSONParser().parse(context.getJsonString());
            JSONObject atpContextObject = new JSONObject();
            performContext(atpContext, atpContextObject);
            contextToMerge.merge(atpContextObject);
        } catch (Exception e) {
            LOGGER.error("Cannot parse ATP context ", e);
            throw reportException("Cannot parse ATP context because of error: " + e.getMessage());
        }
    }

    protected void performContext(JSONObject flatContext, JSONObject contextObject) {
        for (String item : (Iterable<String>) flatContext.keySet()) {
            if (item.contains(".")) {
                Object value = (flatContext.get(item) == null) ? null : flatContext.get(item);
                contextObject = parseItem(item, value, contextObject);
            } else {
                parseItemValue(item, flatContext, contextObject);
            }
        }
    }

    private boolean itemNameNotValid(String name) {
        return StringUtils.isEmpty(name) || ".".equals(name) || StringUtils.isWhitespace(name);
    }

    @SuppressWarnings("unchecked")
    protected JSONObject parseItem(String contextItem, Object itemValue, JSONObject contextObject) {
        String[] arrayOfElements = contextItem.split("\\.");
        int amountOfElements = arrayOfElements.length - 1;
        int elementNumber = 0;
        String element;
        String nextElement;
        JSONObject currentObject = contextObject;
        if (elementNumber == amountOfElements && currentObject.containsKey(contextItem)) {
            return contextObject;
        }
        while (elementNumber < amountOfElements) {
            element = arrayOfElements[elementNumber];
            nextElement = arrayOfElements[elementNumber + 1];
            if (!currentObject.containsKey(element)) {
                currentObject.put(element, new JSONObject());
            }
            Object obj = currentObject.get(element);
            if (!(obj instanceof JSONObject)) {
                currentObject.put(element, new JSONObject());
            }
            JSONObject in = (JSONObject) currentObject.get(element); // get into created/existing (element)
            if (!in.containsKey(nextElement)) {
                // if element (key) is the last -> add empty string to value for this element and fill it bellow
                ((Map) currentObject.get(element)).put(nextElement, amountOfElements - elementNumber != 1
                        ? new JSONObject() : "");
            }
            //take the next element from array of elements
            elementNumber++;
            currentObject = (JSONObject) currentObject.get(element);
        }
        element = arrayOfElements[arrayOfElements.length - 1]; //last element
        if ((itemValue instanceof String && StringUtils.isEmpty((String) itemValue)) || Objects.isNull(itemValue)) {
            currentObject.putIfAbsent(element, itemValue);
        } else if (itemValue instanceof JSONObject) {
            JSONObject childJsonObject = (JSONObject) itemValue;
            JSONObject newChildJsonObject = new JSONObject();
            performContext(childJsonObject, newChildJsonObject);
            if (currentObject.containsKey(element) && "".equals(currentObject.get(element))) {
                currentObject.remove(element);
            }
            currentObject.putIfAbsent(element, newChildJsonObject);
        } else if (itemValue instanceof JSONArray) {
            JSONArray childJsonArray = (JSONArray) itemValue;
            JSONArray newChildJsonArray = new JSONArray();
            for (Object obj : childJsonArray) {
                if (obj instanceof JSONObject) {
                    JSONObject childJsonObject = (JSONObject) obj;
                    JSONObject newChildJsonObject = new JSONObject();
                    performContext(childJsonObject, newChildJsonObject);
                    newChildJsonArray.add(newChildJsonObject);
                } else {
                    newChildJsonArray.add(obj);
                }
            }
            currentObject.put(element, newChildJsonArray);
        } else {
            currentObject.put(element, itemValue);
        }
        return contextObject;
    }

    private void parseItemValue(String item, JSONObject flatContext, JSONObject contextObject) {
        if (flatContext.get(item) instanceof JSONObject) {
            JSONObject childJsonObject = (JSONObject) flatContext.get(item);
            JSONObject newChildJsonObject = new JSONObject();

            performContext(childJsonObject, newChildJsonObject);

            contextObject.putIfAbsent(item, newChildJsonObject);

        } else if (flatContext.get(item) instanceof JSONArray) {
            JSONArray childJsonArray = (JSONArray) flatContext.get(item);
            JSONArray newChildJsonArray = new JSONArray();

            for (Object obj : childJsonArray) {
                if (obj instanceof JSONObject) {
                    JSONObject childJsonObject = (JSONObject) obj;
                    JSONObject newChildJsonObject = new JSONObject();
                    performContext(childJsonObject, newChildJsonObject);
                    newChildJsonArray.add(newChildJsonObject);
                } else {
                    newChildJsonArray.add(obj);
                }
            }
            contextObject.putIfAbsent(item, newChildJsonArray);
        } else {
            contextObject.putIfAbsent(item, flatContext.get(item));
        }
    }

    private void prepareEnvironment() {
        String url = getUrl();
        if (StringUtils.isBlank(url)) {
            throw reportException("Can't determine environment; cause: configuration#currentResource.URL is null or "
                    + "empty!");
        } else {
            getSystemsFromCallChains();
            if (!systems.get(OUTBOUND).isEmpty()) {
                List<Server> servers = CoreObjectManager.getInstance()
                        .getSpecialManager(Server.class, ServerObjectManager.class)
                        .getByUrlSlashedAndProjectId(url, this.getProjectId());
                if (servers != null && !servers.isEmpty()) {
                    Environment env = CoreObjectManager.getInstance()
                            .getSpecialManager(Environment.class, EnvironmentManager.class)
                            .findByServerAndSystems(
                                    (BigInteger) servers.get(0).getID(),
                                    getIds(systems.get(OUTBOUND)));
                    if (env != null) {
                        environment = env;
                    } else {
                        throw reportException(String.format("Can't determine environment by "
                                + "configuration#currentResource"
                                + ".URL (URL is: '%s'); cause: There is no environment with proper system+server "
                                + "settings.", url));
                    }
                } else {
                    throw reportException(String.format("Can't determine environment; cause: Server is not found by "
                            + "configuration#currentResource.URL (URL is: '%s')!", url));
                }
            } else {
                throw reportException(String.format("Can't determine environment by configuration#currentResource.URL"
                        + " (URL is: '%s'); cause: Systems List is empty for callchain!", url));
            }
        }
    }

    private List<BigInteger> getIds(Set<System> objs) {
        return objs.stream().map(system -> (BigInteger) system.getID()).collect(Collectors.toList());
    }

    private void getSystemsFromCallChains() {
        Set<System> inboundSystems = new HashSet<>();
        Set<System> outboundSystems = new HashSet<>();
        Set<Object> chainIds = new HashSet<>();

        systems.put(INBOUND, inboundSystems);
        systems.put(OUTBOUND, outboundSystems);

        for (CallchainRunInfo callchainRunInfo : callchainsToExecute) {
            if (callchainRunInfo.getCallChain() != null
                    && !(chainIds.contains(callchainRunInfo.getCallChain().getID()))) {
                outboundSystems.addAll(CoreObjectManager.getInstance()
                        .getSpecialManager(System.class, SystemObjectManager.class)
                        .getReceiverSystemsFromCallChainSteps(callchainRunInfo.getCallChain().getID()));
                chainIds.add(callchainRunInfo.getCallChain().getID());
            }
        }
    }

    public BigInteger getLogRecordId() {
        return logRecordId;
    }

    public void setLogRecordId(BigInteger logRecordId) {
        this.logRecordId = logRecordId;
    }

    public ScopeEntity getScope() {
        return scope;
    }

    public void setScope(ScopeEntity scope) {
        this.scope = scope;
    }

    public ConfigurationEntity getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConfigurationEntity configuration) {
        this.configuration = configuration;
    }

    public ContextEntity getContextToReturn() {
        ContextEntity contextEntity = new ContextEntity();
        contextEntity.setJsonString((atpContext == null) ? "{}" : atpContext.toJSONString());
        return contextEntity;
    }

    private String getUrl() {
        TestAutomationResource currentResource = configuration.getCurrentResource();
        if (currentResource != null) {
            List<ResourceAttribute> attributes = currentResource.getAttributes();
            if (attributes != null) {
                for (ResourceAttribute attribute : attributes) {
                    if ("url".equals(attribute.getName())) {
                        return attribute.getValue();
                    }
                }
            }
            throw reportException(String.format("Can't determine environment; cause: configuration#currentResource "
                            + "(id: %s, type: %s): 'url' attribute is missed!", currentResource.getId(),
                    currentResource.getType()));
        } else {
            throw reportException("Can't determine environment; cause: configuration#currentResource is missed!");
        }
    }

    public Map<String, String> getTcpDumpOptions() {
        return tcpDumpOptions;
    }

    public void setTcpDumpOptions(Map<String, String> tcpDumpOptions) {
        this.tcpDumpOptions = tcpDumpOptions;
    }

    public String getBvAction() {
        return bvAction;
    }

    public void setBvAction(String bvAction) {
        this.bvAction = bvAction;
    }

    public List<CallchainRunInfo> getCallchainsToExecute() {
        return callchainsToExecute;
    }

    public void setCallchainsToExecute(List<CallchainRunInfo> callchainsToExecute) {
        this.callchainsToExecute = callchainsToExecute;
    }

    public TestRunContext getRamTestRunContext() {
        return ramTestRunContext;
    }

    public void setRamTestRunContext(TestRunContext ramTestRunContext) {
        this.ramTestRunContext = ramTestRunContext;
    }

    public boolean isValidateMessageOnStep() {
        return validateMessageOnStep;
    }

    public void setValidateMessageOnStep(boolean validateMessageOnStep) {
        this.validateMessageOnStep = validateMessageOnStep;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public IllegalArgumentException reportException(String message) {
        LOGGER.error("{} failed: {}", erInfo(), message);
        IllegalArgumentException exception = new IllegalArgumentException(message);
        AbstractInstance abstractInstance = createInstanceForReport();
        Report.openSection(abstractInstance, "Errors");
        Report.error(abstractInstance, exception.getMessage(), exception.getMessage(), exception);
        Report.closeSection(abstractInstance);
        Report.stopRun(abstractInstance.getContext(), Status.FAILED);
        return exception;
    }

    private String erInfo() {
        return "ER from " + this.startedFrom.toString()
                + " (project: " + this.project
                + ", RunId: " + this.ram2TestRunId
                + ", Test Run Name: "
                + this.testRunName + ")";
    }

    public void reportError(String sectionName, String title, String message, Throwable exception) {
        AbstractInstance abstractInstance = createInstanceForReport();
        Report.openSection(abstractInstance, sectionName);
        Report.error(abstractInstance, title, message, exception);
        Report.closeSection(abstractInstance);
        Report.stopRun(abstractInstance.getContext(), Status.FAILED);
    }

    public BigInteger getProjectId() {
        return projectId;
    }

    public void setProjectId(BigInteger projectId) {
        this.projectId = projectId;
    }

    public java.util.UUID getProjectUuid() {
        return projectUuid;
    }

    public void setProjectUuid(java.util.UUID projectUuid) {
        this.projectUuid = projectUuid;
    }

    private void setProjectInfo(ContextEntity contextEntity) {
        try {
            String projectUUID = TestRunInfoBuilder.getParameterValueFromContextEntity(contextEntity, "projectId");
            if (StringUtils.isEmpty(projectUUID)) {
                String error = "Project UUID from ATP-request is empty! ITF-project can not be found!";
                LOGGER.error(error);
                throw new IllegalArgumentException(error);
            } else {
                UUID projectUuid = UUID.fromString(projectUUID);
                BigInteger internalProjectId = CoreObjectManager.getInstance().getSpecialManager(StubProject.class,
                        SearchManager.class).getEntityInternalIdByUuid(projectUuid);
                if (internalProjectId == null) {
                    String error = String.format("ITF-project was not found by UUID = %s", projectUuid);
                    LOGGER.error(error);
                    throw new IllegalArgumentException(error);
                } else {
                    setProjectId(internalProjectId);
                    setProjectUuid(projectUuid);
                }
            }
        } catch (JsonProcessingException e) {
            String error = "Can not parse the request from ATP to get the project UUID!";
            LOGGER.error(error);
            throw new IllegalArgumentException(error, e);
        }
    }

    public UUID getAtpEnvironmentId() {
        return atpEnvironmentId;
    }

    private void setAtpEnvironmentId(ContextEntity contextEntity) {
        try {
            String environmentId = TestRunInfoBuilder
                    .getParameterValueFromContextEntity(contextEntity, "environmentId");
            if (StringUtils.isEmpty(environmentId)) {
                String error = "Environment UUID from ATP-request is empty! Please check parameter 'environmentId'"
                        + " in ATP context.";
                LOGGER.error(error);
                throw new IllegalArgumentException(error);
            } else {
                atpEnvironmentId = UUID.fromString(environmentId);
            }
        } catch (JsonProcessingException e) {
            String error = "Can not parse the request from ATP to get the environment UUID!";
            LOGGER.error(error);
            throw new IllegalArgumentException(error, e);
        }
    }
}
