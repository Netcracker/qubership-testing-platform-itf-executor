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

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByProjectIdManager;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.constants.TransportState;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransportState;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RestController
public class TransportStateController {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/check", method = RequestMethod.GET)
    @AuditAction(auditAction = "Check Transports Deploy summary state for project {{#projectId}}/{{#projectUuid}}")
    public String checkDeploy(@RequestParam(value = "projectId") BigInteger projectId,
                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<String> types = getTypes(projectId).keySet();
        boolean ok = true;
        boolean inProgress = true;
        for (String type : types) {
            TransportState state = TransportRegistryManager.getInstance().getState(type);
            switch (state) {
                case NOT_READY:
                case UNDEPLOYED:
                    ok = false;
                    inProgress = false;
                    break;
                case READY:
                    inProgress = false;
                    break;
                default:
                    inProgress = true;
            }
        }
        if (ok && !inProgress) {
            return "{\"state\" : \"ok\"}";//ok, deploying, nok
        } else if (inProgress) {
            return "{\"state\" : \"deploying\"}";//ok, deploying, nok
        } else {
            return "{\"state\" : \"nok\"}";
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/state", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Transport states for project {{#projectId}}/{{#projectUuid}}")
    public List<UITransportState> getStates(
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, AtomicInteger> types = getTypes(projectId);
        List<UITransportState> states = Lists.newArrayListWithCapacity(types.size());
        for (Map.Entry<String, AtomicInteger> entry : types.entrySet()) {
            String type = entry.getKey();
            TransportState state = TransportRegistryManager.getInstance().getState(type);
            UITransportState uiTransportState = new UITransportState();
            uiTransportState.setTypeName(type);
            uiTransportState.setState(state.toString());
            uiTransportState.setUsageInfo(entry.getValue().get());
            if (TransportState.UNDEPLOYED.equals(state) || TransportState.NOT_READY.equals(state)) {
                uiTransportState.setUserName(type);
            } else {
                try {
                    uiTransportState.setUserName(TransportRegistryManager.getInstance().getTransportTypes().get(type));
                    uiTransportState.defineProperties(TransportRegistryManager.getInstance().getProperties(type));
                } catch (Exception e) {
                    uiTransportState.setUserName(type + " (There are exception(s); may be undeployed? "
                            + e.getMessage() + ")");
                }
            }
            states.add(uiTransportState);
        }
        return states;
    }

    private Map<String, AtomicInteger> getTypes(BigInteger projectId) {
        Collection<? extends TransportConfiguration> all =
                CoreObjectManager.getInstance().getSpecialManager(TransportConfiguration.class,
                        SearchByProjectIdManager.class).getByProjectId(projectId);
        Map<String, AtomicInteger> types = Maps.newHashMapWithExpectedSize(10);
        for (TransportConfiguration configuration : all) {
            if (types.containsKey(configuration.getTypeName())) {
                types.get(configuration.getTypeName()).incrementAndGet();
            } else {
                types.put(configuration.getTypeName(), new AtomicInteger(1));
            }
        }
        return types;
    }
}
