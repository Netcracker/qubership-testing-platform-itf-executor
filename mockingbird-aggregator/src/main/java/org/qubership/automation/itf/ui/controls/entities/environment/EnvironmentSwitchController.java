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

package org.qubership.automation.itf.ui.controls.entities.environment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.assertj.core.util.Lists;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvironmentManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.EnvironmentObjectManager;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.environment.InboundInfo;
import org.qubership.automation.itf.ui.messages.objects.environment.TransportInfo;
import org.qubership.automation.itf.ui.messages.objects.environment.UIServerInbound;
import org.qubership.automation.itf.ui.messages.objects.transport.UIInboundConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UITriggerConfiguration;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Tags({
        @Tag(name = SwaggerConstants.ENVIRONMENT_TRIGGER_QUERY_API,
                description = SwaggerConstants.ENVIRONMENT_TRIGGER_QUERY_API_DESCR),
        @Tag(name = SwaggerConstants.ENVIRONMENT_TRIGGER_COMMAND_API,
                description = SwaggerConstants.ENVIRONMENT_TRIGGER_COMMAND_API_DESCR)
})
public class EnvironmentSwitchController extends ControllerHelper {

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "environment/status", method = RequestMethod.GET)
    @Operation(summary = "GetEnvironmentState",
            description = "Retrieve environment status by id",
            tags = {SwaggerConstants.ENVIRONMENT_TRIGGER_QUERY_API})
    public String getStatus(@RequestParam(value = "id", defaultValue = "0") String id,
                            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        Environment environment = updateEnvStateFromDb(id);
        return "{ \"state\" : \"" + environment.getEnvironmentState().toString() + "\", \"result\" : \"success\" }";
    }

    /**
     * Get environment info by id.
     *
     * @param id          environment object
     * @param projectUuid ATP project UUID
     * @return a list of UIObject objects.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment/inbound/info", method = RequestMethod.GET)
    @Operation(summary = "GetEnvironmentInfo",
            description = "Retrieve environment info by id",
            tags = {SwaggerConstants.ENVIRONMENT_TRIGGER_QUERY_API})
    public Map<String, Object> getInboundInfo(
            @RequestParam(value = "id", defaultValue = "0") BigInteger id,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws JsonProcessingException {
        List<UIServerInbound> uiInbounds = new ArrayList<>();
        for (String info : CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, EnvironmentManager.class).getInboundInfo(id)) {
            InboundInfo inboundInfo = objectMapper.readValue(info, InboundInfo.class);

            UIServerInbound serverInbound = new UIServerInbound();
            serverInbound.setName(inboundInfo.getSystemServer());
            List<UIInboundConfiguration> uiInboundConfigurations = Lists.newArrayList();
            for (TransportInfo transportInfo : inboundInfo.getTransports()) {
                UIInboundConfiguration uiInboundConfiguration = new UIInboundConfiguration();
                UIObject uiTransport = new UIObject();
                uiTransport.setName(transportInfo.getTransportName());
                uiInboundConfiguration.setTransport(uiTransport);
                List<UITriggerConfiguration> triggerConfigurations = Lists.newArrayList();
                transportInfo.getTriggers().stream().filter(uiTriggerInfo -> uiTriggerInfo.getTriggerId() != null)
                        .forEach(uiTriggerInfo -> {
                            UITriggerConfiguration triggerConfiguration = new UITriggerConfiguration();
                            triggerConfiguration.setName(uiTriggerInfo.getTriggerName());
                            triggerConfiguration.setState(
                                    TriggerState.fromString(uiTriggerInfo.getTriggerState()).getState());
                            triggerConfigurations.add(triggerConfiguration);
                        });
                uiInboundConfiguration.setTriggers(triggerConfigurations);
                uiInboundConfigurations.add(uiInboundConfiguration);
            }
            serverInbound.setConfigurations(uiInboundConfigurations);
            uiInbounds.add(serverInbound);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("inboundsInfo", uiInbounds);
        return result;
    }

    /**
     * Update Environment state by calculated state retrieved from database, then store the Environment
     * This method subject to change after the transition of the service "atp-itf-stubs" to a separate DB,
     * see - ATPII-35615
     *
     * @return Environment object
     */
    private Environment updateEnvStateFromDb(@Nonnull String objectId) {
        CoreObjectManagerService objectManagerService = CoreObjectManager.getInstance();
        TriggerState stateFromDb = TriggerState.fromString(objectManagerService
                .getSpecialManager(Environment.class, EnvironmentObjectManager.class)
                .getEnvironmentStateById(objectId));
        Environment environment = objectManagerService.getManager(Environment.class).getById(objectId);
        if (Objects.nonNull(environment) && !stateFromDb.equals(environment.getEnvironmentState())) {
            environment.setEnvironmentState(stateFromDb);
            environment.store();
        }
        return environment;
    }
}
