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

package org.qubership.automation.itf.transport.soap.http.outbound;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.qubership.automation.itf.transport.soap.http.SOAPOverHTTPHelper.prepareBusContext;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nullable;

import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.component.cxf.CxfProducer;
import org.apache.camel.component.cxf.DataFormat;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.impl.ProducerCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.annotation.Options;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.transport.http.Helper;
import org.qubership.automation.itf.transport.http.outbound.HTTPOutboundTransport;
import org.qubership.automation.itf.transport.soap.http.SOAPOverHTTPHelper;
import org.qubership.automation.itf.xsd.XSDValidationResult;
import org.qubership.automation.itf.xsd.XSDValidator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;

@UserName("Outbound SOAP Over HTTP Synchronous")
public class SOAPOverHTTPOutboundTransport extends HTTPOutboundTransport {

    private static final Cache<ConfiguredTransport, CxfConfig> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(30, MINUTES).build();
    private static final int STRIPES = 4096;
    private static final Striped<Lock> LOCK_STRIPED = Striped.lazyWeakLock(STRIPES);

    @Parameter(shortName = PropertyConstants.Soap.WSDL_PATH,
            longName = "Path to WSDL file",
            description = PropertyConstants.Soap.WSDL_PATH_DESCRIPTION,
            fileDirectoryType = "wsdl-xsd")
    private String wsdlFile;

    @Parameter(shortName = PropertyConstants.Soap.WSDL_CONTAINS_XSD,
            longName = "Does WSDL File contain XSD?",
            description = "Does WSDL File contain XSD?")
    @Options(value = {"No", "Yes"})
    private String isWsdlContainsXSD;

    @Parameter(shortName = PropertyConstants.Soap.REQUEST_XSD_PATH,
            longName = "Validate Request by XSD",
            description = PropertyConstants.Soap.XSD_PATH_DESCRIPTION,
            optional = true,
            fileDirectoryType = "wsdl-xsd")
    private String requestXSD;

    @Parameter(shortName = PropertyConstants.Soap.RESPONSE_XSD_PATH,
            longName = "Validate Response by XSD",
            description = PropertyConstants.Soap.XSD_PATH_DESCRIPTION,
            optional = true,
            fileDirectoryType = "wsdl-xsd")
    private String responseXSD;

    @Override
    public String getShortName() {
        return "SOAP Outbound";
    }

    /**
     * @param message  {@link Message} for getting connection properties
     * @param template {@link ProducerTemplate} for sending request
     * @param headers  {@link Map} of headers
     * @param endpoint URL to endpoint
     * @return result of request as {@link Exchange}
     */
    @Override
    protected Exchange createRequestExchange(Message message, ProducerTemplate template, Map<String, Object> headers,
                                             String endpoint) {
        String wsdlPathString = FilenameUtils
                .normalize((String) message.getConnectionProperties().get(PropertyConstants.Soap.WSDL_PATH));
        if (StringUtils.isBlank(wsdlPathString)) {
            throw new IllegalArgumentException("Path/URL to WSDL file is not specified");
        }
        if (wsdlPathString.contains("../") || wsdlPathString.contains("\\..")) {
            throw new IllegalArgumentException("Path/URL to WSDL file contains path elements which can"
                    + "cause Path Traversal vulnerability ('../' and/or '\\..').");
        }
        CxfEndpoint cxfEndpoint;
        CxfProducer cxfProducer;
        ConnectionProperties properties = new ConnectionProperties(message.getConnectionProperties());
        properties.remove("ContextId");
        String transportId = (String) (message.getConnectionProperties().get("transportId"));
        ConfiguredTransport configuredTransport = new ConfiguredTransport(transportId, properties);
        try {
            CxfConfig cxfConfig;
            synchronized (LOCK_STRIPED.get(configuredTransport)) {
                cxfConfig = CACHE.getIfPresent(configuredTransport);
                if (cxfConfig == null) {
                    cxfEndpoint = new CxfEndpoint();
                    cxfEndpoint.setWsdlURL(SOAPOverHTTPHelper.getAndCheckPath(wsdlPathString, true, "WSDL file"));
                    cxfEndpoint.setAddress(endpoint);
                    cxfEndpoint.setCamelContext(template.getCamelContext());
                    Bus defaultBus = prepareBusContext(this);
                    cxfEndpoint.setBus(defaultBus);
                    cxfEndpoint.setDataFormat(DataFormat.RAW);
                    setExtraProperties(cxfEndpoint, (Map<String, Object>) (message.getConnectionProperties()
                            .get(PropertyConstants.Http.PROPERTIES)));
                    cxfEndpoint.start();
                    cxfProducer = new CxfProducer(cxfEndpoint);
                    cxfProducer.start();
                    cxfConfig = new CxfConfig(cxfEndpoint, cxfProducer);
                    CACHE.put(configuredTransport, cxfConfig);
                }
            }
            cxfEndpoint = cxfConfig.getCxfEndpoint();
            cxfEndpoint.setCamelContext(template.getCamelContext());
            cxfProducer = cxfConfig.getCxfProducer();
            fixNPE_clientNull(template, cxfEndpoint, cxfProducer);
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new IllegalArgumentException("Path/URL to WSDL file is invalid (" + wsdlPathString + ")", e);
        } catch (Exception e) {
            throw new RuntimeException("Error configuring transport to send CXF Message. Stacktrace: " + e);
        }
        return template.request(cxfEndpoint, fillOutputExchange(message, headers));
    }

    @Override
    protected Exchange createRequestExchange(Message message, ProducerTemplate template, Map<String, Object> headers,
                                             String endpoint, HttpComponent httpComponent) throws Exception {
        return createRequestExchange(message, template, headers, endpoint);
    }

    @Override
    protected org.apache.camel.Message composeBody(org.apache.camel.Message camelMessage, Message itfMessage)
            throws Exception {
        return Helper.composeBodyForSOAPOutbound(camelMessage, itfMessage);
    }

    public void validateRequest(Message message) {
        validate(message, PropertyConstants.Soap.REQUEST_XSD_PATH);
    }

    public void validateResponse(Message message) {
        validate(message, PropertyConstants.Soap.RESPONSE_XSD_PATH);
    }

    /*  The method is to avoid NullPointerException in the CxfProducer
     *   The exception is due to client is null,
     *   the client is null due to:
     *       - "owr" cxfProducer is not used for sending, a producer from ProducerCache is used instead,
     *       - the Cache is empty, so Producer is created, but it's not started!!!
     *       - so, client remains null
     *   In the camel 2.17.0 there is no exception. But in the 2.18.x the exception is thrown.
     *   The exception was reported as fixed in the Camel 2.9.2,
     * but...
     *
     *   The fix is to be rewritten if possible, because getProducerCache() is private method,
     *   and producers is private field,
     *   so there are no legal ways to manage them.
     */
    private void fixNPE_clientNull(ProducerTemplate template, CxfEndpoint cxfEndpoint, CxfProducer cxfProducer)
            throws Exception {
        Method method = template.getClass().getDeclaredMethod("getProducerCache");
        method.setAccessible(true);
        ProducerCache producerCache = (ProducerCache) (method.invoke(template));
        Field field = producerCache.getClass().getDeclaredField("producers");
        field.setAccessible(true);
        Map<String, Producer> producers = (Map<String, Producer>) (field.get(producerCache));
        producers.put(cxfEndpoint.getEndpointUri(), cxfProducer);
    }

    private void validate(Message message, String requestXsdPath) {
        XSDValidationResult result = validateMessage(message, requestXsdPath);
        if (result != null && result.isFailed()) {
            message.setText(result.toString());
        }
    }

    @Nullable
    private XSDValidationResult validateMessage(Message message, String xsdPathParameter) {
        String xsdFilePath = message.getConnectionPropertiesParameter(xsdPathParameter);
        if (StringUtils.isBlank(xsdFilePath) || StringUtils.isBlank(message.getText())) {
            return null;
        }
        XSDValidator validator = new XSDValidator();
        return validator.validate(message.getText(), xsdFilePath);
    }

    @Override
    public String viewEndpoint(ConnectionProperties connectionProperties) {
        return null;
    }

    @Override
    public Mep getMep() {
        return Mep.OUTBOUND_REQUEST_RESPONSE_SYNCHRONOUS;
    }

    @Override
    public String getEndpointPrefix() {
        return "/mockingbird-transport-soap-http";
    }

    /* Set cxfEndpoint properties (see https://camel.apache.org/cxf.html)
     *   Values are checked before, and there could be specific processing of some property names like 'endpoint',
     * 'dataFormat'
     */
    protected void setExtraProperties(CxfEndpoint cxfEndpoint, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        for (Map.Entry<String, Object> item : properties.entrySet()) {
            if (StringUtils.isBlank(item.getKey()) || item.getValue() == null) {
                continue;
            }
            String key = item.getKey().trim();
            switch (key) {
                case "endpoint":
                    cxfEndpoint.setPortName((String) item.getValue());
                    break;
                case "defaultOperationName":
                    cxfEndpoint.setDefaultOperationName((String) item.getValue());
                    break;
                default:
                    props.put(key, item.getValue());
            }
        }
        if (!props.isEmpty()) {
            cxfEndpoint.setProperties(props);
        }
    }

    private class ConfiguredTransport {

        TreeMap<String, Object> properties;
        private String transportId;
        private String componentId;

        public ConfiguredTransport() {
            this.transportId = "";
            setComponentId();
            this.properties = new TreeMap<>();
        }

        public ConfiguredTransport(String transportId, ConnectionProperties properties) {
            this.transportId = transportId;
            setComponentId();
            this.properties = new TreeMap<>(properties);
        }

        public String getTransportId() {
            return transportId;
        }

        public void setTransportId(String transportId) {
            this.transportId = transportId;
        }

        public TreeMap<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(ConnectionProperties properties) {
            this.properties = new TreeMap<>(properties);
        }

        public String getComponentId() {
            return componentId;
        }

        private void setComponentId() {
            this.componentId = "out" + this.transportId + "-" + UUID.randomUUID();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.transportId);
            hash = 97 * hash + Objects.hashCode(this.properties);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConfiguredTransport other = (ConfiguredTransport) obj;
            if (!Objects.equals(this.transportId, other.transportId)) {
                return false;
            }
            return Objects.equals(this.properties, other.properties);
        }
    }

    private class CxfConfig {

        private CxfProducer cxfProducer;
        private CxfEndpoint cxfEndpoint;

        public CxfConfig(CxfEndpoint cxfEndpoint, CxfProducer cxfProducer) {
            this.cxfEndpoint = cxfEndpoint;
            this.cxfProducer = cxfProducer;
        }

        public CxfProducer getCxfProducer() {
            return cxfProducer;
        }

        public void setCxfProducer(CxfProducer cxfProducer) {
            this.cxfProducer = cxfProducer;
        }

        public CxfEndpoint getCxfEndpoint() {
            return cxfEndpoint;
        }

        public void setCxfEndpoint(CxfEndpoint cxfEndpoint) {
            this.cxfEndpoint = cxfEndpoint;
        }
    }
}
