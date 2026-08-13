/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = {"classpath*:*core-test-context.xml"})
public class KeysRegeneratorTest {

    /*
        Disabled by KAG, because, it seems, the class should be in another module:
            - VelocityTemplateEngine will be actually used to process keys, but it's in the corresponding module
            (not here), which is compiled later.
     */
    @Disabled
    @Test
    public void testKeyGeneration() throws Exception {
        InstanceContext context = new InstanceContext();
        String key1 = "key1";
        String key2 = "key2";
        context.put(key1, "1234");
        String value = "abcd";
        String value2 = "value2";
        Map<String, String> keysToRegenerate = new HashMap<>();
        keysToRegenerate.put(key1, value);
        keysToRegenerate.put(key2, value2);
        KeysRegenerator.getInstance().regenerateKeys(context, keysToRegenerate);
        Assertions.assertEquals(value, context.get(key1));
        Assertions.assertEquals(value2, context.get(key2));
    }
}
