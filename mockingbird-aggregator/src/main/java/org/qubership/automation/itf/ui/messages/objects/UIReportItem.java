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
import java.util.Set;

import com.google.common.collect.Maps;

public class UIReportItem {

    private String id;
    private String name;
    private String initiator;
    private String initiatorId;
    private String system;
    private String operation;
    private String status;
    private String environment;
    private String startTime;
    private String endTime;
    private String duration;
    private String client;
    private Map<String, String> reportLinks;
    private String[] bindingKeys;
    private String contextVariable;
    private String callchainExecutionData;
    private Map<String, String> reportSituations;

    public UIReportItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Map<String, String> getReportLinks() {
        return this.reportLinks;
    }

    public void setReportLinks(Map<String, String> reportLinks) {
        this.reportLinks = Maps.newHashMap(reportLinks);
    }

    public String[] getBindingKeys() {
        return bindingKeys;
    }

    public void setBindingKeys(Set<String> bindingKeys) {
        this.bindingKeys = bindingKeys.toArray(new String[bindingKeys.size()]);
    }

    public String getContextVariable() {
        return contextVariable;
    }

    public void setContextVariable(String contextVariable) {
        this.contextVariable = contextVariable;
    }

    public String getCallchainExecutionData() {
        return callchainExecutionData;
    }

    public void setCallchainExecutionData(String callchainExecutionData) {
        this.callchainExecutionData = callchainExecutionData;
    }

    public Map<String, String> getReportSituations() {
        return this.reportSituations;
    }

    public void setReportSituations(Map<String, String> reportSituations) {
        this.reportSituations = Maps.newHashMap(reportSituations);
    }
}
