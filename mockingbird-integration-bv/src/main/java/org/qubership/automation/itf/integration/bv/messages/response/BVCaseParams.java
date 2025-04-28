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

package org.qubership.automation.itf.integration.bv.messages.response;

public class BVCaseParams {

    private String status;
    private String containsObjects;
    private String containsTestruns;
    private String objectsCount;
    private String lastTestrunCreatedDate;
    private String lastTestrunStatus;

    public String getLastTestrunCreatedDate() {
        return lastTestrunCreatedDate;
    }

    public void setLastTestrunCreatedDate(String lastTestrunCreatedDate) {
        this.lastTestrunCreatedDate = lastTestrunCreatedDate;
    }

    public String getLastTestrunStatus() {
        return lastTestrunStatus;
    }

    public void setLastTestrunStatus(String lastTestrunStatus) {
        this.lastTestrunStatus = lastTestrunStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContainsObjects() {
        return containsObjects;
    }

    public void setContainsObjects(String containsObjects) {
        this.containsObjects = containsObjects;
    }

    public String getContainsTestruns() {
        return containsTestruns;
    }

    public void setContainsTestruns(String containsTestruns) {
        this.containsTestruns = containsTestruns;
    }

    public String getObjectsCount() {
        return objectsCount;
    }

    public void setObjectsCount(String objectsCount) {
        this.objectsCount = objectsCount;
    }
}
