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

package org.qubership.automation.itf.ui.controls;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.generator.id.UniqueIdGenerator;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.service.export.ItfReplicationService;
import org.qubership.automation.itf.ui.services.javers.history.HistoryRestoreService;
import org.qubership.automation.itf.ui.services.javers.history.HistoryRetrieveService;
import org.qubership.automation.itf.ui.services.javers.history.model.HistoryCompareEntity;
import org.qubership.automation.itf.ui.services.javers.history.model.HistoryEntityConstant;
import org.qubership.automation.itf.ui.services.javers.history.model.HistoryItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JaversController {

    private final HistoryRestoreService historyRestoreService;
    private final HistoryRetrieveService historyRetrieveService;
    private final ItfReplicationService itfReplicationService;

    @Autowired
    public JaversController(HistoryRestoreService historyRestoreService, HistoryRetrieveService historyRetrieveService,
                            ItfReplicationService itfReplicationService) {
        this.historyRestoreService = historyRestoreService;
        this.historyRetrieveService = historyRetrieveService;
        this.itfReplicationService = itfReplicationService;
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping("/api/v1/history/{projectUuid}/{type}/{id}")
    public ResponseEntity<HistoryItemResponse> getAllHistory(
            @PathVariable(value = "projectUuid") UUID projectUuid,
            @PathVariable(value = "type") HistoryEntityConstant type,
            @PathVariable(value = "id") BigInteger objectId,
            @RequestParam(value = "offset", defaultValue = "0", required = false) Integer offset,
            @RequestParam(value = "limit", defaultValue = "10", required = false) Integer limit) {
        HistoryItemResponse response =
                historyRetrieveService.getAllHistory(objectId, type.getEntityClass(), offset, limit);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping("/api/v1/entityversioning/{projectUuid}/{type}/{id}")
    public ResponseEntity<List<HistoryCompareEntity>> getEntitiesByVersion(
            @PathVariable(value = "projectUuid") UUID projectUuid,
            @PathVariable(value = "type") HistoryEntityConstant type,
            @PathVariable(value = "id") BigInteger objectId,
            @RequestParam(value = "versions") List<Long> versions) {
        return ResponseEntity.ok(
                historyRetrieveService.getEntitiesByVersions(objectId, type.getEntityClass(), versions));
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @PostMapping("/api/v1/history/restore/{projectUuid}/{type}/{id}/revision/{revision}")
    public ResponseEntity<Void> restoreToRevision(@PathVariable(value = "projectUuid") UUID projectUuid,
                                                  @PathVariable(value = "type") HistoryEntityConstant type,
                                                  @PathVariable(value = "id") BigInteger objectId,
                                                  @PathVariable(value = "revision") Long revision) {
        LinkedList<Runnable> executeAfter =
                historyRestoreService.restoreToRevision(objectId, type.getEntityClass(), revision, projectUuid);
        executeAfter.forEach(Runnable::run);
        return ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping("/history/testReplicate")
    public void testHistoryForReplicate(@RequestParam(value = "systemId") BigInteger systemId, BigInteger projectId) {
        System system = CoreObjectManager.getInstance().getManager(System.class).getById(systemId);
        SystemTemplate template = new SystemTemplate();
        template.setID(UniqueIdGenerator.generate());
        template.setParent(system);
        template.setName("Test History For Replication");
        template.setText("TEST");
        template.setVersion(1);
        template.setProjectId(projectId);
        itfReplicationService.replicateStorableWithHistory(template);
    }

}
