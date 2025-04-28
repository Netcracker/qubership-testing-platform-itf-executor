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

package org.qubership.automation.itf.report.extension;

import java.io.Serializable;
import java.math.BigInteger;

import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.automation.itf.core.model.extension.Extension;
import org.qubership.automation.itf.core.util.constants.StartedFrom;

public class TCContextRamExtension implements Extension, Serializable {

    private BigInteger runId;
    private String erName;
    private String suiteName;
    private String mailList;
    private BigInteger sectionId;
    private String reportUrl;
    private String projectName;
    private String externalAppName;
    private Boolean externalRun;
    private Boolean async = true;
    private StartedFrom startedFrom;

    private transient TestRunContext runContext;

    public BigInteger getRunId() {
        return runId;
    }

    public TCContextRamExtension setRunId(BigInteger runId) {
        this.runId = runId;
        return this;
    }

    public String getErName() {
        return erName;
    }

    public TCContextRamExtension setErName(String erName) {
        this.erName = erName;
        return this;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public TCContextRamExtension setSuiteName(String suiteName) {
        this.suiteName = suiteName;
        return this;
    }

    public String getMailList() {
        return mailList;
    }

    public TCContextRamExtension setMailList(String mailList) {
        this.mailList = mailList;
        return this;
    }

    public BigInteger getSectionId() {
        return sectionId;
    }

    public TCContextRamExtension setSectionId(BigInteger sectionId) {
        this.sectionId = sectionId;
        return this;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public TCContextRamExtension setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
        return this;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getExternalAppName() {
        return externalAppName;
    }

    public void setExternalAppName(String externalAppName) {
        this.externalAppName = externalAppName;
    }

    public Boolean getExternalRun() {
        return externalRun;
    }

    public void setExternalRun(Boolean externalRun) {
        this.externalRun = externalRun;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public TestRunContext getRunContext() {
        return runContext;
    }

    public void setRunContext(TestRunContext runContext) {
        this.runContext = runContext;
    }

    public StartedFrom getStartedFrom() {
        return startedFrom;
    }

    public void setStartedFrom(StartedFrom startedFrom) {
        this.startedFrom = startedFrom;
    }
}
