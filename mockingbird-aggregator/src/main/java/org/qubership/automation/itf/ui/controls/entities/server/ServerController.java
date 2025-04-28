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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.ServerObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.message.delete.DeleteEntityResultMessage;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.environment.UIServer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController extends AbstractController<UIServer, Server> {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/server/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Servers from project {{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/server/allbyparent", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Servers in Folder by id {{#parentId}} in the project {{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "parentId", defaultValue = "0") String parentId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getAll(parentId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/server", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Server by id {{#id}} in the project {{#projectUuid}}")
    public UIServer getById(@RequestParam(value = "id", defaultValue = "0") String id,
                            @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/server", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Server under Folder id {{#parentId}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public UIServer create(
            @RequestParam(value = "parent", defaultValue = "0") String parentId,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.create(parentId, projectId);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/server/duplicate", method = RequestMethod.POST)
    @AuditAction(auditAction = "Copy Servers in the project {{#projectId}}/{{#projectUuid}}")
    public ArrayList<UIServer> create(
            @RequestParam(value = "parent", defaultValue = "0") String parentId,
            @RequestBody ArrayList<UIServer> uiServerSources,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        ArrayList<UIServer> uiServerDests = new ArrayList<>();
        for (UIServer uiServerSource : uiServerSources) {
            UIServer serverDest = super.create(uiServerSource.getParent().getId(), projectId);
            serverDest.setName(uiServerSource.getName().concat(" copy"));
            serverDest.setUrl(uiServerSource.getUrl());
            uiServerDests.add(super.update(serverDest));
        }
        return uiServerDests;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/server", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Server by id {{#uiServer.id}} in the project {{#projectUuid}}")
    public UIServer update(@RequestBody UIServer uiServer,
                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.update(uiServer);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/server", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Servers from Project {{#projectUuid}}")
    public DeleteEntityResultMessage<String, UIObject> delete(
            @RequestParam(value = "ignoreUsages", defaultValue = "false") Boolean ignoreUsages,
            @RequestBody UIIds ids,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, List<UIObject>> serversWithTriggers = new HashMap<>();
        Map<String, String> usingServers = new HashMap<>();
        for (String id : ids.getIds()) {
            Server server = getManager(Server.class).getById(id);
            ControllerHelper.throwExceptionIfNull(server, "", id, Server.class, "delete server(s)");
            List<UIObject> triggers = new ArrayList<>();
            Map<String, List<BigInteger>> blockingTriggers = getManager(Server.class).findImportantChildren(server);
            if (blockingTriggers != null && !blockingTriggers.isEmpty()) {
                /*
                    Currently in UI triggers list is not used, even in error message.
                    So, results from .findImportantChildren() are simply wrapped into UIObjects.
                    May be, further changes would be made: return count of objects instead of objects list.
                 */
                //Wrap Transport Triggers
                if (blockingTriggers.containsKey("TransportTriggers")) {
                    for (BigInteger triggerId : blockingTriggers.get("TransportTriggers")) {
                        UIObject uiObject = new UIObject();
                        uiObject.setId(triggerId.toString());
                        uiObject.setClassName("TransportTrigger");
                        triggers.add(uiObject);
                    }
                }
            }
            if (!triggers.isEmpty()) {
                serversWithTriggers.put(server.getID().toString(), triggers);
            } else {
                Collection<UsageInfo> usageInfo = getManager(Server.class).remove(server, ignoreUsages);
                if (usageInfo != null) {
                    usingServers.put(server.getID().toString(), usageInfoListAsString(usageInfo));
                }
            }
        }
        return new DeleteEntityResultMessage<>(serversWithTriggers, usingServers);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/server/usages", method = RequestMethod.POST)
    public Map<String, Object> getUsages(@RequestParam(value = "projectUuid") UUID projectUuid,
                                         @RequestBody UIIds uiServersObj) {
        Map<String, Object> res = new HashMap<>();
        Set<Object> environments = new HashSet<>();
        for (String serverId : uiServersObj.getIds()) {
            Server server = getManager(Server.class).getById(serverId);
            Collection<UsageInfo> usages = server.findUsages();
            for (UsageInfo usage : usages) {
                environments.add(String.valueOf(usage.getReferer().getID()));
            }
        }
        res.put("usages", environments);
        return res;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/server/deleteUnusedOutboundConfigurations", method = RequestMethod.DELETE)
    public UIResult deleteUnusedConfigurationByProjectId(@RequestParam(value = "projectId") BigInteger projectId) {
        try {
            int deletedCount = CoreObjectManager.getInstance()
                    .getSpecialManager(Server.class, ServerObjectManager.class)
                    .deleteUnusedOutboundConfigurationsByProjectId(projectId);
            return new UIResult(true, String.format("%s rows are deleted", deletedCount));
        } catch (Exception ex) {
            return new UIResult(false, String.format("Deleting isn't performed due to error: %s", ex.getMessage()));
        }
    }

    @Override
    protected Server _beforeUpdate(UIServer uiServer, Server server) {
        server.setUrl(uiServer.getUrl());
        return super._beforeUpdate(uiServer, server);
    }

    @Override
    protected Class<Server> _getGenericUClass() {
        return Server.class;
    }

    @Override
    protected UIServer _newInstanceTClass(Server object) {
        return new UIServer(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        return getManager(Folder.class).getById(parentId);
    }
}
