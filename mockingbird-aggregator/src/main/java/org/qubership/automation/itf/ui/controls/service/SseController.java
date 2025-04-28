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

package org.qubership.automation.itf.ui.controls.service;

import java.io.IOException;
import java.util.Map;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SseController {

    private final Map<String, SseEmitter> sseEmitters;

    @Value("${atp-itf-executor.sse-timeout}")
    private Long sseEmitterTimeout;
    @Value("${atp-itf-executor.sse-reconnect-time}")
    private Long sseReconnectTime;

    @Autowired
    public SseController(Map<String, SseEmitter> sseEmitters) {
        this.sseEmitters = sseEmitters;
    }

    /**
     * Endpoint to create SSE-emitter.
     *
     * @param sessionId request identifier
     * @return created emitter for particular request identifier
     */
    @CrossOrigin
    @GetMapping(path = "/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuditAction(auditAction = "Handle connection for session {{#sessionId}}")
    public SseEmitter handle(@RequestParam String sessionId) throws IOException {
        SseEmitter emitter = new SseEmitter(sseEmitterTimeout);
        sseEmitters.put(sessionId, emitter);
        emitter.onCompletion(() -> {
            sseEmitters.remove(sessionId);
            log.info("Remove the completed event emitter with sessionId:{}", sessionId);
        });
        emitter.onTimeout(() -> {
            sseEmitters.remove(sessionId);
            log.info("Remove event emitter on timeout with sessionId:{}", sessionId);
        });
        emitter.onError((ex) -> log.info("SseEmitter with sessionId:{} got error:{}", sessionId, ex));
        SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
                .id(sessionId)
                .name("init")
                .data("{\"message\":\"init sse connection...\"}")
                .reconnectTime(sseReconnectTime);
        emitter.send(sseEvent);
        log.info("Connection successfully established with sessionId:{}", sessionId);
        return emitter;
    }

}
