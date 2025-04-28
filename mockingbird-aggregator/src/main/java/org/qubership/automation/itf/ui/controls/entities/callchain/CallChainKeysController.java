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

package org.qubership.automation.itf.ui.controls.entities.callchain;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIKey;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Transactional(readOnly = true)
@RestController
public class CallChainKeysController extends ControllerHelper {

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).CALLCHAIN.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/callchain/keys", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Keys for CallChain by id {{#id}} in the project {{#projectUuid}}")
    public UIWrapper<List<UIKey>> getKeys(@RequestParam(value = "id", defaultValue = "0") String id,
                                          @RequestParam(value = "projectUuid") UUID projectUuid) {
        CallChain callChain = getManager(CallChain.class).getById(id);
        throwExceptionIfNull(callChain, null, id, CallChain.class, "get CallChain keys");
        return new UIWrapper<>(getUIKeys(callChain.getKeys()));
    }
}
