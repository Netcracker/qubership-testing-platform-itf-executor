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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.OutboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.integration.ec.UIECIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class UIServerOutbound extends UIServer {

    private static final String INPUT_TYPE_REFERENCE = "reference";
    private static final Function<OutboundTransportConfiguration,
            UIECIConfiguration<OutboundTransportConfiguration>> TO_UI_OUTBOUND_CONF
            = new Function<OutboundTransportConfiguration, UIECIConfiguration<OutboundTransportConfiguration>>() {
        @Override
        public UIECIConfiguration<OutboundTransportConfiguration> apply(OutboundTransportConfiguration input) {
            UIECIConfiguration<OutboundTransportConfiguration> uiConfiguration = new UIECIConfiguration<>(input);
            uiConfiguration.fillUserTypeNameFromConfiguration(input);
            uiConfiguration.setEcId((input).getEcId());
            List<UIProperty> uiProperties;
            try {
                Map<String, PropertyDescriptor> transportParameters =
                        TransportRegistryManager.getInstance().getProperties(input.getTypeName());
                //Properties
                boolean transportDeployed = transportParameters != null;
                if (transportDeployed) {
                    uiProperties = Lists.newArrayListWithExpectedSize(transportParameters.size());
                    for (PropertyDescriptor descriptor : transportParameters.values()) {
                        if (descriptor.isForServer()) {
                            UIProperty uiProperty = new UIProperty(descriptor, input.get(descriptor.getShortName()));
                            if (INPUT_TYPE_REFERENCE.equals(uiProperty.getInputType())
                                    && !StringUtils.EMPTY.equals(uiProperty.getValue())) {
                                String id = uiProperty.getValue();
                                if (id == null) {
                                    uiProperty.setReferenceValue(null);
                                } else {
                                    Storable storable;
                                    Class<?> propertyType = Class.forName(uiProperty.getReferenceClass());
                                    if ("Template".equals(propertyType.getSimpleName())) {
                                        storable = TemplateHelper.getById(id);
                                    } else {
                                        storable = CoreObjectManager.getInstance()
                                                .getManager(propertyType.asSubclass(Storable.class)).getById(id);
                                    }
                                    if (storable == null) {
                                        uiProperty.setReferenceValue(null);
                                    } else {
                                        uiProperty.setReferenceValue(new UIObject(storable));
                                    }
                                }
                            }
                            uiProperties.add(uiProperty);
                        }
                    }
                } else {
                    uiProperties = UIConfiguration.defineNonDeployed(input);
                }
            } catch (Exception e) {
                uiProperties = UIConfiguration.defineNonDeployed(input);
            }
            uiConfiguration.setProperties(uiProperties);
            return uiConfiguration;
        }
    };
    private UIObject system;
    private ImmutableList<UIConfiguration> configurations;
    private String url;

    public UIServerOutbound() {
    }

    public UIServerOutbound(Server storable) {
        super(storable);
        this.url = storable.getUrl();
    }

    public UIObject getSystem() {
        return system;
    }

    public void setSystem(UIObject system) {
        this.system = system;
    }

    public ImmutableList<UIConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<UIECIConfiguration<OutboundTransportConfiguration>> configurations) {
        this.configurations = ImmutableList.copyOf(configurations);
    }

    public void defineSystem(System system) {
        this.system = new UIObject(system);
    }

    public void defineConfiguration(Collection<OutboundTransportConfiguration> configurations) {
        setConfigurations(Collections2.transform(configurations, TO_UI_OUTBOUND_CONF));
    }
}
