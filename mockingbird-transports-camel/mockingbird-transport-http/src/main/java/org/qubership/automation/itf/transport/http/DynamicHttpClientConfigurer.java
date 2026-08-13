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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;

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
        if (trustAll) {
            // Create SSL context that trusts all certificates
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            SSLContext sslContext;
            try {
                sslContextBuilder.loadTrustMaterial(TrustAllStrategy.INSTANCE);
                sslContext = sslContextBuilder.build();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                throw new IllegalStateException("Http client is not initialized", e);
            }

            // 2. Build the TlsStrategy using the Builder
            //    This is the correct, modern API for 5.4.x.
            //      Commented, old-style SSLConnectionSocketFactory is still used.
            //      May be, will be rewritten in further migrations
            /*
            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    // The key part: setting the hostname verifier to Noop
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
             */

            // 2. Create an SSLConnectionSocketFactory using the SSLContext
            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();

            // now, we create connection-manager using our Registry.
            //      -- allows multithreaded use
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            httpClientBuilder.setConnectionManager(connMgr);

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(HTTP_CLIENT_TIMEOUT_VALUE))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(HTTP_CLIENT_TIMEOUT_VALUE))
                    .setResponseTimeout(Timeout.ofMilliseconds(HTTP_CLIENT_TIMEOUT_VALUE))
                    .build();

            httpClientBuilder.setDefaultRequestConfig(config);
        }
    }
}
