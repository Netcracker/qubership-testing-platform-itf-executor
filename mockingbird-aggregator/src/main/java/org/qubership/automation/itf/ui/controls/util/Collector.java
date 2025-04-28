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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.IllegalClassException;
import org.aspectj.apache.bcel.classfile.ClassFormatException;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;

public class Collector {

    public static Set<System> collectSystemsFromSituationSteps(Collection<SituationStep> steps) {
        HashSet<System> collectedSystems = new HashSet<>();
        for (Step step : steps.stream().filter(Objects::nonNull).collect(Collectors.toSet())) {
            if (step instanceof SituationStep) {
                SituationStep situationStep = (SituationStep) step;
                if (situationStep.getSituation() != null) {
                    collectedSystems.add(situationStep.getSituation().getParent().getParent());
                }
                situationStep.getEndSituations().stream().filter(Objects::nonNull)
                        .forEach(endS -> collectedSystems.add(endS.getParent().getParent()));
                situationStep.getExceptionalSituations().stream().filter(Objects::nonNull)
                        .forEach(exS -> collectedSystems.add(exS.getParent().getParent()));
            } else if (step instanceof EmbeddedStep) {
                //do not affect embedded
            } else {
                throw new ClassFormatException("Unexpected step type: " + step.getClass().getName());
            }
        }
        return collectedSystems;
    }

    public static Set<SituationStep> collectSituationStepsFromStorables(
            Collection<? extends Storable> storables, Class<? extends Storable> clazz) throws ClassNotFoundException {
        Set steps;
        if (ChainFolder.class.equals(clazz)) {
            steps = Collector.collectSituationStepsFromCallChains(
                    Collector.collectCallChainsFromFolders((Set) storables));
        } else if (CallChain.class.equals(clazz)) {
            steps = Collector.collectSituationStepsFromCallChains((Set) storables);
        } else if (Step.class.isAssignableFrom(clazz)) {
            steps = storables.stream()
                    //remove embedded steps
                    .filter(step -> SituationStep.class.isAssignableFrom(step.getClass())).collect(Collectors.toSet());
        } else {
            throw new ClassNotFoundException("Class " + clazz.getName() + " isn't supported by reference regenerator.");
        }
        return steps;
    }

    public static Set<System> collectSystemsFromSystems(Collection<System> systems) {
        HashSet<System> collectedSystems = new HashSet<>();
        for (System system : systems) {
            for (Operation operation : system.getOperations()) {
                collectedSystems.addAll(collectSystemsFromSituations(operation.getSituations()));
            }
        }
        return collectedSystems;
    }

    /**
     * @param situations - Situations collection.
     * @return Set&lt;System&gt; collected from senders/receivers of integration step under situations.
     */
    public static Set<System> collectSystemsFromSituations(Collection<Situation> situations) {
        HashSet<System> collectedSystems = new HashSet<>();
        for (Situation situation : situations) {
            IntegrationStep step = situation.getIntegrationStep();
            if (step != null) {
                if (step.getReceiver() != null) {
                    collectedSystems.add(step.getReceiver());
                }
                if (step.getSender() != null) {
                    collectedSystems.add(step.getSender());
                }
            }
        }
        return collectedSystems;
    }

    /**
     * @param callChains - Callchains collection.
     * @return Set&lt;? extends Step&gt;, but actually set contains only Situation steps.
     */
    public static Set<? extends Step> collectSituationStepsFromCallChains(Collection<CallChain> callChains) {
        return callChains.stream().flatMap(storable -> storable.getSteps().stream())
                //removing non-SituationStep objects
                .filter(step -> (step != null && SituationStep.class.isAssignableFrom(step.getClass())))
                .collect(Collectors.toSet());
    }

    /**
     * Collect Callchains from Callchains/ChainFolders collection.
     *
     * @param storables callchains and callchain folders
     * @return Set&lt;CallChain&gt;
     */
    public static Set<CallChain> collectCallChainsFromCallChainsAndFolders(Collection<Storable> storables) {
        HashSet<CallChain> callChains = new HashSet<>();
        HashSet<ChainFolder> callChainFolders = new HashSet<>();
        for (Storable storable : storables) {
            if (storable instanceof CallChain) {
                callChains.add((CallChain) storable);
            } else if (storable instanceof ChainFolder) {
                callChainFolders.add((ChainFolder) storable);
            } else {
                throw new IllegalClassException("Unexpected class: " + storable);
            }
        }
        callChains.addAll(collectCallChainsFromFolders(callChainFolders));
        return callChains;
    }

    /**
     * Collect set of callchains from chainFolders recursively.
     *
     * @param chainFolders - chainFolders collection.
     * @return Set&lt;CallChain&gt;
     */
    public static Set<CallChain> collectCallChainsFromFolders(Set<ChainFolder> chainFolders) {
        // fill with self-steps
        Set<CallChain> collectedCallChains = chainFolders
                .stream()
                .flatMap(chainFolder -> chainFolder.getObjects().stream())
                .collect(Collectors.toSet());
        // loop over nested folders
        if (chainFolders.size() > 0) {
            collectedCallChains.addAll(collectCallChainsFromFolders((Set) chainFolders
                    .stream()
                    .flatMap(chainFolder -> chainFolder.getSubFolders().stream())
                    .collect(Collectors.toSet())));
        }
        return collectedCallChains;
    }

    /**
     * Collect Systems from Systems/SystemFolders collection.
     *
     * @param storables Systems and System folders
     * @return Set&lt;System&gt;
     */
    public static Set<System> collectSystemsFromSystemsAndFolders(Collection<Storable> storables) {
        HashSet<System> systems = new HashSet<>();
        HashSet<SystemFolder> systemFolders = new HashSet<>();
        for (Storable storable : storables) {
            if (storable instanceof System) {
                systems.add((System) storable);
            } else if (storable instanceof SystemFolder) {
                systemFolders.add((SystemFolder) storable);
            } else {
                throw new IllegalClassException("Unexpected class: " + storable);
            }
        }
        systems.addAll(collectSystemsFromFolders(systemFolders));
        return systems;
    }

    /**
     * Collect set of systems from systemFolders recursively.
     *
     * @param systemFolders - system folders collection.
     * @return Set&lt;System&gt;
     */
    public static Set<System> collectSystemsFromFolders(Set<SystemFolder> systemFolders) {
        // fill with self-steps
        Set<System> collectedSystems = systemFolders
                .stream()
                .flatMap(systemFolder -> systemFolder.getObjects().stream())
                .collect(Collectors.toSet());
        // loop over nested folders
        if (systemFolders.size() > 0) {
            collectedSystems.addAll(collectSystemsFromFolders((Set) systemFolders
                    .stream()
                    .flatMap(systemFolder -> systemFolder.getSubFolders().stream())
                    .collect(Collectors.toSet())));
        }
        return collectedSystems;
    }

    /**
     * @param systems - systems collection.
     * @return Map &lt;template.NaturalId, template&gt;
     */
    public static Map<String, Template> collectTemplatesFromSystems(Collection<System> systems) {
        HashMap<String, Template> replacements = new HashMap<>();
        for (System system : systems) {
            //templates from system
            for (Template t : system.returnTemplates()) {
                if (t.getNaturalId() != null) {
                    replacements.put(t.getNaturalId().toString(), t);
                }
            }
            //templates from operation
            for (Operation o : system.getOperations()) {
                for (Template t : o.returnTemplates()) {
                    if (t.getNaturalId() != null) {
                        replacements.put(t.getNaturalId().toString(), t);
                    }
                }
            }
        }
        return replacements;
    }

    /**
     * @param system target system
     * @return &lt;operation.NaturalId, operation&gt;
     */
    public static Map<String, Operation> collectOperationsFromSystem(System system) {
        HashMap<String, Operation> replacements = new HashMap<>();
        for (Operation o : system.getOperations()) {
            if (o.getNaturalId() != null) {
                replacements.put(o.getNaturalId().toString(), o);
            }
        }
        return replacements;
    }

    /**
     * @param systems target systems
     * @return HashMap &lt;system.NaturalId, HashMap of system&lt;situation.NaturalId, situation&gt;&gt;,
     */
    public static HashMap<String, HashMap<String, Situation>>
    collectSituationsFromSystemsMap(HashMap<System, System> systems) {
        HashMap<String, HashMap<String, Situation>> replacements = new HashMap<>();
        for (System targetSystem : systems.keySet()) {
            HashMap<String, Situation> systemReplacements = new HashMap<>();
            System replacementSystem = systems.get(targetSystem);
            if (replacementSystem.getNaturalId() != null) {
                for (Operation operation : replacementSystem.getOperations()) {
                    for (Situation situation : operation.getSituations()) {
                        if (situation.getNaturalId() != null) {
                            systemReplacements.put(situation.getNaturalId().toString(), situation);
                        }
                    }
                }
                replacements.put(targetSystem.getID().toString(), systemReplacements);
            }
        }
        return replacements;
    }
}



