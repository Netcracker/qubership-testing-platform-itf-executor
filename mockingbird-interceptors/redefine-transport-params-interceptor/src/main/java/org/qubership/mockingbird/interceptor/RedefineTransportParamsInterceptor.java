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
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.interceptor.ParametersInterceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.annotation.ApplyToTransport;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplyToTransport(
        transports = {
                "org.qubership.automation.itf.transport.jms.outbound.JMSOutboundTransport"
        })
@Named(value = "Redefine Transport Parameters")
public class RedefineTransportParamsInterceptor extends ParametersInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedefineTransportParamsInterceptor.class);

    public RedefineTransportParamsInterceptor() {
    }

    public RedefineTransportParamsInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    @Override
    public Message apply(Message data) throws Exception {
        LOGGER.info("Redefine parameters started...");
        InterceptorParams parameters = interceptor.getParameters();
        if (parameters != null) {
            for (Map.Entry<String, String> paramEntry : parameters.entrySet()) {
                String paramName = paramEntry.getKey();
                String redefinedValue = paramEntry.getValue();
                if (StringUtils.isNotEmpty(redefinedValue)) {
                    LOGGER.info("Redefine parameter '{}' from '{}' to '{}'", paramName,
                            data.getConnectionProperties().get(paramName), redefinedValue);
                    data.getConnectionProperties().put(paramName, redefinedValue);
                }
            }
        }
        LOGGER.info("Redefine parameters completed successfully.");
        return data;
    }

    @Override
    public String validate() {
        return InterceptorHelper.validateApplicabilityParametersForActivation(interceptor, this);
    }

    @Override
    public List<InterceptorPropertyDescriptor> getParameters(List<PropertyDescriptor> redefinedProperties) {
        List<InterceptorPropertyDescriptor> result = new ArrayList<>();
        InterceptorParams params = interceptor.getParameters();
        for (PropertyDescriptor propertyDescriptor : redefinedProperties) {
            InterceptorPropertyDescriptor interceptorPropertyDescriptor;
            if (propertyDescriptor.getOptions() != null) {
                interceptorPropertyDescriptor = new InterceptorPropertyDescriptor(propertyDescriptor.getShortName(),
                        propertyDescriptor.getLongName(),
                        propertyDescriptor.getDescription(),
                        InterceptorConstants.LIST,
                        propertyDescriptor.getOptions(),
                        (params == null) ? null : params.get(propertyDescriptor.getShortName()),
                        true);
            } else {
                interceptorPropertyDescriptor = new InterceptorPropertyDescriptor(propertyDescriptor.getShortName(),
                        propertyDescriptor.getLongName(),
                        propertyDescriptor.getDescription(),
                        InterceptorConstants.TEXTFIELD,
                        (params == null) ? null : params.get(propertyDescriptor.getShortName()),
                        true);
            }
            result.add(interceptorPropertyDescriptor);
        }
        return result;
    }
}
