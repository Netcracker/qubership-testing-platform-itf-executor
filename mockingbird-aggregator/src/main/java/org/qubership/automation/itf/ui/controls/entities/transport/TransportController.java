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

import static org.qubership.automation.itf.ui.controls.util.ControllerHelper.getManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.NotValidValueException;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.message.delete.DeleteEntityResultMessage;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.model.usage.UsageInfo;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.helper.Comparators;
import org.qubership.automation.itf.core.util.transport.base.AbstractTransportImpl;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.entities.util.ConfigurationControllerHelper;
import org.qubership.automation.itf.ui.controls.entities.util.ResponseCacheHelper;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UITypedObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@RestController
public class TransportController extends AbstractController<UITransport, TransportConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportController.class);

    @Autowired
    private ConfigurationControllerHelper configurationControllerHelper;

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Transports by parent id {{#parentId}} in the project {{#projectUuid}}")
    public List<? extends UIObject> getAll(@RequestParam(value = "parent", defaultValue = "0") String parentId,
                                           @RequestParam(value = "isFull", defaultValue = "true") boolean isFull,
                                           @RequestParam(value = "displayType",
                                                   defaultValue = "selectList") String displayType,
                                           @RequestParam(value = "projectId") BigInteger projectId,
                                           @RequestParam(value = "projectUuid") UUID projectUuid) {
        if (StringUtils.isEmpty(parentId) || "0".equals(parentId)) {
            throw new NotValidValueException(
                    "Get All Transports: operation can be performed only for parent System; parent id can't be empty");
        }
        if (isFull) {
            return super.getAll(parentId);
        } else {
            List<UITransport> uiTransports = new ArrayList<>();
            boolean toTableDisplay = "table".equals(displayType);
            Collection<? extends TransportConfiguration> configurations = getManager(TransportConfiguration.class)
                    .getAllByParentId(parentId);
            for (TransportConfiguration transport : configurations) {
                if (transport != null) {
                    UITransport uiTransport = new UITransport();
                    uiTransport.fillForQuickDisplay(transport, toTableDisplay);
                    uiTransports.add(uiTransport);
                }
            }
            return uiTransports;
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/transport", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Transport Configuration by id {{#id}} in the project {{#projectUuid}}")
    public UITransport get(@RequestParam(value = "id", defaultValue = "0") String id,
                           @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        TransportConfiguration transport = getManager(TransportConfiguration.class).getById(id);
        ControllerHelper.throwExceptionIfNull(transport, "", id, TransportConfiguration.class, "get Transport by id");
        UITransport uiTransport = new UITransport(transport);
        if (!uiTransport.defineProperties(transport)) {
            uiTransport.setName(uiTransport.getName() + " [Implementation not deployed]");
        }
        uiTransport.setTransportInterceptors(getUIInterceptors(transport));
        return uiTransport;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/transport", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Transport under System with id {{#parentId}} in the project {{#projectUuid}}")
    public UIObject create(
            @RequestParam(value = "system", defaultValue = "0") String parentId,
            @RequestBody UITypedObject uiTypedObject,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        return super.create(parentId, uiTypedObject.getName(), uiTypedObject.getType());
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/transport", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Transport from System with id {{#id}} from project "
            + "{{#projectId}}/{{#projectUuid}}")
    public Map<String, Object> delete(
            @RequestParam(value = "system", defaultValue = "0") String id,
            @RequestParam(value = "ignoreUsages", defaultValue = "false") Boolean ignoreUsages,
            @RequestBody UIIds uiDeleteObjectReq,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        System system = getManager(System.class).getById(id);
        List<TransportConfiguration> filteredTransportConfigurations = system.getTransports().stream().filter(
                transportConfiguration -> Arrays.asList(uiDeleteObjectReq.getIds())
                        .contains(transportConfiguration.getID().toString())
        ).collect(Collectors.toList());
        Map<String, List<UIObject>> transportsWithTriggers = new HashMap<>();
        Map<String, String> usingTransports = new HashMap<>();
        List<TransportConfiguration> transportsWithoutTriggers = new ArrayList<>();

        //Search for Transport Triggers
        for (TransportConfiguration transportConfiguration : filteredTransportConfigurations) {
            List<UIObject> triggers = new ArrayList<>();
            if (transportConfiguration.getMep().isInbound()) {
                Map<String, List<BigInteger>> blockingTriggers = getManager(TransportConfiguration.class)
                        .findImportantChildren(transportConfiguration);
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
                    transportsWithTriggers.put(transportConfiguration.getID().toString(), triggers);
                } else {
                    transportsWithoutTriggers.add(transportConfiguration);
                }
            } else {
                transportsWithoutTriggers.add(transportConfiguration);
            }
        }
        Iterator<TransportConfiguration> iter = transportsWithoutTriggers.iterator();
        while (iter.hasNext()) {
            TransportConfiguration transportConfiguration = iter.next();
            Collection<UsageInfo> usageInfoList =
                    getManager(TransportConfiguration.class).remove(transportConfiguration, ignoreUsages);
            if (usageInfoList != null) {
                usingTransports.put(transportConfiguration.getID().toString(), usageInfoListAsString(usageInfoList));
            }
            iter.remove();
        }
        system.store();
        system.flush();
        Map<String, Object> result = new HashMap<>();
        result.put("parentVersion", system.getVersion());
        result.put("result", new DeleteEntityResultMessage<>(transportsWithTriggers, usingTransports));
        return result;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).TRANSPORT.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/transport", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Transport by id {{#id}} in the project {{#projectUuid}}")
    public UITransport update(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestBody UITransport uiTransport,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        TransportConfiguration objectTransport = getManager(TransportConfiguration.class).getById(id);
        beforeStoreUpdated(objectTransport, uiTransport);
        ResponseCacheHelper.beforeUpdatedForRestAndSoapTransport(objectTransport, uiTransport, projectId);
        if (uiTransport.getProperties() != null) {
            for (UIProperty entryUIProperty : uiTransport.getProperties()) {
                configurationControllerHelper.setProperty(objectTransport, entryUIProperty, projectUuid);
            }
        }
        if (!uiTransport.defineProperties(objectTransport)) {
            uiTransport.setName(uiTransport.getName() + " [Implementation not deployed]");
        }
        return storeUpdated(objectTransport, uiTransport);
    }

    private Collection<UIInterceptor> getUIInterceptors(TransportConfiguration transport) {
        Collection<UIInterceptor> result = new ArrayList<>();
        List<Interceptor> interceptors = transport.getInterceptors();
        interceptors.sort(Comparators.INTERCEPTOR_COMPARATOR);
        for (Interceptor interceptor : interceptors) {
            try {
                result.add(new UIInterceptor(interceptor));
            } catch (Exception e) {
                LOGGER.error("Cannot instantiate the \"{}\" interceptor. Check that the appropriate interceptor's"
                                + " implementation wad added and interceptor successfully registered.",
                        interceptor.getName(), e);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    @RequestMapping(value = "/transport/getTransportPropertiesByType", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Transport properties by type {{#type}}")
    public List<UIProperty> getTransportPropertiesByType(
            @RequestParam(value = "type") Class<? extends AbstractTransportImpl> type) throws TransportException {
        Map<String, PropertyDescriptor> transportParameters =
                TransportRegistryManager.getInstance().getProperties(type.getName());
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(transportParameters.size());
        for (PropertyDescriptor descriptor : transportParameters.values()) {
            UIProperty uiProperty = new UIProperty(descriptor);
            uiProperties.add(uiProperty);
        }
        return uiProperties;
    }

    @Transactional(readOnly = true)
    @RequestMapping(value = "/transport/getTransportPropertiesForTrigger", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Transport properties by type {{#type}} for trigger")
    public List<UIProperty> getTransportPropertiesForTrigger(
            @RequestParam(value = "type") Class<? extends AbstractTransportImpl> type) throws TransportException {
        Map<String, PropertyDescriptor> transportParameters =
                TransportRegistryManager.getInstance().getProperties(type.getName());
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(transportParameters.size());
        for (PropertyDescriptor propertyDescriptor : transportParameters.values()) {
            if (propertyDescriptor.isForServer()) {
                uiProperties.add(new UIProperty(propertyDescriptor));
            }
        }
        return uiProperties;
    }

    @Override
    protected Class<TransportConfiguration> _getGenericUClass() {
        return TransportConfiguration.class;
    }

    @Override
    protected UITransport _newInstanceTClass(TransportConfiguration object) {
        return new UITransport(object);
    }

    @Override
    protected Storable _getParent(String parentId) {
        return getManager(System.class).getById(parentId);
    }
}
