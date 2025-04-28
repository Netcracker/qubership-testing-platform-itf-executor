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

package org.qubership.automation.itf.transport.diameter.interceptors;

import java.util.Collections;

import org.junit.Test;
import org.qubership.automation.diameter.connection.DiameterConnection;
import org.qubership.automation.diameter.connection.ResponseListener;
import org.springframework.util.Assert;

public class RARInterceptorTest extends ResponseListener {

    RARInterceptor rarInterceptor = new RARInterceptor(null, null);


    @Test
    public void interceptRARCorrectSession() {
        String sessionId = "123456";
        String message = "<RAR><Session-Id>" + sessionId + "</Session-Id></RAR>";
        rarInterceptor.setSessionId(sessionId);
        Assert.isTrue(rarInterceptor.isApplicable(message),
                "RAR-interceptor with correct session: isApplicable() works wrong");
    }

    @Test
    public void interceptRARNoCorrectSession() {
        String sessionId = "123456";
        String message = "<RAR><Session-Id>" + sessionId + "</Session-Id></RAR>";
        rarInterceptor.setSessionId("213445");
        Assert.isTrue(!rarInterceptor.isApplicable(message),
                "RAR-interceptor with incorrect session: isApplicable() works wrong");
    }

    @Test
    public void interceptNoSession() {
        String message = "<RAR><Session-Id>" + "45453456" + "</Session-Id></RAR>";
        Assert.isTrue(rarInterceptor.isApplicable(message),
                "RAR-interceptor with no session: isApplicable() works wrong");
    }

    @Test
    public void interceptNoRAR() {
        String message = "<ARA><Session-Id>" + "45453456" + "</Session-Id></ARA>";
        Assert.isTrue(!rarInterceptor.isApplicable(message),
                "RAR-interceptor but non-RAR message: isApplicable() works wrong");
    }

    @Test
    public void test() {
        RARInterceptor rarInterceptor0 = new RARInterceptor(null, null);
        rarInterceptor0.setSessionId("1");
        RARInterceptor rarInterceptor1 = new RARInterceptor(null, null);
        rarInterceptor1.setSessionId("1");
        RARInterceptor rarInterceptor2 = new RARInterceptor(null, null);
        rarInterceptor2.setSessionId("2");
        DiameterConnection connection = new DiameterConnection();
        connection.addInterceptors(Collections.singleton(rarInterceptor1));
        connection.addInterceptors(Collections.singleton(rarInterceptor2));
        connection.addInterceptors(Collections.singleton(rarInterceptor0));
        connection.isOpen();
    }
}
