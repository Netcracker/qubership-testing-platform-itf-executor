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

package org.qubership.automation.itf.core.util.format;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.collect.Maps;

public class Formatters {

    private static final Logger LOGGER = LoggerFactory.getLogger(Formatters.class);
    private static final DocumentBuilder XML_DOCUMENT_BUILDER;
    private static final Transformer XML_TRANSFORMER;
    private static final Formatter XML_FORMATTER;
    private static final Formatter EMPTY_FORMATTER = text -> {
        /*
            Level is changed from 'info' to 'debug' by Alexander Kapustin, 2018-04-19
            The reason is: 'FORMATTERS' map currently contains DiameterOutbound & rarely used SOAP transports,
                so there will be a lot of spam in the ITF.log at the 'INFO' level.
            May be level will be reverted to 'info' when all messages of all transports are to be formatted
         */
        LOGGER.debug("There is no appropriate formatter for the message. No format changes will be applied.");
        return text;
    };

    private static final Map<String, Formatter> FORMATTERS;

    static {
        FORMATTERS = Maps.newHashMap();
        XML_DOCUMENT_BUILDER = initXmlDocumentBuilder();
        XML_TRANSFORMER = initXmlTransformer();
        XML_FORMATTER = new Formatter() {
            @Override
            public String format(String text) {
                if (XML_DOCUMENT_BUILDER == null || XML_TRANSFORMER == null) {
                    LOGGER.debug("XML formatting of the message escaped - xmlDocumentBuilder and/or xmlTransformer are "
                            + "NOT initialized.");
                    return text;
                }
                if (StringUtils.isBlank(text)) return text;
                return formatXml(text);
            }

            private synchronized String formatXml(String text) {
                if (XML_DOCUMENT_BUILDER == null || XML_TRANSFORMER == null) {
                    return text;
                }
                try {
                    Document doc = XML_DOCUMENT_BUILDER.parse(new InputSource(new StringReader(text)));
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    XML_TRANSFORMER.transform(new DOMSource(doc), new StreamResult(
                            new OutputStreamWriter(out, StandardCharsets.UTF_8)));
                    return out.toString(StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    LOGGER.info("XML formatting of the message is failed - no format changes are applied. "
                            + "Change log level to DEBUG to see details.");
                    LOGGER.debug("XML formatting of the message is failed - no format changes are applied:",
                            e);
                    return text;
                }
            }
        };

        if (XML_DOCUMENT_BUILDER != null && XML_TRANSFORMER != null) {
            /*Outbound transports*/
            FORMATTERS.put("org.qubership.automation.itf.transport.diameter.outbound.DiameterOutbound",
                    XML_FORMATTER);
            FORMATTERS.put("org.qubership.automation.itf.transport.soap.jms.outbound.SoapOverJMSOutbound",
                    XML_FORMATTER);
            FORMATTERS.put("org.qubership.automation.itf.transport.soap.http.outbound.SOAPOverHTTPOutboundTransport",
                    XML_FORMATTER);
            /*Inbound transports*/
            FORMATTERS.put("org.qubership.automation.itf.transport.soap.jms.inbound.SoapOverJMSInbound",
                    XML_FORMATTER);
            FORMATTERS.put("org.qubership.automation.itf.transport.soap.http.inbound.SOAPOverHTTPInboundTransport",
                    XML_FORMATTER);
        }
    }

    private Formatters() {
    }

    private static DocumentBuilder initXmlDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.warn("XMLDocumentBuilder init is failed - formatting won't be applied to incoming XML messages", ex);
            return null;
        }
    }

    private static Transformer initXmlTransformer() {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes" /* "no" */);
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            return transformer;
        } catch (TransformerConfigurationException ex) {
            LOGGER.warn("XMLTransformer init is failed - formatting won't be applied to incoming XML messages", ex);
            return null;
        }
    }

    public static Formatter getFormatter(String transportClassName) {
        Formatter formatter = FORMATTERS.get(transportClassName);
        return formatter != null ? formatter : EMPTY_FORMATTER;
    }

    public static Formatter getFormatterOrNull(String transportClassName) {
        return FORMATTERS.get(transportClassName);
    }

    public static boolean isEmptyFormatter(Formatter formatter) {
        return EMPTY_FORMATTER.equals(formatter);
    }
}
