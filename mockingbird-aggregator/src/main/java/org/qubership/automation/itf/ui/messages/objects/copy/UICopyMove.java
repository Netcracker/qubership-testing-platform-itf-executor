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

package org.qubership.automation.itf.ui.messages.objects.copy;

import java.util.List;

import org.json.simple.JSONObject;
import org.qubership.automation.itf.ui.messages.objects.UIObject;

import com.google.common.collect.Lists;

public class UICopyMove {

    private List<UIObject> sources = Lists.newArrayList();
    private UIObject destination;
    private boolean copyFlag;
    private JSONObject other;
    private String projectId;

    public UIObject getDestination() {
        return destination;
    }

    public void setDestination(UIObject destination) {
        this.destination = destination;
    }

    public List<UIObject> getSources() {
        return sources;
    }

    public void setSources(List<UIObject> sources) {
        this.sources = sources;
    }

    public boolean getCopyFlag() {
        return copyFlag;
    }

    public void setCopyFlag(boolean copyFlag) {
        this.copyFlag = copyFlag;
    }

    public JSONObject getOther() {
        return other;
    }

    public void setOther(JSONObject other) {
        this.other = other;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
