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

package org.qubership.automation.itf.ui.controls.service.diameter;

import java.math.BigInteger;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.multitenancy.core.header.CustomHeader;
import org.qubership.automation.diameter.config.DiameterParser;
import org.qubership.automation.diameter.config.DiameterParserType;
import org.qubership.automation.itf.core.message.DictionaryReloadMessage;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DiameterTransportController {

    private ExecutorToMessageBrokerSender executorToMessageBrokerSender;

    @Autowired
    public void setExecutorToMessageBrokerSender(ExecutorToMessageBrokerSender executorToMessageBrokerSender) {
        this.executorToMessageBrokerSender = executorToMessageBrokerSender;
    }

    /**
     * This controller reloads (re-read) existing diameter dictionary.
     *
     * @param transport transport id to get transport configuration (DiameterTransport - short name of transport
     *                  property in DiameterOutbound as it described in descriptor - "configPath"
     * @param parser    string name of diameter parser: Qubership Diameter or Marben.
     * @param project   project id (itf internal project id)
     * @return {@link UIResult} - notification with successfully result or with error message and cause.
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @PostMapping(value = "/forceDictionaryReload")
    @AuditAction(auditAction = "Force dictionary reload for transport id {{#transport}}, parser {{#parser}} in the "
            + "project {{#project}}/{{#projectUuid}}")
    public UIResult forceDictionaryReload(@RequestParam(value = "transport") BigInteger transport,
                                          @RequestParam(value = "parser") String parser,
                                          @RequestParam(value = "project") BigInteger project,
                                          @RequestParam(value = "projectUuid") UUID projectUuid,
                                          @RequestHeader(value = CustomHeader.X_PROJECT_ID) String tenantId) {
        String dictionaryPath = CoreObjectManager.getInstance().getManager(TransportConfiguration.class)
                .getById(transport).get("configPath");
        Class<? extends DiameterParser> parserClass = DiameterParserType.defineParserClass(parser);
        executorToMessageBrokerSender.sendMessageToSyncReloadDictionaryTopic(
                new DictionaryReloadMessage(dictionaryPath, parserClass, project), tenantId);
        return new UIResult(true,
                String.format("Diameter dictionary is successfully reloaded from path: %s\n Parser: %s",
                        dictionaryPath, parserClass.getSimpleName()));
    }
}
