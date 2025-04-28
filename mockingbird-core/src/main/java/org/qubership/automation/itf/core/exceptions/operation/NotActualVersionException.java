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

package org.qubership.automation.itf.core.exceptions.operation;

import org.qubership.automation.itf.core.exceptions.ItfExecutorException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//reason code of operation exceptions starts with 4
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "ITFEXE-4002")
public class NotActualVersionException extends ItfExecutorException {
    public static final String DEFAULT_MESSAGE =
            "Object '%s' can not be saved because it was changed by another user.\n"
                    + "UI version = %s, actual version = %s.\n"
                    + "Please reload object first. After that you will be able to change and save it.";

    public NotActualVersionException(String objectName, String uiVersion, String dbVersion) {
        super(String.format(DEFAULT_MESSAGE, objectName, uiVersion, dbVersion));
    }
}
