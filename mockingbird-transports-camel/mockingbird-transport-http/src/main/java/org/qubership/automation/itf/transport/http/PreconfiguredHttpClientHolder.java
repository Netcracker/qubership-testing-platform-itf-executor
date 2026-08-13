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

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;

public class PreconfiguredHttpClientHolder {
    private static final int HTTP_CLIENT_TIMEOUT_VALUE = 300000;
    private static final CloseableHttpClient HTTPCLIENT = configureClient();

    public static CloseableHttpClient get() {
        return HTTPCLIENT;
    }

    private static CloseableHttpClient configureClient() {
        try {
            // 1. Create an SSLContext that trusts all certificates
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();

            // 2. Create an SSLConnectionSocketFactory using the SSLContext
            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            // 3. Create a ConnectionManager and set the SSL socket factory on it
            PoolingHttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(sslSocketFactory)
                            .build();

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(HTTP_CLIENT_TIMEOUT_VALUE))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(HTTP_CLIENT_TIMEOUT_VALUE))
                    .setResponseTimeout(Timeout.ofMilliseconds(HTTP_CLIENT_TIMEOUT_VALUE))
                    .build();

            // 4. Build the HttpClient and set the custom ConnectionManager
            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setConnectionManagerShared(true) // Important for resource management
                    .setDefaultRequestConfig(config)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Http client is not initialized", e);
        }
    }
}
