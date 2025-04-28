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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;

public class TemplateProcessor {

    private static final TemplateProcessor PROCESSOR = new TemplateProcessor();
    private static final TemplateEngine TEMPLATE_ENGINE = TemplateEngineFactory.get();
    private static final String US = "us";

    public static TemplateProcessor getInstance() {
        return PROCESSOR;
    }

    public String process(@Nullable Template template, @Nonnull String message, @Nonnull InstanceContext context,
                          @Nonnull ConnectionProperties connectionProperties) {
        Object obj = connectionProperties.obtain("properties");
        if (obj == null || obj instanceof String) {
            return message;
        }
        Map<String, String> userSettings = (Map<String, String>) obj;
        if (userSettings.isEmpty()) {
            return message;
        }
        final JsonContext jsonContext = new JsonContext();
        jsonContext.putAll(userSettings);
        context.put(US, jsonContext);
        return TEMPLATE_ENGINE.process(template, message, context);
    }
}
