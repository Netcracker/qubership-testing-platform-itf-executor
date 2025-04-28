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

package org.qubership.automation.itf.core.instance.step.impl.chain;

import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;

public class LoadTemplate extends AbstractChainUnit {

    @Override
    public boolean isAcceptable(PropertyDescriptor descriptor) {
        return descriptor.loadTemplate();
    }

    @Override
    public void accept(PropertyDescriptor descriptor, Message message, InstanceContext context) {
        ConnectionProperties connectionProperties = (ConnectionProperties) message.getConnectionProperties();
        String templateId = connectionProperties.obtain(descriptor.getShortName());
        if (templateId == null) {
            return;
        }
        Template template = TemplateHelper.getById(templateId);
        if (template != null) {
            message.getConnectionProperties().put(descriptor.getShortName(),
                    TemplateProcessor.getInstance().process(template, template.getText(), context,
                            connectionProperties));
        }
    }
}
