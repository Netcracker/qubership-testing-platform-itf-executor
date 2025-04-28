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

package org.qubership.automation.itf.transport.diameter.interceptors;

import static org.qubership.automation.diameter.interceptor.InterceptorTypes.ASR;
import static org.qubership.automation.diameter.interceptor.InterceptorTypes.RAR;
import static org.qubership.automation.diameter.interceptor.InterceptorTypes.SNR;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.qubership.automation.diameter.connection.ExtraChannel;
import org.qubership.automation.diameter.interceptor.CEAInterceptor;
import org.qubership.automation.diameter.interceptor.DPRInterceptor;
import org.qubership.automation.diameter.interceptor.DWRInterceptor;
import org.qubership.automation.diameter.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.communication.message.DiameterTriggerExecutionMessage;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.DiameterExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExternalInterceptor extends Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalInterceptor.class);
    private Object transportId;
    private Object tcContextId;

    public ExternalInterceptor() {
    }

    /**
     * Create external interceptor with parameters.
     *
     * @param transportId     transport id.
     * @param tcContextId     tc context id.
     * @param interceptorType interceptor type
     */
    public ExternalInterceptor(Object transportId, Object tcContextId, String interceptorType) {
        this.transportId = transportId;
        this.tcContextId = tcContextId;
        setType(interceptorType);
    }

    protected void setTransportId(Object transportId) {
        this.transportId = transportId;
    }

    protected void setTcContextId(Object tcContextId) {
        this.tcContextId = tcContextId;
    }

    @Override
    protected boolean _onReceive(String message, ExtraChannel extraChannel) {
        boolean isApplicable = isApplicable(message);
        if (isApplicable) {
            postSituation(message);
        }
        return isApplicable;
    }

    abstract boolean isApplicable(String message);

    private void postSituation(String textMessage) {
        DiameterExecutorService.getDiameterEventProducer().produceEventDiameter(
                new DiameterTriggerExecutionMessage(new Message(textMessage), this.transportId, this.tcContextId,
                        UUID.randomUUID().toString()));
    }

    public static class InterceptorFactory {

        private static final InterceptorFactory INSTANCE = new InterceptorFactory();
        private final Map<String, Class<? extends Interceptor>> interceptorStorage = new HashMap<>();

        private InterceptorFactory() {
            interceptorStorage.put("DWA", DWRInterceptor.class);
            interceptorStorage.put("CEA", CEAInterceptor.class);
            interceptorStorage.put("CCA", CCAInterceptor.class);
            interceptorStorage.put("ASA", ASAInterceptor.class);
            interceptorStorage.put("RAA", RAAInterceptor.class);
            interceptorStorage.put("SLA", SLAInterceptor.class);
            interceptorStorage.put("STA", STAInterceptor.class);
            interceptorStorage.put("RAR", RARInterceptor.class);
            interceptorStorage.put("ASR", ASRInterceptor.class);
            interceptorStorage.put("SNR", SNRInterceptor.class);
            interceptorStorage.put("DPR", DPRInterceptor.class);
        }

        public static InterceptorFactory getInstance() {
            return INSTANCE;
        }

        /**
         * Create diameter interceptor by diameter command short name.
         *
         * @param name short name (e.g SNR, RAR, ASR).
         * @return diameter interceptor.
         */
        public Interceptor create(String name) {
            Class<? extends Interceptor> interceptorClass = interceptorStorage.computeIfAbsent(name, key -> {
                throw new IllegalArgumentException("Unknown interceptor name: " + key);
            });
            try {
                return interceptorClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to create interceptor", e);
            }
        }

        /**
         * Create diameter interceptor by diameter command short name.
         *
         * @param name            short name.
         * @param sessionId       diameter session id.
         * @param tcContextId     tc context id
         * @param transportId     transport id
         * @param deleteOnReceive true if you need to delete interceptor after receiving message.
         * @return diameter interceptor.
         */
        public Interceptor create(String name, String sessionId, Object tcContextId, Object transportId,
                                  boolean deleteOnReceive) {
            Interceptor interceptor = create(name);
            interceptor.setSessionId(sessionId);
            interceptor.setDeleteOnReceive(deleteOnReceive);
            if (RAR.equalsIgnoreCase(name)) {
                RARInterceptor rarInterceptor = (RARInterceptor) interceptor;
                rarInterceptor.setTransportId(transportId);
                rarInterceptor.setTcContextId(tcContextId);
                return rarInterceptor;
            } else if (SNR.equalsIgnoreCase(name)) {
                SNRInterceptor snrInterceptor = (SNRInterceptor) interceptor;
                snrInterceptor.setTcContextId(tcContextId);
                snrInterceptor.setTransportId(transportId);
                return snrInterceptor;
            } else if (ASR.equalsIgnoreCase(name)) {
                ASRInterceptor asrInterceptor = (ASRInterceptor) interceptor;
                asrInterceptor.setTcContextId(tcContextId);
                asrInterceptor.setTransportId(transportId);
                return asrInterceptor;
            }
            return interceptor;
        }
    }
}
