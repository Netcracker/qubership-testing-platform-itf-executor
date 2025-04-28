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

public class StarterSituationExistanceChecker {

    private ExistenceChecker parentChecker;
    private String senderName;
    private String receiverName;
    private ExistenceChecker operationChecker;
    private String templateName;

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public ExistenceChecker getOperationChecker() {
        return operationChecker;
    }

    public void setOperationChecker(ExistenceChecker operationChecker) {
        this.operationChecker = operationChecker;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public ExistenceChecker getParentChecker() {
        return parentChecker;
    }

    public void setParentChecker(ExistenceChecker parentChecker) {
        this.parentChecker = parentChecker;
    }
}
