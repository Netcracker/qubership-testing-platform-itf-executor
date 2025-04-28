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

public interface BvEndpoints {
    // Endpoints to BV backend - start
    String CREATE_TC_ENDPOINT = "/api/bvtool/project/%s/testcases/v1/create";
    String REMOVE_TC_ENDPOINT = "/api/bvtool/project/%s/testcases/v1/remove";
    String IS_EXIST_ENDPOINT = "/api/bvtool/project/%s/testcases/v1/getTestCaseStatus";
    String READ_ENDPOINT = "/api/bvtool/project/%s/api/v1/read";
    String READ_COMPARE_ENDPOINT = "/api/bvtool/project/%s/api/v1/readAndCompare";
    String COMPARE_ENDPOINT = "/api/bvtool/project/%s/api/v1/compare";
    String CREATING_NEW_TR_ENDPOINT = "/api/bvtool/project/%s/public/v1/createTr";
    String VALIDATE_MESSAGE_ENDPOINT = "/api/bvtool/project/%s/public/v1/quickCompare"; // For message quick
    // validations at situations only
    String GET_TESTCASE_PARAMETERS_ENDPOINT = "/api/bvtool/project/%s/testcases/v1/getParameters"; // Get validation
    // parameters of the testcase - before message quick validations at a situation
    String COPY_TC_WITH_NAME_ENDPOINT = "/api/bvtool/project/%s/testcases/v1/copyWithName"; // Get validation
    // parameters of the testcase - before message quick validations at a situation
    // Endpoints to BV backend - end

    // Endpoints to BV frontend - start
    String ENDPOINT_FOR_LINK_TO_TR = "/project/%s/bvtool/bvt/validation?trid=";
    String ENDPOINT_FOR_LINK_TO_TC = "/project/%s/bvtool/bvt/testcases?tcid=";
    // Endpoints to BV frontend - end
}
