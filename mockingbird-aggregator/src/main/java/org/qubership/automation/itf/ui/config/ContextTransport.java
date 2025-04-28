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

package org.qubership.automation.itf.ui.config;

import org.qubership.automation.itf.core.util.finder.TransportRegistryFinder;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.core.util.transport.registry.base.AbstractTransportRegistry;
import org.qubership.automation.itf.executor.transports.holder.TransportHolder;
import org.qubership.automation.itf.executor.transports.registry.TransportRegistryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextTransport.class);
    private AbstractTransportRegistry registry;

    public void init() {
        LOGGER.info("Start transports loading...");
        try {
            registry = new TransportRegistryLoader();
            TransportRegistryFinder.getInstance().setTransportRegistry(registry);
            TransportRegistryManager transportRegistryManager = TransportRegistryManager.getInstance();
            transportRegistryManager.init(registry);
            transportRegistryManager.registerTransports(TransportHolder.getInstance().getTransports());
            registry.setLoaded(true);
            LOGGER.info("Transports are loaded successfully.");
        } catch (Exception e) {
            LOGGER.error("Error initialing transport object manager or modules", e);
        }
    }

    public void destroyed() {
        TransportRegistryManager.getInstance().destroy();
    }
}
