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

package org.qubership.automation.itf.ui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;

import org.json.simple.JSONArray;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.ui.messages.objects.UIDataSet;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParameter;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParametersGroup;

import com.google.common.collect.Sets;

public class UIHelperTest {

    @Test
    public void testToJsonContextCreatesCorrectJsonContext() throws Exception {
        UIHelper uiHelper = new UIHelper();
        UIDataSet dataSet = createDataSet();
        JsonContext context = uiHelper.toJSONContext(dataSet);
        assertNotNull(context);
        JSONArray array = context.get("GroupName.p", JSONArray.class);
        assertEquals(3, array.size());
        for (int index = 0; index < array.size(); index++) {
            assertEquals(String.valueOf(index), array.get(index));
        }
    }

    private UIDataSet createDataSet() {
        UIDataSet uiDataSet = new UIDataSet();
        uiDataSet.setName("TestContext");
        HashSet<UIDataSetParametersGroup> dataSetParametersGroup = Sets.newHashSet();
        UIDataSetParametersGroup group = new UIDataSetParametersGroup();
        group.setName("GroupName");
        dataSetParametersGroup.add(group);
        uiDataSet.setDataSetParametersGroup(dataSetParametersGroup);
        HashSet<UIDataSetParameter> dataSetParameter = Sets.newHashSet();
        dataSetParameter.add(new UIDataSetParameter("p[0]", "0"));
        dataSetParameter.add(new UIDataSetParameter("p[1]", "1"));
        dataSetParameter.add(new UIDataSetParameter("p[2]", "2"));
        group.setDataSetParameter(dataSetParameter);
        return uiDataSet;
    }
}
