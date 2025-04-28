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

import javax.servlet.Servlet;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@Configuration
public class ServletConfiguration {

    /**
     * @return instance of {@link ServletRegistrationBean} for camel REST servlet registration.
     */
    @Bean
    public ServletRegistrationBean<Servlet> camelServletRegistrationBean() {
        ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<>(
                new CamelHttpTransportServlet());
        registration.addUrlMappings("/mockingbird-transport-rest/*");
        registration.setName("CamelServlet");
        registration.setLoadOnStartup(1);
        registration.setAsyncSupported(true);
        return registration;
    }


    @Bean
    public InternalResourceViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver internalResourceViewResolver = new InternalResourceViewResolver();
        internalResourceViewResolver.setPrefix("/transports/mockingbird-transport-diameter/jsp/");
        internalResourceViewResolver.setSuffix(".jsp");
        return internalResourceViewResolver;
    }

}
