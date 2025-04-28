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

package org.qubership.automation.itf.executor.service;

import static org.qubership.automation.itf.executor.service.ExecutionServicesConstants.CALL_CHAIN_EXECUTOR_SERVICE;
import static org.qubership.automation.itf.executor.service.ExecutionServicesConstants.EXECUTION_PROCESS_MANAGER_SERVICE;
import static org.qubership.automation.itf.executor.service.ExecutionServicesConstants.SITUATION_EXECUTOR_SERVICE;
import static org.qubership.automation.itf.executor.service.ExecutionServicesConstants.TC_CONTEXT_SERVICE;

import java.util.HashMap;
import java.util.Map;

import org.qubership.automation.itf.core.instance.situation.SituationExecutorService;
import org.qubership.automation.itf.core.instance.testcase.chain.CallChainExecutorService;
import org.qubership.automation.itf.core.instance.testcase.execution.ExecutionProcessManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExecutionServices {

    private static final Map<String, Object> executorServiceMap = new HashMap<>();

    @Autowired
    public ExecutionServices(CallChainExecutorService callChainExecutorService,
                             SituationExecutorService situationExecutorService,
                             ExecutionProcessManagerService executionProcessManagerService,
                             TCContextService tcContextService) {
        executorServiceMap.put(CALL_CHAIN_EXECUTOR_SERVICE, callChainExecutorService);
        executorServiceMap.put(SITUATION_EXECUTOR_SERVICE, situationExecutorService);
        executorServiceMap.put(EXECUTION_PROCESS_MANAGER_SERVICE, executionProcessManagerService);
        executorServiceMap.put(TC_CONTEXT_SERVICE, tcContextService);
    }

    public static CallChainExecutorService getCallChainExecutorService() {
        return (CallChainExecutorService) executorServiceMap.get(CALL_CHAIN_EXECUTOR_SERVICE);
    }

    public static SituationExecutorService getSituationExecutorService() {
        return (SituationExecutorService) executorServiceMap.get(SITUATION_EXECUTOR_SERVICE);
    }

    public static ExecutionProcessManagerService getExecutionProcessManagerService() {
        return (ExecutionProcessManagerService) executorServiceMap.get(EXECUTION_PROCESS_MANAGER_SERVICE);
    }

    public static TCContextService getTCContextService() {
        return (TCContextService) executorServiceMap.get(TC_CONTEXT_SERVICE);
    }

}
