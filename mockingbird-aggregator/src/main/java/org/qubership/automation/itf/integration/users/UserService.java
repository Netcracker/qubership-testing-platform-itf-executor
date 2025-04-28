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

package org.qubership.automation.itf.integration.users;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.qubership.automation.itf.ui.model.LoginInfo;
import org.qubership.automation.itf.ui.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private static final String WARN_NO_AUTH = "Authentication is disabled - there are no access restrictions.";

    /**
     * Get {@link User} logged user.
     *
     * @return object with type {@link User}.
     */
    public User getLoggedUser() {
        User user = new User();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof KeycloakAuthenticationToken) {
            AccessToken accessToken = ((KeycloakPrincipal<?>) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal()).getKeycloakSecurityContext().getToken();
            user.setName(accessToken.getName());
        } else {
            setUndefinedUser(user);
        }
        return user;
    }

    /**
     * Get current user token.
     *
     * @return token with type {@link String}.
     */
    public String getLoggedUserToken() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof KeycloakPrincipal) {
            return ((KeycloakPrincipal<?>) principal).getKeycloakSecurityContext().getTokenString();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Check if the user has a support role.
     *
     * @return boolean result.
     */
    public boolean checkUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof KeycloakAuthenticationToken) {
            Set<String> roles = ((SimpleKeycloakAccount) authentication.getDetails()).getRoles();
            return roles.contains("ATP_SUPPORT") || roles.contains("ATP_ADMIN");
        }
        return true;
    }

    /**
     * Get current logged user info.
     *
     * @return object with type {@link User}.
     */
    public User getCurrentUserInfo() {
        AccessToken accessToken;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = new User();
        if (principal instanceof KeycloakPrincipal) {
            accessToken = ((KeycloakPrincipal<?>) principal).getKeycloakSecurityContext().getToken();
            user.setId(((KeycloakPrincipal<?>) principal).getName());
            user.setName(accessToken.getName());
        } else {
            setUndefinedUser(user);
        }
        return user;
    }

    /**
     * Get current login info.
     *
     * @return object with type {@link LoginInfo}.
     */
    public LoginInfo getLoginInfo() {
        AccessToken accessToken;
        boolean isAuthOff = false;
        User user = new User();
        LoginInfo loginInfo = new LoginInfo();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof KeycloakAuthenticationToken) {
            Set<String> roles = ((SimpleKeycloakAccount) authentication.getDetails()).getRoles();
            loginInfo.setSupport(roles.contains("ATP_SUPPORT") || roles.contains("ATP_ADMIN"));
            Object principal = authentication.getPrincipal();
            if (principal instanceof KeycloakPrincipal) {
                loginInfo.setToken(((KeycloakPrincipal<?>) principal).getKeycloakSecurityContext().getTokenString());
                accessToken = ((KeycloakPrincipal<?>) principal).getKeycloakSecurityContext().getToken();
                user.setId(((KeycloakPrincipal<?>) principal).getName());
                user.setName(accessToken.getName());
            } else {
                isAuthOff = true;
            }
        } else {
            isAuthOff = true;
        }
        if (isAuthOff) {
            setUndefinedUser(user);
            loginInfo.setSupport(true);
            loginInfo.setToken(StringUtils.EMPTY);
        }
        loginInfo.setUser(user);
        return loginInfo;
    }

    private void setUndefinedUser(User user) {
        log.warn(WARN_NO_AUTH);
        user.setName("undefined");
    }
}
