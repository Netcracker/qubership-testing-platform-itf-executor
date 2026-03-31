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

package org.qubership.automation.itf.executor.context;

import static org.junit.Assert.assertEquals;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.context.JsonStorable;

public class ContextTest {

    public static final String JSON_STRING_1 = """
            {
            	"simpleObject": "value",
            	"simpleObjectState": "valueState",
            	"simpleMap": {
            		"mapEntry1": "mapValue1",
            		"mapEntry2": "mapValue2",
            		"mapEntryState": "mapValueState"
            	},
            	"simpleList": ["value1",
            	"value2",
            	"valueState"
            	],
            	"listOverride": ["value1",
            	"value2",
            	"value3"
            	],
            	"mapOfMap": {
            		"mapEntry1": {
            			"mapMapEntry1": "mapMapValue1",
            			"mapMapEntry2": "mapMapValue2",
            			"mapMapEntryState": "mapMapValueState"
            		},\s
            		"mapEntryState": {
            			"mapMapEntry1": "mapMapValue1",
            			"mapMapEntry2": "mapMapValue2",
            			"mapMapEntryState": "mapMapValueState"
            		}
            	}
            }\
            """;
    public static final String JSON_STRING_2 = """
            {
            	"simpleObject": "valueNew",
            	"simpleMap": {
            		"mapEntry1": "mapValue1New",
            		"mapEntry2": "mapValue2New",
            	},
            	"simpleList": ["value1New",
            	"value2New"
            	],
            	"listOverride": ["value1New",
            	"value2New",
            	"value3New",
            	"value4Add"
            	],
            	"mapOfMap": {
            		"mapEntry1": {
            			"mapMapEntry1": "mapMapValue1New",
            			"mapMapEntry2": "mapMapValue2New",
            		}
            	},\s
            	"simpleMapNew": {
            		"mapEntry1": "mapValue1",
            		"mapEntry2": "mapValue2",
            		"mapEntryState": "mapValueState"
            	},
            }\
            """;

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
