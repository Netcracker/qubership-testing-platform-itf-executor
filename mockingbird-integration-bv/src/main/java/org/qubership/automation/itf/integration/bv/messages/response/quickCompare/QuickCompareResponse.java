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

package org.qubership.automation.itf.integration.bv.messages.response.quickCompare;

import java.util.List;

/* QuickCompare response example:
{
	"tcId": "<testcase id in BV>",
	"tcName":"KAG Rest sync test Bereitstellung ISP_noIP",
	"trDate":null,"trId":"1","resultLink":"http://taprod_bv_ci:8080/bvtool/cresult.jsp?trid=1", // These 3 fields are
	not used really
	"compareResult": "MODIFIED",
	"steps": [{
		"objectId": "<parameter id in BV>",
		"stepName": "<parameter name in BV>",
		"compareResult": "MODIFIED",
		"diffs": [{
			"orderId": 1,
			"expected": "0-0",
			"actual": "",
			"description": "ER row# 1 is MISSED in AR.",
			"result": "MODIFIED"
		},
		{
			"orderId": 2,
			"expected": "",
			"actual": "0-0",
			"description": "AR has EXTRA row# 1.",
			"result": "MODIFIED"
		}]
	}]
}
*
* */
public class QuickCompareResponse {
    List<Step> steps;
    private String tcId, tcName;
    private String trDate, trId, resultLink; // Do NOT use these fields!
    private String compareResult;

    public QuickCompareResponse() {
    }

    public String getTcId() {
        return tcId;
    }

    public void setTcId(String tcId) {
        this.tcId = tcId;
    }

    public String getTcName() {
        return tcName;
    }

    public void setTcName(String tcName) {
        this.tcName = tcName;
    }

    public String getTrDate() {
        return trDate;
    }

    public void setTrDate(String trDate) {
        this.trDate = trDate;
    }

    public String getTrId() {
        return trId;
    }

    public void setTrId(String trId) {
        this.trId = trId;
    }

    public String getResultLink() {
        return resultLink;
    }

    public void setResultLink(String resultLink) {
        this.resultLink = resultLink;
    }

    public String getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(String compareResult) {
        this.compareResult = compareResult;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

}
