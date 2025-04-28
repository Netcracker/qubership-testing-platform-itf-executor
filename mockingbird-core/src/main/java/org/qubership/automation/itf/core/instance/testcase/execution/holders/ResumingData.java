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

package org.qubership.automation.itf.core.instance.testcase.execution.holders;

public class ResumingData {

    private String subscriberId;
    private String parentSubscriberId;
    private boolean needToContinue;

    public ResumingData(String subscriberId, String parentSubscriberId, boolean needToContinue) {
        this.subscriberId = subscriberId;
        this.parentSubscriberId = parentSubscriberId;
        this.needToContinue = needToContinue;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getParentSubscriberId() {
        return parentSubscriberId;
    }

    public boolean isNeedToContinue() {
        return needToContinue;
    }
}
