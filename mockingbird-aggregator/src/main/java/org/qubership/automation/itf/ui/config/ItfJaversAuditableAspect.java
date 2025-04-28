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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.javers.core.Javers;
import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.auditable.CommitPropertiesProvider;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.system.System;

@Aspect
public class ItfJaversAuditableAspect extends ItfAbstractSpringAuditableRepositoryAspect {

    protected ItfJaversAuditableAspect(Javers javers,
                                       AuthorProvider authorProvider,
                                       CommitPropertiesProvider commitPropertiesProvider) {
        super(javers, authorProvider, commitPropertiesProvider);
    }

    @AfterReturning(
            value = "execution(* replicateStorableWithHistory(..)) && args(storable)",
            argNames = "pjp,storable"
    )
    public void onReplicateExecuted(JoinPoint pjp, Storable storable) {
        if (storable instanceof System) {
            System system = (System) storable;
            system.getSystemParsingRules().forEach(parsingRule -> onReplicateExecuted(pjp, parsingRule));
            system.getOperations().forEach(operation -> onReplicateExecuted(pjp, operation));
            system.getSystemTemplates().forEach(template -> onReplicateExecuted(pjp, template));
        } else {
            processOnSave(pjp, storable, true);
        }
    }
}
