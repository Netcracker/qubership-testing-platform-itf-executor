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

package org.qubership.automation.itf.configuration.utils;

import org.json.simple.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONContextUtilsTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static JsonContext convertJsonObject(String json) throws Exception {
        ObjectNode jsonObj = ObjectNode.class.cast(MAPPER.readTree(json));
        return JSONContextUtils.convert(jsonObj, MAPPER);
    }

    @Test
    public void equalParametersUnderDifferentParentsArePreserved() throws Exception {
        String input = "{\"Typ\":\"\",\"Internet_Access_Params\":{\"SN\":\"\",\"Typ\":\"\"},\"Params\":{\"Typ\":\"\"," +
                "\"UNI_keyB_1\":\"UNI_1_35369354\"}}";
        JsonContext result = convertJsonObject(input);
        Assert.assertNotNull(result.get("Typ"));
        Assert.assertNotNull(result.get("Params.Typ"));
        Assert.assertNotNull(result.get("Internet_Access_Params.Typ"));
    }

    @Test
    public void parametersWithTheSameNameAreInPlace() throws Exception {
        String input = "{\"Typ\":\"1\",\"Internet_Access_Params\":{\"SN\":\"\",\"Typ\":\"2\"}," +
                "\"Params\":{\"Typ\":\"3\",\"UNI_keyB_1\":\"UNI_1_35369354\"}}";
        JsonContext result = convertJsonObject(input);
        Assert.assertEquals(result.get("Typ"), "1");
        Assert.assertEquals(result.get("Params.Typ"), "3");
        Assert.assertEquals(result.get("Internet_Access_Params.Typ"), "2");
    }

    @Test
    public void paramsWithIndexesAreConvertedToArray() throws Exception {
        String input = "{\"Typ[2]\":\"2\",\"Typ[1]\":\"1\",\"letters\":{\"ab[1]\":\"a\",\"ab[2]\":\"b\"}}";
        JsonContext result = convertJsonObject(input);
        Object typ = result.get("Typ");
        Object ab = result.get("letters.ab");
        Assert.assertTrue(typ instanceof JSONArray);
        Assert.assertTrue(ab instanceof JSONArray);
        JSONArray typArray = (JSONArray) typ;
        JSONArray abArray = (JSONArray) ab;
        Assert.assertEquals(typArray.get(1), "1");
        Assert.assertEquals(typArray.get(2), "2");
        Assert.assertEquals(abArray.get(1), "a");
        Assert.assertEquals(abArray.get(2), "b");
    }

}
