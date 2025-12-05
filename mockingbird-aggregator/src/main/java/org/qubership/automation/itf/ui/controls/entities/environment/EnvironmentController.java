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

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.EnvironmentManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.EnvironmentObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.message.delete.DeleteEntityResultMessage;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.TriggerConfiguration;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.report.LinkCollectorConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.annotation.UserName;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.report.ReportLinkCollector;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironment;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironmentItem;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.objects.transport.UITriggerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@RestController
public class EnvironmentController extends AbstractController<UIEnvironment, Environment> {

    private final ReportLinkCollector reportLinkCollector;

    @Autowired
    public EnvironmentController(ReportLinkCollector reportLinkCollector) {
        this.reportLinkCollector = reportLinkCollector;
    }

    /**
     * Get all environment by project id.
     *
     * @param projectUuid ATP project UUID
     * @param projectId   ITF project id
     * @return a list of UIObject objects.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Environments for project {{#projectId}}/{{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam UUID projectUuid,
                                           @RequestParam BigInteger projectId) {
        return asListUIObject(CoreObjectManager.getInstance()
                        .getSpecialManager(Environment.class, EnvironmentObjectManager.class).getByProjectId(projectId),
                false, true);
    }

    @Override
    @Transactional(readOnly = true)
    @RequestMapping(value = "/environment/allbyparent", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Environments by parent id {{#parentId}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "parentId", defaultValue = "0") String parentId) {
        return super.getAll(parentId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/environment", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Environment by id {{#id}} in the project {{#projectUuid}}")
    public UIEnvironment getById(@RequestParam(value = "id", defaultValue = "0") String id,
                                 @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.getById(id);
    }

    /*
        This request was added because the page formation on the monitoring tab goes
        through feign client (see MonitoringController)
     */
    @Transactional(readOnly = true)
    @RequestMapping(value = "/environment/{id}", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Environment by id {{#id}} via feign")
    public UIEnvironment feignGetById(@PathVariable(value = "id") String id) {
        return super.getById(id);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/environment", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Environment under parent id {{#parentId}} in the project "
            + "{{#projectId}}/{{#projectUuid}}")
    public UIEnvironment create(
            @RequestParam(value = "parentId", defaultValue = "0") String parentId,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.create(parentId, projectId);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/environment", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Environment by id {{#uiEnvironment.id}} in the project {{#projectUuid}}")
    public UIEnvironment update(@RequestBody UIEnvironment uiEnvironment,
                                @RequestParam(value = "projectUuid") UUID projectUuid) {
        return super.update(uiEnvironment);
    }

    /**
     * Delete environment(s).
     *
     * @param objectsToDelete list of objects to delete
     * @param projectUuid     ATP project UUID
     * @return the result as a DeleteEntityResultMessage model.
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/environment", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Environments from project {{#projectUuid}}")
    public DeleteEntityResultMessage<String, UITriggerConfiguration> deleteEnv(
            @RequestBody Collection<UIEnvironment> objectsToDelete,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Map<String, List<UITriggerConfiguration>> envsWithActiveTriggers = new HashMap<>();
        Map<String, String> usingEnvs = new HashMap<>();

        for (UIObject uiObject : objectsToDelete) {
            List<UITriggerConfiguration> activeTriggers = new ArrayList<>();
            Environment envToDelete = getManager(Environment.class).getById(uiObject.getId());
            Collection<Server> serversUnderEnv = envToDelete.getInbound().values();
            for (Server server : serversUnderEnv) {
                if (server != null
                        && server.findUsages().stream().filter(usageInfo -> usageInfo.getProperty().equals("inbound"))
                        .allMatch(usageInfo -> usageInfo.getReferer().getID().equals(envToDelete.getID()))) {
                    for (InboundTransportConfiguration inboundTransportConfiguration : server.getInbounds()) {
                        for (TriggerConfiguration triggerConfiguration :
                                inboundTransportConfiguration.getTriggerConfigurations()) {
                            if (triggerConfiguration.getState().isOn()) {
                                activeTriggers.add(new UITriggerConfiguration(triggerConfiguration));
                            }
                        }
                    }
                }
            }
            if (!activeTriggers.isEmpty()) {
                envsWithActiveTriggers.put(envToDelete.getID().toString(), activeTriggers);
            } else {
                Collection<UsageInfo> usageInfo = envToDelete.remove();
                if (usageInfo != null) {
                    usingEnvs.put(envToDelete.getID().toString(), usageInfoListAsString(usageInfo));
                }
            }
        }
        return new DeleteEntityResultMessage<>(envsWithActiveTriggers, usingEnvs);
    }

    /**
     * Bulk replace substring in the transport properties of the Environment.
     * And replace ip of server.
     *
     * @param serverData - serverData[0] - old ip address, serverData[1] - new ip address,
     *                   serverData[2], [3], ... - ids of Environments to process
     * @return the result as a UIResult model.
     */
    @Transactional
    @RequestMapping(value = "/environment/bulk", method = RequestMethod.POST)
    @AuditAction(auditAction = "Bulk Update Servers in the Environment")
    public UIResult bulkServerUpdate(@RequestBody String[] serverData) {
        serverData[0] = serverData[0].trim();
        serverData[1] = serverData[1].trim();
        for (int i = 2; i < serverData.length; i++) {
            Environment environment = getManager(Environment.class).getById(serverData[i]);
            updateServerConfigurations(serverData[0], serverData[1], environment.getInbound(), true);
            updateServerConfigurations(serverData[0], serverData[1], environment.getOutbound(), false);
        }
        return new UIResult(true, "success");
    }

    /**
     * Find dublicate inbound configuration on environment.
     *
     * @param projectId ITF project id
     * @param projectUuid ATP project UUID
     * @return a list of objects.
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @GetMapping(value = "/environment/inbound/findDuplicate")
    public List<Map<String, Object>> findDuplicateConfigurationBySystemServer(
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        List<Object[]> list = CoreObjectManager.getInstance()
                .getSpecialManager(Environment.class, EnvironmentManager.class)
                .findDuplicateConfigurationBySystemServer(projectId);
        return list.stream().map(obj -> {
            Map<String, Object> configuration = new HashMap<>();
            configuration.put("environmentId", obj[0].toString());
            configuration.put("environmentName", obj[1]);
            configuration.put("systemId", obj[2].toString());
            configuration.put("systemName", obj[3]);
            configuration.put("serverId", obj[4].toString());
            configuration.put("serverName", obj[5]);
            return configuration;
        }).collect(Collectors.toList());
    }

    private void updateServerConfigurations(String oldServerIp,
                                            String newServerIp,
                                            Map<System, Server> configurations,
                                            boolean isInbound) {
        for (Map.Entry<System, Server> entry : configurations.entrySet()) {
            System system = entry.getKey();
            Server server = entry.getValue();
            if (isInbound) {
                for (InboundTransportConfiguration inbound : server.getInbounds(system)) {
                    for (String key : inbound.keySet()) {
                        String oldValue = inbound.get(key);
                        if (oldValue.contains(oldServerIp)) {
                            inbound.put(key, performReplace(oldValue, oldServerIp, newServerIp));
                        }
                    }
                }
            } else {
                for (OutboundTransportConfiguration outbound : server.getOutbounds(system)) {
                    for (String key : outbound.keySet()) {
                        String oldValue = outbound.get(key);
                        if (oldValue.contains(oldServerIp)) {
                            outbound.put(key, performReplace(oldValue, oldServerIp, newServerIp));
                        }
                    }
                }
            }
            if (server.getUrl().equals(oldServerIp)) {
                server.setUrl(newServerIp);
            }
        }
    }

    private String performReplace(String oldValue, String oldServerIp, String newServerIp) {
        int indexPosition = oldValue.indexOf(oldServerIp);
        String secondPartSubString = oldValue.substring(indexPosition + oldServerIp.length());
        return oldValue.substring(0, indexPosition) + newServerIp + secondPartSubString;
    }

    @Override
    protected Environment _beforeUpdate(UIEnvironment uiEnvironment, Environment environment) {
        updateEnvironment(uiEnvironment.getInbound(), environment.getInbound());
        updateEnvironment(uiEnvironment.getOutbound(), environment.getOutbound());
        if (uiEnvironment.getReportLinkCollectors() != null) {
            ObjectManager<TransportConfiguration> transportConfigurationObjectManager = CoreObjectManager.getInstance()
                    .getManager(TransportConfiguration.class);
            for (LinkCollectorConfiguration configuration : environment.getReportCollectors()) {
                transportConfigurationObjectManager.remove(configuration, true);
            }
            environment.getReportCollectors().clear();
            for (UIConfiguration uiConfiguration : uiEnvironment.getReportLinkCollectors()) {
                LinkCollectorConfiguration configuration = new LinkCollectorConfiguration();
                configuration.setParent(environment);
                configuration.setName(uiConfiguration.getName());
                configuration.setTypeName(uiConfiguration.getType());
                reportLinkCollector.registerCollectors(Collections.singletonList(uiConfiguration.getType()));
                Collection<PropertyDescriptor> properties =
                        reportLinkCollector.getProperties(uiConfiguration.getType());
                for (UIProperty uiProperty : uiConfiguration.getProperties()) {
                    for (PropertyDescriptor descriptor : properties) {
                        if (descriptor.getShortName().equals(uiProperty.getName())) {
                            try {
                                Class<?> aClass = Class.forName(descriptor.getTypeName());
                                if (Storable.class.isAssignableFrom(aClass)) {
                                    String id = (uiProperty.getReferenceValue()) != null
                                            ? uiProperty.getReferenceValue().getId() : "";
                                    configuration.put(descriptor.getShortName(), id);
                                } else {
                                    configuration.put(descriptor.getShortName(), uiProperty.getValue());
                                }
                            } catch (ClassNotFoundException e) {
                                configuration.put(descriptor.getShortName(), uiProperty.getValue());
                            }
                        }
                    }
                }
                environment.getReportCollectors().add(configuration);
            }
        }
        return super._beforeUpdate(uiEnvironment, environment);
    }

    @Override
    protected Class<Environment> _getGenericUClass() {
        return Environment.class;
    }

    @Override
    protected UIEnvironment _newInstanceTClass(Environment object) {
        if (object.getEnvironmentState() == null) {
            object.setEnvironmentState(TriggerState.EMPTY);
        }
        UIEnvironment uiEnvironment = new UIEnvironment(object);
        List<UIConfiguration> configurations = Lists.newArrayList();
        for (LinkCollectorConfiguration configuration : object.getReportCollectors()) {
            try {
                configurations.add(createLinkCollectorConfiguration(configuration));
            } catch (ClassNotFoundException e) {
                LOGGER.error("Error creating collector view", e);
            }
        }
        uiEnvironment.setReportLinkCollectors(configurations);
        return uiEnvironment;
    }

    @Override
    protected Storable _getParent(String parentId) {
        return getManager(Folder.class).getById(parentId);
    }

    private void updateEnvironment(ImmutableList<UIEnvironmentItem> environmentItems,
                                   Map<System, Server> systemServerMap) {
        if (environmentItems != null) {
            systemServerMap.clear();
            for (UIEnvironmentItem entry : environmentItems) {
                System system = getManager(System.class).getById(entry.getSystem().getId());
                Server server = getManager(Server.class).getById(entry.getServer().getId());
                if (system != null && server != null) {
                    systemServerMap.put(system, server);
                }
            }
        }
    }

    private UIConfiguration createLinkCollectorConfiguration(LinkCollectorConfiguration configuration)
            throws ClassNotFoundException {
        UIConfiguration uiConfiguration = new UIConfiguration(configuration);
        uiConfiguration.setClassName(LinkCollectorConfiguration.class.getName());
        uiConfiguration.setType(configuration.getTypeName());
        try {
            UserName annotation = Class.forName(configuration.getTypeName()).getAnnotation(UserName.class);
            uiConfiguration.setUserTypeName(annotation == null ? configuration.getTypeName() : annotation.value());
        } catch (Exception e) {
            uiConfiguration.setUserTypeName(configuration.getTypeName());
        }
        Collection<PropertyDescriptor> properties =
                reportLinkCollector.getProperties(configuration.getTypeName());
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(properties.size());
        for (PropertyDescriptor descriptor : properties) {
            String o = configuration.get(descriptor.getShortName());
            Class<?> propertyType = Class.forName(descriptor.getTypeName());
            UIProperty uiProperty;
            if (StringUtils.isBlank(o)) {
                uiProperty = new UIProperty(descriptor);
            } else if (Storable.class.isAssignableFrom(propertyType)) {
                Storable storable =
                        CoreObjectManager.getInstance().getManager(propertyType.asSubclass(Storable.class)).getById(o);
                uiProperty = new UIProperty(descriptor, new UIObject(storable));
            } else {
                uiProperty = new UIProperty(descriptor, o);
            }
            uiProperties.add(uiProperty);
        }
        uiConfiguration.setProperties(uiProperties);
        return uiConfiguration;
    }
}
