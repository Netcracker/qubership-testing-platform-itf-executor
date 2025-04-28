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

package org.qubership.mockingbird.interceptor;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.annotation.ApplyToTransport;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@ApplyToTransport(transports = {"org.qubership.automation.itf.transport.jms.inbound.JMSInboundTransport"})
@Named(value = "Decrypting XML message")
public class DecryptXMLInterceptor extends ContentInterceptor {

    private static final String XPATH = "xPath";
    private static final String XPATH_DESCRIPTION = "xPath for searching the data which will be decrypted";
    private static final Logger LOGGER = LoggerFactory.getLogger(DecryptXMLInterceptor.class);

    public DecryptXMLInterceptor() {
    }

    public DecryptXMLInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    @Override
    public Message apply(Message data) {
        LOGGER.info("Decrypting XML message started...");
        String message = data.getText();
        if (StringUtils.isEmpty(message)) {
            LOGGER.info("Message is empty. No changes have been applied.");
            return data;
        } else {
            try {
                List<String> encryptedDataAsStringList = XMLHelper.getDataAsString(
                        XMLHelper.getDataByXPath(XMLHelper.stringToXml(message),
                                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(XPATH)));
                for (String encryptedData : encryptedDataAsStringList) {
                    String decryptedData = decrypt(encryptedData);
                    message = CipherHelper.replaceText(message, encryptedData, decryptedData);
                }
                data.setText(message);
                LOGGER.info("Decrypting XML message completed successfully.");
            } catch (ParserConfigurationException | IOException | SAXException e) {
                LOGGER.error("Cannot convert message to XML-format. No changes have been applied.", e);
            } catch (XPathExpressionException e) {
                LOGGER.error("Cannot get the message to XML-format. No changes have been applied.", e);
            } catch (TransformerException e) {
                LOGGER.error("Cannot transform the founded node to string. No changes have been applied.", e);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                     | InvalidKeyException | BadPaddingException | IllegalBlockSizeException
                     | InvalidKeySpecException e) {
                LOGGER.error("Cannot encrypt the data from message. No changes have been applied.", e);
            }
        }
        return data;
    }

    @Override
    public String validate() {
        return InterceptorHelper.validate(interceptor, this);
    }

    @Override
    public List<InterceptorPropertyDescriptor> getParameters() {
        List<InterceptorPropertyDescriptor> parameters = CipherHelper.getCipherParametersDefaultList(interceptor);
        InterceptorPropertyDescriptor xpath = new InterceptorPropertyDescriptor(XPATH,
                XPATH,
                XPATH_DESCRIPTION,
                InterceptorConstants.TEXTFIELD,
                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(XPATH),
                false);
        parameters.add(xpath);
        return parameters;
    }

    private String decrypt(String strToDecrypt) throws InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        Cipher cipher = CipherHelper.getCipher(Cipher.DECRYPT_MODE, interceptor);
        byte[] encryptedData = CipherHelper.applyDecoding(strToDecrypt,
                (interceptor.getParameters() == null) ? null : interceptor.getParameters().get(CipherHelper.ENCODING));
        return new String(cipher.doFinal(encryptedData));
    }
}
