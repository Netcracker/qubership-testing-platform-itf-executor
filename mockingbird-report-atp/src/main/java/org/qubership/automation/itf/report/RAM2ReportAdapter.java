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

package org.qubership.automation.itf.report;

import static org.qubership.automation.itf.core.instance.situation.TCContextDiffCache.TC_CONTEXT_DIFF_CACHE;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.atp.adapter.common.AtpRamAdapter;
import org.qubership.atp.adapter.common.adapters.providers.RamAdapterProvider;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.ram.enums.OpenMode;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.CustomLink;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.logrecords.ItfLogRecord;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.RequestHeader;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import org.qubership.automation.itf.core.instance.step.impl.IntegrationStepHelper;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.report.ReportLinkCreator;
import org.qubership.automation.itf.core.util.FlatMapUtil;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.SituationLevelValidation;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.manager.ExtensionManager;
import org.qubership.automation.itf.core.util.transport.service.report.ReportAdapter;
import org.qubership.automation.itf.executor.transports.holder.TransportHolder;
import org.qubership.automation.itf.report.extension.InstanceRam2Extension;
import org.qubership.automation.itf.report.extension.TCContextRamExtension;
import org.qubership.automation.itf.report.items.ATPReportMessage;
import org.qubership.automation.itf.report.util.ATPReportUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.MapDifference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.json.JSONObject;

public class RAM2ReportAdapter implements ReportAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RAM2ReportAdapter.class);

    private static final int ATP_NAME_MAX_LENGTH = 200;
    /*  size of '...'.length + 1 (+1 because start from 0, at substring)
        - we need get value like 'ATP ER Lon name with exe...'
     */
    private static final String DOTS = "...";
    private static final int ATP_NAME_TRIM_SIZE = ATP_NAME_MAX_LENGTH - DOTS.length() + 1;
    private String logName;
    private final ConcurrentHashMap<Object, AtpRamAdapter> adapters = new ConcurrentHashMap<>();
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String getShortTransportName(AbstractInstance instance) {
        if (instance instanceof StepInstance) {
            return getShortTransportName(((IntegrationStep) ((StepInstance) instance).getStep()).getOperation().getTransport().getTypeName());
        } else {
            return "ITF";
        }
    }

    private static String getShortTransportName(String fullTransportName) {
        return TransportHolder.getInstance().getShortName(fullTransportName);
    }

    private AtpRamAdapter getAdapter(Object sessionId) {
        return adapters.computeIfAbsent(sessionId, adapter -> RamAdapterProvider.getNewAdapter(logName));
    }

    @Override
    public void openSection(AbstractInstance instance, String title) {
        if (instance instanceof SituationInstance) {
            return; // Sections for situations are reported after the situation is finished.
        }
        if (!prepareTestRunContext(instance, false)) {
            return;
        }
        TestRunContext testRunContext = getAdapter(instance.getContext().getTC().getID()).getContext();
        String parentSectionId = testRunContext.getAtpCompaund().getSectionId();
        String currentSectionId = testRunContext.getCurrentSectionId();
        if (StringUtils.isBlank(currentSectionId)) {
            currentSectionId = UUID.randomUUID().toString();
        }
        testRunContext = getAdapter(instance.getContext().getTC().getID())
                .openSection(title, "",
                        instance instanceof CallChainInstance && isInitiator(instance)
                                ? parentSectionId : currentSectionId,
                        null,
                        "PASSED");
        if (instance.getStartTime() != null) {
            testRunContext.getCurrentSection().setStartDate(new Timestamp(instance.getStartTime().getTime()));
        }
        InstanceRam2Extension ramExtension = ExtensionManager.getInstance().getExtension(instance,
                InstanceRam2Extension.class);
        ramExtension.setSectionId(parentSectionId);
        LOGGER.debug("After open section: logRecordUuid {}, CurrentSectionId {}", testRunContext.getLogRecordUuid(),
                currentSectionId);
        if (instance instanceof CallChainInstance) {
            setCustomLinkToCallChain(testRunContext.getCurrentSection(),
                    instance.getContext().getTC().getProjectUuid().toString(),
                    ((CallChainInstance) instance).getTestCaseId());
        }
        getAdapter(instance.getContext().getTC().getID()).setContext(testRunContext);
    }

    @Override
    public void closeSection(AbstractInstance containerInstance) {
        synchronized (containerInstance) {
            if (!prepareTestRunContext(containerInstance, true)) {
                return;
            }
            AtpRamAdapter adapter = getAdapter(containerInstance.getContext().getTC().getID());
            if (containerInstance instanceof SituationInstance) {
                reportEndOfSituationAndSteps(adapter, (SituationInstance) containerInstance);
            } else {
                if (!(containerInstance instanceof StepInstance) && containerInstance.getError() != null) {
                    adapter.getContext().getCurrentSection().setMessage(containerInstance.getError().toString());
                    adapter.getContext().getCurrentSection().setTestingStatus(TestingStatuses.FAILED);
                }
                updateSectionName(adapter, containerInstance);
                TestRunContext testRunContext = adapter.closeSection();
                if (containerInstance.getEndTime() != null) {
                    testRunContext.getCurrentSection().setEndDate(new Timestamp(containerInstance.getEndTime().getTime()));
                }
                adapter.setContext(testRunContext);
                LOGGER.debug("After close section CurrentSectionId {}", testRunContext.getCurrentSectionId());
            }
        }
    }

    private void reportEndOfSituationAndSteps(AtpRamAdapter adapter, SituationInstance situationInstance) {
        TestRunContext testRunContext = adapter.getContext();
        List<StepInstance> stepInstances = situationInstance.getStepInstances();
        String errorMessage = getErrorMessage(situationInstance.getErrorName(),
                situationInstance.getErrorMessage(),
                situationInstance.getError());
        MapDifference<String, Object> diff = TC_CONTEXT_DIFF_CACHE.getIfPresent(situationInstance.getID().toString());
        if (stepInstances.isEmpty()) {
            /*  The most possible variant of that
                - some exception occurred in the Situation but before stepInstance even created.
            */
            org.qubership.atp.adapter.common.entities.Message logRecord =
                    new org.qubership.atp.adapter.common.entities.Message(
                            UUID.randomUUID().toString(),
                            testRunContext.getCurrentSectionId(),
                            situationInstance.getName(),
                            errorMessage,
                            situationInstance.getStatus().toString().toUpperCase(),
                            "TECHNICAL",
                            false);
            logRecord.setStartDate(new Timestamp(situationInstance.getStartTime().getTime()));
            if (situationInstance.getEndTime() != null) {
                logRecord.setEndDate(new Timestamp(situationInstance.getEndTime().getTime()));
            }
            if (!situationInstance.getLabels().isEmpty()) {
                logRecord.setValidationLabels(new HashSet<>(situationInstance.getLabels()));
            }
            setCustomLinkToSituation(logRecord,
                    situationInstance.getContext().tc().getProjectUuid().toString(),
                    situationInstance);
            adapter.message(logRecord);
            adapter.updateContextVariables(logRecord.getUuid(), fillContextVariables(diff));
        } else {
            org.qubership.atp.adapter.common.entities.Message lastStepMessage = null;
            for (int i = 0, stepInstancesSize = stepInstances.size(); i < stepInstancesSize; i++) {
                StepInstance stepInstance = stepInstances.get(i);
                if ((i < stepInstancesSize - 1)
                        && IntegrationStepHelper.notLastValidationAttempt(stepInstance, stepInstances.get(i + 1))) {
                    continue;
                }
                org.qubership.atp.adapter.common.entities.Message msg = createMessage(
                        situationInstance.getName(),
                        stepInstance,
                        situationInstance.getStatus().toString().toUpperCase(),
                        "",
                        UUID.randomUUID().toString(),
                        testRunContext.getCurrentSectionId(),
                        getShortTransportName(stepInstance));
                msg.setStartDate(new Timestamp(stepInstance.getStartTime().getTime()));
                if (stepInstance.getEndTime() != null) {
                    msg.setEndDate(new Timestamp(stepInstance.getEndTime().getTime()));
                }
                if (!(Status.PASSED.equals(situationInstance.getStatus()))) {
                    msg.setMessage(errorMessage);
                }
                if (!situationInstance.getLabels().isEmpty()) {
                    msg.setValidationLabels(new HashSet<>(situationInstance.getLabels()));
                }
                setCustomLinkToSituation(msg,
                        situationInstance.getContext().tc().getProjectUuid().toString(),
                        situationInstance);
                JSONObject bvResult = JSONObject.fromObject(getBvResult(situationInstance, "bvResultForRam2"));
                if (!bvResult.isEmpty()) {
                    adapter.restMessage(msg, bvResult);
                } else {
                    adapter.restMessage(msg);
                }
                lastStepMessage = msg;
            }
            if (lastStepMessage != null) {
                adapter.updateContextVariables(lastStepMessage.getUuid(), fillContextVariables(diff));
            }
        }
        TC_CONTEXT_DIFF_CACHE.invalidate(situationInstance.getID().toString());
    }

    private Object getBvResult(AbstractInstance situationInstance, String key) {
        // Checking against nulls is needed for situations failed before sp-context is created
        if (situationInstance.getContext() == null || situationInstance.getContext().getSP() == null) {
            return null;
        }
        return situationInstance.getContext().getSP().get(key);
    }

    private void setCustomLinkToCallChain(LogRecord logRecord, String projectUuid, BigInteger callChainId) {
        String prefixLink = String.format("/project/%s/itf#", projectUuid);
        List<CustomLink> customLinks = logRecord.getCustomLinks();
        if (Objects.isNull(customLinks)) {
            customLinks = new ArrayList<>();
            logRecord.setCustomLinks(customLinks);
        }
        customLinks.add(new CustomLink("Open Step in ITF",
                String.format("%s/callchain/%s", prefixLink, callChainId), OpenMode.NEW_TAB));
    }

    private void setCustomLinkToSituation(org.qubership.atp.adapter.common.entities.Message logRecord,
                                          String projectUuid, SituationInstance situationInstance) {
        String prefixLink = String.format("/project/%s/itf#", projectUuid);
        List<CustomLink> customLinks = logRecord.getCustomLinks();
        if (Objects.isNull(customLinks)) {
            customLinks = new ArrayList<>();
            logRecord.setCustomLinks(customLinks);
        }
        customLinks.add(new CustomLink("Open Step in ITF",
                getLinkToSituation(prefixLink, situationInstance), OpenMode.NEW_TAB));
    }

    private String getLinkToSituation(String prefix, SituationInstance situationInstance) {
        if (Objects.isNull(situationInstance.getSituationById())
                || Objects.isNull(situationInstance.getOperationId())
                || Objects.isNull(situationInstance.getSystemId())) {
            LOGGER.error("An error occurred while creating custom link.");
            return StringUtils.EMPTY;
        }
        return String.format("%s/system/%s/operation/%s/situation/%s",
                prefix,
                situationInstance.getSystemId(),
                situationInstance.getOperationId(),
                situationInstance.getSituationId());
    }

    private List<org.qubership.atp.ram.dto.response.MessageParameter> fillMessageParameters(List<MessageParameter> params) {
        List<org.qubership.atp.ram.dto.response.MessageParameter> lst = new ArrayList<>();
        if (params != null && !params.isEmpty()) {
            for (MessageParameter param : params) {
                org.qubership.atp.ram.dto.response.MessageParameter msgParam =
                        new org.qubership.atp.ram.dto.response.MessageParameter();
                msgParam.setName(param.getParamName());
                msgParam.setValue(
                        (param.getMultipleValue().isEmpty()) ? "" :
                                (param.getMultipleValue().size() == 1)
                                        ? param.getSingleValue() : param.getMultipleValue().toString()
                );
                lst.add(msgParam);
            }
        }
        return lst;
    }

    private ContextVariable fillContextVariable(String key, Object beforeValue, Object afterValue) {
        ContextVariable contextVariable = new ContextVariable();
        contextVariable.setName(key);
        contextVariable.setBeforeValue((beforeValue == null) ? null : beforeValue.toString());
        contextVariable.setAfterValue((afterValue == null) ? null : afterValue.toString());
        return contextVariable;
    }

    private List<ContextVariable> fillContextVariables(SpContext spContext) {
        List<ContextVariable> lst = new ArrayList<>();
        if (spContext != null && !spContext.isEmpty()) {
            Map<String, Object> flatMap = FlatMapUtil.flatten(spContext);
            for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                lst.add(fillContextVariable(entry.getKey(), null, entry.getValue()));
            }
        }
        return lst;
    }

    private List<ContextVariable> fillContextVariables(MapDifference<String, Object> diff) {
        List<ContextVariable> lst = new ArrayList<>();
        if (diff != null) {
            for (Map.Entry<String, Object> entry : diff.entriesOnlyOnLeft().entrySet()) {
                lst.add(fillContextVariable(entry.getKey(), entry.getValue(), null));
            }
            for (Map.Entry<String, Object> entry : diff.entriesOnlyOnRight().entrySet()) {
                lst.add(fillContextVariable(entry.getKey(), null, entry.getValue()));
            }
            for (Map.Entry<String, MapDifference.ValueDifference<Object>> entry : diff.entriesDiffering().entrySet()) {
                lst.add(fillContextVariable(entry.getKey(), entry.getValue().leftValue(),
                        entry.getValue().rightValue()));
            }
            for (Map.Entry<String, Object> entry : diff.entriesInCommon().entrySet()) {
                lst.add(fillContextVariable(entry.getKey(), entry.getValue(), entry.getValue()));
            }
        }
        return lst;
    }

    private void updateSectionName(AtpRamAdapter adapter, AbstractInstance containerInstance) {
        String newName;
        if (containerInstance instanceof SituationInstance) {
            newName = "Situation [" + containerInstance.getName() + "]";
        } else if (containerInstance instanceof StepInstance) {
            newName = containerInstance.getName();
        } else if (containerInstance instanceof CallChainInstance) {
            newName = "Call chain [" + containerInstance.getName() + "]";
        } else {
            return;
        }
        LogRecord logRecord = adapter.getContext().getCurrentSection();

        /*
            There is no instance id (our instance id) in the logRecord,
            so we can not compare ids and determine, if this containerInstance is the owner of this logRecord or not.
            So, the only way currently is:
                - check prefixes and do NOT update name in cases:
                    - logRecord name starts with "Call chain [" but containerInstance is SituationInstance
                    - logRecord name starts with "Situation [" but containerInstance is CallChainInstance
         */
        if (((logRecord.getName().startsWith("Call chain [") && containerInstance instanceof CallChainInstance)
                || (logRecord.getName().startsWith("Situation [") && containerInstance instanceof SituationInstance))
                && !newName.equals(logRecord.getName())) {
            logRecord.setName(newName);
        }
    }

    public HashMap<String, Object> createMapWithSnapshot(String simpleSnapShot) {
        File snapshot = null;
        try {
            snapshot = File.createTempFile("snapshot", null);
            FileUtils.writeStringToFile(snapshot, simpleSnapShot, "UTF-8");
        } catch (IOException e) {
            LOGGER.error("Can not create temp file", e);
        }
        HashMap<String, Object> mapWithSnapshot = new HashMap<>();
        mapWithSnapshot.put("screenshot_name", "ram2Screenshot");
        mapWithSnapshot.put("screenshot_file", snapshot);
        mapWithSnapshot.put("screenshot_type", "text/html");
        LOGGER.debug("Map with snapshot: {}", mapWithSnapshot);
        return mapWithSnapshot;
    }

    private String getMessageByMepType(AbstractInstance containerInstance, SpContext spContext) {
        ATPReportMessage reportRecord = new ATPReportMessage(containerInstance) {
            @Override
            public boolean logRecord() {
                return false;
            }
        };
        TcContext tc = containerInstance.getContext().getTC();
        reportRecord.setTcContext(tc);
        String snapShotAsString = null;
        String contextMessage;
        Message message;
        Message responseMessage;
        Collection<MessageParameter> messageParameters;
        if (containerInstance instanceof StepInstance) {
            Mep mep = ((StepInstance) containerInstance).getMep();
            switch (mep) {
                case OUTBOUND_REQUEST_ASYNCHRONOUS:
                case INBOUND_RESPONSE_ASYNCHRONOUS: {
                    contextMessage = GSON.toJson(spContext); //for pretty-print
                    message = spContext.getOutgoingMessage();
                    messageParameters = spContext.getMessageParameters();
                    snapShotAsString = reportRecord.buildSimpleSnapShotRam2(message, messageParameters, contextMessage);
                    break;
                }
                case OUTBOUND_RESPONSE_ASYNCHRONOUS:
                case INBOUND_REQUEST_ASYNCHRONOUS: {
                    contextMessage = GSON.toJson(spContext); //for pretty-print
                    message = spContext.getIncomingMessage();
                    messageParameters = spContext.getMessageParameters();
                    snapShotAsString = reportRecord.buildSimpleSnapShotRam2(message, messageParameters, contextMessage);
                    break;
                }
                case OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS:
                case INBOUND_REQUEST_RESPONSE_SYNCHRONOUS:
                case INBOUND_REQUEST_SYNCHRONOUS:
                case INBOUND_RESPONSE_SYNCHRONOUS: {
                    contextMessage = GSON.toJson(spContext);
                    messageParameters = spContext.getMessageParameters();
                    message = (mep.isOutbound()) ? spContext.getOutgoingMessage() : spContext.getIncomingMessage();
                    if (mep.isBothDirection()) {
                        responseMessage = (mep.isOutbound()) ? spContext.getIncomingMessage() :
                                spContext.getOutgoingMessage();
                        snapShotAsString = reportRecord.buildSimpleSnapShotRam2(message, messageParameters,
                                contextMessage, responseMessage);
                    } else {
                        snapShotAsString = reportRecord.buildSimpleSnapShotRam2(message, messageParameters,
                                contextMessage);
                    }
                    break;
                }
                default: {
                    LOGGER.warn("Logged MEP value doesn't exist");
                }
            }
        }
        return snapShotAsString;
    }

    private void callMessageWithMap(AbstractInstance containerInstance, String title, String message,
                                    String simpleSnapShotAsString, String status) {
        HashMap<String, Object> attributesHashMap = null;
        try {
            attributesHashMap = createMapWithSnapshot(simpleSnapShotAsString);
            message(containerInstance, title, message, status, attributesHashMap);
        } finally {
            if (attributesHashMap != null) {
                attributesHashMap.computeIfPresent("screenshot_file",
                        (key, value) -> FileUtils.deleteQuietly((File) value));
            }
        }
    }

    @Override
    public void info(AbstractInstance containerInstance, String title, SpContext spContext) {
        /*  StepInstances are reported together with parent (SituationInstance),
            when SituationInstance is closed.
         */
        if (containerInstance instanceof StepInstance) {
            return;
        }
        String messageFromSpContext = getMessageFromSpContext(spContext);
        String simpleSnapShotAsString = getMessageByMepType(containerInstance, spContext);
        callMessageWithMap(containerInstance, title, messageFromSpContext, simpleSnapShotAsString, "PASSED");
    }

    @Override
    public void info(AbstractInstance containerInstance, String title, String message) {
        message(containerInstance, title, message, "PASSED", Collections.emptyMap());
    }

    @Override
    public void warn(AbstractInstance containerInstance, String title, String message) {
        message(containerInstance, title, message, "WARNING", Collections.emptyMap());
    }

    @Override
    public void error(AbstractInstance containerInstance, String title, SpContext spContext, Throwable exception) {
        /*  StepInstances are reported together with parent (SituationInstance),
            when SituationInstance is closed.
         */
        if (containerInstance instanceof StepInstance) {
            return;
        }
        String messageWithException = getMessage(getMessageFromSpContext(spContext), exception);
        String simpleSnapShotAsString = getMessageByMepType(containerInstance, spContext);
        callMessageWithMap(containerInstance, title, messageWithException, simpleSnapShotAsString, "FAILED");
    }

    @Override
    public void error(AbstractInstance containerInstance, String title, String message, Throwable exception) {
        message(containerInstance, title, getMessage(message, exception), "FAILED", Collections.emptyMap());
    }

    @Override
    public void terminated(AbstractInstance containerInstance, String title, String message, Throwable exception) {
        message(containerInstance, title, getMessage(message, exception), "FAILED", Collections.emptyMap());
    }

    private String getMessageFromSpContext(SpContext spContext) {
        if (spContext == null) {
            return StringUtils.EMPTY;
        }
        Message incomingMessage = spContext.getIncomingMessage();
        if (incomingMessage == null || incomingMessage.getText() == null) {
            return StringUtils.EMPTY;
        }
        return incomingMessage.getText();
    }

    private void message(AbstractInstance containerInstance, String title, String message, String status, Map<String,
            Object> params) {
        if (!prepareTestRunContext(containerInstance, true)) {
            return;
        }
        Object sessionId = containerInstance.getContext().getTC().getID();
        InstanceRam2Extension extension = ExtensionManager.getInstance().getExtension(containerInstance,
                InstanceRam2Extension.class);
        TestRunContext testRunContext = getAdapter(sessionId).getContext();
        String parentId = extension.getSectionId();
        if (parentId == null && containerInstance.getParent() != null) {
            parentId = ExtensionManager.getInstance().getExtension(containerInstance.getParent(),
                    InstanceRam2Extension.class).getSectionId();
        }
        if (parentId == null) {
            parentId = testRunContext.getAtpCompaund().getSectionId();
        }
        if (containerInstance instanceof CallChainInstance && "FAILED".equals(status) && !testRunContext.getSections().empty()) {
            parentId = testRunContext.getSections().peek().getUuid().toString();
        } else if (containerInstance instanceof StepInstance && "FAILED".equals(status)) {
            parentId = testRunContext.getLastSectionInStep();
        }
        testRunContext.addSection(createRecord(title, parentId, containerInstance));
        LOGGER.debug("In message: after add section CurrentSectionId {}", testRunContext.getCurrentSectionId());
        org.qubership.atp.adapter.common.entities.Message logRecord =
                new org.qubership.atp.adapter.common.entities.Message(testRunContext.getCurrentSectionId(),
                        testRunContext.getLastSectionInStep(), title, message, status, "TECHNICAL", false);

        if ("FAILED".equals(status) && !(containerInstance instanceof CallChainInstance)) {
            if (containerInstance instanceof SituationInstance) {
                getAdapter(sessionId).message(logRecord);
            } else {
                getAdapter(sessionId).restMessage(createMessage(title, containerInstance, status, "",
                        testRunContext.getCurrentSectionId(),
                        testRunContext.getLastSectionInStep(),
                        getShortTransportName(containerInstance)));
            }
        } else {
            if (containerInstance instanceof CallChainInstance || (title.startsWith("Timeout for"))) {
                if (title.equals("Report links after execution")) {
                    logRecord.setParentRecordId(testRunContext.getAtpCompaund().getSectionId());
                }
                getAdapter(sessionId).message(logRecord);
            } else {
                JSONObject bvResult = JSONObject.fromObject(getBvResult(containerInstance, "bvResultForRam2"));
                if (!bvResult.isEmpty()) {
                    getAdapter(sessionId).restMessage(updateBVStatus(containerInstance,
                            createMessage(title, containerInstance, status, "",
                                    testRunContext.getCurrentSectionId(),
                                    testRunContext.getLastSectionInStep(),
                                    getShortTransportName(containerInstance)), bvResult), bvResult);
                } else {
                    getAdapter(sessionId).restMessage(createMessage(title, containerInstance, status, "",
                            testRunContext.getCurrentSectionId(),
                            testRunContext.getLastSectionInStep(),
                            getShortTransportName(containerInstance)));
                }
            }
        }
        LOGGER.debug("RAM2: TR_ID: '{}', SectionId: '{}', LR_NAME: '{}', LR_UUID: '{}', Message: {}, Params: {}",
                testRunContext.getTestRunId(), testRunContext.getCurrentSectionId(),
                title, testRunContext.getLogRecordUuid(), message, params);
        testRunContext.removeSection();
        if ("FAILED".equals(status)) {
            testRunContext.getCurrentSection().setTestingStatusHard(TestingStatuses.FAILED);
        }
        getAdapter(sessionId).setContext(testRunContext);
        LOGGER.debug("In message: After close section CurrentSectionId {}", testRunContext.getCurrentSectionId());
    }

    private ItfLogRecord createRecord(String name, String parentId, AbstractInstance containerInstance) {
        ItfLogRecord record = new ItfLogRecord();
        record.setUuid(UUID.randomUUID());
        record.setParentRecordId(UUID.fromString(parentId));
        record.setName(name);
        record.setMessage("");
        record.setTestingStatus(null);
        record.setType(TypeAction.ITF);
        record.setStartDate(new Timestamp(System.currentTimeMillis()));
        record.setRequest(createRequest(containerInstance));
        record.setResponse(createResponse(containerInstance));
        if (containerInstance instanceof SituationInstance && !((SituationInstance) containerInstance).getLabels().isEmpty()) {
            record.setValidationLabels(new HashSet<>(((SituationInstance) containerInstance).getLabels()));
        }
        return record;
    }

    private org.qubership.atp.adapter.common.entities.Message createMessage(String title,
                                                                            AbstractInstance instance,
                                                                            String status, String linkToTool,
                                                                            String id, String parentId,
                                                                            String type) {
        org.qubership.atp.adapter.common.entities.Message message =
                new org.qubership.atp.adapter.common.entities.Message();
        message.setUuid(id);
        message.setName(title);
        message.setType("TRANSPORT");
        message.setParentRecordId(parentId);
        message.setTestingStatus(status);
        message.setProtocolType(type);
        if (StringUtils.isNotEmpty(linkToTool)) {
            message.setLinkToTool(linkToTool);
        }
        message.setRequest(createRequest(instance));
        message.setResponse(createResponse(instance));
        if (instance instanceof SituationInstance && !((SituationInstance) instance).getLabels().isEmpty()) {
            message.setValidationLabels(new HashSet<>(((SituationInstance) instance).getLabels()));
        }
        message.setMessageParameters(fillMessageParameters(instance.getContext().getSP().getMessageParameters()));
        message.setStepContextVariables(fillContextVariables(instance.getContext().getSP()));
        return message;
    }

    private boolean prepareTestRunContext(AbstractInstance instance, boolean checkCurrentSection) {
        LOGGER.debug("Start prepare TestRunContext; current context is null");
        TCContextRamExtension ramExtension = ExtensionManager.getInstance()
                .getExtension(instance.getContext().getTC(), TCContextRamExtension.class);
        if (ramExtension == null) {
            LOGGER.error("ramExtension should not be null");
            return false;
        }
        TestRunContext testRunContext = ramExtension.getRunContext();
        if (null != testRunContext) {
            if (checkCurrentSection && testRunContext.getCurrentSection() == null) {
                LOGGER.warn("Current section should not be null; reporting action is skipped for instance {}",
                        instance);
                return false;
            }
            LOGGER.info("Ram2TestRunId: {}, Ram2SectionId: {}", testRunContext.getTestRunId(),
                    testRunContext.getCurrentSectionId());
            getAdapter(instance.getContext().getTC().getID()).setContext(testRunContext);
        } else {
            /*
                'Preconditions.checkNotNull' is replaced with 'LOGGER.error' due to:
                    - 'Preconditions.checkNotNull' spams log file and console with repeating full stack trace of the
                    same error for each step more than once,
                    - then, in the invoker method, we check if testRunContext != null again and simply return from
                    the method.
                So, there is no need to throw the exception here.
                May be, it should be revised later.
             */
            //Preconditions.checkNotNull(testRunContext, "testRunContext should not be null");
            LOGGER.error("TestRun Context should not be null! Reporting to RAM2 is skipped.");
        }
        return testRunContext != null;
    }

    @Override
    public void startRun(TcContext context) {
        TCContextRamExtension ramExtension = ExtensionManager.getInstance().getExtension(context,
                TCContextRamExtension.class);
        if (ramExtension == null) {
            return;
        }
        Preconditions.checkNotNull(ramExtension.getRunContext(), "runContext should not be null");
        LOGGER.debug("startRun: ramExtension run external {} ", ramExtension.getExternalRun());
        TestRunContext ram2Context = ramExtension.getRunContext();
        if (ramExtension.getExternalRun() == null || Strings.isNullOrEmpty(ramExtension.getRunContext().getTestRunId())) {
            Preconditions.checkNotNull(ramExtension.getRunId(), "TestRunId should not be empty");
            String suiteName = ramExtension.getSuiteName();
            if (suiteName == null) {
                suiteName = context.getInitiator().getStepContainer().getName();
            }
            suiteName = trimIfNameIsLong(suiteName);
            String storyName = context.getInitiator().getStepContainer().getName();
            if (context.getInitiator() instanceof CallChainInstance && ((CallChainInstance) context.getInitiator()).getDatasetName() != null) {
                storyName = storyName + ':' + ((CallChainInstance) context.getInitiator()).getDatasetName();
            }
            storyName = trimIfNameIsLong(storyName);
            String erName = ramExtension.getErName();
            if (erName == null) {
                erName = suiteName + '_' + DateTimeFormatter.ofPattern("dd/MM/yy_hh:mm:ss").format(LocalDateTime.now());
            }
            erName = trimIfNameIsLong(erName);
            if (Strings.isNullOrEmpty(ram2Context.getExecutionRequestName())) {
                ram2Context.setExecutionRequestName(erName);
            }
            if (Strings.isNullOrEmpty(ram2Context.getTestRunName())) {
                ram2Context.setTestRunName(storyName);
            }
            if (Strings.isNullOrEmpty(ram2Context.getTestSuiteName())) {
                ram2Context.setTestSuiteName(suiteName);
            }
            logName = storyName;
            StartRunRequest startRunRequest = createStartRunRequest(ram2Context);
            LOGGER.debug("RAM2_info: run id {}, ER id {}", ramExtension.getRunId(),
                    ram2Context.getAtpExecutionRequestId());
            TestRunContext testRunContext = getAdapter(context.getID()).startAtpRun(startRunRequest, ram2Context);
            getAdapter(context.getID()).setContext(testRunContext);
        }
    }

    @Override
    public void startAtpRun(String testRunId) {
        TestRunContext ram2Context = TestRunContextHolder.getContext(testRunId);
        LOGGER.debug("Creating StartRunRequest for TR {}", testRunId);
        StartRunRequest startRunRequest = createStartRunRequest(ram2Context);
        getAdapter(ram2Context.getTestRunId()).startAtpRun(startRunRequest, ram2Context);
    }

    private StartRunRequest createStartRunRequest(TestRunContext ram2Context) {
        StartRunRequest.RequestBuilder startRunRequest = StartRunRequest.getRequestBuilder()
                .setProjectName(ram2Context.getProjectName())
                .setTestPlanName(ram2Context.getTestPlanName())
                .setTestCaseName(ram2Context.getTestCaseName())
                .setTestSuiteName(ram2Context.getTestSuiteName())
                .setExecutionRequestName(ram2Context.getExecutionRequestName())
                .setTestRunName(ram2Context.getTestRunName())
                .setAtpExecutionRequestId(UUID.fromString(ram2Context.getAtpExecutionRequestId()))
                .setTestRunId(ram2Context.getTestRunId());
        if (StringUtils.isNotEmpty(ram2Context.getProjectId())) {
            startRunRequest.setProjectId(UUID.fromString(ram2Context.getProjectId()));
        }
        if (StringUtils.isNotEmpty(ram2Context.getTestPlanId())) {
            startRunRequest.setTestPlanId(UUID.fromString(ram2Context.getTestPlanId()));
        }
        return startRunRequest.build();
    }

    @Nullable
    private String trimIfNameIsLong(@Nullable String name) {
        if (name == null) {
            return null;
        }
        if (name.length() > ATP_NAME_MAX_LENGTH) {
            return name.substring(0, ATP_NAME_TRIM_SIZE) + DOTS;
        }
        return name;
    }

    @Override
    public void stopRun(InstanceContext context, Status status) {
        AtpRamAdapter adapter = getAdapter(context.getTC().getID());
        TestRunContext testRunContext = adapter.getContext();
        while (!StringUtils.isBlank(testRunContext.getCurrentSectionId())) {
            testRunContext.getCurrentSection().setName(TemplateEngineFactory.process(null,
                    testRunContext.getCurrentSection().getName(), context));
            adapter.setContext(adapter.closeSection());
        }
        adapters.remove(context.getTC().getID());
    }

    @Override
    public void stopAllRuns() {
    }

    public org.qubership.atp.adapter.common.entities.Message updateBVStatus(AbstractInstance instance,
                                                                            org.qubership.atp.adapter.common.entities.Message message,
                                                                            JSONObject bvResult) {
        if (instance instanceof SituationInstance) {
            SituationInstance situationInstance = (SituationInstance) instance;
            if (situationInstance.getSituationById().getValidateIncoming().equals(SituationLevelValidation.FAIL) &&
                    bvResult.containsKey("compareResult") && !bvResult.get("compareResult").equals("IDENTICAL")) {
                message.setTestingStatus("FAILED");
            } else {
                message.setTestingStatus("WARNING"); // Changed from "PASSED" for testing purposes only
            }
        }
        return message;
    }

    @Override
    public void reportCallChainInfo(CallChainInstance instance) {
        // Report Links and Info sections will be under main callchain only
        if (isInitiator(instance)) {
            info(instance, "Report links",
                    ReportLinkCreator.getInstance().buildReportLinks(instance.getContext().tc()));
            info(instance, "Info", ATPReportUtil.buildRunParamsInfo(instance));
        }
    }

    private boolean isInitiator(AbstractInstance instance) {
        return instance.getContext().tc().getInitiator().getID().equals(instance.getID());
    }

    private Request createRequest(AbstractInstance instance) {
        if (instance.getContext().getSP() == null) {
            return null;
        }
        Request request = new Request();
        request.setHeadersList(new ArrayList<>());
        Message message = null;
        if (instance instanceof StepInstance) {
            message = defineMessage(instance, instance.getContext().getSP(), true);
        }
        if (message != null) {
            if (message.getText() != null) {
                request.setBody(message.getText());
            }
            request.getHeadersList().addAll(convertMapToRequestHeaders(message.getHeaders()));
            RequestHeader camelHttpMethodHeader = extractHeaderByName(request.getHeadersList(), "CamelHttpMethod");
            if (camelHttpMethodHeader != null) {
                request.setMethod(camelHttpMethodHeader.getValue());
            }
        }
        request.setTimestamp(new Timestamp(new Date().getTime()));
        if (instance.getContext().getSP().containsKey("endpointForRam")) {
            request.setEndpoint(instance.getContext().getSP().get("endpointForRam").toString());
        }
        if (instance instanceof StepInstance) {
            Mep mep = ((StepInstance) instance).getMep();
            if (mep != null && mep.isOutbound()) {
                if (instance.getTransportConfiguration() != null) {
                    request.getHeadersList().addAll(convertMapToRequestHeaders(instance.getTransportConfiguration()));
                }
            }
        }
        return request;
    }

    private Response createResponse(AbstractInstance instance) {
        if (instance.getContext().getSP() == null) {
            return null;
        }
        Response response = new Response();
        response.setHeadersList(new ArrayList<>());
        Message message = null;
        if (instance instanceof StepInstance) {
            message = defineMessage(instance, instance.getContext().getSP(), false);
        }
        if (message != null) {
            if (message.getText() != null) {
                response.setBody(message.getText());
            }
            response.getHeadersList().addAll(convertMapToRequestHeaders(message.getHeaders()));
            // TODO: it should be tested against all ITF transports
            RequestHeader camelHttpResponseCode = extractHeaderByName(response.getHeadersList(),
                    "CamelHttpResponseCode");
            if (camelHttpResponseCode != null) {
                response.setCode(camelHttpResponseCode.getValue());
            }
        }
        response.setTimestamp(new Timestamp(new Date().getTime()));
        if (instance.getContext().getSP().containsKey("endpointForRam")) {
            response.setEndpoint(instance.getContext().getSP().get("endpointForRam").toString());
        }
        if (instance instanceof StepInstance) {
            Mep mep = ((StepInstance) instance).getMep();
            if (mep != null && mep.isInbound()) {
                if (instance.getTransportConfiguration() != null) {
                    response.getHeadersList().addAll(convertMapToRequestHeaders(instance.getTransportConfiguration()));
                }
            }
        }
        return response;
    }

    private Message defineMessage(AbstractInstance instance, SpContext spContext, boolean isRequest) {
        Message message = null;
        Mep mep = ((StepInstance) instance).getMep();
        switch (mep) {
            case OUTBOUND_REQUEST_ASYNCHRONOUS:
            case INBOUND_RESPONSE_ASYNCHRONOUS: {
                message = (isRequest) ? spContext.getOutgoingMessage() : spContext.getIncomingMessage();
                break;
            }
            case OUTBOUND_RESPONSE_ASYNCHRONOUS:
            case INBOUND_REQUEST_ASYNCHRONOUS: {
                message = (isRequest) ? spContext.getIncomingMessage() : spContext.getOutgoingMessage();
                break;
            }
            case OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS:
            case INBOUND_REQUEST_RESPONSE_SYNCHRONOUS:
            case INBOUND_REQUEST_SYNCHRONOUS:
            case INBOUND_RESPONSE_SYNCHRONOUS: {
                if (isRequest) {
                    message = (mep.isOutbound()) ? spContext.getOutgoingMessage() : spContext.getIncomingMessage();
                } else {
                    message = (mep.isOutbound()) ? spContext.getIncomingMessage() : spContext.getOutgoingMessage();
                }
                break;
            }
            default: {
                LOGGER.warn("Unknown MEP '{}' of StepInstance {}!", mep, instance);
            }
        }
        return message;
    }

    private List<RequestHeader> convertMapToRequestHeaders(Map<String, ?> headers) {
        List<RequestHeader> list = new ArrayList<>();
        String description = "";
        for (Map.Entry<String, ?> header : headers.entrySet()) {
            if (header.getValue() instanceof List) {
                for (Object elem : (List) (header.getValue())) {
                    list.add(new RequestHeader(header.getKey(), elem.toString(), description));
                }
            } else {
                list.add(new RequestHeader(header.getKey(), header.getValue().toString(), description));
            }
        }
        return list;
    }

    private RequestHeader extractHeaderByName(List<RequestHeader> headers, String headerName) {
        return headers.stream().filter(requestHeader -> headerName.equals(requestHeader.getName()))
                .findFirst().orElse(null);
    }

    @Override
    public void terminate() {
    }

    public boolean needToReport(TcContext context) {
        switch (context.getStartedFrom()) {
            case ITF_STUB:
            case EXECUTOR:
                // this value is set if the ExecuteStep request from Executor doesn't contain ATP and RAM2 testrun ids
            case ITF_UI:
                return false;
            case RAM2:
                return true;
            default:
                return false;
        }
    }

    private String getMessage(String message, Throwable exception) {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message);
        }
        if (exception != null) {
            sb.append("<pre>").append(ExceptionUtils.getStackTrace(exception)).append("</pre>");
        }
        return sb.toString();
    }

    private String getErrorMessage(String errorName, String errorMessage, Throwable exception) {
        StringBuilder sb = new StringBuilder();
        if (errorName != null) {
            sb.append(errorName);
        }
        if (errorMessage != null) {
            sb.append("<pre>").append(errorMessage).append("</pre>");
        } else if (exception != null) {
            sb.append("<pre>").append(ExceptionUtils.getStackTrace(exception)).append("</pre>");
        }
        return sb.toString();
    }
}
