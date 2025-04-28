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

package org.qubership.automation.itf.integration.converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

import org.modelmapper.spi.ConditionalConverter;
import org.modelmapper.spi.MappingContext;

public class OffsetDateTime2TimestampConverter implements ConditionalConverter<OffsetDateTime, Timestamp> {
    @Override
    public MatchResult match(Class<?> sourceType, Class<?> destinationType) {
        if (OffsetDateTime.class.isAssignableFrom(sourceType)
                && Timestamp.class.isAssignableFrom(destinationType)) {
            return MatchResult.FULL;
        }
        return MatchResult.NONE;
    }

    @Override
    public Timestamp convert(MappingContext<OffsetDateTime, Timestamp> context) {
        if (context.getSource() == null) {
            return null;
        }
        return new Timestamp(context.getSource().toEpochSecond());
    }
}
