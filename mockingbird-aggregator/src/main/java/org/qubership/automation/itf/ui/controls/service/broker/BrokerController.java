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

package org.qubership.automation.itf.ui.controls.service.broker;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.executor.service.SecurityHelper;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrokerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerController.class);
    private final ExecutorToMessageBrokerSender executorToMessageBrokerSender;

    @Autowired
    public BrokerController(ExecutorToMessageBrokerSender executorToMessageBrokerSender) {
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @PostMapping(value = "broker/sendMessage")
    @AuditAction(auditAction = "Send message with {{#queueType}} queueType and {{#queueNameParameterName}} "
            + "queueNameParameterName from project {{#projectUuid}} and tenantId {{#tenantId}}")
    public UIResult sendMessage(@RequestBody JSONObject message, @RequestParam UUID projectUuid,
                                @RequestParam String queueNameParameterName, @RequestParam String queueType,
                                @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        UIResult result = new UIResult();
        try {
            SecurityHelper.addAuthContextToMessage(message);
            executorToMessageBrokerSender.sendMessage(message, queueNameParameterName, queueType, tenantId);
        } catch (Exception e) {
            result.setSuccess(false);
            String errorMsg = String.format("Error: Message was not delivered to %s in ActiveMQ: %s", queueType,
                    e.getMessage());
            result.setMessage(errorMsg);
            LOGGER.error(errorMsg);
        }
        return result;
    }
}
