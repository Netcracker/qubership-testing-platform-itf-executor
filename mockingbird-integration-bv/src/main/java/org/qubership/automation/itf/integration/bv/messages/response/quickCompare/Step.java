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

public class Step {
    List<Difference> diffs;
    private String objectId;
    private String stepName;
    private String compareResult;
    private String highlightedEr;
    private String highlightedAr;
    private String description;
    private boolean important;

    public Step() {
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(String compareResult) {
        this.compareResult = compareResult;
    }

    public String getHighlightedEr() {
        return highlightedEr;
    }

    public void setHighlightedEr(String highlightedEr) {
        this.highlightedEr = highlightedEr;
    }

    public String getHighlightedAr() {
        return highlightedAr;
    }

    public void setHighlightedAr(String highlightedAr) {
        this.highlightedAr = highlightedAr;
    }

    public List<Difference> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<Difference> diffs) {
        this.diffs = diffs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }
}
