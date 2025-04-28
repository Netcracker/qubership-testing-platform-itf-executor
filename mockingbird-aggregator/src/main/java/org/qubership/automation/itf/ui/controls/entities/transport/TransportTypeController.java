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

package org.qubership.automation.itf.ui.controls.entities.transport;

import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.UITypeList;
import org.qubership.automation.itf.ui.util.UIHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransportTypeController {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/types", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all supported Transport Types")
    public UITypeList getTransportTypes(@RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        Map<String, String> objectTransportTypes = TransportRegistryManager.getInstance().getTransportTypes();
        return UIHelper.convertMapOfTypeToUITypeList(objectTransportTypes);
    }
}
