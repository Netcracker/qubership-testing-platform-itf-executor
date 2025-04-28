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

package org.qubership.automation.itf.integration.atp2.kafka.service.impl;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.exceptions.common.NotValidValueException;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchManager;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ItfLiteEvent;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ItfLiteEventDiameter;
import org.qubership.automation.itf.integration.atp2.kafka.dto.ItfLiteEventRestSoap;
import org.qubership.automation.itf.integration.atp2.kafka.dto.RequestTransportType;
import org.qubership.automation.itf.integration.atp2.kafka.dto.messages.request.DiameterRequestData;
import org.qubership.automation.itf.integration.atp2.kafka.dto.messages.request.RestSoapRequestData;
import org.qubership.automation.itf.integration.atp2.kafka.dto.messages.response.ResponseData;
import org.qubership.automation.itf.integration.atp2.kafka.dto.messages.response.ResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("ItfLiteExportService")
public class ItfLiteExportServiceImpl {

    private final static String REST_OUTBOUND_TRANSPORT =
            "org.qubership.automation.itf.transport.rest.outbound.RESTOutboundTransport";
    private final static String DIAMETER_OUTBOUND_TRANSPORT =
            "org.qubership.automation.itf.transport.diameter.outbound.DiameterOutbound";
    private final static String SOAP_OUTBOUND_TRANSPORT =
            "org.qubership.automation.itf.transport.soap.http.outbound.SOAPOverHTTPOutboundTransport";

    private final KafkaProducerService kafkaProducerService;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ItfLiteExportServiceImpl(KafkaProducerService kafkaProducerService,
                                    Environment environment) {
        this.kafkaProducerService = kafkaProducerService;
        this.environment = environment;
    }

    /**
     * The method creates a template (and situation) according to the data received from kafka,
     * the result of the method is sent as a message to kafka.
     *
     * @param event        - data event
     * @param itfLiteEvent - data message with type Rest or Soap
     */
    @Transactional
    public void createTemplateFromRestSoapTransportType(String event, ItfLiteEventRestSoap itfLiteEvent)
            throws JsonProcessingException {
        log.info("'Create Template' Event is received: {}", event);
        RestSoapRequestData request = itfLiteEvent.getRequest();
        ResponseData response = new ResponseData();
        try {
            OperationTemplate template = createTemplate(itfLiteEvent);
            template.setName(request.getName());
            template.setText(request.getBody() == null ? null : request.getBody().getContent());
            template.fillTransportProperties(createTransportConfig(template, request));
            template.store();
            log.info("Template {} is created", template);
            createSituation(itfLiteEvent, template);
            response.setItfRequestUrl(getItfRequestUrl(itfLiteEvent, template.getID()));
            response.setStatus(ResponseStatus.Done);
        } catch (Exception e) {
            processException(itfLiteEvent.getId(), e, response);
        } finally {
            sendMessageToKafka(itfLiteEvent.getId(), request.getId(), response);
        }
    }

    /**
     * The method creates a template (and situation) according to the data received from kafka,
     * the result of the method is sent as a message to kafka.
     *
     * @param event        - data event
     * @param itfLiteEvent - data message with type Diameter
     */
    @Transactional
    public void createTemplateFromDiameterTransportType(String event, ItfLiteEventDiameter itfLiteEvent) {
        log.info("'Create Template' Event is received: {}", event);
        DiameterRequestData request = itfLiteEvent.getRequest();
        ResponseData response = new ResponseData();
        try {
            OperationTemplate template = createTemplate(itfLiteEvent);
            template.setName(request.getName());
            template.setText(request.getBody() == null ? null : request.getBody().getContent());
            Collection<OutboundTemplateTransportConfiguration> templateProperties = Lists.newArrayList();
            OutboundTemplateTransportConfiguration outboundTemplateTransportConfiguration =
                    new OutboundTemplateTransportConfiguration(DIAMETER_OUTBOUND_TRANSPORT, template);
            Map<String, String> configuration = Maps.newHashMap();
            configuration.put("interceptorName", request.getResponseType());
            outboundTemplateTransportConfiguration.fillConfiguration(configuration);
            templateProperties.add(outboundTemplateTransportConfiguration);
            template.fillTransportProperties(templateProperties);
            template.store();
            log.info("Template {} is created", template);
            createSituation(itfLiteEvent, template);
            response.setItfRequestUrl(getItfRequestUrl(itfLiteEvent, template.getID()));
            response.setStatus(ResponseStatus.Done);
        } catch (Exception e) {
            processException(itfLiteEvent.getId(), e, response);
        } finally {
            sendMessageToKafka(itfLiteEvent.getId(), request.getId(), response);
        }
    }

    private void processException(UUID itfLiteEventId, Exception exception, ResponseData response) {
        String msg = exception instanceof IllegalArgumentException
                ? "Request has an inappropriate Transport"
                : String.format("Unexpected exception when create Template with eventId '%s'", itfLiteEventId);
        log.error(msg, exception);
        response.setStatus(ResponseStatus.Error);
        response.setErrorMessage(msg);
    }

    private void sendMessageToKafka(UUID itfLiteEventId, UUID requestId, ResponseData response) {
        try {
            log.info("Send message to kafka with eventId '{}'", itfLiteEventId);
            response.setId(itfLiteEventId);
            response.setRequestId(requestId);
            kafkaProducerService.send(objectMapper.writeValueAsString(response), itfLiteEventId.toString());
        } catch (Exception e) {
            String msg = String.format(
                    "Exception thrown when sending with key {%s}. KafkaProducerService can't write to"
                            + "kafka. Request {%s}", response.getId(), response);
            log.error(msg, e);
        }
    }

    private OperationTemplate createTemplate(ItfLiteEvent event) {
        Operation parentOperation = getOperation(event.getOperationId(), "create Template under it");
        String requestTransport;
        if (event instanceof ItfLiteEventRestSoap) {
            requestTransport = ((ItfLiteEventRestSoap) event).getRequest().getTransportType();
        } else if (event instanceof ItfLiteEventDiameter) {
            requestTransport = ((ItfLiteEventDiameter) event).getRequest().getTransportType();
        } else {
            requestTransport = StringUtils.EMPTY;
        }
        String parentTransportType = parentOperation.getTransport().getTypeName();
        if (("REST".equals(requestTransport) && !REST_OUTBOUND_TRANSPORT.equals(parentTransportType))
                || ("SOAP".equals(requestTransport) && !SOAP_OUTBOUND_TRANSPORT.equals(parentTransportType))
                || ("Diameter".equals(requestTransport) && !DIAMETER_OUTBOUND_TRANSPORT.equals(parentTransportType))) {
            throw new NotValidValueException(String.format("Request and Operation Transport Types mismatch: "
                    + "'%s' vs '%s'", requestTransport, parentTransportType));
        }
        OperationTemplate template = CoreObjectManager.getInstance().getManager(OperationTemplate.class)
                .create(parentOperation);
        template.setName("New " + template.getClass().getSimpleName());
        template.setDescription("Exported from Itf-Lite, request " + event.getId() + " at " + ZonedDateTime.now());
        return template;
    }

    private Situation createSituation(ItfLiteEvent event, OperationTemplate template) {
        if (Objects.isNull(event.getReceiver())) {
            return null;
        }
        Operation operation = getOperation(event.getOperationId(), "create Situation under it");
        System receiver = getSystem(event.getReceiver(), "create Situation with it as 'Receiver'");
        System system = getSystem(event.getSystemId(), "create Situation with it as 'Sender'");
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).create(operation);
        situation.setName("New Situation");
        situation.setDescription("Exported from Itf-Lite, request " + event.getId() + " at " + ZonedDateTime.now());
        IntegrationStep step = new IntegrationStep(situation);
        step.setOperation(operation);
        step.setReceiver(receiver);
        step.setSender(system);
        step.setTemplate(template);
        step.setName(step.getSender().getName() + " sends " + step.getOperation().getName() + " to "
                + step.getReceiver().getName() + " with template " + step.returnStepTemplate().getName());
        situation.store();
        log.info("Situation {} is created", situation);
        return situation;
    }

    private Operation getOperation(BigInteger objectId, String message) {
        Operation operation = CoreObjectManager.getInstance().getManager(Operation.class).getById(objectId);
        if (Objects.isNull(operation)) {
            throw new ObjectNotFoundException("Operation", objectId.toString(), null, message);
        }
        return operation;
    }

    private System getSystem(BigInteger objectId, String message) {
        System system = CoreObjectManager.getInstance().getManager(System.class).getById(objectId);
        if (Objects.isNull(system)) {
            throw new ObjectNotFoundException("System", objectId.toString(), null, message);
        }
        return system;
    }

    private String fillHeaders(Map<String, String> headers) {
        StringBuilder result = new StringBuilder();
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                result.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
        }
        return result.toString().endsWith("\n")
                ? result.substring(0, result.toString().length() - 1) : result.toString();
    }

    private String fillEndpoint(String url, Map<String, String> queryParameters) {
        StringBuilder result = new StringBuilder();
        result.append(url.substring(url.indexOf("/", 8)));
        if (Objects.nonNull(queryParameters) && !queryParameters.isEmpty()) {
            result.append("?");
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                result.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        return result.toString().endsWith("&")
                ? result.substring(0, result.toString().length() - 1) : result.toString();
    }

    private Collection<OutboundTemplateTransportConfiguration> createTransportConfig(OperationTemplate template,
                                                                                     RestSoapRequestData request) {
        OutboundTemplateTransportConfiguration outboundTemplateTransportConfiguration;
        switch (RequestTransportType.valueOf(request.getTransportType().toUpperCase())) {
            case REST: {
                outboundTemplateTransportConfiguration = new OutboundTemplateTransportConfiguration(
                        REST_OUTBOUND_TRANSPORT, template);
                break;
            }
            case SOAP: {
                outboundTemplateTransportConfiguration = new OutboundTemplateTransportConfiguration(
                        SOAP_OUTBOUND_TRANSPORT, template);
                break;
            }
            default: {
                throw new NotValidValueException("Unknown transport type: " + request.getTransportType());
            }
        }
        Map<String, String> configuration = Maps.newHashMap();
        configuration.put("method", request.getHttpMethod());
        configuration.put("endpoint", fillEndpoint(request.getUrl(), request.getQueryParameters()));
        configuration.put("headers", fillHeaders(request.getRequestHeaders()));
        outboundTemplateTransportConfiguration.fillConfiguration(configuration);
        Collection<OutboundTemplateTransportConfiguration> templateProperties = Lists.newArrayList();
        templateProperties.add(outboundTemplateTransportConfiguration);
        return templateProperties;
    }

    private String getItfRequestUrl(ItfLiteEvent itfLiteEvent, Object templateId) {
        String itfUrl = itfLiteEvent.getItfUrl();
        StringBuilder result = new StringBuilder();

        String url;
        if (itfUrl.contains("api/atp-itf-executor/v1")) {
            // "atp.catalogue.url" property should not be null by config. But, to avoid code check warnings...
            url = environment.getProperty("atp.catalogue.url", "atp.catalogue.url");
            result.append(url.endsWith("/") ? url : url + "/");
            result.append("project/").append(itfLiteEvent.getProjectId().toString());
            result.append("/itf#/template/").append(templateId);
        } else if (itfUrl.contains("atp-itf-executor")) {
            // "configurator.url" property should not be null by config. But, to avoid code check warnings...
            url = environment.getProperty("configurator.url", "configurator.url");
            result.append(url.endsWith("/") ? url : url + "/");
            BigInteger projectId = CoreObjectManager.getInstance()
                    .getSpecialManager(StubProject.class, SearchManager.class)
                    .getEntityInternalIdByUuid(itfLiteEvent.getProjectId());
            result.append("project/").append(projectId);
            result.append("#/template/").append(templateId);
        }
        return result.toString();
    }
}
