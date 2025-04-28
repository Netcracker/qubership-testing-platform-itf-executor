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

package org.qubership.automation.itf.ui.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.qubership.automation.itf.integration.converter.OffsetDateTime2TimestampConverter;
import org.qubership.automation.itf.integration.converter.Timestamp2OffsetDateTimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class MvcConfiguration {

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper jackson2ObjectMapper = new ObjectMapper();
        jackson2ObjectMapper.registerModule(new JavaTimeModule());
        jackson2ObjectMapper.findAndRegisterModules();
        jackson2ObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jackson2ObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return jackson2ObjectMapper;
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.getConfiguration().getConverters().add(new Timestamp2OffsetDateTimeConverter());
        modelMapper.getConfiguration().getConverters().add(new OffsetDateTime2TimestampConverter());
        return modelMapper;
    }

}
