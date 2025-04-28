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

package org.qubership.automation.itf.ui.controls.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.holder.ActiveInterceptorHolder;
import org.qubership.automation.itf.core.util.holder.InterceptorHolder;
import org.qubership.automation.itf.core.util.provider.InterceptorProvider;
import org.qubership.automation.itf.ui.controls.ApplicabilityParamsController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIListImpl;
import org.qubership.automation.itf.ui.messages.exception.UIException;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.template.UITemplate;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIInterceptor;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIInterceptorChain;
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

@RestController
public class InterceptorController extends ControllerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterceptorController.class);

    private final ApplicabilityParamsController applicabilityParamsController;

    @Autowired
    public InterceptorController(ApplicabilityParamsController applicabilityParamsController) {
        this.applicabilityParamsController = applicabilityParamsController;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/interceptors/bytransport", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Interceptors by transport name {{#transportName}} and interceptor group " +
            "{{#interceptorGroup}} in the project {{#projectUuid}}")
    public UIInterceptorChain getInterceptorsByTransportName(
            @RequestParam(value = "transportName") final String transportName,
            @RequestParam(value = "interceptorGroup") final String interceptorGroup,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        UIInterceptorChain result = new UIInterceptorChain();
        Map<String, Class<?>> interceptors = InterceptorHolder.getInstance().getInterceptors().get(transportName);
        if (interceptors != null) {
            for (Map.Entry<String, Class<?>> interceptor : interceptors.entrySet()) {
                if (IsInGroup(interceptor.getValue(), interceptorGroup)) {
                    UIInterceptor uiInterceptor = new UIInterceptor();
                    uiInterceptor.setName(interceptor.getKey());
                    uiInterceptor.setClassName(interceptor.getValue().getName());
                    uiInterceptor.setTransportName(transportName);
                    uiInterceptor.setInterceptorGroup(interceptorGroup);
                    result.getInterceptorChain().add(uiInterceptor);
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/interceptors/transport_by_provider", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Transport types with Interceptors by interceptor provider id " +
            "{{#interceptorProviderId}} in the project {{#projectUuid}}")
    public UIListImpl getTransportTypesWithInterceptors(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        UIListImpl<UIObject> result = new UIListImpl<>();
        List<Interceptor> interceptors = getInterceptorProvider(interceptorProviderId).getInterceptors();
        if (interceptors != null) {
            Set<String> transportTypesSet = new HashSet<>();
            for (Interceptor interceptor : interceptors) {
                transportTypesSet.add(interceptor.getTransportName());
            }
            for (String transportType : transportTypesSet) {
                UIObject obj = new UIObject();
                obj.setClassName(transportType);
                result.addObject(obj);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/interceptors/by_provider_transport", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Interceptors by interceptor provider id {{#interceptorProviderId}} and Transport " +
            "{{#transportName}} name in the project {{#projectUuid}}")
    public UIInterceptorChain getInterceptorsByProviderAndTransport(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestParam(value = "transportName", defaultValue = "0") String transportName,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        UIInterceptorChain uiInterceptorChain = new UIInterceptorChain();
        InterceptorProvider interceptorProvider = getInterceptorProvider(interceptorProviderId);
        List<Interceptor> interceptors = interceptorProvider.getInterceptors();
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                if (transportName.equals(interceptor.getTransportName())) {
                    try {
                        uiInterceptorChain.getInterceptorChain().add(new UIInterceptor(interceptor));
                        uiInterceptorChain.setParentVersion(interceptorProvider.getVersion().toString());
                    } catch (Exception e) {
                        LOGGER.error("Cannot instantiate the \"{}\" interceptor. Check if the appropriate "
                                + "interceptor's implementation was added and interceptor was successfully registered"
                                + ".\nStacktrace: ", interceptor.getName(), e);
                    }
                }
            }
        }
        return uiInterceptorChain;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'CREATE')")
    @RequestMapping(value = "/interceptors", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Interceptors by interceptor provider id {{#interceptorProviderId}} in the " +
            "project {{#projectUuid}}")
    public List<UIInterceptor> addInterceptors(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestBody UIInterceptorChain uiInterceptorChain,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        InterceptorProvider interceptorProvider = getInterceptorProvider(interceptorProviderId);
        int maxOrder = getInterceptorsMaxOrder(interceptorProvider.getInterceptors());
        for (UIInterceptor uiInterceptor : uiInterceptorChain.getInterceptorChain()) {
            Interceptor interceptor = createInterceptorByProvider(interceptorProvider);
            if (interceptorProvider instanceof Template) {
                uiInterceptor.setParent(new UITemplate((Template) interceptorProvider));
            }
            if (interceptorProvider instanceof TransportConfiguration) {
                uiInterceptor.setParent(new UITransport((TransportConfiguration) interceptorProvider));
            }
            fillInterceptorParams(interceptor, interceptorProvider, uiInterceptor);
            addInterceptorConfiguration(interceptor, uiInterceptor.getTransportName(),
                    interceptorParamsToMap(uiInterceptor));
            interceptor.setOrder(++maxOrder);
            interceptorProvider.getInterceptors().add(interceptor);
            interceptor.store();
            uiInterceptor.setId(interceptor.getID().toString());
        }
        return uiInterceptorChain.getInterceptorChain();
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/interceptor", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Save Interceptor with id {{#uiInterceptor.id}} and Interceptor provider id " +
            "{{#interceptorProviderId}} in the project {{#projectUuid}}")
    public UIResult saveInterceptor(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestBody UIInterceptor uiInterceptor,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Interceptor interceptor = findInterceptorByIdAndProvider(uiInterceptor.getId(),
                getInterceptorProvider(interceptorProviderId));
        throwExceptionIfNull(interceptor, uiInterceptor.getName(), uiInterceptor.getId(), Interceptor.class,
                "get Interceptor by id");
        UIResult result = uiInterceptor.validate();
        if (result.isSuccess()) {
            result = applicabilityParamsController.update(uiInterceptor.getApplicabilityParams(), projectUuid,
                    interceptor.getClass());
            if (result.isSuccess()) {
                interceptor.setName(uiInterceptor.getName());
                List<InterceptorParams> paramsList = interceptor.getInterceptorParams();
                InterceptorParams params = paramsList.isEmpty() ? new InterceptorParams() : paramsList.get(0);
                params.update(interceptorParamsToMap(uiInterceptor));
                if (paramsList.isEmpty()) {
                    paramsList.add(params);
                }
                if (interceptor.isActive()) {
                    reactivateInterceptor(ActiveInterceptorHolder.getInstance().getActiveInterceptors()
                            .get(interceptorProviderId), interceptor, interceptorProviderId);
                }
            }
        }
        interceptor.store();
        interceptor.flush();
        result.getData().put("parentVersion", interceptor.getParent().getVersion().toString());
        return result;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/interceptor/state", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Change state of the Interceptor with id {{#uiInterceptor.id}} and Interceptor " +
            "provider id {{#interceptorProviderId}} in the project {{#projectUuid}}")
    public UIResult changeState(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestBody UIInterceptor uiInterceptor,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Interceptor interceptor = findInterceptorByIdAndProvider(uiInterceptor.getId(),
                getInterceptorProvider(interceptorProviderId));
        throwExceptionIfNull(interceptor, uiInterceptor.getName(), uiInterceptor.getId(), Interceptor.class,
                "get Interceptor by id");
        if (uiInterceptor.isActive()) {
            try {
                String validationResult = interceptor.validate();
                if (StringUtils.isNotEmpty(validationResult)) {
                    return new UIResult(false, validationResult);
                }
            } catch (Exception e) {
                String error = "Error while validation of the interceptor's state changing.";
                LOGGER.error(error, e);
                throw new UIException(error);
            }
        }
        interceptor.setActive(uiInterceptor.isActive());
        Map<String, Interceptor> objectInterceptorMap =
                ActiveInterceptorHolder.getInstance().getActiveInterceptors().get(interceptorProviderId);
        if (!uiInterceptor.isActive()) {
            objectInterceptorMap.remove(interceptor.getID().toString());
        } else {
            reactivateInterceptor(objectInterceptorMap, interceptor, interceptorProviderId);
        }
        return new UIResult();
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/interceptor/order", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Change order of Interceptors under Interceptor provider id {{#interceptorProviderId}}" +
            " in the project {{#projectUuid}}")
    public void changeOrder(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestBody UIInterceptorChain uiInterceptors,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        InterceptorProvider interceptorProvider = getInterceptorProvider(interceptorProviderId);
        for (Interceptor interceptor : interceptorProvider.getInterceptors()) {
            for (UIInterceptor uiInterceptor : uiInterceptors.getInterceptorChain()) {
                if (Objects.equals(interceptor.getID().toString(), uiInterceptor.getId())) {
                    interceptor.setOrder(uiInterceptor.getOrder());
                    break;
                }
            }
        }
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'DELETE')")
    @RequestMapping(value = "/interceptors", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Interceptors under Interceptor provider id {{#interceptorProviderId}} in the " +
            "project {{#projectUuid}}")
    public UIInterceptorChain deleteInterceptor(
            @RequestParam(value = "interceptorProviderId", defaultValue = "0") String interceptorProviderId,
            @RequestBody UIInterceptorChain uiInterceptors,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws TransportException {
        InterceptorProvider interceptorProvider = getInterceptorProvider(interceptorProviderId);
        Collection<Interceptor> listForDeletedInterceptors = new ArrayList<>();
        for (Interceptor interceptor : interceptorProvider.getInterceptors()) {
            for (UIInterceptor uiInterceptor : uiInterceptors.getInterceptorChain()) {
                if (Objects.equals(interceptor.getID().toString(), uiInterceptor.getId())) {
                    listForDeletedInterceptors.add(interceptor);
                    break;
                }
            }
        }
        Map<String, Interceptor> objectInterceptorMap =
                ActiveInterceptorHolder.getInstance().getActiveInterceptors().get(interceptorProviderId);
        if (objectInterceptorMap != null) {
            for (Interceptor deletedInterceptor : listForDeletedInterceptors) {
                objectInterceptorMap.remove(deletedInterceptor.getID().toString());
            }
        }
        interceptorProvider.getInterceptors().removeAll(listForDeletedInterceptors);
        listForDeletedInterceptors.forEach(Interceptor::remove);
        UIInterceptorChain result = new UIInterceptorChain();
        for (Interceptor interceptor : interceptorProvider.getInterceptors()) {
            UIInterceptor uiInterceptor = new UIInterceptor(interceptor);
            result.getInterceptorChain().add(uiInterceptor);
        }
        interceptorProvider.store();
        interceptorProvider.flush();
        result.setParentVersion(interceptorProvider.getVersion().toString());
        return result;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/interceptors/refresh_interceptor_holder", method = RequestMethod.GET)
    @AuditAction(auditAction = "Refresh Interceptors Holder, project {{#projectUuid}}")
    public void refreshInterceptorHolder(@RequestParam(value = "projectUuid") UUID projectUuid) {
        InterceptorHolder.getInstance().clearInterceptorHolder();
        InterceptorHolder.getInstance().fillInterceptorHolder();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).INTERCEPTOR.getName(),"
            + "#projectUuid, 'UPDATE')")
    @RequestMapping(value = "/interceptors/refresh_active_interceptor_holder", method = RequestMethod.GET)
    @AuditAction(auditAction = "Refresh active Interceptors Holder, project {{#projectUuid}}")
    public void refreshActiveInterceptorHolder(@RequestParam(value = "projectUuid") UUID projectUuid) {
        //TODO: Should be changed for multi-tenancy, because below code will clear all,
        // but fill only from the current cluster.
        ActiveInterceptorHolder.getInstance().clearActiveInterceptorHolder();
        ActiveInterceptorHolder.getInstance().fillActiveInterceptorHolder();
    }
}
