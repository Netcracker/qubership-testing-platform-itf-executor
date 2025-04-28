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

package org.qubership.automation.itf.integration.bv.messages.request;

import java.util.List;

import org.qubership.automation.itf.integration.bv.messages.RequestResponseData;

public class RequestData extends RequestResponseData {
    private List<Label> labels;
    private String type;
    private BvSource[] sources;
    private BvReadType readType;
    private Servers[] servers;
    private TestCase[] testCases;
    private BvReadMode readMode;
    private ReportData report;
    private List<ValidationObject> validationObjects;

    public RequestData() {
    }

    public RequestData(String tcId) {
        this.tcId = tcId;
    }

    public ReportData getReport() {
        return report;
    }

    public void setReport(ReportData report) {
        this.report = report;
    }

    public BvReadType getReadType() {
        return readType;
    }

    public void setReadType(BvReadType readType) {
        this.readType = readType;
    }

    public TestCase[] getTestCases() {
        return testCases;
    }

    public void setTestCases(TestCase[] testCases) {
        this.testCases = testCases;
    }

    public BvReadMode getReadMode() {
        return readMode;
    }

    public void setReadMode(BvReadMode readMode) {
        this.readMode = readMode;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public Servers[] getServers() {
        return servers;
    }

    public void setServers(Servers[] servers) {
        this.servers = servers;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BvSource[] getSources() {
        return sources;
    }

    public void setSources(BvSource[] sources) {
        this.sources = sources;
    }

    public List<ValidationObject> getValidationObjects() {
        return validationObjects;
    }

    public void setValidationObjects(List<ValidationObject> validationObjects) {
        this.validationObjects = validationObjects;
    }
}
