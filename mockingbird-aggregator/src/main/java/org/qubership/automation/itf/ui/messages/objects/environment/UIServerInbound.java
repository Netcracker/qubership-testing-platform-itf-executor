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

package org.qubership.automation.itf.ui.messages.objects.environment;

import java.rmi.RemoteException;
import java.util.Collection;

import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIInboundConfiguration;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class UIServerInbound extends UIServer {

    private static final Function<InboundTransportConfiguration, UIInboundConfiguration> TO_UI_INPUT_CONF =
            new Function<InboundTransportConfiguration, UIInboundConfiguration>() {
        @Override
        public UIInboundConfiguration apply(InboundTransportConfiguration input) {
            UIInboundConfiguration uiConfiguration = new UIInboundConfiguration(input);
            try {
                uiConfiguration.defineProperties(input);
                uiConfiguration.defineTriggers(input.getTriggerConfigurations());
            } catch (TransportException | RemoteException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return uiConfiguration;
        }
    };
    private UIObject system;
    private ImmutableList<UIInboundConfiguration> configurations;

    public UIServerInbound() {
    }

    public UIServerInbound(Server storable) {
        super(storable);
        this.url = storable.getUrl();
    }

    public UIObject getSystem() {
        return system;
    }

    public void setSystem(UIObject system) {
        this.system = system;
    }

    public ImmutableList<UIInboundConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<UIInboundConfiguration> configurations) {
        this.configurations = ImmutableList.copyOf(configurations);
    }

    public void defineSystem(System system) {
        this.system = new UIObject(system);
    }

    public void defineConfiguration(Collection<InboundTransportConfiguration> configurations) throws RemoteException {
        setConfigurations(Collections2.transform(configurations, TO_UI_INPUT_CONF));
    }
}
