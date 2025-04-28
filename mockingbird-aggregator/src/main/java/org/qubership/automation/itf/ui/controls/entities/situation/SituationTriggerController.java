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

package org.qubership.automation.itf.ui.controls.entities.situation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ByProject;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerConstants;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.qubership.automation.itf.ui.messages.objects.UITriggerRelation;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UIEventTrigger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RestController
public class SituationTriggerController {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/downstream/start", method = RequestMethod.GET)
    public UIObjectList getDownstreamSituationsOnStart(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        return getDownstreamSituations(id, SituationEventTrigger.On.START, projectId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/downstream/finish", method = RequestMethod.GET)
    public UIObjectList getDownstreamSituationsOnFinish(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectId") BigInteger projectId,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        return getDownstreamSituations(id, SituationEventTrigger.On.FINISH, projectId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.automation.itf.ui.util.UserManagementEntities).SITUATION.getName(),"
            + "#projectUuid, 'READ')")
    @RequestMapping(value = "/situation/triggers", method = RequestMethod.GET)
    public List<UITriggerRelation> getTriggers(
            @RequestParam(value = "id", defaultValue = "0") String id,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        //TODO Always returns empty list. Is this method useful or can be deleted?
        return new ArrayList<>();
    }

    private UIObjectList getDownstreamSituations(String id, SituationEventTrigger.On on, BigInteger projectId) {
        Set<Situation> downstreamSituations = Sets.newHashSetWithExpectedSize(20);
        // TODO: Replace getAllByProject() and following filtering with native query performing the same
        Collection<? extends Situation> all = CoreObjectManager.getInstance().getSpecialManager(Situation.class,
                ByProject.class).getAllByProject(projectId);
        for (Situation situation : all) {
            for (SituationEventTrigger trigger : situation.getSituationEventTriggers()) {
                Situation triggerSituation = trigger.getSituation();
                SituationEventTrigger.On triggerOn = trigger.getOn();
                if (triggerSituation != null && triggerSituation.getID().toString().equals(id) && triggerOn != null
                        && triggerOn.equals(on)) {
                    downstreamSituations.add(situation);
                }
            }
        }
        UIObjectList uiObjectList = new UIObjectList();
        for (Situation downstreamSituation : downstreamSituations) {
            UISituation uiSituation = new UISituation(downstreamSituation);
            //and cleanup triggers non-connected with this situation
            List<UIEventTrigger> triggersToStay = Lists.newArrayListWithCapacity(uiSituation.getTriggers().size());
            for (UIEventTrigger trigger : uiSituation.getTriggers()) {
                if (ControllerConstants.SITUATION_EVENT_TRIGGER_TYPE.getStringValue().equals(trigger.getType())
                        && trigger.getListen() != null && trigger.getListen().getId() != null
                        && trigger.getListen().getId().equals(id)) {
                    triggersToStay.add(trigger);
                }
            }
            uiSituation.setTriggers(triggersToStay);
            uiObjectList.addObject(uiSituation);
        }
        return uiObjectList;
    }
}
