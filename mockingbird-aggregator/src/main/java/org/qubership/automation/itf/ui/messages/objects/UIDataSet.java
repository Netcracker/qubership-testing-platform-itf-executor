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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class UIDataSet extends UIObject {

    private String bvCaseId;
    private ImmutableSet<UIDataSetParametersGroup> dataSetParametersGroup;
    private boolean bvCaseExist;
    private String dsLink;
    private boolean isDefault;

    public UIDataSet() {
    }

    public ImmutableSet<UIDataSetParametersGroup> getDataSetParametersGroup() {
        return dataSetParametersGroup;
    }

    public void setDataSetParametersGroup(Set<UIDataSetParametersGroup> dataSetParametersGroup) {
        if (dataSetParametersGroup != null) {
            this.dataSetParametersGroup = ImmutableSet.copyOf(dataSetParametersGroup);
        } else {
            this.dataSetParametersGroup = null;
        }
    }

    public String getBvCaseId() {
        return bvCaseId;
    }

    public void setBvCaseId(String bvCaseId) {
        this.bvCaseId = bvCaseId;
    }

    public boolean isBvCaseExist() {
        return bvCaseExist;
    }

    public void setBvCaseExist(boolean bvCaseExist) {
        this.bvCaseExist = bvCaseExist;
    }

    public String getDsLink() {
        return dsLink;
    }

    public void setDsLink(String dsLink) {
        this.dsLink = dsLink;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
