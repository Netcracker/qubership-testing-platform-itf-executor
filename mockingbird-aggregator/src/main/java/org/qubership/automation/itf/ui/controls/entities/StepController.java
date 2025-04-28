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

package org.qubership.automation.itf.ui.controls.entities;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.exceptions.configuration.ConfigurationException;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.exception.StorageException;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIIds;
import org.qubership.automation.itf.ui.messages.UIListImpl;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIStep;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

@RestController
public class StepController extends ControllerHelper {

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/step/all", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get all Steps under parent id {{#parent}} in the project {{#projectUuid}}")
    public UIListImpl getList(
            @RequestParam(value = "parent", defaultValue = "0") String parent,
            @RequestParam(value = "isFull", defaultValue = "true") boolean isFull,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        Collection<Step> stepList = getSteps(parent);
        if (!isFull) {
            return getObjectList(stepList);
        } else {
            UIListImpl<UIStep> uiStepUIList = new UIListImpl();
            Set<UIStep> uiSteps = Sets.newHashSet();
            for (Step entry : stepList) {
                uiSteps.add(new UIStep(entry));
            }
            uiStepUIList.setObjects(uiSteps);
            return uiStepUIList;
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/step", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get Step by id {{#id}} in the project {{#projectUuid}}")
    public UIStep get(@RequestParam(value = "id", defaultValue = "0") String id,
                      @RequestParam(value = "projectUuid") UUID projectUuid) {
        Step step = CoreObjectManager.getInstance().getManager(Step.class).getById(id);
        throwExceptionIfNull(step, "", id, Step.class, "open step");
        return new UIStep(step);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"CREATE\")")
    @RequestMapping(value = "/step", method = RequestMethod.POST)
    @AuditAction(auditAction = "Create Step with name {{#name}} under parent id {{#id}} in the project "
            + "{{#projectUuid}}")
    public UIObject add(@RequestParam(value = "parent") String id,
                        @RequestBody String name,
                        @RequestParam(value = "projectUuid") UUID projectUuid) {
        AbstractStorable parent = CoreObjectManager.getInstance().getManager(Situation.class).getById(id);
        if (parent != null) {
            checkStepCount(parent);
            return addStepToParent(parent, name);
        }
        return null;
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/step", method = RequestMethod.PUT)
    @AuditAction(auditAction = "Update Steps under parent id {{id}} in the project {{#projectUuid}}")
    public void put(
            @RequestParam(value = "parent", defaultValue = "0") String id,
            @RequestBody List<UIStep> steps,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        saveSteps(steps, id);
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"DELETE\")")
    @RequestMapping(value = "/step", method = RequestMethod.DELETE)
    @AuditAction(auditAction = "Delete Steps from parent id {{#id}} in the project {{#projectUuid}}")
    public void delete(
            @RequestParam(value = "parent") String id,
            @RequestBody UIIds uiDeleteObjectReq,
            @RequestParam(value = "projectUuid") UUID projectUuid) {
        AbstractStorable holder = CoreObjectManager.getInstance().getManager(Situation.class).getById(id);
        throwExceptionIfNull(holder, null, id, Situation.class, "delete step(s)");
        deleteSteps(holder, uiDeleteObjectReq.getIds());
        holder.store();
    }

    private void checkStepCount(AbstractStorable entity) {
        if (entity != null) {
            if (entity instanceof Situation) {
                if (((Situation) entity).getSteps() != null) {
                    if (((Situation) entity).getSteps().size() == 2) {
                        throw new ConfigurationException("The number of steps in SITUATION can not be more than 2.");
                    }
                }
            }
        }
    }

    private UIObject addStepToParent(AbstractStorable parent, String name) {
        IntegrationStep step = CoreObjectManager.getInstance().getManager(IntegrationStep.class).create(parent,
                name, IntegrationStep.TYPE);
        UIObject uiObject = new UIObject();
        step.setName(name);
        step.setEnabled(true);
        uiObject.defineObjectParam(step);
        return uiObject;
    }

    private void deleteSteps(AbstractStorable parent, String[] ids) throws StorageException {
        Iterator<Step> stepIterator = null;
        if (parent instanceof Situation) {
            stepIterator = ((Situation) parent).getSteps().iterator();
        }
        for (String entry : ids) {
            while (stepIterator.hasNext()) {
                Step tmp = stepIterator.next();
                if (tmp.getID().toString().equals(entry)) {
                    tmp.remove();
                    stepIterator.remove();
                }
            }
        }
    }

    private List<Step> getSteps(String parentId) throws ObjectNotFoundException {
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(parentId);
        throwExceptionIfNull(situation, null, parentId, Situation.class, "get steps under Situation");
        return situation.getSteps();
    }
}
