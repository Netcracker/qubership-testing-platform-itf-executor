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

package org.qubership.automation.itf.ui.controls;

import org.qubership.automation.itf.integration.users.UserService;
import org.qubership.automation.itf.ui.model.LoginInfo;
import org.qubership.automation.itf.ui.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get {@link User} logged user.
     *
     * @return object with type {@link User}.
     */
    @PreAuthorize(value = "isAuthenticated()")
    @RequestMapping(value = "/user/current", method = RequestMethod.GET)
    public User getLoggedUser() {
        return userService.getLoggedUser();
    }

    /**
     * Get current user token.
     *
     * @return token with type {@link String}.
     */
    @PreAuthorize(value = "isAuthenticated()")
    @RequestMapping(value = "/user/token", method = RequestMethod.GET)
    public String getCurrentUserToken() {
        String loggedUserToken = userService.getLoggedUserToken();
        return "Bearer ".concat(loggedUserToken);
    }

    /**
     * Check if the user has a support role.
     *
     * @return boolean result.
     */
    @PreAuthorize(value = "isAuthenticated()")
    @RequestMapping(value = "/user/isSupport", method = RequestMethod.GET)
    public boolean checkUserRole() {
        return userService.checkUserRole();
    }

    /**
     * Get current logged user info.
     *
     * @return object with type {@link User}.
     */
    @PreAuthorize(value = "isAuthenticated()")
    @RequestMapping(value = "/user/info", method = RequestMethod.GET)
    public User getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * Get current login info.
     *
     * @return object with type {@link LoginInfo}.
     */
    @PreAuthorize(value = "isAuthenticated()")
    @RequestMapping(value = "/user/login_info", method = RequestMethod.GET)
    public LoginInfo getLoginInfo() {
        LoginInfo loginInfo = userService.getLoginInfo();
        loginInfo.setToken("Bearer ".concat(loginInfo.getToken())); // Backward compatibility with getCurrentUserToken()
        return loginInfo;
    }
}
