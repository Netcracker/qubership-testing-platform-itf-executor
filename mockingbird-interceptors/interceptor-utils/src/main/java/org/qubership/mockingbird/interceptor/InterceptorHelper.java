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

package org.qubership.mockingbird.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.interceptor.TransportInterceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.ApplicabilityParams;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.qubership.automation.itf.core.util.provider.InterceptorProvider;

public class InterceptorHelper {

    public static String validate(Interceptor interceptor, TransportInterceptor transportInterceptor) {
        String error = validateApplicabilityParametersForActivation(interceptor, transportInterceptor);
        if (StringUtils.isNotEmpty(error)) return error;
        else {
            List<InterceptorParams> interceptorParams = interceptor.getInterceptorParams();
            if (interceptorParams.isEmpty()) return "Interceptor can't be activated, because parameters are empty.";
            for (InterceptorParams params : interceptorParams) {
                if (params.isEmpty()) return "Interceptor can't be activated, because parameters are empty.";
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (StringUtils.isEmpty(param.getValue()) && !isOptional(transportInterceptor, param.getKey()))
                        return String.format("Interceptor can't be activated, because '%s' parameter is empty.",
                                param.getKey());
                }
            }
        }
        return StringUtils.EMPTY;
    }

    public static String validateApplicabilityParametersForActivation(Interceptor interceptor,
                                                                      TransportInterceptor transportInterceptor) {
        List<ApplicabilityParams> applicabilityParams = interceptor.getApplicabilityParams();
        if (applicabilityParams.isEmpty()) return StringUtils.EMPTY;
        for (ApplicabilityParams params : applicabilityParams) {
            if (StringUtils.isEmpty(params.get(PropertyConstants.Applicability.ENVIRONMENT)))
                return "Interceptor can not be activated, because there is an empty environment "
                        + "in applicability parameters.";
        }
        InterceptorProvider interceptorProvider = (InterceptorProvider) interceptor.getParent();
        List<Interceptor> activeParamsInterceptors = getActiveParamInterceptors(
                interceptorProvider.getInterceptors(), interceptor);
        if (activeParamsInterceptors.isEmpty()) return StringUtils.EMPTY;
        for (Interceptor activeParamsInterceptor : activeParamsInterceptors) {
            for (ApplicabilityParams comparableParams : activeParamsInterceptor.getApplicabilityParams()) {
                for (ApplicabilityParams currentParams : applicabilityParams) {
                    if (currentParams.get(PropertyConstants.Applicability.ENVIRONMENT).equals(
                            comparableParams.get(PropertyConstants.Applicability.ENVIRONMENT))
                            && currentParams.get(PropertyConstants.Applicability.SYSTEM).equals(
                            comparableParams.get(PropertyConstants.Applicability.SYSTEM))) {
                        return String.format("Interceptor can't be activated, because the %s interceptor with the "
                                + "same applicability parameters is active.", activeParamsInterceptor.getName());
                    }
                }
            }
        }
        return StringUtils.EMPTY;
    }

    public static List<Interceptor> getActiveParamInterceptors(Collection<Interceptor> interceptors,
                                                               Interceptor currentInterceptor) {
        List<Interceptor> result = new ArrayList<>();
        for (Interceptor interceptor : interceptors) {
            if (interceptor.isActive() && !interceptor.getID().toString().equals(currentInterceptor.getID().toString())
                    && interceptor.getTypeName().equals(currentInterceptor.getTypeName())) {
                result.add(interceptor);
            }
        }
        return result;
    }

    private static boolean isOptional(TransportInterceptor transportInterceptor, String parameterName) {
        if (transportInterceptor instanceof ContentInterceptor) {
            for (InterceptorPropertyDescriptor descriptor :
                    ((ContentInterceptor) transportInterceptor).getParameters()) {
                if (descriptor.getName().equals(parameterName)) {
                    return descriptor.isOptional();
                }
            }
        }
        return true;
    }
}
