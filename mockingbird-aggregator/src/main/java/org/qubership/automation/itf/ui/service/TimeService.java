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

package org.qubership.automation.itf.ui.service;

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.LOG_APPENDER_DATE_FORMAT;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import org.qubership.automation.itf.core.util.config.Config;

public class TimeService {

    private static final String DATE_FORMAT = Config.getConfig().getString(LOG_APPENDER_DATE_FORMAT);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static String getFormattedDate(Date date) {
        Optional<Date> optional = Optional.ofNullable(date);
        return optional.map(date1 -> DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(date1.toInstant(),
                ZoneId.systemDefault()))).orElse("");
    }
}
