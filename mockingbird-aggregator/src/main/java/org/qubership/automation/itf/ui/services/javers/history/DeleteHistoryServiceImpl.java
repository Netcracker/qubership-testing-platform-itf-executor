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

package org.qubership.automation.itf.ui.services.javers.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javers.core.metamodel.object.SnapshotType;
import org.qubership.automation.itf.core.hibernate.spring.repositories.executor.history.JaversCommitPropertyRepository;
import org.qubership.automation.itf.core.hibernate.spring.repositories.executor.history.JaversCommitRepository;
import org.qubership.automation.itf.core.hibernate.spring.repositories.executor.history.JaversGlobalIdRepository;
import org.qubership.automation.itf.core.hibernate.spring.repositories.executor.history.JaversSnapshotRepository;
import org.qubership.automation.itf.core.model.javers.JaversCountResponse;
import org.qubership.automation.itf.core.model.jpa.history.JvGlobalIdEntity;
import org.qubership.automation.itf.core.model.jpa.history.JvSnapshotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteHistoryServiceImpl implements DeleteHistoryService {

    private static final Integer FIRST_PAGE = 0;

    private final JaversSnapshotRepository snapshotRepository;
    private final JaversGlobalIdRepository globalIdRepository;
    private final JaversCommitRepository commitRepository;
    private final JaversCommitPropertyRepository commitPropertyRepository;

    @Transactional(rollbackFor = Exception.class)
    public void deleteOldAndUpdateAsInitial(Long globalId, List<JvSnapshotEntity> snapshots) {
        snapshots.forEach(snapshot -> deleteOldSnapshot(globalId, snapshot));
        findTheOldestSnapshotByGlobalIdAndUpdateTypeAsInitial(globalId);
    }

    private void deleteOldSnapshot(Long globalId, JvSnapshotEntity snapshot) {
        final Long commitId = snapshot.getCommitId();
        final Long version = snapshot.getVersion();
        snapshotRepository.deleteByVersionAndGlobalIdAndCommitId(version, globalId, commitId);
        log.info("Snapshots with version '{}', globalId '{}', commitId '{}' are deleted.", version, globalId, commitId);
        Long commitCount = snapshotRepository.countByCommitId(commitId);
        // remove all commits and properties by commitId if they are no longer referenced by snapshots
        if (commitCount.equals(0L)) {
            commitPropertyRepository.deleteByIdCommitId(commitId);
            commitRepository.deleteById(commitId);
            log.info("Commit properties and commits with commitId '{}' are deleted.", commitId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOldSnapshots(long maxRevisionCount) {
        findGlobalIdAndCount(maxRevisionCount).forEach(response -> {
            final Long globalId = response.getId();
            final long numberOfOldSnapshots = response.getCount() - maxRevisionCount;
            if (numberOfOldSnapshots >= 1) {
                final List<JvSnapshotEntity> oldSnapshots = findOldSnapshots(globalId, numberOfOldSnapshots);
                deleteOldAndUpdateAsInitial(globalId, oldSnapshots);
            }
        });
    }

    /**
     * Delete terminated snapshots.
     *
     * @param pageSize number of snapshots deleted in one step.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTerminatedSnapshots(Integer pageSize) {
        while (true) {
            Page<JvSnapshotEntity> page =
                    snapshotRepository.findAllByTypeIs(SnapshotType.TERMINAL, PageRequest.of(FIRST_PAGE, pageSize));
            deleteTerminatedSnapshots(page);
            if (!page.hasNext()) {
                break;
            }
        }
    }

    private void deleteTerminatedSnapshots(Page<JvSnapshotEntity> page) {
        List<JvSnapshotEntity> terminalSnapshots = page.getContent();
        Set<Long> globalIds = getIds(terminalSnapshots, JvSnapshotEntity::getGlobalId);
        if (globalIds.isEmpty()) {
            return;
        }
        log.info("Number of terminal globalIds '{}'.", globalIds.size());
        List<JvSnapshotEntity> snapshots = new ArrayList<>();
        doAction(globalIds, ids -> snapshots.addAll(snapshotRepository.findAllByGlobalIdIn(ids)));
        Set<Long> commitIds = getIds(snapshots, JvSnapshotEntity::getCommitId);
        List<JvGlobalIdEntity> jvGlobalIdEntities = globalIdRepository.findAllByOwnerIdIn(globalIds);
        if (!jvGlobalIdEntities.isEmpty()) {
            Set<Long> innerGlobalIds = jvGlobalIdEntities.stream()
                    .map(JvGlobalIdEntity::getId).collect(Collectors.toSet());
            doAction(innerGlobalIds, snapshotRepository::deleteByGlobalIdIn);
            doAction(innerGlobalIds, globalIdRepository::deleteByIdIn);
        }
        log.info("Number of terminal commitIds '{}'.", commitIds.size());
        doAction(globalIds, snapshotRepository::deleteByGlobalIdIn);
        log.info("Terminated snapshots are deleted.");
        doAction(globalIds, globalIdRepository::deleteByIdIn);
        log.info("Terminated globalIds are deleted.");
        removeUsedCommitId(commitIds);
        doAction(commitIds, commitPropertyRepository::deleteByIdCommitIdIn);
        log.info("Terminated commit properties are deleted.");
        doAction(commitIds, commitRepository::deleteByIdIn);
        log.info("Terminated commits are deleted.");
    }

    /**
     * Get the oldest snapshot and update snapshot type with INITIAL value.
     *
     * @param globalId globalId
     * @return {@link JvSnapshotEntity} entity;
     */
    private JvSnapshotEntity findTheOldestSnapshotByGlobalIdAndUpdateTypeAsInitial(Long globalId) {
        JvSnapshotEntity snapshot = snapshotRepository.findFirstByGlobalIdOrderByVersionAsc(globalId);
        if (Objects.isNull(snapshot)) {
            return null;
        }
        snapshot.setType(SnapshotType.INITIAL);
        return snapshotRepository.save(snapshot);
    }

    private List<JvSnapshotEntity> findOldSnapshots(Long globalId, Long count) {
        PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(count));
        List<JvSnapshotEntity> oldSnapshots =
                snapshotRepository.findAllByGlobalIdOrderByVersionAsc(globalId, pageRequest);
        log.info("Number of old snapshots for globalId '{}': {}.", globalId, oldSnapshots.size());
        return oldSnapshots;
    }

    /**
     * Get globalId and number of old objects.
     *
     * @param maxRevisionCount number of the last revisions.
     * @return {@link List} of {@link JaversCountResponse}
     */
    private List<JaversCountResponse> findGlobalIdAndCount(long maxRevisionCount) {
        List<JaversCountResponse> response = snapshotRepository.findGlobalIdAndCountGreaterThan(maxRevisionCount);
        log.info("Number of unique globalIds:  {}.", response.size());
        return response;
    }

    private <T> void doAction(Collection<T> collection, Consumer<? super List<T>> action) {
        Iterators.partition(collection.iterator(), 100).forEachRemaining(action);
    }

    private <T, R> Set<R> getIds(List<T> snapshots, Function<T, R> function) {
        return snapshots.stream()
                .map(function)
                .collect(Collectors.toSet());
    }

    private void removeUsedCommitId(Set<Long> commitIds) {
        Set<Long> commitIdsToRemove = new HashSet<>();
        for (Long commitId : commitIds) {
            if (snapshotRepository.countByCommitId(commitId) > 0) {
                commitIdsToRemove.add(commitId);
            }
        }
        commitIds.removeAll(commitIdsToRemove);
    }
}
