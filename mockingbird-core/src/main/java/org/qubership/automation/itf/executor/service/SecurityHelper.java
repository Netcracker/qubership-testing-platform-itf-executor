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

package org.qubership.automation.itf.executor.service;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.SerializationUtils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityHelper {

    private static final String AUTH_CONTEXT = "authContext";

    private static String serialize(Authentication authentication) {
        byte[] bytes = SerializationUtils.serialize(authentication);
        return DatatypeConverter.printBase64Binary(bytes);
    }

    private static Authentication deserialize(String authentication) {
        byte[] decoded = DatatypeConverter.parseBase64Binary(authentication);
        Authentication auth = (Authentication) SerializationUtils.deserialize(decoded);
        return auth;
    }

    public static void addAuthContextToMessage(JSONObject message) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String authContext = SecurityHelper.serialize(auth);
            if (StringUtils.isNotEmpty(authContext)) {
                message.put(AUTH_CONTEXT, authContext);
            }
        } catch (Exception e) {
            log.error("An error occurred at processing Security Context.", e);
        }
    }

    public static void propagateSecurityContext(JsonNode message) {
        try {
            if (message.has(AUTH_CONTEXT)) {
                String authContext = message.get(AUTH_CONTEXT).asText();
                if (StringUtils.isNotEmpty(authContext)) {
                    Authentication auth = SecurityHelper.deserialize(authContext);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            log.error("An error occurred at processing Security Context.", e);
        }
    }
}
