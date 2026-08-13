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

package org.qubership.automation.itf.integration.users;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.ui.model.LoginInfo;
import org.qubership.automation.itf.ui.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
        String id = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Object principal = jwtAuthenticationToken.getPrincipal();
            if (principal instanceof Jwt jwt) {
                id = jwt.getClaim("sub");
                user.setId(id);
                user.setName(jwt.getClaimAsString("preferred_username"));
            }
        }
        if (id == null) {
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            return jwt.getTokenValue();
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
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            // Get raw roles directly from JWT claims.
            //  Recommended way - collect them via .getAuthorities(), but here we need role names only.
            Jwt jwt = jwtAuthenticationToken.getToken();
            List<String> roles = getRealmRoles(jwt);
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
        User user = new User();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Object principal = jwtAuthenticationToken.getPrincipal();
            if (principal instanceof Jwt jwt) {
                String id = jwt.getClaim("sub");
                user.setId(id);
                user.setName(jwt.getClaimAsString("preferred_username"));
            }
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
        boolean isAuthOff = false;
        User user = new User();
        LoginInfo loginInfo = new LoginInfo();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            List<String> roles = getRealmRoles(jwt);
            loginInfo.setSupport(roles.contains("ATP_SUPPORT") || roles.contains("ATP_ADMIN"));
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt) {
                loginInfo.setToken(jwt.getTokenValue());
                user.setId(jwt.getId());
                user.setName(jwt.getClaimAsString("preferred_username"));
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

    /**
     * Get realm roles from JWT. To be moved to auth stubbed library after migration is completed.
     *
     * @param jwt java web token
     * @return realm roles
     */
    private List<String> getRealmRoles(Jwt jwt) {
        final Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List list) {
            return (List<String>) list;
        }
        return new ArrayList<>();
    }

}
