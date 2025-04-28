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

package org.qubership.automation.itf.integration.atp.model;

import java.math.BigInteger;

import lombok.Data;

@Data
public class ExecuteStepRequest {

    private String projectName;
    private String testCaseName;
    private String testRunName;
    private BigInteger testRunId;
    private String testSuiteName;
    private BigInteger testSuiteId;
    private BigInteger parentStepId;
    private BigInteger logRecordId;
    private String atpRamUrl;
    private String expectedResult;
    private String executionRequestName;
    private String executionRequestId;
    private String testPlanName;
    private String testPlanId;
    private StepSection section;
    private ConfigurationEntity configuration;
    private ScopeEntity scope;
    private ContextEntity context;
    private TestEnvironmentConfiguration testEnvironmentConfiguration;
}
