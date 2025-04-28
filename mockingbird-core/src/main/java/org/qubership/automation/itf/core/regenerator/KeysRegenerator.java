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

package org.qubership.automation.itf.core.regenerator;

import java.util.Map;

import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;

public class KeysRegenerator {

    private static final KeysRegenerator INSTANCE = new KeysRegenerator();

    public static KeysRegenerator getInstance() {
        return INSTANCE;
    }

    public void regenerateKeys(InstanceContext context, Map<String, String> keysToRegenerate) throws Exception {
        if (keysToRegenerate == null || context == null) {
            return;
        }
        for (Map.Entry<String, String> entry : keysToRegenerate.entrySet()) {
            String result = TemplateEngineFactory.process(null, entry.getValue(), context);
            context.put(entry.getKey(), result);
        }
    }
}
