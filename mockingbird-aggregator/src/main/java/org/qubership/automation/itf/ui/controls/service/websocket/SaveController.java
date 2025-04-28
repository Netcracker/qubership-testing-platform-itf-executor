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

package org.qubership.automation.itf.ui.controls.service.websocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class SaveController {

    private Logger LOGGER = LoggerFactory.getLogger(SaveController.class);

    @MessageMapping("/save")
    @SendTo("/topic/message")
    @AuditAction(auditAction = "Send message {{#message}}")
    public JSONObject sendMessage(SimpMessageHeaderAccessor messageHeaderAccessor, String message) {
        String ip = messageHeaderAccessor.getSessionAttributes().get("ip").toString();
        LOGGER.info(message + ip);
        try {
            JSONObject object = (JSONObject) new JSONParser().parse(message);
            object.put("ip", ip);
            return object;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
