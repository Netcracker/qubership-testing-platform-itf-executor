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

package org.qubership.automation.itf.ui.messages.objects;

import java.util.Map;

import com.google.common.collect.Maps;

public class UIRequestBody {

    private Map<String, String> params = Maps.newHashMap();
    private UIDataSet dataSet;
    private DataSetsInfo[] dataSetsInfo;

    public UIRequestBody() {
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public UIDataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(UIDataSet dataSet) {
        this.dataSet = dataSet;
    }

    public DataSetsInfo[] getDataSetsInfo() {
        return dataSetsInfo;
    }

    public void setDataSetsInfo(DataSetsInfo[] dataSetsInfo) {
        this.dataSetsInfo = dataSetsInfo;
    }
}
