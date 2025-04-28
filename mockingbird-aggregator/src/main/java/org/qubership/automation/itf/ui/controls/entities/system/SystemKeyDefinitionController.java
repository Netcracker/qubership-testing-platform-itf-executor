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

package org.qubership.automation.itf.ui.controls.entities.system;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.KeyDefinitionProvider;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UISystem;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemKeyDefinitionController extends ControllerHelper {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/incoming", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Incoming Context Definitions of {{#type}} with id {{#id}} in the project "
            + "{{#projectUuid}}")
    public UIWrapper<String> getIncoming(@RequestParam(value = "id") String id,
                                         @RequestParam(value = "type") String type,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        return getKeyDefinition(id, type, true);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/outgoing", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Outgoing Context Definitions of {{#type}} with id {{#id}} in the project "
            + "{{#projectUuid}}")
    public UIWrapper<String> getOutgoing(@RequestParam(value = "id") String id,
                                         @RequestParam(value = "type") String type,
                                         @RequestParam(value = "projectUuid") UUID projectUuid) {
        return getKeyDefinition(id, type, false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/system/operationdefinition", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Operation Definition on System with id {{#id}} in the project {{#projectUuid}}")
    public UISystem getOperationDefinition(@RequestParam(value = "id", defaultValue = "0") String id,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        System system = CoreObjectManager.getInstance().getManager(System.class).getById(id);
        throwExceptionIfNull(system, StringUtils.EMPTY, id, System.class, "get System by id");
        UISystem uiSystem = new UISystem();
        uiSystem.setOperationDefinition(new UIWrapper<>(getDefinitionValue(system.getOperationKeyDefinition())));
        return uiSystem;
    }

    private UIWrapper<String> getKeyDefinition(String id, String type, boolean isIncoming) {
        KeyDefinitionProvider parent = getKeyDefinitionProviderById(id, type);
        throwExceptionIfNull(parent, StringUtils.EMPTY, id, KeyDefinitionProvider.class,
                "get " + type + " by id");
        String keyDefinition = (isIncoming)
                ? parent.getIncomingContextKeyDefinition() : parent.getOutgoingContextKeyDefinition();
        return new UIWrapper<>(getDefinitionValue(keyDefinition));
    }

    private KeyDefinitionProvider getKeyDefinitionProviderById(String id, String type) {
        return ("system".equals(type))
                ? CoreObjectManager.getInstance().getManager(System.class).getById(id)
                : CoreObjectManager.getInstance().getManager(Operation.class).getById(id);
    }

}
