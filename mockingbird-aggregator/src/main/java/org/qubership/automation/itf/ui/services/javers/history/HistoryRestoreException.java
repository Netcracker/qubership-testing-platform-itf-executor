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

package org.qubership.automation.itf.ui.services.javers.history;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//reason code of history exceptions starts with 6
@ResponseStatus(
        value = HttpStatus.INTERNAL_SERVER_ERROR,
        reason = "ITFEXE-6001"
)
public class HistoryRestoreException extends AtpException {

    public static final String DEFAULT_MESSAGE = "Error occurred when tried to restore object %s%s%s";
    private static final String TYPE_INSERT = "[type=%s] ";
    private static final String ID_INSERT = "[id=%s] ";
    private static final String ERROR_INSERT = "\nError message: \n'%s'";

    public HistoryRestoreException(String errorMessage) {
        this(errorMessage, null, null);
    }

    public HistoryRestoreException(String errorMessage, String id, String type) {
        super(String.format(DEFAULT_MESSAGE,
                        StringUtils.isNotEmpty(id) ? String.format(ID_INSERT, id) : "",
                        StringUtils.isNotEmpty(type) ? String.format(TYPE_INSERT, type) : "",
                        StringUtils.isNotEmpty(errorMessage) ? String.format(ERROR_INSERT, errorMessage) : ""
                )
        );
    }
}
