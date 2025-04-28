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

package org.qubership.automation.itf.ui.controls.entities.server;

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByProjectIdManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.TriggerSample;
import org.qubership.automation.itf.core.model.communication.message.ServerTriggerSyncRequest;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.TriggerConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.entities.util.ConfigurationControllerHelper;
import org.qubership.automation.itf.ui.controls.entities.util.ResponseCacheHelper;
import org.qubership.automation.itf.ui.controls.service.integration.ItfStubsRequestsService;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.environment.UIServerInbound;
import org.qubership.automation.itf.ui.messages.objects.environment.UIServerOutbound;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIInboundConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.objects.transport.UITriggerConfiguration;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;

@RestController
@Tags({
        @Tag(name = SwaggerConstants.SERVER_CONFIGURATION_QUERY_API,
                description = SwaggerConstants.SERVER_CONFIGURATION_QUERY_API_DESCR),
        @Tag(name = SwaggerConstants.SERVER_CONFIGURATION_COMMAND_API,
                description = SwaggerConstants.SERVER_CONFIGURATION_COMMAND_API_DESCR)
})
@RequiredArgsConstructor
public class ServerConfigurationController {

    private static final String INBOUND = "inbound";
    private static final String OUTBOUND = "outbound";

    private final ConfigurationControllerHelper configurationControllerHelper;
    private final ItfStubsRequestsService itfStubsRequestsService;

    @Value("${atp.multi-tenancy.enabled}")
    private Boolean multiTenancyEnabled;

    @Transactional(readOnly = true)
    @Modifying
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/server/outbound", method = RequestMethod.GET)
    @Operation(summary = "GetOutbound",
            description = "Retrieve outbound for system by server id, system id",
            tags = {SwaggerConstants.SERVER_CONFIGURATION_QUERY_API})
    @AuditAction(auditAction = "Get all Outbounds for System id {{#systemId}} and Server id {{#serverId}} in the "
            + "project {{#projectUuid}}")
    public UIServerOutbound getOutboundForSystem(
            @RequestParam(value = "serverId", defaultValue = "0") String serverId,
            @RequestParam(value = "systemId", defaultValue = "0") String systemId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        String operation = "retrieve outbound for system by server id, system id";
        Server server = (Server) getAndCheckObject(serverId, Server.class, operation);
        System system = (System) getAndCheckObject(systemId, System.class, operation);
        UIServerOutbound serverOutbound = new UIServerOutbound(server);
        serverOutbound.defineSystem(system);
        serverOutbound.defineConfiguration(server.getOutbounds(system));
        return serverOutbound;
    }

    @Transactional(readOnly = true)
    @Modifying
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/server/inbound", method = RequestMethod.GET)
    @Operation(summary = "GetInbound",
            description = "Retrieve inbound for system by server id, system id",
            tags = {SwaggerConstants.SERVER_CONFIGURATION_QUERY_API})
    @AuditAction(auditAction = "Get all Inbounds for System id {{#systemId}} and Server id {{#serverId}} in the "
            + "project {{#projectUuid}}")
    public UIServerInbound getInboundForSystem(
            @RequestParam(value = "serverId", defaultValue = "0") String serverId,
            @RequestParam(value = "systemId", defaultValue = "0") String systemId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        String operation = "retrieve inbound for system by server id, system id";
        Server server = (Server) getAndCheckObject(serverId, Server.class, operation);
        System system = (System) getAndCheckObject(systemId, System.class, operation);
        UIServerInbound serverInbound = new UIServerInbound(server);
        serverInbound.defineSystem(system);
        serverInbound.defineConfiguration(server.getInbounds(system));
        return serverInbound;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/server/outbound", method = RequestMethod.PUT)
    @Operation(summary = "SetOutbound",
            description = "Set outbound for system by server id, system id",
            tags = {SwaggerConstants.SERVER_CONFIGURATION_COMMAND_API})
    @AuditAction(auditAction = "Setup Outbounds for System id {{#systemId}} and Server id {{#serverId}} in the "
            + "project {{#projectUuid}}")
    public void setupOutbound(
            @RequestParam(value = "serverId", defaultValue = "0") String serverId,
            @RequestParam(value = "systemId", defaultValue = "0") String systemId,
            @RequestBody UIServerOutbound serverOutbound,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        String operation = "set outbound for system by server id, system id";
        Server server = (Server) getAndCheckObject(serverId, Server.class, operation);
        System system = (System) getAndCheckObject(systemId, System.class, operation);
        server.setUrl(serverOutbound.getUrl());
        if (serverOutbound.getConfigurations() != null) {
            for (UIConfiguration uiConfiguration : serverOutbound.getConfigurations()) {
                Configuration configuration = server.getOutbound(system, uiConfiguration.getType());
                if ("Outbound REST Synchronous".equals(uiConfiguration.getUserTypeName())
                        || "Outbound SOAP Over HTTP Synchronous".equals(uiConfiguration.getUserTypeName())) {
                    ResponseCacheHelper.beforeUpdatedForRestAndSoapTransport(configuration, uiConfiguration, projectId);
                }
                for (UIProperty uiProperty : uiConfiguration.getProperties()) {
                    configurationControllerHelper.setProperty(configuration, uiProperty, projectUuid);
                }
            }
        }
        server.store();
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/server/inbound", method = RequestMethod.PUT)
    @Operation(summary = "SetInbound",
            description = "Set inbound for system by server id, system id",
            tags = {SwaggerConstants.SERVER_CONFIGURATION_COMMAND_API})
    @AuditAction(auditAction = "Setup Inbounds for System id {{#systemId}} and Server id {{#serverId}} in the project"
            + " {{#projectUuid}}")
    public ServerTriggerSyncRequest setupInbound(
            @RequestParam(value = "serverId", defaultValue = "0") String serverId,
            @RequestParam(value = "systemId", defaultValue = "0") String systemId,
            @RequestParam(value = "quickSave", defaultValue = "false") boolean quickSave,
            @RequestBody UIServerInbound serverInbound,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        ServerTriggerSyncRequest serverTriggerSyncRequest = new ServerTriggerSyncRequest();
        String operation = "set inbound for system by server id, system id";
        Server server = (Server) getAndCheckObject(serverId, Server.class, operation);
        System system = (System) getAndCheckObject(systemId, System.class, operation);
        server.setUrl(serverInbound.getUrl());
        Collection<InboundTransportConfiguration> inbound = server.getInbounds(system);
        if (serverInbound.getConfigurations() != null) {
            for (UIInboundConfiguration configuration : serverInbound.getConfigurations()) {
                for (InboundTransportConfiguration entry : inbound) {
                    if (entry.getReferencedConfiguration().getID().toString().equals(
                            configuration.getTransport().getId())) {
                        for (UIProperty property : configuration.getProperties()) {
                            if ("Overridden".equals(property.getOverridden())
                                    || "Edited".equals(property.getOverridden())) {
                                configurationControllerHelper.setProperty(entry, property, projectUuid);
                            } else {
                                entry.remove(property.getName());
                            }
                        }
                        serverTriggerSyncRequest = synchronizeTriggers(configuration.getTriggers(), entry, quickSave,
                                projectUuid);
                    }
                }
            }
        }
        server.store();
        return serverTriggerSyncRequest;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "server/checkSystemServerDuplications", method = RequestMethod.GET)
    @Operation(summary = "CheckDuplicates",
            description = "Check for System-Server duplicates",
            tags = {SwaggerConstants.SERVER_CONFIGURATION_QUERY_API})
    @AuditAction(auditAction = "Check System+Server duplicates under Environments in the project "
            + "{{projectId}}/{{#projectUuid}}")
    public String checkSystemServerDuplications(@RequestParam(value = "projectId") BigInteger projectId,
                                                @RequestParam(value = "projectUuid") UUID projectUuid) {
        JSONObject result = new JSONObject();
        StringBuilder res = new StringBuilder();
        //noinspection unchecked
        Collection<? extends Environment> environments =
                CoreObjectManager.getInstance().getSpecialManager(Environment.class, SearchByProjectIdManager.class)
                        .getByProjectId(projectId);
        for (Environment env1 : environments) {
            for (Environment env2 : environments) {
                if (env1.getID() != env2.getID()) {
                    res.append(findDuplications(env1, env2, INBOUND));
                    res.append(findDuplications(env1, env2, OUTBOUND));
                }
            }
        }
        result.put("info", res.toString());
        return result.toJSONString();
    }

    private Storable getAndCheckObject(String objectId, Class clazz, String operation) {
        Storable obj = getManager(clazz).getById(objectId);
        ControllerHelper.throwExceptionIfNull(obj, "", objectId, clazz, operation);
        return obj;
    }

    private String findDuplications(Environment env1, Environment env2, String type) {
        StringBuilder result = new StringBuilder();
        Iterator iterator;
        Set<Map.Entry<System, Server>> env2Entry;
        if (INBOUND.equals(type)) {
            iterator = env1.getInbound().entrySet().iterator();
            env2Entry = env2.getInbound().entrySet();
        } else {
            iterator = env1.getOutbound().entrySet().iterator();
            env2Entry = env2.getOutbound().entrySet();
        }
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            if (env2Entry.contains(pair)) {
                result.append(env2.getName()).append(" has duplicated System/Server pair: ")
                        .append(pair.getKey()).append(" - ").append(pair.getValue()).append(";<br><br>");
            }
        }
        return result.toString();
    }

    private ServerTriggerSyncRequest synchronizeTriggers(final Collection<UITriggerConfiguration> uiTriggers,
                                                         InboundTransportConfiguration parent, boolean quickSaveMode,
                                                         UUID projectUuid) {
        ServerTriggerSyncRequest serverTriggerSyncRequest = new ServerTriggerSyncRequest();
        if (uiTriggers != null) {
            serverTriggerSyncRequest.getTriggerIdToDeactivate().addAll(
                    deleteMissedTriggers(uiTriggers, parent, projectUuid));
            addNewTriggers(uiTriggers, parent, projectUuid);
            serverTriggerSyncRequest.getTriggerIdToReactivate().addAll(
                    updateExistingTriggers(uiTriggers, parent, quickSaveMode, projectUuid));
        }
        return serverTriggerSyncRequest;
    }

    private List<TriggerSample> deleteMissedTriggers(final Collection<UITriggerConfiguration> uiTriggers,
                                                     InboundTransportConfiguration parent, UUID projectUuid) {
        List<TriggerSample> triggerSamples = new ArrayList<>();
        Collection<TriggerConfiguration> triggersToDelete = Collections2.filter(parent.getTriggerConfigurations(),
                input -> {
                    String idstr = Objects.toString(input.getID());
                    for (UITriggerConfiguration uiEventTrigger : uiTriggers) {
                        if (uiEventTrigger.getId() != null && uiEventTrigger.getId().equals(idstr)) {
                            return false;
                        }
                    }
                    return true;
                });
        for (TriggerConfiguration trigger : triggersToDelete) {
            triggerSamples.add(itfStubsRequestsService.createTriggerSample(trigger, projectUuid));
            trigger.remove();
            CoreObjectManager.managerFor(TriggerConfiguration.class).remove(trigger, true);
        }
        Lists.newArrayList(triggersToDelete).forEach(parent.getTriggerConfigurations()::remove);
        return triggerSamples;
    }

    private void addNewTriggers(final Collection<UITriggerConfiguration> uiTriggers,
                                InboundTransportConfiguration parent, UUID projectUuid) {
        Collection<UITriggerConfiguration> triggersToAdd = Collections2.filter(uiTriggers,
                input -> Strings.isNullOrEmpty(input.getId()));
        for (UITriggerConfiguration uiTrigger : triggersToAdd) {
            TriggerConfiguration trigger = new TriggerConfiguration(parent);
            setTriggerParameters(trigger, uiTrigger, projectUuid);
            trigger.store();
            parent.getTriggerConfigurations().add(trigger);
        }
    }

    private List<TriggerSample> updateExistingTriggers(final Collection<UITriggerConfiguration> uiTriggers,
                                                       InboundTransportConfiguration parent, boolean quickSaveMode,
                                                       UUID projectUuid) {
        List<TriggerSample> triggerSamples = new ArrayList<>();
        Collection<UITriggerConfiguration> triggersToModify = Collections2.filter(uiTriggers,
                input -> !Strings.isNullOrEmpty(input.getId()));
        for (UITriggerConfiguration uiTrigger : triggersToModify) {
            for (TriggerConfiguration trigger : parent.getTriggerConfigurations()) {
                if (trigger.getID().toString().equals(uiTrigger.getId())) {
                    setTriggerParameters(trigger, uiTrigger, projectUuid);
                    triggerSamples.add(itfStubsRequestsService.createTriggerSample(trigger, projectUuid));
                }
            }
        }
        return triggerSamples;
    }

    private void setTriggerParameters(TriggerConfiguration trigger, UITriggerConfiguration uiTrigger,
                                      UUID projectUuid) {
        trigger.setName(uiTrigger.getName());
        if (trigger.getID() == null) {
            trigger.setState(TriggerState.INACTIVE);
        }
        for (UIProperty property : uiTrigger.getProperties()) {
            if ("Overridden".equals(property.getOverridden()) || "Edited".equals(property.getOverridden())) {
                configurationControllerHelper.setProperty(trigger, property, projectUuid);
            } else {
                trigger.remove(property.getName());
            }
        }
    }
}
