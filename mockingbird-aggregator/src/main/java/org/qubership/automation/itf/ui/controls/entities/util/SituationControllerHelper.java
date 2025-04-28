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

package org.qubership.automation.itf.ui.controls.entities.util;

import java.util.Collection;

import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.Pair;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.ui.controls.entities.EventTriggerController;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIOperation;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SituationControllerHelper extends ControllerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SituationControllerHelper.class);

    public static EventTriggerSyncActivationRequest synchronizeSituations(final UIOperation uiOperation,
                                                                          Operation operation) {
        EventTriggerSyncActivationRequest activationRequest = new EventTriggerSyncActivationRequest();
        if (uiOperation.getSituations() != null && uiOperation.getSituations().isLoaded()) {
            //first - delete all missed situations in operation
            Collection<Situation> situationsToDelete = Collections2.filter(operation.getSituations(),
                    situation -> {
                        for (UISituation uiSituation : uiOperation.getSituations().getData()) {
                            if (uiSituation.getId() != null
                                    && uiSituation.getId().equals(java.util.Objects.toString(situation.getID()))) {
                                return false;
                            }
                        }
                        return true;
                    });
            for (Situation situation : situationsToDelete) {
                situation.remove();
            }
            Lists.newArrayList(situationsToDelete).forEach(operation.getSituations()::remove);
            //second - add situations which have no ids in request
            Collection<UISituation> situationsToAdd =
                    Collections2.filter(Lists.newArrayList(uiOperation.getSituations().getData()),
                            uiSituation -> Strings.isNullOrEmpty(uiSituation.getId()));
            for (UISituation uiSituation : situationsToAdd) {
                Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).create(operation);
                fillSituation(uiSituation, situation, operation);
            }
            //and at last - update all others
            Collection<UISituation> situationsToModify =
                    Collections2.filter(Lists.newArrayList(uiOperation.getSituations().getData()),
                            input -> !Strings.isNullOrEmpty(input.getId()));

            /*
                Checking is added: if situation is null due to not found by ID,
                we do NOT throw an exception but print warning and proceed to the next uiSituation.
                May be it's discussable behaviour but... if the situation has already deleted it can not be restored
                at the moment anyway
             */
            for (UISituation uiSituation : situationsToModify) {
                Situation situation =
                        CoreObjectManager.getInstance().getManager(Situation.class).getById(uiSituation.getId());
                if (situation != null) {
                    activationRequest.merge(fillSituation(uiSituation, situation, operation));
                } else {
                    LOGGER.warn("Situation not found by id {}; skipped", uiSituation.getId());
                }
            }
        }
        return activationRequest;
    }

    public static EventTriggerSyncActivationRequest fillSituation(UISituation uiSituation, Situation situation,
                                                                  Operation parent) {
        situation.setName(uiSituation.getName());
        situation.setDescription(uiSituation.getDescription());
        situation.fillLabels(uiSituation.getLabels());
        situation.setIgnoreErrors(uiSituation.isIgnoreErrors());
        if (uiSituation.getKeysToRegenerate() != null) {
            situation.getKeysToRegenerate().clear();
            for (Pair<String, String> regenerationKey : uiSituation.getKeysToRegenerate()) {
                situation.addKeyToRegenerate(regenerationKey.getKey(), regenerationKey.getValue());
            }
        }
        situation.setValidateIncoming(uiSituation.getValidateIncoming());
        situation.setBvTestcase(uiSituation.getBvTestcase());
        //RA: we should not create step if no template to send
        if (uiSituation.getTemplate() != null && !Strings.isNullOrEmpty(uiSituation.getTemplate().getId())) {
            IntegrationStep step;
            if (situation.getIntegrationStep() != null) {
                step = situation.getIntegrationStep();
            } else {
                step = CoreObjectManager.getInstance().getManager(IntegrationStep.class).create(situation,
                        IntegrationStep.TYPE);
            }
            step.setSender(parent.getParent());
            if (uiSituation.getReceiver() != null && !Strings.isNullOrEmpty(uiSituation.getReceiver().getId())) {
                String uiReceiverId = uiSituation.getReceiver().getId();
                if (step.getReceiver() == null || !uiReceiverId.equals(String.valueOf(step.getReceiver().getID()))) {
                    step.setReceiver(CoreObjectManager.getInstance().getManager(System.class).getById(uiReceiverId));
                }
            } else {
                step.setReceiver(null);
            }
            if ((parent.getMep().isOutbound() && parent.getMep().isRequest())
                    || Mep.INBOUND_REQUEST_RESPONSE_SYNCHRONOUS.equals(parent.getMep())) {
                step.setOperation(parent);
            } else {
                if (uiSituation.getOperation() != null) {
                    String uiOperationId = uiSituation.getOperation().getId();
                    if (step.getOperation() == null
                            || !uiOperationId.equals(String.valueOf(step.getOperation().getID()))) {
                        step.setOperation(
                                CoreObjectManager.getInstance().getManager(Operation.class).getById(uiOperationId));
                    }
                } else {
                    step.setOperation(null);
                }
            }
            UIObject uiTemplate = uiSituation.getTemplate();
            if (uiTemplate != null && templateNullOrDifferent(uiTemplate, step.returnStepTemplate())) {
                step.setTemplate(TemplateHelper.getById(
                        uiTemplate.getId(),
                        uiTemplate.getParent() == null ? null : uiTemplate.getParent().getClassName()));
            }
            step.setName(String.format("%s sends %s to %s with template %s",
                    ControllerHelper.getName(step.getSender()), ControllerHelper.getName(step.getOperation()),
                    ControllerHelper.getName(step.getReceiver()), ControllerHelper.getName(step.returnStepTemplate())));
            step.setDelay(uiSituation.getDelay());
            step.setUnit(uiSituation.getUnit());
        } else {
            if (situation.getIntegrationStep() != null) {
                IntegrationStep integrationStep = situation.getIntegrationStep();
                situation.getSteps().remove(integrationStep);
                integrationStep.remove();
            }
        }
        EventTriggerSyncActivationRequest activationRequest =
                EventTriggerController.synchronizeTriggers(uiSituation.getTriggers() == null
                        ? Sets.newHashSet() : Sets.newHashSet(uiSituation.getTriggers()), situation);
        if (parent.getMep().isInboundRequest()) {
            if (!situation.getOperationEventTriggers().isEmpty()) {
                (situation.getOperationEventTriggers().iterator().next()).setPriority(uiSituation.getPriority());
            }
        }
        return activationRequest;
    }

    private static boolean templateNullOrDifferent(UIObject uiTemplate,
                                                   Template<? extends TemplateProvider> stepTemplate) {
        if (stepTemplate == null) {
            return true;
        }
        return !uiTemplate.getId().equals(String.valueOf(stepTemplate.getID()));
    }
}
