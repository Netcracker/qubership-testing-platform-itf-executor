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

import java.math.BigInteger;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutionRequest {

    private String dataset;
    private String environment;
    @JsonProperty("name")
    private String starterChainName;
    @JsonProperty("id")
    private String starterChainId;
    @JsonProperty("external.app.name")
    private String externalAppName;
    private BigInteger testRunId;
    private BigInteger externalSectionId;
    @JsonProperty("ram.er.name")
    private String ramExecRequestName;
    @JsonProperty("atp.project.name")
    private String ramProject;
    @JsonProperty("ram.suite.name")
    private String ramSuite;
    @JsonProperty("ram.mail.list")
    private String ramMailList;

    private String ram2TestRunId;
    private String ram2SectionId;
    private String ram2SectionName;

    private Map<String, Map<String, String>> dataSetList;

    private String projectId;

    private Map<String, String> dataSetMap;
    @JsonProperty("ram.log.async")
    private Boolean logAsync = false;
    @JsonProperty("validateAtEnd")
    private Boolean validateAtEnd = false;
    @JsonProperty("validateOnSituation")
    private Boolean validateOnSituation = false;
    // Merge dataset with dataSetMap/dataSetList from JSON
    @JsonProperty("mergeDatasetWithContext")
    private Boolean mergeDatasetWithContext = false;

    public Map<String, String> getDataSetMap() {
        return dataSetMap;
    }

    public void setDataSetMap(Map<String, String> dataSetMap) {
        this.dataSetMap = dataSetMap;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getStarterChainName() {
        return starterChainName;
    }

    public void setStarterChainName(String starterChainName) {
        this.starterChainName = starterChainName;
    }

    public String getStarterChainId() {
        return starterChainId;
    }

    public void setStarterChainId(String starterChainId) {
        this.starterChainId = starterChainId;
    }

    public String getExternalAppName() {
        return externalAppName;
    }

    public void setExternalAppName(String externalAppName) {
        this.externalAppName = externalAppName;
    }

    public BigInteger getTestRunId() {
        return testRunId;
    }

    public void setTestRunId(BigInteger testRunId) {
        this.testRunId = testRunId;
    }

    public BigInteger getExternalSectionId() {
        return externalSectionId;
    }

    public void setExternalSectionId(BigInteger externalSectionId) {
        this.externalSectionId = externalSectionId;
    }

    public String getRamExecRequestName() {
        return ramExecRequestName;
    }

    public void setRamExecRequestName(String ramExecRequestName) {
        this.ramExecRequestName = ramExecRequestName;
    }

    public String getRamProject() {
        return ramProject;
    }

    public void setRamProject(String ramProject) {
        this.ramProject = ramProject;
    }

    public String getRamSuite() {
        return ramSuite;
    }

    public void setRamSuite(String ramSuite) {
        this.ramSuite = ramSuite;
    }

    public String getRamMailList() {
        return ramMailList;
    }

    public void setRamMailList(String ramMailList) {
        this.ramMailList = ramMailList;
    }

    public Map<String, Map<String, String>> getDataSetList() {
        return dataSetList;
    }

    public void setDataSetList(Map<String, Map<String, String>> dataSetList) {
        this.dataSetList = dataSetList;
    }

    public Boolean getLogAsync() {
        return logAsync;
    }

    public void setLogAsync(Boolean logAsync) {
        this.logAsync = logAsync;
    }

    public String getRam2TestRunId() {
        return ram2TestRunId;
    }

    public void setRam2TestRunId(String ram2TestRunId) {
        this.ram2TestRunId = ram2TestRunId;
    }

    public String getRam2SectionId() {
        return ram2SectionId;
    }

    public void setRam2SectionId(String ram2SectionId) {
        this.ram2SectionId = ram2SectionId;
    }

    public String getRam2SectionName() {
        return ram2SectionName;
    }

    public void setRam2SectionName(String ram2SectionName) {
        this.ram2SectionName = ram2SectionName;
    }

    public Boolean getValidateAtEnd() {
        return validateAtEnd;
    }

    public void setValidateAtEnd(Boolean validateAtEnd) {
        this.validateAtEnd = validateAtEnd;
    }

    public Boolean getValidateOnSituation() {
        return validateOnSituation;
    }

    public void setValidateOnSituation(Boolean validateOnSituation) {
        this.validateOnSituation = validateOnSituation;
    }

    public Boolean getMergeDatasetWithContext() {
        return mergeDatasetWithContext;
    }

    public void setMergeDatasetWithContext(Boolean mergeDatasetWithContext) {
        this.mergeDatasetWithContext = mergeDatasetWithContext;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
