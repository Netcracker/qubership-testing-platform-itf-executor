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

package org.qubership.automation.itf.transport.diameter.interceptors;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class Helper {
    public static boolean checkTagAndSession(String message, String startingTag, String sessionID) {
        if (Objects.isNull(sessionID)) {
            return checkTag(message, startingTag);
        }
        // Tags can contain avpCode attribute, so SessionID checking become more complicated.
        return message != null
                && StringUtils.startsWithIgnoreCase(message, startingTag)
                && message.contains("<Session-Id")
                && message.contains(">" + sessionID + "</Session-Id>");
    }

    public static boolean checkRequestTagAndSession(String message, String startingTag, String sessionID) {
        if (Objects.nonNull(sessionID)) {
            if (message.contains(sessionID)) {
                return Helper.checkTag(message, startingTag);
            } else {
                return false;
            }
        }
        return checkTag(message, startingTag);
    }

    public static boolean checkTag(String message, String startingTag) {
        return message != null && StringUtils.startsWithIgnoreCase(message, startingTag);
    }
}
