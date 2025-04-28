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

package org.qubership.automation.itf.integration.atp.action;

public enum ATPActionConstants {
    //Common
    ITF_TC_URL_TEMPLATE("#/callchain/"),
    MOCKINGBIRD_DESCRIPTION("Integration Testing Framework"),
    //ATP-actions
    RUN_ITF_CASE_BY_NAME_NAME("Run ITF-case by its name"),
    RUN_ITF_CASE_BY_NAME_DESCRIPTION("Action for running the ITF-case by its name"),
    RUN_ITF_CASE_BY_NAME_MASK("Run \"\" integration call chain"),
    RUN_ITF_CASE_BY_NAME_TEMPLATE("Run \"(.*)\" integration call chain$"),

    RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME("Run ITF-case by its name using the dataset"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_DESCRIPTION("Action for running the ITF-case by its name using the dataset"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_MASK("Run \"\" integration call chain using \"\" dataset"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_TEMPLATE("Run \"(.*)\" integration call chain using \"(.*)\" dataset$"),

    RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_NAME("Run ITF-case with default dataset"),
    RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_DESCRIPTION("Action for running ITF-case with default dataset"),
    RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_MASK("Run \"\" call chain with default dataset"),
    RUN_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_TEMPLATE("Run \"(.*)\" call chain with default dataset$"),

    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_NAME("Run the ITF-case with validation by its name using the "
            + "dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_DESCRIPTION("Action for running the ITF-case with validation by "
            + "its name using the dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_MASK("Run and validate \"\" integration call chain using \"\" "
            + "dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_TEMPLATE("Run and validate \"(.*)\" integration call chain using "
            + "\"(.*)\" dataset$"),

    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_NAME("Run ITF-case with validation with default dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_DESCRIPTION("Action for running the ITF-case with "
            + "validation with default dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_MASK("Run and validate \"\" call chain with default "
            + "dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DEFAULT_DATASET_TEMPLATE("Run and validate \"(.*)\" call chain with "
            + "default dataset$"),

    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_ATP_DATASET_NAME("Run ITF-case with validation with atp dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_ATP_DATASET_DESCRIPTION("Action for running the ITF-case with validation "
            + "with atp dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_ATP_DATASET_MASK("Run and validate \"\" call chain with atp dataset"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_ATP_DATASET_TEMPLATE("Run and validate \"(.*)\" call chain with atp "
            + "dataset$"),

    RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_NAME("Run ITF-case with dataset and options"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_DESCRIPTION("Action for running the ITF-case with dataset and "
            + "options"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_MASK("Run \"\" integration call chain using \"\" dataset with "
            + "options tcpdumpEnabled \"\" tcpdumpFilter \"\" tcpdumpPacketCount \"\" bvEnabled \"\" bvAction \"\""),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_NAME_AND_OPTIONS_TEMPLATE("Run \"(.*)\" integration call chain using \"(.*)\" "
            + "dataset with options tcpdumpEnabled \"(.*)\" tcpdumpFilter \"(.*)\" tcpdumpPacketCount \"(.*)\" "
            + "bvEnabled \"(.*)\" bvAction \"(.*)\"$"),

    RUN_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_NAME("Run ITF-case with dataset label and dsl path"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_DESCRIPTION("Action for running the ITF-case with "
            + "dataset label and DSL (DataSetList Path)"),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_MASK("Run integration callchain \"\" with datasets "
            + "by label \"\" in the dsl \"\""),
    RUN_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_TEMPLATE("Run integration callchain \"(.*)\" with "
            + "datasets by label \"(.*)\" in the dsl \"(.*)\"$"),

    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_NAME("Run and Validate ITF-case with "
            + "dataset label and dsl path"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_DESCRIPTION("Action for running and "
            + "Validate the ITF-case with dataset label and DSL (DataSetList Path)"),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_MASK("Run and validate integration "
            + "callchain \"\" with datasets by label \"\" in the dsl \"\""),
    RUN_AND_VALIDATE_ITF_CASE_BY_NAME_WITH_DATASET_LABEL_AND_DATASETLIST_PATH_TEMPLATE("Run and validate integration "
            + "callchain \"(.*)\" with datasets by label \"(.*)\" in the dsl \"(.*)\"$"),

    RUN_ITF_CASES_BY_LABEL_WITH_DEFAULT_DATASET_NAME("Run ITF-cases by label with default datasets"),
    RUN_ITF_CASES_BY_LABEL_WITH_DEFAULT_DATASET_DESCRIPTION("Action for running the ITF-cases by the label with "
            +
            "default datasets"),
    RUN_ITF_CASES_BY_LABEL_WITH_DEFAULT_DATASET_MASK("Run integrations callchains by label \"\" with default datasets"),
    RUN_ITF_CASES_BY_LABEL_WITH_DEFAULT_DATASET_TEMPLATE("Run integrations callchains by label \"(.*)\" with default "
            + "datasets$"),

    RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_NAME("Run ITF-cases by label with all dsls"),
    RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_DESCRIPTION("Action for running the ITF-cases by the label with all "
            + "datasets in all dsls under each case"),
    RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_MASK("Run integrations callchains by label \"\" with all dsls"),
    RUN_ITF_CASES_BY_LABEL_WITH_ALL_DATASETS_TEMPLATE("Run integrations callchains by label \"(.*)\" with all dsls$"),

    RUN_AND_VALIDATE_ITF_CASE_NAME("Run ITF-case with validation on step"),
    RUN_AND_VALIDATE_ITF_CASE_DESCRIPTION("Action for running the ITF-case with validation on step"),
    RUN_AND_VALIDATE_ITF_CASE_MASK("Run \"\" integration call chain with validation on step"),
    RUN_AND_VALIDATE_ITF_CASE_TEMPLATE("Run \"(.*)\" integration call chain with validation on step$"),

    DEFAULT("DEFAULT"),
    TRUE("true"),

    //Extra actions
    VALIDATE_EXTRA_ACTION("validate"),

    //Parameters
    MASK("atp.action.mask"),
    DEPRECATED("atp.action.deprecated"),
    ACTION_TEMPLATE("atp.action.template"),

    //Action parameters indexes
    CALLCHAIN_INDEX(1),
    LABEL_INDEX(1),
    DATASET_INDEX(2),
    DATASET_LABEL_INDEX(2),
    DATASETLIST_PATH_INDEX(3),
    TCPDUMP_ENABLED_INDEX(3),
    TCPDUMP_FILTER_INDEX(4),
    TCPDUMP_PACKET_COUNT_INDEX(5),
    BV_ENABLED_INDEX(6),
    BV_ACTION_INDEX(7);

    private String stringValue;
    private int intValue;

    ATPActionConstants(String stringValue) {
        this.stringValue = stringValue;
    }

    ATPActionConstants(int intValue) {
        this.intValue = intValue;
    }

    public String stringValue() {
        return stringValue;
    }

    public int intValue() {
        return intValue;
    }
}
