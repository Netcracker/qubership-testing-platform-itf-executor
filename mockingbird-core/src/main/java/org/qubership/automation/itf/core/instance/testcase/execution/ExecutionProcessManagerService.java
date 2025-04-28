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

package org.qubership.automation.itf.core.instance.testcase.execution;

import org.qubership.automation.itf.core.instance.testcase.execution.holders.DefferedSituationInstanceHolder;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.NextCallChainEventSubscriberHolder;
import org.qubership.automation.itf.core.instance.testcase.execution.holders.SubscriberData;
import org.qubership.automation.itf.core.model.event.NextCallChainEvent;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.SituationInstance;
import org.qubership.automation.itf.core.model.jpa.instance.chain.CallChainInstance;
import org.qubership.automation.itf.executor.provider.EventBusProvider;
import org.qubership.automation.itf.executor.service.ExecutionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExecutionProcessManagerService {

    public static final String PAUSE_EVENT = "pause";
    public static final String RESUME_EVENT = "resume";
    public static final String TERMINATE_EVENT = "terminate";
    private static final String UPDATE_CONTEXT_EVENT = "updateContext";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionProcessManagerService.class);
    private final EventBusProvider eventBusProvider;

    /*  To change state of the context to Paused and to post Pause event to callChainEventSubscriber.
     *   To be paused, the context must be:
     *       1. In the 'IN PROGRESS' state (currently it's checked before an invoking of the method)
     *       2. Initiated by a callchain (currently it's checked before an invoking of the method)
     * */
    public void pause(TcContext tcContext) {
        ExecutionServices.getTCContextService().pause(tcContext);
    }

    public void updateContext(TcContext tcContext) {
        SubscriberData resumingData = NextCallChainEventSubscriberHolder.getInstance()
                .getSubscriberData(tcContext.getID());
        if (resumingData != null) {
            ExecutionServices.getTCContextService().updateContext(tcContext);
            eventBusProvider.post(createEvent(UPDATE_CONTEXT_EVENT, resumingData,
                    (CallChainInstance) tcContext.getInitiator()));
        }
    }

    @Transactional
    public void resume(TcContext tcContext) {
        SituationInstance situationInstance = DefferedSituationInstanceHolder.getInstance().get(tcContext.getID());
        if (situationInstance == null) {
            fail(tcContext);
            LOGGER.error("Error occurred while resuming the {} context: there is no situation instance to resume in "
                    + "the context", tcContext.getName());
        } else {
            executeDeferredSituationInstance(situationInstance, tcContext);
        }
    }

    public void finish(TcContext tcContext) {
        DefferedSituationInstanceHolder.getInstance().remove(tcContext.getID());
        ExecutionServices.getTCContextService().finish(tcContext);
        NextCallChainEventSubscriberHolder.getInstance().remove(tcContext.getID());
    }

    public void fail(TcContext tcContext) {
        ExecutionServices.getTCContextService().fail(tcContext);
        NextCallChainEventSubscriberHolder.getInstance().remove(tcContext.getID());
    }

    public void failByTimeout(TcContext tcContext) {
        ExecutionServices.getTCContextService().failByTimeout(tcContext);
        NextCallChainEventSubscriberHolder.getInstance().remove(tcContext.getID());
    }

    private void executeDeferredSituationInstance(SituationInstance situationInstance, TcContext tcContext) {
        SubscriberData resumingData = NextCallChainEventSubscriberHolder.getInstance()
                .getSubscriberData(tcContext.getID());
        ExecutionServices.getSituationExecutorService().executeInstance(situationInstance, null, null,
                createEvent(RESUME_EVENT, resumingData, (CallChainInstance) tcContext.getInitiator()));
    }

    private NextCallChainEvent createEvent(String eventType, SubscriberData resumingData,
                                           CallChainInstance callChainInstance) {
        NextCallChainEvent event = null;
        switch (eventType) {
            case PAUSE_EVENT:
                event = new NextCallChainEvent.Pause(resumingData.getParentSubscriberId(), callChainInstance);
                break;
            case RESUME_EVENT:
                event = !resumingData.isNeedToContinue()
                        ? new NextCallChainEvent.ResumeWithoutContinue(resumingData.getParentSubscriberId(),
                        callChainInstance)
                        : new NextCallChainEvent.Resume(resumingData.getParentSubscriberId(), callChainInstance);
                break;
            case UPDATE_CONTEXT_EVENT:
                event = new NextCallChainEvent.UpdateContext(resumingData.getParentSubscriberId(), callChainInstance);
                eventBusProvider.post(event);
            default:
        }
        if (event != null) {
            event.setID(resumingData.getSubscriberId());
        }
        return event;
    }
}
