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

package org.qubership.automation.itf.core.report.producer;

import java.math.BigInteger;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.BooleanUtils;
import org.javers.common.collections.Sets;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.annotation.AtpSpanTag;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.core.execution.DaemonThreadPoolFactory;
import org.qubership.automation.itf.core.instance.step.impl.IntegrationStepHelper;
import org.qubership.automation.itf.core.metric.MetricsAggregateService;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractContainerInstance;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.model.jpa.instance.step.StepInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants;
import org.qubership.automation.itf.core.util.generator.id.UniqueIdGenerator;
import org.qubership.automation.itf.core.util.mdc.MdcField;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@Component
public class ReportWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportWorker.class);

    private static final int WARN_ABOUT_SIZE = Config.getConfig()
            .getIntOrDefault("report.producer.warnAboutSize", 5000000);
    private static final int MAX_SIZE = Config.getConfig()
            .getIntOrDefault("report.producer.maxSize", 6000000);
    private static final int SERIALIZATION_FIELD_MAX_SIZE = Config.getConfig()
            .getIntOrDefault("serialization.field.maxSize", 5000000);

    @Value("${report.producer.useGroupingMessages}")
    private boolean useGroupingMessages;

    @Value("${management.metrics.context.size.collect}")
    private boolean metricsContextSizeCollect;

    @Value("${management.metrics.context.size.collect.for.stubs}")
    private boolean metricsContextSizeCollectForStubs;

    @Value("${management.metrics.context.size.collect.threshold}")
    private int metricsContextSizeCollectThreshold;

    private ExecutorToMessageBrokerSender executorToMessageBrokerSender;
    private MetricsAggregateService metricsAggregateService;

    @Autowired
    public ReportWorker(ExecutorToMessageBrokerSender executorToMessageBrokerSender,
                        MetricsAggregateService metricsAggregateService) {
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
        this.metricsAggregateService = metricsAggregateService;
    }

    private static String objectDescription(Storable object) {
        if (object instanceof AbstractInstance) {
            return object.getClass().getSimpleName() + " id=" + object.getID()
                    + ", name '" + object.getName() + "'"
                    + " of " + objectDescription(((AbstractInstance) object).getContext().getTC());
        } else {
            return object.getClass().getSimpleName() + " id=" + object.getID()
                    + ", name '" + object.getName() + "'";
        }
    }

    private static boolean isReportExecutionEnabled(BigInteger projectId) {
        // Temporarily (?) changed to global property
        return BooleanUtils.toBoolean(Config.getConfig().getStringOrDefault(
                ProjectSettingsConstants.REPORT_EXECUTION_ENABLED,
                ProjectSettingsConstants.REPORT_EXECUTION_ENABLED_DEFAULT_VALUE));
    }

    private static ObjectMapper getMapper(BigInteger projectId) {
        if (isReportExecutionEnabled(projectId)) {
            ObjectMapper mapper = ReportUtilsCache.getInstance().getMapper(projectId);
            if (mapper == null) {
                synchronized (projectId) {
                    SimpleModule module = new SimpleModule("mb") {
                        @Override
                        public void setupModule(SetupContext context) {
                            super.setupModule(context);
                            context.appendAnnotationIntrospector(new JacksonAnnotationIntrospector());
                        }
                    };
                    mapper = new ObjectMapper();
                    mapper.registerModule(module);
                    mapper.disable(SerializationFeature.INDENT_OUTPUT);
                    mapper.setFilterProvider(configureFilterProvider());
                    ReportUtilsCache.getInstance().addMapper(projectId, mapper);
                }
            }
            return mapper;
        } else {
            return null;
        }
    }

    private static SimpleFilterProvider configureFilterProvider() {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("reportWorkerFilter_InstanceContext",
                SimpleBeanPropertyFilter.serializeAllExcept(
                        "transport", "version", "history", "collectHistory", "prefix", "description", "empty",
                        "messageBrokerSelectorValue", "extendsParameters", "extensionsJson"));
        filterProvider.addFilter("reportWorkerFilter_TCContext", reportWorkerFilterTcContext());
        filterProvider.addFilter("reportWorkerFilter_SPContext", reportWorkerFilterSpContext());
        filterProvider.addFilter("reportWorkerFilter_MessageParameter",
                SimpleBeanPropertyFilter.serializeAllExcept(
                        "prefix", "description", "name", "autosave", "version", "storableProp", "extendsParameters"));
        filterProvider.addFilter("reportWorkerFilter_CallChainInstance",
                SimpleBeanPropertyFilter.serializeAllExcept(
                        "prefix", "description", "datasetDefault", "running", "finished", "transportConfiguration",
                        "version", "storableProp", "extendsParameters", "extensionsJson"));
        filterProvider.addFilter("reportWorkerFilter_SituationInstance",
                SimpleBeanPropertyFilter.serializeAllExcept(
                        "prefix", "description", "running", "finished", "transportConfiguration", "version",
                        "storableProp", "extendsParameters", "extensionsJson"));
        filterProvider.addFilter("reportWorkerFilter_StepInstance",
                SimpleBeanPropertyFilter.serializeAllExcept(
                        "prefix", "description", "running", "finished", "version", "step", "storableProp",
                        "extendsParameters",
                        "extensionsJson"));
        filterProvider.addFilter("reportWorkerFilter_Message", reportWorkerFilterMessage());
        return filterProvider;
    }

    private static SimpleBeanPropertyFilter reportWorkerFilterMessage() {
        Set<String> properties = Sets.asSet("name", "parent", "prefix", "description", "file",
                "transportProperties", "failedMessage", "version", "storableProp", "extendsParameters",
                "extensionsJson");

        SimpleBeanPropertyFilter.SerializeExceptFilter filter
                = new SimpleBeanPropertyFilter.SerializeExceptFilter(properties) {
            @Override
            public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
                                         PropertyWriter writer) throws Exception {
                if ("text".equals(writer.getName()) && Objects.nonNull(pojo)) {
                    String body = ((Message)pojo).getText();
                    if (body.length() > SERIALIZATION_FIELD_MAX_SIZE) {
                        String newBody = String.format("Skip message body because big object, size - %s. " +
                                        "Please contact administrator for details.",
                                body.length());
                        jgen.writeStringField("text", newBody);
                        return;
                    }

                }
                super.serializeAsField(pojo, jgen, provider, writer);
            }
        };
        return filter;
    }

    private static SimpleBeanPropertyFilter reportWorkerFilterTcContext() {
        Set<String> properties = Sets.asSet("version", "history", "collectHistory", "prefix", "description",
                "empty", "lastAccess", "needToReportToAtp", "validationFailed", "extendsParameters",
                "natural_id", "runStepByStep", "running", "finished", "runnable", "parent", "partNum");
        SimpleBeanPropertyFilter.SerializeExceptFilter filter
                = new SimpleBeanPropertyFilter.SerializeExceptFilter(properties) {
            @Override
            public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
                                         PropertyWriter writer) throws Exception {
                if ("jsonString".equals(writer.getName()) && Objects.nonNull(pojo)) {
                    String jsonString = ((TcContext)pojo).getJsonString();
                    if (jsonString.length() > SERIALIZATION_FIELD_MAX_SIZE) {
                        String newJsonString = String.format("{\"big_object\"" +
                                        ":\"Skip test case context because big object, size - %s. " +
                                        "Please contact administrator for details.\"}",
                                jsonString.length());
                        jgen.writeStringField("jsonString", newJsonString);
                        return;
                    }
                }
                super.serializeAsField(pojo, jgen, provider, writer);
            }
        };
        return filter;
    }

    private static SimpleBeanPropertyFilter reportWorkerFilterSpContext() {
        Set<String> properties = Sets.asSet("version", "history", "collectHistory", "prefix", "description",
                "empty", "extendsParameters");
        SimpleBeanPropertyFilter.SerializeExceptFilter filter
                = new SimpleBeanPropertyFilter.SerializeExceptFilter(properties) {
            @Override
            public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
                                         PropertyWriter writer) throws Exception {
                if ("jsonString".equals(writer.getName()) && Objects.nonNull(pojo)) {
                    String jsonString = ((SpContext)pojo).getJsonString();
                    if (jsonString.length() > SERIALIZATION_FIELD_MAX_SIZE) {
                        String newJsonString = String.format("{\"big_object\"" +
                                        ":\"Skip step context because big object, size - %s. " +
                                        "Please contact administrator for details.\"}",
                                jsonString.length());
                        jgen.writeStringField("jsonString", newJsonString);
                        return;
                    }
                }
                super.serializeAsField(pojo, jgen, provider, writer);
            }
        };
        return filter;
    }

    private void send(Storable object,
                      Date date,
                      boolean reportExecutionEnabled,
                      BigInteger projectId,
                      String tenantId) {
        if (reportExecutionEnabled) {
            ObjectMapper mapper = getMapper(projectId);
            try {
                serializeAndSend(object, date, projectId, mapper, tenantId);
            } catch (Throwable e) {
                Throwable cause = e.getCause();
                LOGGER.warn("Error executing reporting task: {} {}\nObject: {}",
                        e.getMessage(),
                        (cause != null) ? "\nCaused by: " + cause : "",
                        objectDescription(object));
                if (cause instanceof ConcurrentModificationException) {
                    // Retry once... It's rood WA, but...
                    try {
                        LOGGER.info("Retrying once after ConcurrentModificationException (sleep 100 ms before)...");
                        Thread.sleep(100L);
                        serializeAndSend(object, date, projectId, mapper, tenantId);
                    } catch (InterruptedException ex) {
                        // It never happens
                    } catch (Throwable ex) {
                        LOGGER.error("Error executing reporting task: {} {}\nObject: {}",
                                ex.getMessage(),
                                (ex.getCause() != null) ? "\nCaused by: " + ex.getCause() : "",
                                objectDescription(object));
                    }
                }
            }
        }
    }

    private void send(String text, long time, String id, String type, String tenantId, int partNum) throws Throwable {
        try {
            executorToMessageBrokerSender.sendMessageToReportingQueue(createTextMessage(text, time, id, type,
                    tenantId, partNum));
            LOGGER.debug("Message is sent: id - {}, type - {}", id, type);
        } catch (JMSException e) {
            throw new Throwable(String.format("Error while sending message: id - %s, type - %s", id, type), e);
        }
    }

    private void serializeAndSend(Storable object,
                                  Date date,
                                  BigInteger projectId,
                                  ObjectMapper mapper,
                                  String tenantId) throws Throwable {
        if (object instanceof CallChainInstance) {
            TcContext tc = ((CallChainInstance) object).getContext().getTC();
            if (tc != null && tc.getInitiator() == object) {
                LOGGER.debug("CallChainInstance {} is Initiator of TcContext {}, sending is skipped",
                        object.getID(), tc.getID());
                return;
            }
        } else if (object instanceof StepInstance) {
            return; // We will send SituationInstance with child StepInstances instead
        }
        String jsonString = mapper.writeValueAsString(object);
        String type;
        int partNum;
        if (object instanceof TcContext && ((TcContext) object).getInitiator() != null) {
            TcContext tcContext = (TcContext) object;
            AbstractContainerInstance initiator = tcContext.getInitiator();
            if (initiator.getName() == null) {
                /*
                 It's the only real case when tcContext and initiator objects are got from
                 Hazelcast distributed cache, when tcContext is found by key.
                 It means that tcContext and initiator objects were already reported, and
                 that it's incorrect to report initiator object now!
                */
                type = "TcContext";
            } else {
                // Combined message is composed to send initiator info together
                String jsonStringInitiator = mapper.writeValueAsString(initiator);
                jsonString = "{\"TcContext\":" + jsonString + ",\"Initiator\":" + jsonStringInitiator + "}";
                type = "Combined_TcContext_Initiator";
            }
            partNum = tcContext.getPartNum();
            collectContextSizeMetric(tcContext, initiator, jsonString.length());
        } else if (object instanceof SituationInstance) {
            SituationInstance situationInstance = (SituationInstance) object;
            String logMessage =
                    "Report SituationInstance: [" + situationInstance.getID() + "] " + situationInstance.getName();
            jsonString = "{\"SituationInstance\":"
                    + jsonString
                    + (situationInstance.getID().equals(situationInstance.getContext().tc().getInitiator().getID())
                    ? ",\"isInitiator\" : true" : ",\"isInitiator\" : false")
                    + ",\"StepInstances\": [";
            if (situationInstance.getStepInstances() != null) {
                boolean notFirst = false;
                List<StepInstance> stepInstances = situationInstance.getStepInstances();
                for (int i = 0, stepInstancesSize = stepInstances.size(); i < stepInstancesSize; i++) {
                    StepInstance stepInstance = stepInstances.get(i);
                    if ((i < stepInstancesSize - 1)
                            && IntegrationStepHelper.notLastValidationAttempt(stepInstance, stepInstances.get(i + 1))) {
                        continue;
                    }
                    fillReportingObjectsIds(stepInstance);
                    String jsonStringStepInstance = mapper.writeValueAsString(stepInstance);
                    jsonString = jsonString + (notFirst ? "," : "") + jsonStringStepInstance;
                    logMessage += ", StepInstance: [" + stepInstance.getID() + "] " + stepInstance.getName();
                    notFirst = true;
                }
            }
            jsonString = jsonString + "]}";
            type = "Combined_SituationInstance_StepInstances";
            partNum = situationInstance.getContext().tc().getPartNum();
            LOGGER.debug(logMessage);
        } else {
            type = object.getClass().getSimpleName();
            LOGGER.debug("Message of type {} is sent", type);
            if (object instanceof AbstractInstance) {
                partNum = ((AbstractInstance) object).getContext().tc().getPartNum();
            } else {
                partNum = 1;
                LOGGER.warn("Object type {}: Cannot determine partNum; 1 is set", type);
            }
        }
        if (WARN_ABOUT_SIZE > 0 || MAX_SIZE > 0) {
            int length = jsonString.length();
            if (MAX_SIZE > 0 && length > MAX_SIZE) {
                LOGGER.error("Attempt to send too big object: projectId {}, object: {}, message size: {} - REJECTED",
                        projectId, objectDescription(object), length);
                return;
            } else if (WARN_ABOUT_SIZE > 0 && length >= WARN_ABOUT_SIZE) {
                LOGGER.warn("Attempt to send too big object: projectId {}, object: {}, message size: {}",
                        projectId, objectDescription(object), length);
            }
        }
        long startTime = System.currentTimeMillis();
        send(jsonString,
                date.getTime(),
                !Objects.isNull(object.getID()) ? object.getID().toString() : UUID.randomUUID().toString(),
                type, tenantId, partNum);
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 100L) {
            LOGGER.info("ReportWorker: too long send - {} ms, {}", duration, objectDescription(object));
        }
    }

    private void collectContextSizeMetric(TcContext tcContext, AbstractContainerInstance initiator, int size) {
        if (!metricsContextSizeCollect
                || (metricsContextSizeCollectThreshold > 0 && size < metricsContextSizeCollectThreshold)
                || (!metricsContextSizeCollectForStubs && initiator instanceof SituationInstance)) {
            return;
        }
        metricsAggregateService.incrementContextSizeCountToProject(
                tcContext.getProjectUuid(),
                (initiator instanceof SituationInstance ? "[Stub] " : "")
                        + (initiator.getName() == null ? tcContext.getName() : initiator.getName()),
                size);
    }

    private void fillReportingObjectsIds(StepInstance stepInstance) {
        InstanceContext context = stepInstance.getContext();
        if (context != null) {
            setIdIfNull(context);
            SpContext spContext = context.getSP();
            if (spContext != null) {
                setIdIfNull(spContext);
                setIdIfNull(spContext.getIncomingMessage());
                setIdIfNull(spContext.getOutgoingMessage());
                if (spContext.getMessageParameters() != null) {
                    for (MessageParameter parameter : spContext.getMessageParameters()) {
                        setIdIfNull(parameter);
                    }
                }
            }
        }
    }

    private void setIdIfNull(Storable object) {
        if (object != null && object.getID() == null) {
            object.setID(UniqueIdGenerator.generateReportingId());
        }
    }

    private TextMessage createTextMessage(String text, long time, String id, String type, String tenantId, int partNum)
            throws JMSException {
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText(text);
        message.setLongProperty("Time", time);
        message.setStringProperty("ObjectID", id);
        message.setStringProperty("ObjectType", type);
        if (useGroupingMessages) {
            message.setStringProperty("JMSXGroupID", id);
        }
        message.setStringProperty(CustomHeader.X_PROJECT_ID, tenantId);
        message.setIntProperty("partNum", partNum);
        return message;
    }

    private ExecutorService getExecutorService(BigInteger projectId) {
        if (isReportExecutionEnabled(projectId)) {
            ExecutorService executorService = ReportUtilsCache.getInstance().getExecutorService(projectId);
            if (executorService == null) {
                executorService =
                        DaemonThreadPoolFactory.cachedThreadPool(getReportExecutionSenderThreadPoolSize(projectId),
                                "ReportWorker - ");
                ReportUtilsCache.getInstance().addExecutorService(projectId, executorService);
            }
            return executorService;
        } else {
            return null;
        }
    }

    @AtpJaegerLog(spanTags = @AtpSpanTag(key = "submit.object.name", value = "#object.name"))
    public void submit(Storable object, Date date, BigInteger projectId, String tenantId) {
        MdcUtils.put(MdcField.TRACE_ID.toString(), MDC.get(MdcField.STUB_TRACE_ID.toString()));
        boolean reportExecutionEnabled = isReportExecutionEnabled(projectId);
        if (reportExecutionEnabled) {
            if (isReportInDifferentThread(projectId)) {
                getExecutorService(projectId).submit(
                        new Worker(object, date, reportExecutionEnabled, projectId, tenantId));
            } else {
                send(object, date, reportExecutionEnabled, projectId, tenantId);
            }
        }
    }

    private boolean isReportInDifferentThread(BigInteger projectId) {
        // Temporarily (?) changed to global property
        return BooleanUtils.toBoolean(Config.getConfig().getStringOrDefault(
                ProjectSettingsConstants.REPORT_IN_DIFFERENT_THREAD,
                ProjectSettingsConstants.REPORT_IN_DIFFERENT_THREAD_DEFAULT_VALUE));
    }

    private int getReportExecutionSenderThreadPoolSize(BigInteger projectId) {
        // Temporarily (?) changed to global property
        return Integer.parseInt(Config.getConfig().getStringOrDefault(
                ProjectSettingsConstants.REPORT_EXECUTION_SENDER_THREAD_POOL_SIZE,
                ProjectSettingsConstants.REPORT_EXECUTION_SENDER_THREAD_POOL_SIZE_DEFAULT_VALUE));
    }

    private class Worker implements Runnable {

        private Storable object;
        private Date date;
        private boolean reportExecutionEnabled;
        private BigInteger projectId;
        private String projectUuid;

        private Worker(Storable object, Date date, boolean reportExecutionEnabled,
                       BigInteger projectId, String projectUuid) {
            this.object = object;
            this.date = date;
            this.reportExecutionEnabled = reportExecutionEnabled;
            this.projectId = projectId;
            this.projectUuid = projectUuid;
        }

        @Override
        public void run() {
            send(object, date, reportExecutionEnabled, projectId, projectUuid);
        }
    }
}
