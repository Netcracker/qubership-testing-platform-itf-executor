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

package org.qubership.automation.itf.ui.controls.service.diameter;

import java.util.List;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.diameter.DiameterConnectionInfo;
import org.qubership.automation.itf.core.util.DiameterConnectionInfoCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DiameterConnectionsController {

    private DiameterConnectionInfoCacheService diameterConnectionInfoCacheService;

    @Autowired
    public void setDiameterConnectionInfoCacheService(
            DiameterConnectionInfoCacheService diameterConnectionInfoCacheService) {
        this.diameterConnectionInfoCacheService = diameterConnectionInfoCacheService;
    }

    @PreAuthorize("@entityAccess.isSupport()")
    @GetMapping(value = "/tools/connections")
    @AuditAction(auditAction = "Get Diameter connections list")
    public List<DiameterConnectionInfo> getDiameterConnections() {
        return diameterConnectionInfoCacheService.getAllDiameterConnections();
    }

    @PreAuthorize("@entityAccess.isSupport()")
    @GetMapping(value = "/tools/drop")
    @AuditAction(auditAction = "Drop Diameter connection with key {{#key}}")
    public List<DiameterConnectionInfo> dropDiameterConnection(@RequestParam String key) {
        diameterConnectionInfoCacheService.remove(key);
        log.info(key + " is dropped");
        return getDiameterConnections();
    }
}
