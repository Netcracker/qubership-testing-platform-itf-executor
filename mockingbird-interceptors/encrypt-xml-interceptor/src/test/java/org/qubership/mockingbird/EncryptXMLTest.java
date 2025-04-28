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

package org.qubership.mockingbird;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.interceptor.TransportInterceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.model.jpa.interceptor.TemplateInterceptor;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.loader.InterceptorClassLoader;

public class EncryptXMLTest {

    @Ignore
    @Test
    public void testEncrypt() throws ClassNotFoundException {
        InterceptorClassLoader.getInstance().load("C:\\Users\\Kuleshov\\Desktop\\Projects\\ITF\\branches\\itf-db"
                + "-hibernate\\mockingbird-interceptors\\encrypt-xml-interceptor\\target", null);
        Class<? extends TransportInterceptor> interceptorClass = InterceptorClassLoader.getInstance()
                .getClass("org.qubership.mockingbird.interceptor.EncryptXMLInterceptor");
        Interceptor mock = mock(TemplateInterceptor.class);
        InterceptorParams params = mock(InterceptorParams.class);
        when(mock.getParameters()).thenReturn(params);
        when(mock.getParameters().get("Password")).thenReturn("111");
        when(mock.getParameters().get("xPath")).thenReturn("/root/operation/option");
        when(mock.getParameters().get("Cipher algorithm")).thenReturn("DESede");
        when(mock.getParameters().get("Symmetric Block Size")).thenReturn("256");
        when(mock.getParameters().get("Cipher mode")).thenReturn("ECB");
        when(mock.getParameters().get("Cipher padding")).thenReturn("PKCS5Padding");
        when(mock.getParameters().get("Initialization Vector")).thenReturn("1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0");
        when(mock.getParameters().get("Encoding")).thenReturn("Base64");
        try {
            TransportInterceptor transportInterceptor =
                    interceptorClass.getConstructor(Interceptor.class).newInstance(mock);
            Message result = transportInterceptor.apply(new Message("<root><operation><option>test1</option>"
                    + "</operation><operation><option>test2</option></operation></root>"));
            System.out.println(result.getText());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
