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

package org.qubership.automation.itf.ui.messages.objects.eventtrigger;

import java.util.List;
import java.util.stream.Collectors;

import org.qubership.automation.itf.core.model.communication.StubUser;
import org.qubership.automation.itf.core.model.communication.message.EventTriggerSyncActivationRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UIEventTriggerSyncActivationRequest {
    private List<UIEventTriggerBriefInfo> triggersToDeactivate;
    private List<UIEventTriggerBriefInfo> triggersToReactivate;
    private StubUser user;
    private String sessionId;

    public UIEventTriggerSyncActivationRequest(EventTriggerSyncActivationRequest eventTriggerSyncActivationRequest) {
        this.triggersToDeactivate = eventTriggerSyncActivationRequest.getTriggersToDeactivate().stream()
                .map(item -> new UIEventTriggerBriefInfo(item)).collect(Collectors.toList());
        this.triggersToReactivate = eventTriggerSyncActivationRequest.getTriggersToReactivate().stream()
                .map(item -> new UIEventTriggerBriefInfo(item)).collect(Collectors.toList());
        this.user = eventTriggerSyncActivationRequest.getUser();
        this.sessionId = eventTriggerSyncActivationRequest.getSessionId();
    }
}
