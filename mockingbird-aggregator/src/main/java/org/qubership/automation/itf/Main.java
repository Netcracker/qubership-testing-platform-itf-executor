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

import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableM2MRestTemplate;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableOauth2FeignClientInterceptor;
import org.qubership.atp.common.lock.annotation.EnableAtpLockManager;
import org.qubership.atp.common.probes.controllers.DeploymentController;
import org.qubership.atp.integration.configuration.annotation.EnableAtpJaegerLog;
import org.qubership.atp.multitenancy.hibernate.annotation.EnableMultiTenantDataSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                MongoAutoConfiguration.class,
                HazelcastAutoConfiguration.class,
                LiquibaseAutoConfiguration.class
        }
)
@ServletComponentScan(basePackages = "org.qubership.automation.itf.ui.config.servlets")
@ImportResource(locations = {"classpath:kafka-config.xml"})
@ComponentScan(
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "org.qubership.automation.itf.core.config.ReportHibernateConfiguration"),
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "org.qubership.automation.itf.core.hibernate.spring.managers.reports.*")
        })
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = {"org.qubership.automation.itf.core.hibernate.spring.repositories.executor"})
@EnableTransactionManagement
@EnableM2MRestTemplate
@EnableFeignClients(basePackages = {
        "org.qubership.automation.itf.integration.users",
        "org.qubership.automation.itf.integration.reports",
        "org.qubership.automation.itf.integration.environments",
        "org.qubership.atp.integration.configuration.feign",
        "org.qubership.automation.itf.integration.catalogue"})
@EnableOauth2FeignClientInterceptor
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableAtpLockManager
@Import({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class,
        DeploymentController.class
})
@EnableMultiTenantDataSource
@EntityScan(basePackages = "org.qubership.automation.itf.core.model.jpa.history")
@EnableScheduling
@EnableAtpJaegerLog
public class Main {

    /**
     * Main.
     *
     * @param args as usual
     */
    public static void main(String[] args) {
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(Main.class);
        springApplicationBuilder
                .build()
                .addListeners(new ApplicationPidFileWriter("application.pid"));
        springApplicationBuilder.run(args);
    }
}



