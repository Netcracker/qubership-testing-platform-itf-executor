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

package org.qubership.automation.itf.ui.controls.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceRegenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceRegenerator.class);

    public static void regenerateSystems(Set<System> targetSystems, HashMap<System, System> systemsToReplacementMap) {
        HashMap<String, HashMap<String, Situation>> situationReplacementsOnSituationTriggers =
                Collector.collectSituationsFromSystemsMap(systemsToReplacementMap);
        Map<String, Template> templateReplacements =
                Collector.collectTemplatesFromSystems(systemsToReplacementMap.values());
        for (System targetSystem : targetSystems) {
            Map<String, Operation> operationReplacements = Collector.collectOperationsFromSystem(targetSystem);
            for (Operation o : targetSystem.getOperations()) {
                for (Situation situation : o.getSituations()) {
                    IntegrationStep step = situation.getIntegrationStep();
                    if (step != null) {
                        LOGGER.info("Regenerating operations for" + step.toString());
                        if (step.getOperation() != null) {
                            String operationId = step.getOperation().getID().toString();
                            if (operationReplacements.containsKey(operationId)) {
                                step.setOperation(operationReplacements.get(operationId));
                            }
                        }
                        LOGGER.info("Regenerating sender for" + step.toString());
                        if (step.getSender() != null) {
                            if (systemsToReplacementMap.containsKey(step.getSender()))
                                step.setSender(systemsToReplacementMap.get(step.getSender()));
                        }
                        LOGGER.info("Regenerating receiver for" + step.toString());
                        if (step.getReceiver() != null) {
                            if (systemsToReplacementMap.containsKey(step.getReceiver()))
                                step.setReceiver(systemsToReplacementMap.get(step.getReceiver()));
                        }
                        LOGGER.info("Regenerating templates for" + step.toString());
                        if (step.returnStepTemplate() != null) {
                            String templateId = step.returnStepTemplate().getID().toString();
                            if (templateReplacements.containsKey(templateId))
                                step.setTemplate(templateReplacements.get(templateId));
                        }
                        LOGGER.info("Regenerating start-/finish-situations under triggers on situation for" + step.toString());
                        if (situation.getSituationEventTriggers().size() > 0) {
                            for (SituationEventTrigger trigger : situation.getSituationEventTriggers()) {
                                //need to be able to use system->situations map
                                if (trigger.getSituation() != null)
                                    trigger.setSituation(getSituationReplacementIfExists(trigger.getSituation(),
                                            situationReplacementsOnSituationTriggers));
                            }
                        }
                    }
                }
            }
        }
    }

    public static Set<SituationStep> regenerateSituationSteps(HashMap<System, System> systems,
                                                              Set<SituationStep> situationSteps) {
        HashMap<String, HashMap<String, Situation>> situationsReplacementsOnSituationSteps =
                Collector.collectSituationsFromSystemsMap(systems);
        for (SituationStep step : situationSteps) {
            //main situation
            if (step.getSituation() != null) {
                step.setSituation(getSituationReplacementIfExists(step.getSituation(),
                        situationsReplacementsOnSituationSteps));
            }
            //end situations
            if (step.getEndSituations() != null && !(step.getEndSituations().isEmpty())) {
                HashSet<Situation> endSituationReplacements = new HashSet<>();
                for (Situation endSituation : step.getEndSituations()) {
                    if (endSituation != null) {
                        endSituationReplacements.add(getSituationReplacementIfExists(endSituation,
                                situationsReplacementsOnSituationSteps));
                    }
                }
                step.setEndSituations(endSituationReplacements);
            }
            //exc situations
            if (step.getExceptionalSituations() != null && !(step.getExceptionalSituations().isEmpty())) {
                HashSet<Situation> exceptionalSituationReplacements = new HashSet<>();
                for (Situation exceptionalSituation : step.getExceptionalSituations()) {
                    if (exceptionalSituation != null) {
                        exceptionalSituationReplacements.add(getSituationReplacementIfExists(exceptionalSituation,
                                situationsReplacementsOnSituationSteps));
                    }
                }
                step.setExceptionalSituations(exceptionalSituationReplacements);
            }
        }
        return situationSteps;
    }

    private static Situation getSituationReplacementIfExists(Situation targetSituation, HashMap<String,
            HashMap<String, Situation>> situationReplacements) {
        String systemKey = targetSituation.getParent().getParent().getID().toString();
        String replacementKey = targetSituation.getID().toString();
        if (situationReplacements.containsKey(systemKey) && situationReplacements.get(systemKey).containsKey(replacementKey))
            return situationReplacements.get(systemKey).get(replacementKey);
        return targetSituation;
    }
}
