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

import java.math.BigInteger;

import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.report.RunSubscriberInterface;
import org.qubership.automation.itf.core.util.manager.MonitorManager;
import org.qubership.automation.itf.executor.cache.service.CacheServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class RunSubscriber implements RunSubscriberInterface {

    public static final String CLIENT_IP = "clientIP";
    private Logger LOGGER = LoggerFactory.getLogger(RunSubscriber.class);
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/notify")
    public void receive(@Payload ClientDataMessage message) {
        try {
            BigInteger tcContextId = new BigInteger(message.getContextId());
            TcContext tcContext = CacheServices.getTcContextCacheService().getById(tcContextId);
            tcContext.put(CLIENT_IP, message.getClientIP());
            tcContext.store();
            notifyObject(message.getContextId());
        } catch (Exception e) {
            LOGGER.error("Error occurred while receive message", e);
            notifyObject(message.getContextId());
        }
    }

    public void send(@Payload String message, @Payload String clientIP) {
        message = "{\"message\"" + ":" + "\"" + message + "\", \"ip\"" + ":" + "\"" + clientIP + "\"}";
        this.simpMessagingTemplate.convertAndSend("/topic/notify", message);
    }

    private void notifyObject(String id) {
        Object object = MonitorManager.getInstance().get(id);
        synchronized (object) {
            object.notify();
        }
    }
}
