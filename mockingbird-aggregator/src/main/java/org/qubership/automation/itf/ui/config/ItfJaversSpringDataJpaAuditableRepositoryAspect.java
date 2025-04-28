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
import org.javers.spring.auditable.AspectUtil;
import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.auditable.CommitPropertiesProvider;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.ui.services.javers.history.HistoryEntityHelper;
import org.springframework.core.annotation.Order;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(0)
public class ItfJaversSpringDataJpaAuditableRepositoryAspect extends ItfAbstractSpringAuditableRepositoryAspect {

    private static final String NOT_IMPLEMENTED_MESSAGE = "This aspect isn't implemented yet";

    public ItfJaversSpringDataJpaAuditableRepositoryAspect(Javers javers, AuthorProvider authorProvider,
                                                           CommitPropertiesProvider commitPropertiesProvider) {
        super(javers, authorProvider, commitPropertiesProvider);
    }

    @AfterReturning("execution(public * delete(..)) && this(org.springframework.data.repository.CrudRepository)")
    public void onDeleteExecuted(JoinPoint pjp) {
        for (Object deletedObject : AspectUtil.collectArguments(pjp)) {
            if (deletedObject instanceof Storable) {
                try {
                    if (HistoryEntityHelper.isNotSupportEntity(deletedObject.getClass())) {
                        log.debug("Entity with type {} is skipped, because it isn't supported in itf history.",
                                deletedObject.getClass().getName());
                        return;
                    }
                    if (!isHistoryEnabled(deletedObject)) {
                        log.debug("Project setting 'enable.history.versioning' for project id {} is disabled, "
                                + "so history commit is skipped.", ((Storable) deletedObject).getProjectId());
                        return;
                    }
                    deleteHistoryEntity(((Storable) deletedObject).getID(), deletedObject.getClass());
                } catch (Exception e) {
                    log.error("An error occurred while object history processing for type {}.",
                            deletedObject.getClass(), e);
                }
            }
        }
    }

    @AfterReturning("execution(public * deleteById(..)) && this(org.springframework.data.repository.CrudRepository)")
    public void onDeleteByIdExecuted(JoinPoint pjp) {
        // #TODO implemented
        log.error(NOT_IMPLEMENTED_MESSAGE);
    }

    @AfterReturning("execution(public * deleteAll(..)) && this(org.springframework.data.repository.CrudRepository)")
    public void onDeleteAllExecuted(JoinPoint pjp) {
        // #TODO implemented
        log.error(NOT_IMPLEMENTED_MESSAGE);
    }

    @AfterReturning(
            value = "execution(public * save(..)) && this(org.springframework.data.repository.CrudRepository)",
            returning = "responseEntity"
    )
    public void onSaveExecuted(JoinPoint pjp, Object responseEntity) {
        processOnSave(pjp, responseEntity, false);
    }

    @AfterReturning(
            value = "execution(public * saveAll(..)) && this(org.springframework.data.repository.CrudRepository)",
            returning = "responseEntity"
    )
    public void onSaveAllExecuted(JoinPoint pjp, Object responseEntity) {
        // #TODO implemented
        log.error(NOT_IMPLEMENTED_MESSAGE);
    }

    @AfterReturning(
            value = "execution(public * saveAndFlush(..)) && this(org.springframework.data.jpa.repository" +
                    ".JpaRepository)",
            returning = "responseEntity"
    )
    public void onSaveAndFlushExecuted(JoinPoint pjp, Object responseEntity) {
        // #TODO implemented
        log.error(NOT_IMPLEMENTED_MESSAGE);
    }

    @AfterReturning("execution(public * deleteInBatch(..)) && this(org.springframework.data.jpa.repository" +
            ".JpaRepository)")
    public void onDeleteInBatchExecuted(JoinPoint pjp) {
        // #TODO implemented
        log.error(NOT_IMPLEMENTED_MESSAGE);
    }

}
