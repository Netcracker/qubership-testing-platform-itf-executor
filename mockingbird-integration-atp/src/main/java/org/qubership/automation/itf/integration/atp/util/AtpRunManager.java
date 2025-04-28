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

package org.qubership.automation.itf.integration.atp.util;

import static org.qubership.automation.itf.integration.atp.util.DefinitionBuilder.getBackUrl;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.integration.atp.action.ATPActionFactory;
import org.qubership.automation.itf.integration.atp.action.model.impl.AbstractAtpAction;
import org.qubership.automation.itf.integration.atp.model.DefinitionEntry;
import org.qubership.automation.itf.integration.atp.model.StepAdditionalInfo;
import org.qubership.automation.itf.integration.atp.model.StepEntity;
import org.qubership.automation.itf.integration.atp.model.TestingTargetSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AtpRunManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtpRunManager.class);

    private static AtpRunManager ourInstance = new AtpRunManager();
    private Map<BigInteger, TestRunInfo> infos = Maps.newHashMapWithExpectedSize(50);
    private long started = System.currentTimeMillis();

    private AtpRunManager() {
    }

    public static AtpRunManager getInstance() {
        return ourInstance;
    }

    public void put(TestRunInfo info) {
        infos.put(info.getTestRunId(), info);
    }

    public TestRunInfo get(BigInteger runId) {
        return infos.get(runId);
    }

    public void remove(BigInteger runId) {
        infos.remove(runId);
    }

    public long uptime() {
        return System.currentTimeMillis() - started;
    }

    public List<DefinitionEntry> getDefinitions(BigInteger projectId) {
        LOGGER.debug("getDefinitions request from ATP processing is started...");
        List<DefinitionEntry> definitions = Lists.newArrayListWithCapacity(2);
        for (AbstractAtpAction atpAction : ATPActionFactory.getActions()) {
            DefinitionEntry entry = new DefinitionEntry();
            entry.setId(atpAction.getName());
            entry.setDescription(atpAction.getDescription());
            entry.setDeprecated(atpAction.isDeprecated());
            entry.setMask(atpAction.getMask());
            entry.setSupportedSystems(Lists.newArrayList(TestingTargetSystem.T_O_M_S, TestingTargetSystem.R_B_M,
                    TestingTargetSystem.MANO, TestingTargetSystem.ICOMS));
            entry.setAvailableValues(atpAction.getAvailableValues(projectId));
            definitions.add(entry);
        }
        LOGGER.debug("getDefinitions request from ATP is processed.");
        return definitions;
    }

    public String getAvailableActionsHash() {
        Collection<AbstractAtpAction> atpActions = ATPActionFactory.getActions();
        if (!atpActions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (AbstractAtpAction atpAction : atpActions) {
                sb.append(atpAction.getName());
            }
            return String.valueOf(sb.toString().hashCode());
        }
        return StringUtils.EMPTY;
    }

    public List<StepAdditionalInfo> getStepAdditionalInfo(List<StepEntity> stepEntities) {
        List<StepAdditionalInfo> result = Lists.newArrayList();
        for (StepEntity stepEntity : stepEntities) {
            String idOrName = stepEntity.getActions().get(0).getParameters().get(0).getValue();
            Collection<? extends CallChain> callchains = TestRunInfoBuilder.getCallchainsByIdOrName(idOrName, null);
            if (!callchains.isEmpty()) {
                for (CallChain callChain : callchains) {
                    StepAdditionalInfo stepAdditionalInfo = new StepAdditionalInfo();
                    stepAdditionalInfo.setInfoUrlName(callChain.getName());
                    stepAdditionalInfo.setInfoUrl(getCallchainUrl(callChain.getID().toString()));
                    result.add(stepAdditionalInfo);
                }
            } else {
                LOGGER.warn(TestRunInfoBuilder.getCallchainNotFoundErrorMessage(idOrName));
            }
        }
        return result;
    }

    private String getCallchainUrl(String callchainId) {
        String backUrl = getBackUrl();
        return StringUtils.isNotEmpty(backUrl) ? backUrl.concat(callchainId) : StringUtils.EMPTY;
    }
}
