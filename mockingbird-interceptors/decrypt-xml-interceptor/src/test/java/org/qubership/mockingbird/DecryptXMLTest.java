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

public class DecryptXMLTest {
    /*

    //@Before
    public void setUp() throws Exception {
        InterceptorClassLoader.getInstance().cleanClassLoaders();
    }

    @Ignore
    @Test
    public void testDecrypt() throws ClassNotFoundException {
        InterceptorClassLoader.getInstance().load("D:\\ITF\\phase4_prototype_hibernate_spring-data\\mockingbird"
                + "-interceptors\\decrypt-xml-interceptor\\target");
        Class<? extends TransportInterceptor> interceptorClass = InterceptorClassLoader.getInstance().getClass("org"
                + ".qubership.mockingbird.interceptor.DecryptXMLInterceptor");
        Interceptor mock = mock(TransportConfigurationInterceptor.class);
        InterceptorParams params = mock(InterceptorParams.class);
        when(mock.getParameters()).thenReturn(params);
        when(mock.getParameters().get("Password")).thenReturn("111");
        when(mock.getParameters().get("xPath")).thenReturn("/root/operation/text()");
        when(mock.getParameters().get("Cipher algorithm")).thenReturn("DESede");
        when(mock.getParameters().get("Symmetric Block Size")).thenReturn("256");
        when(mock.getParameters().get("Cipher mode")).thenReturn("ECB");
        when(mock.getParameters().get("Cipher padding")).thenReturn("PKCS5Padding");
        when(mock.getParameters().get("Initialization Vector")).thenReturn("1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0");
        when(mock.getParameters().get("Encoding")).thenReturn("Base64");
        try {
            TransportInterceptor transportInterceptor =
                    interceptorClass.getConstructor(Interceptor.class).newInstance(mock);
            Message result = transportInterceptor.apply(new Message("<root><operation>pPE"
                    + "/kpTSgMnySv9z2kjaxdnWvpW7N78Q</operation><operation>pPE/kpTSgMnUJDKWr4xtftnWvpW7N78Q"
                    + "</operation></root>"));
            System.out.println(result.getText());
            assertEquals("<root><operation><option>test1</option></operation><operation><option>test2</option"
                    + "></operation></root>", result.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */
}
