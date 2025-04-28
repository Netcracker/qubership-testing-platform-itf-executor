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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExportImportService {

    /**
     * Collect BulkValidator test cases UUIDs by call chains and call chain steps.
     *
     * @param itfCallChainIds call chains ids.
     * @return List BulkValidator test cases UUIDs.
     */
    public List<UUID> collectBvTcByChainsIds(Set<String> itfCallChainIds) {
        if (itfCallChainIds == null || itfCallChainIds.isEmpty()) {
            return new ArrayList<>();
        }
        ObjectManager<CallChain> objectManager = CoreObjectManager.getInstance().getManager(CallChain.class);
        Set<UUID> bvCasesSet = new HashSet<>();
        for (String chainId : itfCallChainIds) {
            CallChain callChain = objectManager.getById(chainId);
            if (callChain == null) {
                log.warn("Collecting BV testcases: callChain is not found by id {}", chainId);
                continue;
            }
            collectBvCasesByCallChain(bvCasesSet, callChain);
        }
        log.info("Collecting of BulkValidator TestCases by CallChainsIds is finished, {} items", bvCasesSet.size());
        return new ArrayList<>(bvCasesSet);
    }

    /**
     * Collect BulkValidator test cases UUIDs by systems (situations under systems).
     *
     * @param itfSystemsIds systems ids.
     * @return List BulkValidator test cases UUIDs.
     */
    public List<UUID> collectBvTcBySystemsIds(Set<String> itfSystemsIds) {
        if (itfSystemsIds == null || itfSystemsIds.isEmpty()) {
            return new ArrayList<>();
        }
        ObjectManager<System> objectManager = CoreObjectManager.getInstance().getManager(System.class);
        Set<UUID> bvCasesSet = new HashSet<>();
        for (String sysId : itfSystemsIds) {
            System system = objectManager.getById(sysId);
            if (system == null) {
                log.warn("Collecting BV testcases: system is not found by id {}", sysId);
                continue;
            }
            collectBvTcBySystem(bvCasesSet, system);
        }
        log.info("Collecting BulkValidator TestCases by SystemsIds is finished, {} items", bvCasesSet.size());
        return new ArrayList<>(bvCasesSet);
    }

    /**
     * Collect BulkValidator test cases UUIDs by ITF environments. Getting outbounds and inbounds under environments,
     * then get systems under outbounds and inbounds, and collecting
     *
     * @param itfEnvironments Environments ids.
     * @return List BulkValidator test cases UUIDs.
     */
    public List<UUID> collectBvTcByEnvironmentsIds(Set<String> itfEnvironments) {
        if (itfEnvironments == null || itfEnvironments.isEmpty()) {
            return new ArrayList<>();
        }
        ObjectManager<Environment> objectManager = CoreObjectManager.getInstance().getManager(Environment.class);
        Set<UUID> bvCasesSet = new HashSet<>();
        for (String envId : itfEnvironments) {
            Environment environment = objectManager.getById(envId);
            if (environment == null) {
                log.warn("Collecting BV testcases: environment is not found by id {}", envId);
                continue;
            }
            Set<System> outSystems = environment.getOutbound().keySet();
            collectBvTcBySystems(bvCasesSet, outSystems);
            Set<System> inbSystems = environment.getInbound().keySet();
            collectBvTcBySystems(bvCasesSet, inbSystems);
        }
        log.info("Collecting BulkValidator TestCases by EnvironmentsIds is finished, {} items", bvCasesSet.size());
        return new ArrayList<>(bvCasesSet);
    }

    /**
     * Collect DataSetLists UUIDs by call chains ids.
     *
     * @param itfCallChains call chains ids.
     * @return List DSL UUIDs.
     */
    public List<UUID> collectDslByChainsIds(Set<String> itfCallChains) {
        if (itfCallChains == null || itfCallChains.isEmpty()) {
            return new ArrayList<>();
        }
        ObjectManager<CallChain> objectManager = CoreObjectManager.getInstance().getManager(CallChain.class);
        Set<UUID> dataSetLists = new HashSet<>();
        for (String chainId : itfCallChains) {
            CallChain callChain = objectManager.getById(chainId);
            if (callChain == null) {
                log.warn("Collecting DSLs: callChain is not found by id {}", chainId);
                continue;
            }
            Set<String> dslIdsWithVa = callChain.getCompatibleDataSetListIds();
            for (String dslIdWithVa : dslIdsWithVa) {
                if (dslIdWithVa != null) {
                    String[] dslUuid = dslIdWithVa.split("_");
                    if (dslUuid.length >= 2) {
                        addIfNotEmpty(dslUuid[1], dataSetLists);
                    }
                }
            }
        }
        log.info("Collecting DSLs by CallChainsIds is finished, {} items", dataSetLists.size());
        return new ArrayList<>(dataSetLists);
    }

    private void collectBvTcBySystem(Set<UUID> to, System system) {
        if (system == null) {
            return;
        }
        for (Operation operation : system.getOperations()) {
            for (Situation sit : operation.getSituations()) {
                String bvTestcase = sit.getBvTestcase();
                addIfNotEmpty(bvTestcase, to);
            }
        }
    }

    private void collectBvTcBySystems(Set<UUID> to, Set<System> systems) {
        for (System system : systems) {
            collectBvTcBySystem(to, system);
        }
    }

    private void collectBvCasesByCallChain(Set<UUID> bvCasesSet, CallChain callChain) {
        Collection<String> bvCases = callChain.getBvCases().values();
        for (String bvCase : bvCases) {
            addIfNotEmpty(bvCase, bvCasesSet);
        }
        collectBvCasesFromChainSteps(bvCasesSet, callChain);
    }

    private void collectBvCasesFromChainSteps(Set<UUID> bvCasesSet, CallChain callChain) {
        if (callChain == null) {
            return;
        }
        List<Step> callChainSteps = callChain.getSteps();
        for (Step step : callChainSteps) {
            if (step instanceof SituationStep) {
                Situation situation = ((SituationStep) step).getSituation();
                if (situation != null) {
                    addIfNotEmpty(situation.getBvTestcase(), bvCasesSet);
                }
            } else if (step instanceof EmbeddedStep) {
                CallChain chain = ((EmbeddedStep) step).getChain();
                if (chain != null) {
                    collectBvCasesByCallChain(bvCasesSet, chain);
                }
            }
        }
    }

    private void addIfNotEmpty(String stringUuid, Set<UUID> to) {
        if (StringUtils.isEmpty(stringUuid)) {
            return;
        }
        to.add(UUID.fromString(stringUuid));
    }
}
