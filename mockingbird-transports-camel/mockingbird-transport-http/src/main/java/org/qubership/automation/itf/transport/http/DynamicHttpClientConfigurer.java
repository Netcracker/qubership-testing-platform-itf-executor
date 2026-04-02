/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.transport.http;

import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

public class DynamicHttpClientConfigurer implements HttpClientConfigurer {
    private static final int HTTP_CLIENT_TIMEOUT_VALUE = 300000;
    private boolean disableRedirects = false;
    private boolean trustAll = false;

    public DynamicHttpClientConfigurer() {
    }

    public void setDisableRedirects(boolean disableRedirects) {
        this.disableRedirects = disableRedirects;
    }

    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    @Override
    public void configureHttpClient(HttpClientBuilder httpClientBuilder) {
        if (disableRedirects) {
            httpClientBuilder.disableRedirectHandling();
        }
    }
}
