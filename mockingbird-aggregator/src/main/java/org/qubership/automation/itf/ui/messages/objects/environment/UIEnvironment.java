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

import java.util.List;
import java.util.Map;

import org.qubership.automation.itf.core.model.eci.EciConfigurable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.constants.TriggerState;
import org.qubership.automation.itf.core.util.manager.TriggerStateManager;
import org.qubership.automation.itf.ui.controls.entities.environment.EnvironmentController;
import org.qubership.automation.itf.ui.messages.objects.UIECIObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class UIEnvironment extends UIECIObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIEnvironment.class);
    private ImmutableList<UIEnvironmentItem> outbound;
    private ImmutableList<UIEnvironmentItem> inbound;
    private ImmutableList<UIConfiguration> reportLinkCollectors;
    private String inboundState;

    public UIEnvironment() {
    }

    public UIEnvironment(EciConfigurable storable) {
        super(storable);
    }

    public UIEnvironment(Environment environment) {
        super(environment);
        this.setOutbound(convertTransportConfigToUI(environment, environment.getOutbound()));
        this.setInbound(convertTransportConfigToUI(environment, environment.getInbound()));
        TriggerState triggerState = environment.getEnvironmentState();
        if (triggerState == null) {
            triggerState = TriggerStateManager.getInstance().getInboundTriggersState(environment);
        }
        this.setInboundState(triggerState.toString());
    }

    private static UIEnvironmentItem createItem(Map.Entry<System, Server> entry) {
        UIEnvironmentItem item = new UIEnvironmentItem();
        System system = entry.getKey();
        if (system != null) {
            item.defineSystem(system);
        }
        Server server = entry.getValue();
        if (server != null) {
            item.defineServer(server);
        }
        return item;
    }

    public ImmutableList<UIEnvironmentItem> getOutbound() {
        return outbound;
    }

    public void setOutbound(List<UIEnvironmentItem> outbound) {
        this.outbound = ImmutableList.copyOf(outbound);
    }

    public ImmutableList<UIEnvironmentItem> getInbound() {
        return inbound;
    }

    public void setInbound(List<UIEnvironmentItem> inbound) {
        this.inbound = ImmutableList.copyOf(inbound);
    }

    public ImmutableList<UIConfiguration> getReportLinkCollectors() {
        return reportLinkCollectors;
    }

    public void setReportLinkCollectors(List<UIConfiguration> reportLinkCollectors) {
        this.reportLinkCollectors = ImmutableList.copyOf(reportLinkCollectors);
    }

    public String getInboundState() {
        return inboundState;
    }

    public void setInboundState(String inboundState) {
        this.inboundState = inboundState;
    }

    private List<UIEnvironmentItem> convertTransportConfigToUI(Environment environment,
                                                               Map<System, Server> systemServerMap) {
        List<UIEnvironmentItem> inbound = Lists.newArrayList();
        for (Map.Entry<System, Server> entryMap : systemServerMap.entrySet()) {
            try {
                inbound.add(createItem(entryMap));
            } catch (Exception e) {
                LoggerFactory.getLogger(EnvironmentController.class).error("Failed loading environment {}",
                        environment.getName());
            }
        }
        return inbound;
    }
}
