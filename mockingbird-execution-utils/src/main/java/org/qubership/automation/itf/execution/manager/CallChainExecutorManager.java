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

package org.qubership.automation.itf.execution.manager;

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.BV_DEFAULT_ACTION_DEFAULT_VALUE;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.execution.Executor;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByParameterAndProjectIdManager;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.pcap.PcapHelper;
import org.qubership.automation.itf.execution.data.CallchainExecutionData;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.integration.bv.utils.BvHelper;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CallChainExecutorManager extends AbstractExecutorManager implements Executor<CallchainExecutionData> {

    private static final Gson GSON = new GsonBuilder().create();
    private final ProjectSettingsService projectSettingsService;

    @Override
    public CallChainInstance prepare(CallchainExecutionData executionData, boolean startedByAtp) throws Exception {
        CallChain entity = executionData.getCallChain() != null
                ? executionData.getCallChain()
                : CoreObjectManager.getInstance().getManager(CallChain.class).getById(executionData.getCallChainId());
        Environment env = executionData.getEnvironment() != null
                ? executionData.getEnvironment()
                : Iterables.getFirst(
                ((SearchByParameterAndProjectIdManager<Environment>) CoreObjectManager.getInstance()
                        .getSpecialManager(Environment.class, SearchByParameterAndProjectIdManager.class))
                        .getByNameAndProjectId(executionData.getEnvironmentName(),
                                executionData.getProjectId()), null);
        Preconditions.checkNotNull(env, "No environment with name [" + executionData.getEnvironment() + "] found");
        IDataSet dataSet = executionData.getDataset() != null
                ? executionData.getDataset()
                : (executionData.getDatasetId() == null || executionData.getDatasetId().isEmpty())
                ? entity.findDataSetByName(executionData.getDatasetName(), executionData.getProjectId())
                : entity.findDataSetById(executionData.getDatasetId(), executionData.getProjectId());
        CallChainInstance instance = ExecutionServices.getCallChainExecutorService()
                .prepare(executionData.getProjectId(), executionData.getProjectUuid(), entity, null, env,
                        dataSet, executionData.getCustomDataset(),
                        startedByAtp, executionData.isValidateMessageOnStep());
        instance.getContext().setProjectUuid(executionData.getProjectUuid());
        instance.setDatasetDefault(
                entity.getDatasetId() != null && dataSet != null && entity.getDatasetId().equals(dataSet.getIdDs()));
        TcContext tcContext = instance.getContext().tc();
        tcContext.setNeedToReportToAtp(executionData.isNeedToLogInATP() || tcContext.getStartedByAtp());
        tcContext.setAndCalculateNeedToReportToItf();
        if (Objects.isNull(executionData.getDatasetName())) {
            Object datasetName = tcContext.get("DATASET_NAME");
            if (Objects.nonNull(datasetName)) {
                executionData.setDatasetName(datasetName.toString());
                tcContext.setName(String.format("%s [%s]", instance.getName(), executionData.getDatasetName()));
            }
        }
        instance.setCallchainExecutionData(GSON.toJson(executionData));
        if (dataSet != null && executionData.isRunBvCase()) {
            Object action = (tcContext.containsKey("bv.action")) ? tcContext.get("bv.action") : null;
            String resultAction = action != null
                    ? action.toString()
                    : StringUtils.isEmpty(executionData.getBvAction())
                    ? projectSettingsService.get(tcContext.getProjectId(),
                    BV_DEFAULT_ACTION, BV_DEFAULT_ACTION_DEFAULT_VALUE) : executionData.getBvAction();
            BvHelper.addOnCaseFinishValidation(instance, entity, dataSet.getName(), executionData.isRunBvCase(),
                    resultAction);
        }
        if (executionData.isValidateMessageOnStep()) {
            BvHelper.addMessageOnStepValidation(instance);
        }
        if (executionData.getTcpDumpParams() != null && executionData.isCreateTcpDump()) {
            PcapHelper.createPcapManager(tcContext.getID().toString(),
                    tcContext.getProjectId(),
                    executionData.getTcpDumpParams().get(PcapHelper.TCPDUMP_NETWORK_INTERFACE_NAME_KEY),
                    executionData.getTcpDumpParams().get(PcapHelper.TCPDUMP_FILTER_KEY),
                    executionData.getTcpDumpParams().get(PcapHelper.TCPDUMP_PACKET_COUNT_KEY));
        }
        return instance;
    }
}
