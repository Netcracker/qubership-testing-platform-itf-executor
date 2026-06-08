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

package org.qubership.automation.itf.integration.atp2;

import static org.qubership.atp.auth.springbootstarter.Constants.AUTHORIZATION_HEADER_NAME;
import static org.qubership.atp.auth.springbootstarter.Constants.BEARER_TOKEN_TYPE;

import java.util.Optional;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
@Profile({"default"})
public class AuthTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenProvider.class);

    private final AccessTokenProvider accessTokenProvider;
    private final OAuth2ProtectedResourceDetails protectedResourceDetails;
    private final AccessTokenRequest accessTokenRequest = new DefaultAccessTokenRequest();

    /**
     * RamAdapterConfiguration constructor.
     *
     * @param accessTokenProvider      access token provider
     * @param protectedResourceDetails protected resource details
     */
    public AuthTokenProvider(AccessTokenProvider accessTokenProvider,
                             OAuth2ProtectedResourceDetails protectedResourceDetails) {
        this.accessTokenProvider = accessTokenProvider;
        this.protectedResourceDetails = protectedResourceDetails;
        registerHttpClientInterceptors();
    }

    /**
     * Get Authorization token.
     *
     * @return Bearer token
     */
    public Optional<String> getAuthToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //noinspection unchecked
        Optional<String> relayToken = Optional.ofNullable(authentication)
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof KeycloakPrincipal)
                .map(principal -> (KeycloakPrincipal<KeycloakSecurityContext>) principal)
                .map(KeycloakPrincipal::getKeycloakSecurityContext)
                .map(KeycloakSecurityContext::getTokenString);
        if (relayToken.isPresent()) {
            return relayToken;
        }
        OAuth2AccessToken accessToken = accessTokenProvider.obtainAccessToken(
                protectedResourceDetails,
                accessTokenRequest
        );
        return Optional.ofNullable(accessToken.getValue());
    }

    private void registerHttpClientInterceptors() {
        RequestUtils.registerHttpInterceptor((httpRequest, httpContext) -> {
            log.debug("Getting a token. Process [httpRequest={}]", httpRequest);
            Optional<String> bearerToken = getAuthToken();
            if (bearerToken.isPresent()) {
                httpRequest.addHeader(AUTHORIZATION_HEADER_NAME,
                        String.format("%s %s", BEARER_TOKEN_TYPE, bearerToken.get()));
            } else {
                log.warn("Token is empty.");
            }
        });
    }
}
