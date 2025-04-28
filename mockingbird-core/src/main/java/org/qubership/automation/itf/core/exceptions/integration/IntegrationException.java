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

package org.qubership.automation.itf.core.exceptions.integration;

import org.qubership.automation.itf.core.exceptions.ItfExecutorException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//reason code of integration exceptions starts with 3
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "ITFEXE-3001")
public class IntegrationException extends ItfExecutorException {

    public static final String DEFAULT_MESSAGE = "%s";

    public IntegrationException(String message) {
        super(message);
    }
}
