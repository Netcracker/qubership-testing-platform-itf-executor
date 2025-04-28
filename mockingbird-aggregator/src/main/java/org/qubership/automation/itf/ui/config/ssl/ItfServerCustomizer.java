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

package org.qubership.automation.itf.ui.config.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = "embedded.https.enabled", havingValue = "true")
public class ItfServerCustomizer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String CURRENT_HOST = "0.0.0.0";
    private final TrustManagerFactory trustManagerFactory;
    private final KeyManagerFactory keyManagerFactory;
    @Value("${embedded.tls.server.port}")
    private int tlsServerPort;
    @Value("${embedded.ssl.server.port}")
    private int sslServerPort;

    @Autowired
    public ItfServerCustomizer(TrustManagerFactory trustManagerFactory, KeyManagerFactory keyManagerFactory) {
        this.trustManagerFactory = trustManagerFactory;
        this.keyManagerFactory = keyManagerFactory;
    }

    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        factory.addBuilderCustomizers(builder -> {
            createSslContext("TLSv1.2")
                    .ifPresent(tlsContext ->
                            builder.addHttpsListener(tlsServerPort, CURRENT_HOST, tlsContext));
            createSslContext("SSL")
                    .ifPresent(sslContext ->
                            builder.addHttpsListener(sslServerPort, CURRENT_HOST, sslContext));
        });
    }

    private Optional<SSLContext> createSslContext(String protocol) {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Could not create SSLContext for " + protocol + " protocol.", e);
        }
        return Optional.ofNullable(sslContext);
    }
}
