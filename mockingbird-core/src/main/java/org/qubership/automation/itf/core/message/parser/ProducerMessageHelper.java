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

package org.qubership.automation.itf.core.message.parser;

import static org.qubership.automation.itf.core.util.helper.Reflection.toStringMap;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.NativeManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.template.OutboundTemplateTransportConfiguration;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.logger.TimeLogger;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.access.AccessTransport;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class ProducerMessageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerMessageHelper.class);

    private static final ProducerMessageHelper INSTANCE = new ProducerMessageHelper();

    private TemplateEngine engine = TemplateEngineFactory.get();

    private ProducerMessageHelper() {
    }

    public static ProducerMessageHelper getInstance() {
        return INSTANCE;
    }

    public Message produceMessage(Template template, InstanceContext context, Operation operation,
                                  Environment environment) {
        TimeLogger.LOGGER.debug("Start for method: org.qubership.automation.itf.core.message.parser"
                + ".ProducerMessageHelper.produceMessage(Template, InstanceContext, Operation, Environment)");
        if (Objects.isNull(operation) || Objects.isNull(environment)) {
            LOGGER.info("Operation and/or Environment are null");
            TimeLogger.LOGGER.debug("End for method: org.qubership.automation.itf.core.message.parser"
                    + ".ProducerMessageHelper.produceMessage(Template, InstanceContext, Operation, Environment)");
            return produceMessage(template, context, operation);
        }
        Map<String, Storable> owners = Maps.newHashMap();
        owners.put(TemplateEngine.OWNER, template);
        owners.put(TemplateEngine.OPERATION, operation);
        owners.put(TemplateEngine.ENVIRONMENT, environment);
        if (context.tc() != null) {
            owners.put(TemplateEngine.INITIATOR, context.tc().getInitiator());
        }
        Message message = new Message(engine.process(owners, template.getText(), context));
        TimeLogger.LOGGER.debug("End for method: org.qubership.automation.itf.core.message.parser"
                + ".ProducerMessageHelper.produceMessage(Template, InstanceContext, Operation, Environment)");
        return prepareParseTemplateTransportProperties(template, context, message,
                operation.getTransport().getTypeName());
    }

    public Message produceMessage(Template template, InstanceContext context, Operation operation) {
        TimeLogger.LOGGER.debug("End for method: org.qubership.automation.itf.core.message.parser"
                + ".ProducerMessageHelper.produceMessage(Template, InstanceContext, Operation)");
        Message message = new Message(engine.process(template, template.getText(), context));
        TimeLogger.LOGGER.debug("End for method: org.qubership.automation.itf.core.message.parser"
                + ".ProducerMessageHelper.produceMessage(Template, InstanceContext, Operation)");
        return prepareParseTemplateTransportProperties(template, context, message,
                operation.getTransport().getTypeName());
    }

    //for UnitTests
    public Message produceMessage(Template template, InstanceContext context, String transportType) {
        Message message = new Message(engine.process(template, template.getText(), context));
        return prepareParseTemplateTransportProperties(template, context, message, transportType);
    }

    private Message prepareParseTemplateTransportProperties(Template template, InstanceContext context,
                                                            Message message, String transportType) {
        Collection<OutboundTemplateTransportConfiguration> transport =
                CoreObjectManager.getInstance().getSpecialManager(template.getClass(), NativeManager.class).getChildrenByClass(template, OutboundTemplateTransportConfiguration.class, transportType);
        for (OutboundTemplateTransportConfiguration configuration : transport) {
            try {
                message = parseTemplateTransportProperties(message, configuration, template, context);
            } catch (TransportException e) {
                LOGGER.error("Transport type error: ", e);
            }
        }
        return message;
    }

    private Message parseTemplateTransportProperties(Message message,
                                                     OutboundTemplateTransportConfiguration configuration,
                                                     Template template, InstanceContext context) throws TransportException {
        if (Objects.isNull(configuration)) {
            return message;
        }
        AccessTransport accessTransport = TransportRegistryManager.getInstance().find(configuration.getTypeName());
        if (Objects.isNull(accessTransport)) {
            return message;
        }
        try {
            for (PropertyDescriptor property : accessTransport.getProperties()) {
                processingProperty(property, configuration, message, template, context);
            }
        } catch (RemoteException e) {
            throw new TransportException(e);
        }
        return message;
    }

    private void processingProperty(PropertyDescriptor property, OutboundTemplateTransportConfiguration configuration
            , Message message, Template template, InstanceContext context) {
        //TODO The implement is hardcode because it use for headers only. I put comment into org.qubership
        // .automation.itf.common.Constants
        String propertyString = configuration.get(property.getShortName());
        String logCoords = "Connection property '" + property.getShortName() + "'";
        if (property.isDynamic() && !StringUtils.isBlank(propertyString)) {
            if (property.isMap()) {
                if ((propertyString.startsWith("$") || propertyString.startsWith("#"))) {
                    propertyString = engine.process(template, propertyString, context, logCoords);
                }
                Map<String, Object> map = splitToMap(propertyString);
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof List) {
                        List<String> oldlist = (List<String>) entry.getValue();
                        List<String> newlist = new ArrayList<>();
                        for (String elem : oldlist) {
                            newlist.add(engine.process(template, elem, context, logCoords));
                        }
                        entry.setValue(newlist);
                    } else {
                        entry.setValue(engine.process(template, (String) entry.getValue(), context, logCoords));
                    }
                }
                message.getConnectionProperties().put(property.getShortName(), map);
            } else {
                message.getConnectionProperties().put(property.getShortName(),
                        engine.process(template, propertyString, context, logCoords));
            }
        }
    }

    private Map<String, Object> splitToMap(String in) {
        return toStringMap(in);
    }
}
