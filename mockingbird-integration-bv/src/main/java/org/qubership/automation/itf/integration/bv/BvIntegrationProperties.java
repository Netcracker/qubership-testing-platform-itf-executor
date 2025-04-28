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

package org.qubership.automation.itf.integration.bv;

public interface BvIntegrationProperties {

    String REPORT_LINK_SWITCHER = "bv.use.report.link";
    String URL_TO_ISL = "bv.report.link.name";
    String INPUT_PARAMS_CONTEXT_KEYS = "bv.input.params.names";
    String VALIDATE_PARAMS_CONTEXT_KEYS = "bv.valid.params.names";
    String BV_SOURCE = "bv.source.name";
    String BV_CONF_PATH = "bv.conf.path";
    String BV_TEST_CASE_TYPE = "bv.testcase.type";

    String BV_RESPONSE_STATUS_OK = "10000";

    String READ_COMPARE = "ReadCompare";
    String COMPARE = "Compare";
    String CREATE_NEW_TESTRUN = "CreateNewTestRun";
}
