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

package org.qubership.automation.itf.integration;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.communication.EventTriggerBriefInfo;
import org.qubership.automation.itf.core.model.communication.StubUser;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerBulkActivationRequest;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSingleActivationRequest;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerStateResponse;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.exception.TriggerException;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.event.trigger.EventTriggerSwitcherFactory;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.ui.util.EventTriggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class EventTriggerActivationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventTriggerActivationService.class);
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;

    @Autowired
    public EventTriggerActivationService(ExecutorToMessageBrokerSender executorToMessageBrokerSender) {
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
    }

    public void switchTriggerState(EventTriggerSingleActivationRequest eventTriggerSingleActivationRequest,
                                   String tenantId, Boolean turnOn, boolean forceActivate) {
        executorToMessageBrokerSender.sendMessageExecutorConfiguratorEventTriggersTopic(
                doAction(eventTriggerSingleActivationRequest.getUser(),
                        Collections.singletonList(eventTriggerSingleActivationRequest.getTrigger()),
                        eventTriggerSingleActivationRequest.getSessionId(), false, turnOn, false, forceActivate),
                tenantId);
    }

    public EventTriggerStateResponse switchTriggerState(List<BigInteger> triggerIds, String triggerType,
                                                        String sessionId) {
        EventTriggerStateResponse response = new EventTriggerStateResponse(new HashMap<>(), "", new StubUser(),
                sessionId);
        StringBuilder sb = new StringBuilder();
        for (BigInteger triggerId : triggerIds) {
            MutablePair<TriggerState, String> result = switchTrigger(triggerId, triggerType, false, null,
                    false, false);
            response.getStates().put(triggerId, result.getKey());
            if (!result.getValue().isEmpty()) {
                sb.append("For trigger id = ").append(triggerId).append(" error message: ").append(result.getValue())
                        .append('\n');
            }
        }
        response.setErrorMessage(sb.toString());
        return response;
    }

    public void switchTriggersState(EventTriggerBulkActivationRequest eventTriggerBulkActivationRequest,
                                    String tenantId) {
        executorToMessageBrokerSender.sendMessageExecutorConfiguratorEventTriggersTopic(
                doAction(eventTriggerBulkActivationRequest.getUser(),
                        eventTriggerBulkActivationRequest.getEventTriggers(),
                        eventTriggerBulkActivationRequest.getSessionId(), false,
                        eventTriggerBulkActivationRequest.isTurnOn(), false, false), tenantId);
    }

    public void syncTriggersState(EventTriggerSyncActivationRequest eventTriggerSyncActivationRequest,
                                  String tenantId, boolean deleteAfterDeactivation, boolean forceDeactivate) {
        EventTriggerStateResponse deactivationResult = doAction(eventTriggerSyncActivationRequest.getUser(),
                eventTriggerSyncActivationRequest.getTriggersToDeactivate(),
                eventTriggerSyncActivationRequest.getSessionId(), false, null, deleteAfterDeactivation,
                forceDeactivate);
        EventTriggerStateResponse reactivationResult = doAction(eventTriggerSyncActivationRequest.getUser(),
                eventTriggerSyncActivationRequest.getTriggersToReactivate(),
                eventTriggerSyncActivationRequest.getSessionId(), true, null, deleteAfterDeactivation, false);
        //merge two results into one 'deactivationResult'
        deactivationResult.getStates().putAll(reactivationResult.getStates());
        deactivationResult.setErrorMessage(deactivationResult.getErrorMessage() + reactivationResult.getErrorMessage());
        executorToMessageBrokerSender.sendMessageExecutorConfiguratorEventTriggersTopic(deactivationResult, tenantId);
    }

    private EventTriggerStateResponse doAction(StubUser user,
                                               List<EventTriggerBriefInfo> triggers,
                                               String sessionID,
                                               boolean reset,
                                               Boolean turnOn,
                                               boolean deleteAfterDeactivation,
                                               boolean force) {
        EventTriggerStateResponse response = new EventTriggerStateResponse(new HashMap<>(), "", user, sessionID);
        StringBuilder sb = new StringBuilder();
        for (EventTriggerBriefInfo item : triggers) {
            MutablePair<TriggerState, String> result = switchTrigger(item.getId(), item.getType(), reset, turnOn,
                    deleteAfterDeactivation, force);
            response.getStates().put(item.getId(), result.getKey());
            if (!result.getValue().isEmpty()) {
                sb.append("For trigger id = ").append(item.getId()).append(" error message: ").append(result.getValue())
                        .append('\n');
            }
        }
        response.setErrorMessage(sb.toString());
        return response;
    }

    /**
     * Switches trigger state based on its current state.
     *
     * @param triggerId id of trigger to be switched
     * @param reset     if <b>true</b> trigger will be deactivated and after that activated
     * @param turnOn    <li>if <b>true</b> trigger will be activated only if it is not active yet
     *                  <li>if <b>false</b> trigger will be deactivated only if it is active
     *                  <li>if <b>null</b> trigger state will be changed to opposite
     * @return Pair with TriggerState and error message if some error was occurred during switching
     */
    private MutablePair<TriggerState, String> switchTrigger(BigInteger triggerId, String triggerType, boolean reset,
                                                            Boolean turnOn, boolean deleteAfterDeactivation,
                                                            boolean force) {
        TriggerState state;
        String message = "";
        EventTrigger trigger = EventTriggerHelper.getByIdAndType(triggerId, triggerType);
        Exception exception = null;
        boolean changed = false;
        String triggerIdentity;
        if (trigger != null) {
            triggerIdentity = (StringUtils.isBlank(trigger.getName())
                    ? "[id=" + triggerId + "]"
                    : trigger.getName());
            if (reset) {
                LOGGER.info("Reactivating trigger {}...", triggerIdentity);
                try {
                    EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType()).apply(trigger);
                    changed = true;
                } catch (TriggerException e) {
                    LOGGER.warn("Error while reactivating trigger {}", triggerIdentity, e);
                    exception = e;
                }
                if (changed) {
                    LOGGER.info("Trigger {} reactivated", triggerIdentity);
                }
            } else {
                boolean deactivationRequired = (trigger.getState().isOn() || force) && (turnOn == null || !turnOn);
                if (deactivationRequired || deleteAfterDeactivation) {
                    if (deactivationRequired) {
                        LOGGER.info("Deactivating trigger {}...", triggerIdentity);
                        try {
                            EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType())
                                    .deactivate(trigger);
                            changed = true;
                        } catch (TriggerException e) {
                            LOGGER.warn("Error while deactivating trigger {}", triggerIdentity, e);
                            exception = e;
                        }
                        if (changed) {
                            LOGGER.info("Trigger {} deactivated", triggerIdentity);
                        }
                    }
                    if (deleteAfterDeactivation && (changed || !deactivationRequired)) {
                        trigger.remove();
                    }
                } else if ((!trigger.getState().isOn() || force) && (turnOn == null || turnOn)) {
                    if (trigger.getParent() instanceof Situation && trigger instanceof OperationEventTrigger) {
                        Situation situation = (Situation) trigger.getParent();
                        if (conditionsParametersAreEmpty(situation.getOperationEventTriggers().iterator().next())) {
                            Storable alreadyActiveSituation = findAnotherActiveSituation(situation);
                            if (alreadyActiveSituation != null) {
                                message = String.format("Situation [%s] can't be activated because there is "
                                                + "another active [%s] situation without conditions.",
                                        situation.getName(),
                                        alreadyActiveSituation.getName());
                                LOGGER.warn(message);
                                //For case when trigger was activated after restoring from history
                                if (force && trigger.getState().isOn()) {
                                    trigger.setState(TriggerState.ERROR);
                                    trigger.setException(new TriggerException(message));
                                    CoreObjectManager.getInstance()
                                            .getManager(OperationEventTrigger.class).store(trigger);
                                }
                                return new MutablePair<>(trigger.getState(), message);
                            }
                        }
                    }
                    LOGGER.info("Activating trigger {}...", triggerIdentity);
                    try {
                        EventTriggerSwitcherFactory.getSwitcherByEventTriggerType(trigger.getType()).activate(trigger);
                        changed = true;
                    } catch (TriggerException e) {
                        LOGGER.warn("Error while activating trigger {}", triggerIdentity, e);
                        exception = e;
                    }
                    if (changed) {
                        LOGGER.info("Trigger {} activated", triggerIdentity);
                    }
                }
            }
            if (trigger instanceof SituationEventTrigger) {
                if (!deleteAfterDeactivation) {
                    CoreObjectManager.getInstance().getManager(SituationEventTrigger.class).store(trigger);
                }
            } else {
                CoreObjectManager.getInstance().getManager(OperationEventTrigger.class).store(trigger);
            }
            state = trigger.getState();
            if (!changed && exception != null) {
                message = (exception.getCause() != null)
                        ? exception.getCause().getLocalizedMessage()
                        : ExceptionUtils.getStackTrace(exception);
            }
        } else {
            message = "Event trigger does not exist.";
            state = TriggerState.EMPTY;
            LOGGER.warn(message);
        }
        return new MutablePair<>(state, message);
    }

    private boolean conditionsParametersAreEmpty(EventTrigger trigger) {
        return trigger.getConditionParameters() == null || trigger.getConditionParameters().isEmpty();
    }

    private Storable findAnotherActiveSituation(Situation activatedSituation) {
        Operation operation = CoreObjectManager.getInstance().getManager(Operation.class)
                .getById(activatedSituation.getParent().getID());
        if (operation != null) {
            for (Situation situation : operation.getSituations()) {
                if (!situation.getID().equals(activatedSituation.getID())
                        && activeTriggerWithoutConditions(situation)) {
                    return situation;
                }
            }
            return null;
        }
        return null;
    }

    private boolean activeTriggerWithoutConditions(Situation situation) {
        EventTrigger trigger = situation.getOperationEventTriggers().iterator().next();
        return TriggerState.ACTIVE.equals(trigger.getState()) && conditionsParametersAreEmpty(trigger);
    }
}
