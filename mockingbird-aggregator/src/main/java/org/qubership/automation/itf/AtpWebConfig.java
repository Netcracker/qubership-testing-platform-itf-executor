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

package org.qubership.automation.itf;

import org.qubership.automation.itf.ui.config.converter.StringToEnumEciEntityConstantClassConverter;
import org.qubership.automation.itf.ui.config.converter.StringToEnumEdsContentTypeConverter;
import org.qubership.automation.itf.ui.config.converter.StringToEnumFastStubActionConverter;
import org.qubership.automation.itf.ui.config.converter.StringToEnumHistoryEntityConstantConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AtpWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html", "/**", "/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/", "/", "classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToEnumEdsContentTypeConverter());
        registry.addConverter(new StringToEnumEciEntityConstantClassConverter());
        registry.addConverter(new StringToEnumHistoryEntityConstantConverter());
        registry.addConverter(new StringToEnumFastStubActionConverter());
    }
}
