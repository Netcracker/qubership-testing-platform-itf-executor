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

package org.qubership.automation.itf.transport.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

public class DynamicHttpClientConfigurer implements HttpClientConfigurer {
    private static final int HTTP_CLIENT_TIMEOUT_VALUE = 300000;
    private boolean disableRedirects = false, trustAll = false;

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
            // setup a Trust Strategy that allows all certificates.
            SSLContext sslContext;
            try {
                sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                        return true;
                    }
                }).build();
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                throw new IllegalStateException("Http client is not initialized", e);
            }
            httpClientBuilder.setSSLContext(sslContext);

            // don't check Hostnames, either use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you
            // don't want to weaken
            HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();

            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();

            // now, we create connection-manager using our Registry.
            //      -- allows multi-threaded use
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            httpClientBuilder.setConnectionManager(connMgr);

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(HTTP_CLIENT_TIMEOUT_VALUE)
                    .setConnectionRequestTimeout(HTTP_CLIENT_TIMEOUT_VALUE)
                    .setSocketTimeout(HTTP_CLIENT_TIMEOUT_VALUE).build();

            httpClientBuilder.setDefaultRequestConfig(config);
        }
    }
}
