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

package org.qubership.automation.itf.core.template.velocity.directives;

import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.template.velocity.VelocityTemplateEngine;

public class JsonPathDirectiveTest {

    private static final TcContext TC_CONTEXT = mock(TcContext.class);
    private static final Storable STORABLE = mock(Storable.class);
    private static VelocityTemplateEngine engine;
    private String json = "{\n" + "\t'id':123,\n" + "\t'name':'JsonName'\n" + "}";

    @BeforeClass
    public static void prepareTemplate() {
        engine = new VelocityTemplateEngine();
    }

    @Test
    public void testJsonPathDirectiveReturnsJsonValue() {
        String source = "#set($json = \"" + json + "\")\n" + "#json_path($json, \"$.id\")";
        String process = engine.process(STORABLE, source, TC_CONTEXT);
        assertEquals("123", process);
    }

    @Test
    public void testJsonPathDirectiveReturnsTwoValues() {
        String source = "#set($json = \"" + json + "\")\n" + "#json_path($json, \"$.id\", \"$.id\")";
        assertEquals("123123", engine.process(STORABLE, source, TC_CONTEXT));
    }
}
