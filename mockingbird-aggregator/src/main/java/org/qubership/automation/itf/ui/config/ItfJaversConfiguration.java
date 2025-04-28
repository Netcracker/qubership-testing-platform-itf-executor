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

import org.javers.core.Javers;
import org.javers.core.MappingStyle;
import org.javers.repository.api.JaversRepository;
import org.javers.spring.auditable.CommitPropertiesProvider;
import org.javers.spring.jpa.TransactionalJpaJaversBuilder;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.automation.itf.ui.config.codec.BigIntegerTypeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
@ConditionalOnProperty(name = "javers.history.enabled", havingValue = "true")
public class ItfJaversConfiguration {

    @Bean
    public JaversAuthorProvider javersAuthorProvider(Provider<UserInfo> userInfoProvider) {
        return new JaversAuthorProvider(userInfoProvider);
    }

    @Bean
    public Javers javers(JaversRepository javersRepository, JpaTransactionManager transactionManager) {
        return TransactionalJpaJaversBuilder
                .javers()
                .withTxManager(transactionManager)
                .registerJaversRepository(javersRepository)
                .withPackagesToScan("org.qubership.automation.itf.core.model.javers.history")
                .withMappingStyle(MappingStyle.FIELD)
                .registerValueTypeAdapter(new BigIntegerTypeAdapter())
                .build();
    }

    @Bean
    public ItfJaversSpringDataJpaAuditableRepositoryAspect javersSpringDataAuditableAspect(Javers javers,
                                                                                           JaversAuthorProvider javersAuthorProvider,
                                                                                           CommitPropertiesProvider commitPropertiesProvider) {
        return new ItfJaversSpringDataJpaAuditableRepositoryAspect(javers, javersAuthorProvider,
                commitPropertiesProvider);
    }

    @Bean
    public ItfJaversAuditableAspect javersAuditableAspect(Javers javers,
                                                          JaversAuthorProvider javersAuthorProvider,
                                                          CommitPropertiesProvider commitPropertiesProvider) {
        return new ItfJaversAuditableAspect(javers, javersAuthorProvider, commitPropertiesProvider);
    }

}
