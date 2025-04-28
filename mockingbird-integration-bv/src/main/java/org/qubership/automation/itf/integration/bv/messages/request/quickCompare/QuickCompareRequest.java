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

package org.qubership.automation.itf.integration.bv.messages.request.quickCompare;

import java.util.List;

/*
'quickCompare' Request endpoint is: {server-URL}/bvtool/rest/public/quickCompare
'quickCompare' Request format example:
{
	"tcId": "<testcase id in BV>",
	"validationObjects": [{
		"name": "ITF Situation 1",
		"children": [{
			"name": "Incoming",
			"ar": "Value",
		},{
			"name": "Outgoing",
			"ar": "Value",
		}]
	},{
		"name": "Some Param With Value",
		"ar": "12345"
	}]
}
 */
public class QuickCompareRequest {
    String tcId;
    private boolean loadHighlight;
    private List<ValidationParameter> validationObjects;

    public QuickCompareRequest() {
    }

    public String getTcId() {
        return tcId;
    }

    public void setTcId(String tcId) {
        this.tcId = tcId;
    }

    public boolean isLoadHighlight() {
        return loadHighlight;
    }

    public void setLoadHighlight(boolean loadHighlight) {
        this.loadHighlight = loadHighlight;
    }

    public List<ValidationParameter> getValidationObjects() {
        return validationObjects;
    }

    public void setValidationObjects(List<ValidationParameter> validationObjects) {
        this.validationObjects = validationObjects;
    }

}
