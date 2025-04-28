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

package org.qubership.automation.itf.integration.atp.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.qubership.automation.itf.integration.atp.model.ContextEntity;
import org.qubership.automation.itf.integration.atp.model.DataSetEntity;
import org.qubership.automation.itf.integration.atp.model.DataSetItem;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestRunInfoTest {

    private final static String SERVED_FILES_DIR = "src/test/resources/";

    @Test
    public void parseFlatJsonContextToJsonContext() throws IOException, ParseException {

        TestRunInfo runInfo = new TestRunInfo();

        String filename = sanitizePathTraversal("src/test/resources/flatContextToParse.txt");
        String flatContextToParse = new String(Files.readAllBytes(Paths.get(SERVED_FILES_DIR + filename)));
        JSONObject flatContext = (JSONObject) new JSONParser().parse(flatContextToParse);

        filename = sanitizePathTraversal("src/test/resources/erJsonContext.txt");
        String erJsonContext = new String(Files.readAllBytes(Paths.get(SERVED_FILES_DIR + filename)));
        JSONObject expectedContext = (JSONObject) new JSONParser().parse(erJsonContext);

        JSONObject actualContext = new JSONObject();
        runInfo.performContext(flatContext, actualContext);
        org.junit.Assert.assertEquals(expectedContext, actualContext);
    }

    @Test
    public void parseFlatJsonContextToJsonContext_EmptyGroupOrArray_ShouldBeTheSame() throws IOException,
            ParseException {

        TestRunInfo runInfo = new TestRunInfo();

        String filename = sanitizePathTraversal("src/test/resources/flatContextToParse_emptyGroupOrArray.txt");
        String flatContextToParse = new String(Files.readAllBytes(Paths.get(SERVED_FILES_DIR + filename)));
        JSONObject flatContext = (JSONObject) new JSONParser().parse(flatContextToParse);

        filename = sanitizePathTraversal("src/test/resources/erJsonContext_emptyGroupOrArray.txt");
        String erJsonContext = new String(Files.readAllBytes(Paths.get(SERVED_FILES_DIR + filename)));
        JSONObject expectedContext = (JSONObject) new JSONParser().parse(erJsonContext);

        JSONObject actualContext = new JSONObject();
        runInfo.performContext(flatContext, actualContext);
        org.junit.Assert.assertEquals(expectedContext, actualContext);
    }

    private static String sanitizePathTraversal(String filename) {
        Path p = Paths.get(filename);
        return p.getFileName().toString();
    }

    @Test
    public void parseItemDestinationCountryAsGroupAfterDestinationCountryAsString() {
        TestRunInfo runInfo = new TestRunInfo();
        JSONObject context = new JSONObject();
        runInfo.parseItem("DestinationCountry", "Russia", context);
        runInfo.parseItem("DestinationCountry.Index", "123", context);
        runInfo.parseItem("Account", "Account", context);
        runInfo.parseItem("Account.Num", "322", context);
        runInfo.parseItem("Params", "0", context);
        Assert.isTrue(((JSONObject) context.get("DestinationCountry")).containsKey("Index"),
                "'DestinationCountry' group should - but doesn't - contain 'Index' key!");
    }

    @Test
    public void parseItemDestinationCountryAsStringAfterDestinationCountryAsGroup() {
        TestRunInfo runInfo = new TestRunInfo();
        JSONObject context = new JSONObject();
        runInfo.parseItem("DestinationCountry.Index", "123", context);
        runInfo.parseItem("DestinationCountry", "Russia", context);
        runInfo.parseItem("Account", "Account", context);
        runInfo.parseItem("Account.Num", "322", context);
        runInfo.parseItem("Params", "1", context);
        Assert.isTrue(((JSONObject) context.get("DestinationCountry")).containsKey("Index"),
                "'DestinationCountry' group should - but doesn't - contain 'Index' key!");
    }

    @Test
    public void parseItemParamsWithSameNameAndWithDifferentValue() {
        TestRunInfo runInfo = new TestRunInfo();
        JSONObject context = new JSONObject();
        runInfo.parseItem("Params", "Params", context);
        runInfo.parseItem("DestinationCountry.Index", "123", context);
        runInfo.parseItem("DestinationCountry", "Russia", context);
        runInfo.parseItem("Account", "Account", context);
        runInfo.parseItem("Account.Num", "322", context);
        runInfo.parseItem("Params.OriginalHost", "host", context);
        runInfo.parseItem("Params", "Params2", context);
        Assert.isTrue(((JSONObject) context.get("Params")).containsKey("OriginalHost"),
                "'Params' group should - but doesn't - contain 'OriginalHost' key!");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepareContextFromATPTest() {
        TestRunInfo runInfo = new TestRunInfo();
        JSONObject contextFromATP = new JSONObject();
        contextFromATP.put("Params", "Params");
        contextFromATP.put("Params.OriginHost", "host");
        contextFromATP.put("Account.Num", "123");
        contextFromATP.put("Account", "Account");
        contextFromATP.put("Params.RatingMode", "Online");
        contextFromATP.put("DestinationCountry.Zone", "Belgium");
        contextFromATP.put("DestinationCountry", "Belgium-Belgium");
        contextFromATP.put("NotGroup", 123);
        contextFromATP.put("MultipleNestedGroups.first", "123");
        contextFromATP.put("MultipleNestedGroups.first.second", "321");
        contextFromATP.put("MultipleNestedGroups", "123");
        JSONObject result = new JSONObject();
        for (String item : (Iterable<String>) contextFromATP.keySet()) {
            if (item.contains(".")) {
                String value = contextFromATP.get(item).toString();
                result = runInfo.parseItem(item, value, result);
            } else {
                result.putIfAbsent(item, contextFromATP.get(item));
            }
        }
        Assert.isTrue(((JSONObject) (result.get("Params"))).containsKey("OriginHost"),
                "'Params' group should - but doesn't - contain 'OriginHost' key!");
        Assert.isTrue(((JSONObject) (result.get("Account"))).containsKey("Num"),
                "'Account' group should - but doesn't - contain 'Num' key!");
        Assert.isTrue(((JSONObject) (result.get("DestinationCountry"))).containsKey("Zone"),
                "'DestinationCountry' group should - but doesn't - contain 'Zone' key!");
        Assert.isTrue(result.get("NotGroup").toString().equals("123"),
                "'NotGroup' property value is not equal \"123\"!");
        Assert.isTrue(((JSONObject) (result.get("MultipleNestedGroups"))).containsKey("first"),
                "'MultipleNestedGroups' group should - but doesn't - contain 'first' key!");
    }

    @Test
    public void atpContextContainsArraysSuccessfullyMerged() {
        String atpContextString1 = "{\"Params.reservation_id[0]\":\"0\"}";
        TestRunInfo testRunInfo1 = prepareTestRunInfo(atpContextString1);
        Assert.isTrue(((JSONArray) testRunInfo1.getContextToMerge().get("Params.reservation_id")).get(0).equals("0"),
                "Check #1 is failed: 'Params.reservation_id[0]' property value is not equal \"0\"!");

        String atpContextString2 = "{\n"
                + "\"Params.reservation_id[1]\": \"1\",\n"
                + "\"Params.reservation_id\": [\"0\"]\n"
                + "    }";
        TestRunInfo testRunInfo2 = prepareTestRunInfo(atpContextString2);
        Assert.isTrue(((JSONArray) testRunInfo2.getContextToMerge().get("Params.reservation_id")).get(0).equals("0"),
                "Check #2 is failed: 'Params.reservation_id[0]' property value is not equal \"0\"!");
        Assert.isTrue(((JSONArray) testRunInfo2.getContextToMerge().get("Params.reservation_id")).get(1).equals("1"),
                "Check #2 is failed: 'Params.reservation_id[1]' property value is not equal \"1\"!");

        String atpContextString3 = "{\n"
                + "\"Params.reservation_id[0]\": \"1\",\n"
                + "\"Params.reservation_id[1]\": \"2\",\n"
                + "\"Params.reservation_id[3]\": \"4\",\n"
                + "\"Params.reservation_id\": [\"0\",\"1\"]\n"
                + "    }";
        TestRunInfo testRunInfo3 = prepareTestRunInfo(atpContextString3);
        Assert.isTrue(((JSONArray) testRunInfo3.getContextToMerge().get("Params.reservation_id")).get(0).equals("1"),
                "Check #3 is failed: 'Params.reservation_id[0]' property value is not equal \"1\"!");
        Assert.isTrue(((JSONArray) testRunInfo3.getContextToMerge().get("Params.reservation_id")).get(1).equals("2"),
                "Check #3 is failed: 'Params.reservation_id[1]' property value is not equal \"2\"!");
        Assert.isTrue(((JSONArray) testRunInfo3.getContextToMerge().get("Params.reservation_id")).get(2) == null,
                "Check #3 is failed: 'Params.reservation_id[2]' property value is not null!");
        Assert.isTrue(((JSONArray) testRunInfo3.getContextToMerge().get("Params.reservation_id")).get(3).equals("4"),
                "Check #3 is failed: 'Params.reservation_id[3]' property value is not equal \"4\"!");
    }

    private TestRunInfo prepareTestRunInfo(String atpContextString) {
        // ======= Initialize Dataset =======
        DataSetEntity dataSetEntity = new DataSetEntity();
        DataSetItem dataSetItem1 = new DataSetItem();
        dataSetItem1.setName("Params.reservation_id[0]");
        dataSetItem1.setValue("0");
        List<DataSetItem> dataSetItemList = new ArrayList<>();
        dataSetItemList.add(dataSetItem1);
        dataSetEntity.setVariables(dataSetItemList);
        // ========= End Dataset ============

        // ========= Initialize ATP Context =======
        ContextEntity atpContextEntity = new ContextEntity();
        atpContextEntity.setJsonString(atpContextString);
        //========= End ATP Context ==================

        TestRunInfo testRunInfo = new TestRunInfo();
        testRunInfo.setDataSet(dataSetEntity);
        testRunInfo.setContext(atpContextEntity);

        ReflectionTestUtils.invokeMethod(testRunInfo, "prepareContext");
        return testRunInfo;
    }
}
