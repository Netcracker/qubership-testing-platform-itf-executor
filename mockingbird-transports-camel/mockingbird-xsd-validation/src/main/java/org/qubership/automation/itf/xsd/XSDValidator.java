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

package org.qubership.automation.itf.xsd;

import java.io.File;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class XSDValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(XSDValidator.class);
    private static final String SOAP_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
    private static SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private static LoadingCache<String, Schema> schemaCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Schema>() {
                @Override
                public Schema load(@Nonnull String xsdPath) throws SAXException {
                    return schemaFactory.newSchema(new StreamSource[]{
                            new File(xsdPath).exists() ? new StreamSource(xsdPath) : null,
                            //to be able validate soap
                            new StreamSource(SOAP_ENV)
                    });
                }
            });

    public XSDValidationResult validate(String message, String xsdPath) {
        XSDValidationResult result = new XSDValidationResult(message);
        try (StringReader xml = new StringReader(message)) {
            Schema schema = schemaCache.get(xsdPath);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xml));
            return result.setSchemaDocuments(new String[]{xsdPath, SOAP_ENV}).setFailed(false);
        } catch (Exception e) {
            LOGGER.error("XSD Validation is failed.\n XML message: \n" + message, e);
            return result.setException(e)
                    .setSchemaDocuments(new String[]{xsdPath, SOAP_ENV})
                    .setFailed(true);
        }
    }
}
