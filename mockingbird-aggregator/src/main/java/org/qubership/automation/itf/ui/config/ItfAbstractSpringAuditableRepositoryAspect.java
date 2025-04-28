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

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.javers.core.Javers;
import org.javers.core.commit.Commit;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.GlobalIdDTO;
import org.javers.repository.jql.InstanceIdDTO;
import org.javers.repository.jql.QueryBuilder;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.javers.spring.auditable.AspectUtil;
import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.auditable.CommitPropertiesProvider;
import org.javers.spring.auditable.aspect.JaversCommitAdvice;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.history.JvSnapshotEntity;
import org.qubership.automation.itf.core.util.services.CoreServices;
import org.qubership.automation.itf.ui.services.javers.history.HistoryEntityHelper;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItfAbstractSpringAuditableRepositoryAspect {

    private final AuthorProvider authorProvider;
    private final JaversCommitAdvice javersCommitAdvice;
    private final Javers javers;

    protected ItfAbstractSpringAuditableRepositoryAspect(Javers javers,
                                                         AuthorProvider authorProvider,
                                                         CommitPropertiesProvider commitPropertiesProvider) {
        this.javers = javers;
        this.authorProvider = authorProvider;
        this.javersCommitAdvice = new JaversCommitAdvice(javers, authorProvider, commitPropertiesProvider);
    }

    protected void onSave(JoinPoint pjp, Object returnedObject, boolean force) {
        if (getRepositoryInterface(pjp).isPresent() || force) {
            AspectUtil.collectReturnedObjects(returnedObject).forEach(javersCommitAdvice::commitObject);
        }
    }

    protected void onDelete(JoinPoint pjp) {
        getRepositoryInterface(pjp).ifPresent(i -> {
            RepositoryMetadata metadata = DefaultRepositoryMetadata.getMetadata(i);
            for (Object deletedObject : AspectUtil.collectArguments(pjp)) {
                handleDelete(metadata, deletedObject);
            }
        });
    }

    private Optional<Class> getRepositoryInterface(JoinPoint pjp) {
        for (Class i : pjp.getTarget().getClass().getInterfaces()) {
            if (i.isAnnotationPresent(JaversSpringDataAuditable.class) && CrudRepository.class.isAssignableFrom(i)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }


    void handleDelete(RepositoryMetadata repositoryMetadata, Object domainObjectOrId) {
        if (isIdClass(repositoryMetadata, domainObjectOrId)) {
            Class<?> domainType = repositoryMetadata.getDomainType();
            if (javers.findSnapshots(QueryBuilder.byInstanceId(domainObjectOrId, domainType).limit(1).build())
                    .isEmpty()) {
                return;
            }
            javersCommitAdvice.commitShallowDeleteById(domainObjectOrId, domainType);
        } else if (isDomainClass(repositoryMetadata, domainObjectOrId)) {
            if (javers.findSnapshots(QueryBuilder.byInstance(domainObjectOrId).limit(1).build()).isEmpty()) {
                return;
            }
            javersCommitAdvice.commitShallowDelete(domainObjectOrId);
        } else {
            throw new IllegalArgumentException("Domain object or object id expected");
        }
    }

    private boolean isDomainClass(RepositoryMetadata metadata, Object o) {
        return metadata.getDomainType().isAssignableFrom(o.getClass());
    }

    private boolean isIdClass(RepositoryMetadata metadata, Object o) {
        return metadata.getIdType().isAssignableFrom(o.getClass());
    }


    public Commit commit(Object currentVersion) {
        return javers.commit(authorProvider.provide(), currentVersion);
    }

    public void deleteHistoryEntity(Object localId, Class entity) {
        javersCommitAdvice.commitShallowDeleteById(localId, HistoryEntityHelper.getHistoryEntityClass(entity));
    }

    private Commit commitShallowDeleteById(GlobalIdDTO globalId) {
        InstanceIdDTO instanceId = (InstanceIdDTO) globalId;
        Optional<CdoSnapshot> latestSnapshot =
                javers.getLatestSnapshot(instanceId.getCdoId(), instanceId.getEntity());
        return latestSnapshot
                .map(cdoSnapshot -> javers.commitShallowDeleteById(authorProvider.provide(), globalId))
                .orElse(null);
    }

    protected void processOnSave(JoinPoint pjp, Object responseEntity, boolean forceSaveHistory) {
        try {
            if (responseEntity instanceof JvSnapshotEntity) {
                return;
            }
            if (Objects.isNull(((Storable) responseEntity).getID())) {
                log.debug("Entity with type {} is skipped, because has null objectID.",
                        responseEntity.getClass().getName());
                return;
            }
            if (HistoryEntityHelper.isNotSupportEntity(responseEntity.getClass())) {
                log.debug("Entity with type {} is skipped, because not supported in itf history.",
                        responseEntity.getClass().getName());
                return;
            }
            if (!isHistoryEnabled(responseEntity)) {
                log.debug("Project setting 'enable.history.versioning' for project id {} is disabled, "
                        + "so history commit is skipped.", ((Storable) responseEntity).getProjectId());
                return;
            }
            Optional<Object> historyEntity = HistoryEntityHelper.fromStorable((Storable) responseEntity);
            historyEntity.ifPresent(o -> this.onSave(pjp, o, forceSaveHistory));
        } catch (Exception e) {
            log.error("An error occurred while object history processing for type {}.", responseEntity.getClass(), e);
        }
    }

    protected boolean isHistoryEnabled(Object responseEntity) {
        BigInteger projectId = ((Storable) responseEntity).getProjectId();
        if (Objects.isNull(projectId)) {
            return false;
        }
        return Boolean.parseBoolean(
                CoreServices.getProjectSettingsService().get(projectId, "enable.history.versioning", "false")
        );
    }
}
