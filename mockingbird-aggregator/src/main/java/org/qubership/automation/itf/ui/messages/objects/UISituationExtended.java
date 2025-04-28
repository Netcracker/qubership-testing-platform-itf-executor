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

public class UISituationExtended {
    UISituation uiSituation;
    String content;

    public UISituationExtended() {
    }

    public UISituationExtended(UISituation uiSituation, String content) {
        this.uiSituation = uiSituation;
        this.content = content;
    }

    public UISituation getUiSituation() {
        return uiSituation;
    }

    public void setUiSituation(UISituation uiSituation) {
        this.uiSituation = uiSituation;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
