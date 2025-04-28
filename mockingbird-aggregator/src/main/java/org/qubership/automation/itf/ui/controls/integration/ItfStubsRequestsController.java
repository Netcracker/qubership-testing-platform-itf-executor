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

package org.qubership.automation.itf.ui.controls.integration;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.communication.EnvironmentSample;
import org.qubership.automation.itf.core.model.communication.TriggerSample;
import org.qubership.automation.itf.ui.controls.service.integration.ItfStubsRequestsService;
import org.qubership.automation.itf.ui.messages.objects.integration.stubs.UIUpdateTriggerStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ItfStubsRequestsController {

    private final ItfStubsRequestsService itfStubsRequestsService;

    @GetMapping(value = "/trigger/all/active")
    @AuditAction(auditAction = "Get all Active Triggers")
    public List<TriggerSample> getAllActiveTriggers() {
        return itfStubsRequestsService.getAllActiveTriggers();
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/trigger/all")
    @AuditAction(auditAction = "Get all Triggers in the project {{#projectUuid}}")
    public List<TriggerSample> getAllTriggersByProject(@RequestParam("projectUuid") UUID projectUuid) {
        return itfStubsRequestsService.getAllTriggersByProject(projectUuid);
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/trigger/all/reactivate")
    @AuditAction(auditAction = "Get all Active/Error Triggers in the project {{#projectUuid}}")
    public List<TriggerSample> getAllActiveAndErrorTriggersByProject(@RequestParam("projectUuid") UUID projectUuid) {
        return itfStubsRequestsService.getAllActiveAndErrorTriggersByProject(projectUuid);
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/trigger/environment/folder/{id}")
    @AuditAction(auditAction = "Get all Triggers under Folder id {{#envFolderId}}")
    public List<EnvironmentSample> getTriggersByEnvFolder(@PathVariable("id") BigInteger envFolderId) {
        return itfStubsRequestsService.getTriggersByEnvFolder(envFolderId);
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/trigger/environmentId/{id}")
    @AuditAction(auditAction = "Get all Triggers under Environment id {{#environmentId}}")
    public EnvironmentSample getTriggersByEnvironment(@PathVariable("id") BigInteger environmentId) {
        return itfStubsRequestsService.getTriggersByEnvironment(environmentId);
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/trigger/{id}")
    @AuditAction(auditAction = "Get Trigger by id {{#id}}")
    public TriggerSample getTriggerById(@PathVariable("id") BigInteger id) {
        return itfStubsRequestsService.getTriggerById(id);
    }

    @Transactional
    @PatchMapping(value = "/trigger")
    @AuditAction(auditAction = "Update Trigger status by id {{#uiUpdateTriggerStatus.id}}")
    public Result updateTriggerStatus(@RequestBody UIUpdateTriggerStatus uiUpdateTriggerStatus) {
        return itfStubsRequestsService.updateTriggerStatus(uiUpdateTriggerStatus);
    }
}
