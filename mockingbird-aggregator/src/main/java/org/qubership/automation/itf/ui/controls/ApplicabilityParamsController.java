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

package org.qubership.automation.itf.ui.controls;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.ApplicabilityParams;
import org.qubership.automation.itf.core.model.jpa.interceptor.TemplateInterceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.TransportConfigurationInterceptor;
import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.holder.ActiveInterceptorHolder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.common.AbstractController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIApplicabilityParams;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

@RestController
public class ApplicabilityParamsController extends AbstractController<UIApplicabilityParams, ApplicabilityParams> {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/custom/applicability_params", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Applicability Params for Interceptor id {{#interceptorId}} "
            + "in the project {{#projectUuid}}")
    public List<UIApplicabilityParams> getAll(@RequestParam(value = "interceptorId") final String interceptorId,
                                              @RequestParam(value = "projectUuid") UUID projectUuid) {
        List<UIApplicabilityParams> result = Lists.newArrayList();
        Interceptor interceptor = getInterceptor(interceptorId);
        if (interceptor != null) {
            for (ApplicabilityParams applicabilityParams : interceptor.getApplicabilityParams()) {
                result.add(new UIApplicabilityParams(applicabilityParams));
            }
        }
        return result;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/custom/applicability_params", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Applicability Params for Interceptor id {{#interceptorId}} "
            + "in the project {{#projectUuid}}")
    public UIApplicabilityParams create(
            @RequestParam(value = "interceptorId") final String interceptorId,
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam(value = "className") Class<? extends Interceptor> type) {
        return super.create(getInterceptorWithType(interceptorId, type));
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/custom/applicability_params", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Applicability Params in the project {{#projectUuid}}")
    public UIResult update(
            @RequestBody List<UIApplicabilityParams> uiApplicabilityParamsList,
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam(value = "className") Class<? extends Interceptor> type) {
        UIResult result = validate(uiApplicabilityParamsList);
        if (!uiApplicabilityParamsList.isEmpty()) {
            Interceptor interceptor
                    = getInterceptorWithType(uiApplicabilityParamsList.get(0).getParent().getId(), type);
            if (result.isSuccess()) {
                if (interceptor != null) {
                    for (UIApplicabilityParams uiApplicabilityParams : uiApplicabilityParamsList) {
                        for (ApplicabilityParams applicabilityParams : interceptor.getApplicabilityParams()) {
                            if (applicabilityParams.getID().toString().equals(uiApplicabilityParams.getId())) {
                                String envId = uiApplicabilityParams.getEnvironment().getId();
                                String systemId = uiApplicabilityParams.getSystem() != null
                                        ? uiApplicabilityParams.getSystem().getId() : StringUtils.EMPTY;
                                applicabilityParams.put(PropertyConstants.Applicability.ENVIRONMENT, envId);
                                applicabilityParams.put(PropertyConstants.Applicability.SYSTEM, systemId);
                                if (interceptor.isActive()) {
                                    ControllerHelper.reactivateInterceptor(ActiveInterceptorHolder.getInstance()
                                                    .getActiveInterceptors()
                                                    .get(interceptor.getParent().getID().toString()),
                                            interceptor, interceptor.getParent().getID().toString());
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/custom/applicability_params", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Applicability Params from project {{#projectUuid}}")
    public void delete(
            @RequestBody Collection<UIApplicabilityParams> uiApplicabilityParamsList,
            @RequestParam(value = "projectUuid") UUID projectUuid,
            @RequestParam(value = "className") Class<? extends Interceptor> type) {
        if (!uiApplicabilityParamsList.isEmpty()) {
            Interceptor interceptor
                    = getInterceptorWithType(uiApplicabilityParamsList.iterator().next().getParent().getId(), type);
            if (interceptor != null) {
                List<ApplicabilityParams> paramsForDelete = Lists.newArrayList();
                for (UIApplicabilityParams uiApplicabilityParams : uiApplicabilityParamsList) {
                    for (ApplicabilityParams applicabilityParams : interceptor.getApplicabilityParams()) {
                        if (applicabilityParams.getID().toString().equals(uiApplicabilityParams.getId())) {
                            paramsForDelete.add(applicabilityParams);
                        }
                    }
                }
                if (!paramsForDelete.isEmpty()) {
                    interceptor.getApplicabilityParams().removeAll(paramsForDelete);
                }
                paramsForDelete.forEach(AbstractStorable::remove);
                if (interceptor.isActive()) {
                    ControllerHelper.reactivateInterceptor(ActiveInterceptorHolder.getInstance().getActiveInterceptors()
                                    .get(interceptor.getParent().getID().toString()), interceptor,
                            interceptor.getParent().getID().toString());
                }
            }
        }
    }

    @Override
    protected Class<ApplicabilityParams> _getGenericUClass() {
        return ApplicabilityParams.class;
    }

    @Override
    protected UIApplicabilityParams _newInstanceTClass(ApplicabilityParams object) {
        return new UIApplicabilityParams(object);
    }

    @Override
    protected Interceptor _getParent(String parentId) {
        return getInterceptor(parentId);
    }

    private Interceptor getInterceptor(String interceptorId) {
        Interceptor interceptor = CoreObjectManager.getInstance().getManager(TransportConfigurationInterceptor.class)
                .getById(interceptorId);
        if (interceptor == null) {
            interceptor = CoreObjectManager.getInstance().getManager(TemplateInterceptor.class).getById(interceptorId);
        }
        return interceptor;
    }

    private Interceptor getInterceptorWithType(String interceptorId, Class<? extends Interceptor> type) {
        return CoreObjectManager.getInstance().getManager(type).getById(interceptorId);
    }

    private UIResult validate(List<UIApplicabilityParams> uiApplicabilityParams) {
        UIResult result = fieldsAreNotEmpty(uiApplicabilityParams);
        if (result.isSuccess()) {
            result = applicableParametersAreNotDuplicated(uiApplicabilityParams);
        }
        return result;
    }

    private UIResult fieldsAreNotEmpty(List<UIApplicabilityParams> uiApplicabilityParams) {
        for (UIApplicabilityParams applicabilityParams : uiApplicabilityParams) {
            if (applicabilityParams.getEnvironment() == null) {
                return new UIResult(false, "Environment can't be empty. Please fill parameters.");
            }
        }
        return new UIResult();
    }

    private UIResult applicableParametersAreNotDuplicated(List<UIApplicabilityParams> uiApplicabilityParams) {
        for (int i = 0; i < uiApplicabilityParams.size(); i++) {
            for (int g = i + 1; g < uiApplicabilityParams.size(); g++) {
                String sourceEnvId = uiApplicabilityParams.get(i).getEnvironment().getId();
                String comparingEnvId = uiApplicabilityParams.get(g).getEnvironment().getId();
                String sourceSystemId = uiApplicabilityParams.get(i).getSystem() != null
                        ? uiApplicabilityParams.get(i).getSystem().getId() : StringUtils.EMPTY;
                String comparingSystemId = uiApplicabilityParams.get(g).getSystem() != null
                        ? uiApplicabilityParams.get(g).getSystem().getId() : StringUtils.EMPTY;
                if (sourceEnvId.equals(comparingEnvId) && sourceSystemId.equals(comparingSystemId)) {
                    return new UIResult(false, String.format("Applicability Parameters (Environment = %s, "
                                    + "System = %s) already exist in interceptor.",
                            uiApplicabilityParams.get(i).getEnvironment().getName(),
                            uiApplicabilityParams.get(i).getSystem() != null
                                    ? uiApplicabilityParams.get(i).getSystem().getName() : "empty")
                    );
                }
            }
        }
        return new UIResult();
    }
}
