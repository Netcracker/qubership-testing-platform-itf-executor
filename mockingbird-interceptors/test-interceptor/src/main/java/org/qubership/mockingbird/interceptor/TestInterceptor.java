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

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named(value = "Test Interceptor")
public class TestInterceptor extends ContentInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestInterceptor.class);
    private final String LOG_MESSAGE = "Log Message";
    private final String LOG_MESSAGE_DESCRIPTION = "Message for logging";

    public TestInterceptor(Interceptor interceptor) {
        super(interceptor);
    }

    public TestInterceptor() {
    }

    @Override
    public Message apply(Message data) throws Exception {
        InterceptorParams params = interceptor.getParameters();
        LOGGER.info("Message from interceptor: {}\nTest Interceptor has been successfully applied to the message: {}",
                (params == null) ? null : params.get(LOG_MESSAGE),
                data.getText());
        return data;
    }

    @Override
    public String validate() {
        return StringUtils.EMPTY;
    }

    @Override
    public List<InterceptorPropertyDescriptor> getParameters() {
        List<InterceptorPropertyDescriptor> parameters = new ArrayList<>();
        InterceptorParams params = interceptor.getParameters();
        InterceptorPropertyDescriptor testParameter = new InterceptorPropertyDescriptor(LOG_MESSAGE,
                LOG_MESSAGE,
                LOG_MESSAGE_DESCRIPTION,
                InterceptorConstants.TEXTFIELD,
                (params == null) ? null : params.get(LOG_MESSAGE),
                true);
        parameters.add(testParameter);
        return parameters;
    }
}
