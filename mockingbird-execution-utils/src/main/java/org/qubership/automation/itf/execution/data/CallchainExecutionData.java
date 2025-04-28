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

package org.qubership.automation.itf.execution.data;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_CAPTURING_FILTER_DEFAULT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_CAPTURING_FILTER_DEFAULT_DEFAULT_VALUE;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCPDUMP_PACKET_COUNT_DEFAULT_DEFAULT_VALUE;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCP_DUMP_NI_DEFAULT;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.TCP_DUMP_NI_DEFAULT_VALUE;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.execution.ExecutionDataProvider;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.util.pcap.PcapHelper;
import org.qubership.automation.itf.core.util.services.CoreServices;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallchainExecutionData implements ExecutionDataProvider {

    @Setter
    @Getter
    private transient CallChain callChain = null;
    @Setter
    @Getter
    private transient IDataSet dataset = null;
    @Setter
    @Getter
    private transient Environment environment = null;
    @Getter
    private transient JsonContext customDataset;
    @Setter
    @Getter
    private String datasetName;
    @Getter
    private String datasetId;
    @Getter
    private String environmentName;
    private String environmentId;
    @Getter
    private String callChainId;
    @Setter
    @Getter
    private boolean runBvCase;
    @Setter
    @Getter
    private boolean runStepByStep;
    @Setter
    @Getter
    private String bvAction;
    @Setter
    @Getter
    private boolean needToLogInATP;
    @Getter
    private Map<String, String> tcpDumpParams;
    @Setter
    @Getter
    private boolean validateMessageOnStep;
    @Setter
    @Getter
    private boolean createTcpDump;
    private String networkInterfaceName;
    private String tcpdumpFilter;
    private int packetCount;
    @Setter
    @Getter
    private BigInteger projectId;
    @Setter
    @Getter
    private UUID projectUuid;

    public CallchainExecutionData(CallChain callChain, String environmentName, String environmentId,
                                  String datasetName) {
        setCallChain(callChain);
        this.environmentName = environmentName;
        this.environmentId = environmentId;
        this.datasetName = datasetName;
    }

    public CallchainExecutionData(String callChainId, String environmentName, String environmentId, String datasetId,
                                  String datasetName, JsonContext customDataset, boolean runBvCase,
                                  boolean runStepByStep, String bvAction, boolean needToLogInATP,
                                  Map<String, String> tcpDumpParams, boolean validateMessageOnStep,
                                  BigInteger projectId, UUID projectUuid) {
        this.callChainId = callChainId;
        this.environmentName = environmentName;
        this.environmentId = environmentId;
        this.datasetId = datasetId;
        this.datasetName = datasetName;
        this.customDataset = customDataset;
        setOptionSection(runBvCase, runStepByStep, bvAction, needToLogInATP, tcpDumpParams, validateMessageOnStep,
                projectId, projectUuid);
    }

    public CallchainExecutionData(CallChain callChain, Environment environment, IDataSet dataset,
                                  JsonContext customDataset, boolean runBvCase, String bvAction,
                                  Map<String, String> tcpDumpParams,
                                  boolean validateMessageOnStep, BigInteger projectId, UUID projectUuid) {
        setCallChain(callChain);
        setEnvironment(environment);
        if (environment != null) {
            this.environmentName = environment.getName();
            this.environmentId = environment.getID().toString();
        }
        setDataset(dataset);
        if (dataset != null) {
            this.datasetName = dataset.getName();
            this.datasetId = dataset.getIdDs();
        }
        this.customDataset = customDataset;
        setOptionSection(runBvCase, runStepByStep, bvAction, needToLogInATP, tcpDumpParams, validateMessageOnStep,
                projectId, projectUuid);
    }

    private void setOptionSection(boolean runBvCase, boolean runStepByStep, String bvAction, boolean needToLogInATP,
                                  Map<String, String> tcpDumpParams, boolean validateMessageOnStep,
                                  BigInteger projectId, UUID projectUuid) {
        this.runBvCase = runBvCase;
        this.runStepByStep = runStepByStep;
        this.bvAction = bvAction;
        this.needToLogInATP = needToLogInATP;
        this.tcpDumpParams = tcpDumpParams;
        this.validateMessageOnStep = validateMessageOnStep;
        this.projectId = projectId;
        this.projectUuid = projectUuid;
        if (tcpDumpParams != null) {
            this.createTcpDump = Boolean.TRUE;
            this.tcpdumpFilter = setParameter(tcpDumpParams.get(PcapHelper.TCPDUMP_FILTER_KEY),
                    CoreServices.getProjectSettingsService().get(projectId, TCPDUMP_CAPTURING_FILTER_DEFAULT,
                            TCPDUMP_CAPTURING_FILTER_DEFAULT_DEFAULT_VALUE), true);
            tcpDumpParams.put(PcapHelper.TCPDUMP_FILTER_KEY, this.tcpdumpFilter);
            this.networkInterfaceName = setParameter(tcpDumpParams.get(PcapHelper.TCPDUMP_NETWORK_INTERFACE_NAME_KEY),
                    CoreServices.getProjectSettingsService().get(projectId, TCP_DUMP_NI_DEFAULT,
                            TCP_DUMP_NI_DEFAULT_VALUE), false);
            tcpDumpParams.put(PcapHelper.TCPDUMP_NETWORK_INTERFACE_NAME_KEY, this.networkInterfaceName);
            if (StringUtils.isBlank(this.networkInterfaceName)) {
                this.createTcpDump = Boolean.FALSE;
                log.error("Network interface name parameter is missed - TCP dump will be disabled.");
            }
            String strPacketCount = setParameter(tcpDumpParams.get(PcapHelper.TCPDUMP_PACKET_COUNT_KEY),
                    CoreServices.getProjectSettingsService().get(projectId, TCPDUMP_PACKET_COUNT_DEFAULT,
                            TCPDUMP_PACKET_COUNT_DEFAULT_DEFAULT_VALUE), false);
            if (StringUtils.isBlank(strPacketCount)) {
                this.createTcpDump = Boolean.FALSE;
                log.error("Packet count parameter is missed - TCP dump will be disabled.");
            }
            try {
                this.packetCount = Integer.parseInt(strPacketCount != null ? strPacketCount : "0");
                if (this.packetCount < 1) {
                    throw new NumberFormatException("Packet count parameter value (" + this.packetCount + ") must be "
                            + "positive");
                }
                tcpDumpParams.put(PcapHelper.TCPDUMP_PACKET_COUNT_KEY, String.valueOf(this.packetCount));
            } catch (NumberFormatException ex) {
                this.createTcpDump = Boolean.FALSE;
                tcpDumpParams.put(PcapHelper.TCPDUMP_PACKET_COUNT_KEY, strPacketCount);
                log.error("Error while parsing the packet count parameter - TCP dump will be disabled. Error: ", ex);
            }
        }
    }

    private String setParameter(String thisValue, String defaultValue, boolean allowEmpty) {
        if (allowEmpty) {
            if (thisValue != null) {
                return thisValue;
            } else if (defaultValue != null) {
                return defaultValue;
            } else {
                return "";
            }
        } else {
            if (!StringUtils.isBlank(thisValue)) {
                return thisValue;
            } else if (!StringUtils.isBlank(defaultValue)) {
                return defaultValue;
            } else {
                return null;
            }
        }
    }
}
