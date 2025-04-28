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

package org.qubership.automation.itf.ui.controls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.manager.CoreObjectManagerService;
import org.qubership.automation.itf.ui.controls.util.Collector;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.controls.util.ReferenceRegenerator;
import org.qubership.automation.itf.ui.messages.EnumReferenceRegenerationClass;
import org.qubership.automation.itf.ui.messages.ReferenceRegenerationRequest;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.qubership.automation.itf.ui.messages.objects.request.referenceregenerator.UIReplacement;
import org.qubership.automation.itf.ui.messages.objects.request.referenceregenerator.UIReplacementTarget;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/referenceRegenerator")
public class ReferenceRegeneratorController extends ControllerHelper {

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/getCompatibleSystems", method = RequestMethod.POST)
    @AuditAction(auditAction = "Get Compatible Systems")
    public Set<UIReplacement> getCompatibleSystems(@RequestBody ReferenceRegenerationRequest request,
                                                   @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            return getCompatibleSystemsByIds(request.getIds(), request.getEntityClass());
        } catch (Exception e) {
            log.error("Error while compatible systems collecting: {}", e.getMessage(), e);
            throw new IllegalArgumentException(getTopStackTrace(e));
        }
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/regenerateReferences", method = RequestMethod.POST)
    @AuditAction(auditAction = "Regenerate References")
    public void regenerateReferences(@RequestBody UIReplacementTarget request,
                                     @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            doRegenerateOperation(request);
        } catch (Exception e) {
            log.error("Error while regenerate references processing: {}", e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /* --- reference regeneration block START --- */
    private void doRegenerateOperation(UIReplacementTarget request) throws Exception {
        Set<? extends Storable> sourceObjects = new HashSet<>(initObjects(request.getSources()));
        HashMap<System, System> systemsToReplacementMap = initializeSystemToReplacementMap(request.getTargetSystems());
        Class<? extends Storable> clazz = sourceObjects.stream().findAny().orElse(null).getClass();
        if (systemsToReplacementMap.isEmpty() || sourceObjects.isEmpty()) {
            return;
        }
        //we can not have folders on systems, so in this case we have only systems selected directly
        if (System.class.equals(clazz)) {
            ReferenceRegenerator.regenerateSystems((Set<System>) sourceObjects, systemsToReplacementMap);
        } else {
            //check for the remaining types inside method collectSituationStepsFromStorables()
            ReferenceRegenerator.regenerateSituationSteps(systemsToReplacementMap,
                    Collector.collectSituationStepsFromStorables(sourceObjects, clazz));
        }
    }

    private List<Storable> initObjects(Collection<UIObject> sources) {
        List<Storable> result = new ArrayList<>();
        CoreObjectManagerService coreObjectManager = CoreObjectManager.getInstance();
        for (UIIdentifiedObject source : sources) {
            String sourceId = source.getId();
            Class<? extends Storable> sourceClassName =
                    EnumReferenceRegenerationClass.fromValue(source.getClassName()).getEntityClass();
            result.add(coreObjectManager.getManager(sourceClassName).getById(sourceId));
        }
        return result;
    }

    private HashMap<System, System> initializeSystemToReplacementMap(Set<UIReplacement> targetSystems) {
        HashMap<System, System> result = new HashMap<>();
        ObjectManager<System> om = CoreObjectManager.getInstance().getManager(System.class);
        for (UIReplacement targetSystem : targetSystems) {
            result.put(om.getById(targetSystem.getId()), om.getById(targetSystem.getReplacement().getId()));
        }
        return result;
    }
    /* --- reference regeneration block END --- */

    /* --- compatible systems collecting block START --- */
    private Set<UIReplacement> getCompatibleSystemsByIds(String[] ids, Class<? extends Storable> clazz)
            throws ClassNotFoundException, IllegalArgumentException {
        HashSet<Storable> storables = new HashSet<>();
        for (String id : ids) {
            storables.add(CoreObjectManager.getInstance().getManager(clazz.asSubclass(Storable.class)).getById(id));
        }
        if (storables.contains(null)) {
            throw new IllegalArgumentException("Selected objects must be instances of the same class. You can't "
                    + "select folder and storable objects at once.");
        }
        if (System.class.equals(clazz)) {
            return getCompatibleSystemsForSystems(Collector.collectSystemsFromSystems((Set) storables));
        } else if (SystemFolder.class.equals(clazz)) {
            return getCompatibleSystemsForSystems(Collector.collectSystemsFromFolders((Set) storables));
        } else {
            return getCompatibleSystemsForSituationSteps(Collector.collectSituationStepsFromStorables(storables,
                    clazz));
        }
    }

    private HashSet<UIReplacement> getCompatibleSystemsForSystems(Set<System> systems) {
        HashSet<UIReplacement> uiReplacements = new HashSet<>();
        for (System system : systems) {
            UIReplacement uiReplacement = new UIReplacement(system);
            //add other systems
            getManager(System.class)
                    .getByNatureId(system.getID(), null)
                    .forEach(s -> {
                        if (!s.getID().equals(system.getID())) {
                            uiReplacement.addCompatibleSystem(s);
                        }
                    });
            uiReplacements.add(uiReplacement);
        }
        return uiReplacements;
    }

    private Set<UIReplacement> getCompatibleSystemsForSituationSteps(Set<SituationStep> steps) {
        return getCompatibleSystemsForSystems(Collector.collectSystemsFromSituationSteps(steps));
    }
    /* --- compatible systems collecting block END --- */
}
