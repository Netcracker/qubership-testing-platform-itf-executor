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

package org.qubership.automation.itf.executor.context;

import static org.junit.Assert.assertEquals;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonStorable;

public class ContextTest {

    public static final String JSON_STRING_1 = "{\n" +
            "\t\"simpleObject\": \"value\",\n" +
            "\t\"simpleObjectState\": \"valueState\",\n" +
            "\t\"simpleMap\": {\n" +
            "\t\t\"mapEntry1\": \"mapValue1\",\n" +
            "\t\t\"mapEntry2\": \"mapValue2\",\n" +
            "\t\t\"mapEntryState\": \"mapValueState\"\n" +
            "\t},\n" +
            "\t\"simpleList\": [\"value1\",\n" +
            "\t\"value2\",\n" +
            "\t\"valueState\"\n" +
            "\t],\n" +
            "\t\"listOverride\": [\"value1\",\n" +
            "\t\"value2\",\n" +
            "\t\"value3\"\n" +
            "\t],\n" +
            "\t\"mapOfMap\": {\n" +
            "\t\t\"mapEntry1\": {\n" +
            "\t\t\t\"mapMapEntry1\": \"mapMapValue1\",\n" +
            "\t\t\t\"mapMapEntry2\": \"mapMapValue2\",\n" +
            "\t\t\t\"mapMapEntryState\": \"mapMapValueState\"\n" +
            "\t\t}, \n" +
            "\t\t\"mapEntryState\": {\n" +
            "\t\t\t\"mapMapEntry1\": \"mapMapValue1\",\n" +
            "\t\t\t\"mapMapEntry2\": \"mapMapValue2\",\n" +
            "\t\t\t\"mapMapEntryState\": \"mapMapValueState\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";
    public static final String JSON_STRING_2 = "{\n" +
            "\t\"simpleObject\": \"valueNew\",\n" +
            "\t\"simpleMap\": {\n" +
            "\t\t\"mapEntry1\": \"mapValue1New\",\n" +
            "\t\t\"mapEntry2\": \"mapValue2New\",\n" +
            "\t},\n" +
            "\t\"simpleList\": [\"value1New\",\n" +
            "\t\"value2New\"\n" +
            "\t],\n" +
            "\t\"listOverride\": [\"value1New\",\n" +
            "\t\"value2New\",\n" +
            "\t\"value3New\",\n" +
            "\t\"value4Add\"\n" +
            "\t],\n" +
            "\t\"mapOfMap\": {\n" +
            "\t\t\"mapEntry1\": {\n" +
            "\t\t\t\"mapMapEntry1\": \"mapMapValue1New\",\n" +
            "\t\t\t\"mapMapEntry2\": \"mapMapValue2New\",\n" +
            "\t\t}\n" +
            "\t}, \n" +
            "\t\"simpleMapNew\": {\n" +
            "\t\t\"mapEntry1\": \"mapValue1\",\n" +
            "\t\t\"mapEntry2\": \"mapValue2\",\n" +
            "\t\t\"mapEntryState\": \"mapValueState\"\n" +
            "\t},\n" +
            "}";

    @Test
    public void testMerge() throws ParseException, IllegalAccessException, InstantiationException {
        JsonContext toContext = JsonContext.fromJson(JSON_STRING_1, JsonStorable.class);
        JsonContext fromContext = JsonContext.fromJson(JSON_STRING_2, JsonStorable.class);
        toContext.merge(fromContext);
        assertEquals(fromContext.get("simpleObject"), toContext.get("simpleObject"));
        assertEquals("valueNew", toContext.get("simpleObject"));
    }

    @Test
    public void testPutGet() {
        JsonContext sp = new JsonContext();
        sp.setCollectHistory(true);
        sp.create("aaa");
        sp.create("aaa.bbb");
        sp.create("aaa.bbb.ccc", true);
        sp.create("aaa.bbb.ccc[0]");
        sp.create("aaa.bbb.ccc[1]");
        sp.put("aaa.bbb.ccc[0].ddd", "qwe");
        sp.put("aaa.bbb.ccc[0].eee", "ewq");
        sp.put("aaa.bbb.ccc[1].ddd", "asd");
        sp.put("aaa.bbb.ccc[1].eee", "dsa");
        System.out.println(sp.get("aaa.bbb.ccc[0].ddd"));
        System.out.println(sp.get("aaa.bbb"));
        System.out.println(sp.get("aaa.ccc"));
        System.out.println("History: " + sp.getHistory());
    }
}
