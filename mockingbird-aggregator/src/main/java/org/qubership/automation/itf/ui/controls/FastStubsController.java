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
import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.stub.fast.FastConfigurationRequest;
import org.qubership.automation.itf.core.stub.fast.FastConfigurationResponse;
import org.qubership.automation.itf.core.stub.fast.FastStubConfigurationAction;
import org.qubership.automation.itf.ui.services.faststubs.FastStubsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FastStubsController {

    private final FastStubsService fastStubsService;

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @PostMapping(value = "/fast-stubs/candidates")
    @AuditAction(auditAction = "Get fast stubs candidates (situations) by operations '{{#operationIds}}' "
            + "of project {{#projectUuid}}")
    public Object getFastStubsCandidates(@RequestBody List<BigInteger> operationIds,
                                         @RequestParam UUID projectUuid) {
        return fastStubsService.getFastStubsCandidates(operationIds, projectUuid);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @PostMapping(value = "/fast-stubs/configuration")
    @AuditAction(auditAction = "Configuration fast stubs config by operations of project {{#projectUuid}} "
            + "with action {{#action}}")
    public FastConfigurationResponse generateFastStubsConfigs(@RequestBody FastConfigurationRequest fastConfigurationRequest,
                                                              @RequestParam FastStubConfigurationAction action,
                                                              @RequestParam UUID projectUuid) {
        return fastStubsService.generateFastStubsConfigs(fastConfigurationRequest, action, projectUuid);
    }
}
