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

package org.qubership.automation.itf.ui.messages.objects.transport;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.model.jpa.environment.InboundTransportConfiguration;
import org.qubership.automation.itf.core.model.jpa.environment.TriggerConfiguration;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class UIInboundConfiguration extends UIConfiguration {

    private static final String DUMB_ID = "from " + UIInboundConfiguration.class.getSimpleName();
    private static final Function<TriggerConfiguration, UITriggerConfiguration> TO_UI_TRIGGER_CONF =
            new Function<TriggerConfiguration, UITriggerConfiguration>() {
                @Override
                public UITriggerConfiguration apply(TriggerConfiguration input) {
                    UITriggerConfiguration uiTriggerConfiguration = new UITriggerConfiguration(input);
                    try {
                        uiTriggerConfiguration.defineProperties(input);
                    } catch (RemoteException | TransportException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                    uiTriggerConfiguration.setState(input.getState().toString());
                    uiTriggerConfiguration.setError(input.getActivationErrorMessage());
                    return uiTriggerConfiguration;
                }
            };
    private ImmutableList<UITriggerConfiguration> triggers;
    private UIObject transport;
    private UITriggerConfiguration etalonTrigger;

    public UIInboundConfiguration() {
    }

    public UIInboundConfiguration(InboundTransportConfiguration configuration) {
        super(configuration);
        transport = new UIObject(configuration.getReferencedConfiguration());
        try {
            TriggerConfiguration someTrigger = new TriggerConfiguration(configuration);
            someTrigger.setID(DUMB_ID);
            etalonTrigger = new UITriggerConfiguration(someTrigger);
            etalonTrigger.defineProperties(someTrigger);
        } catch (RemoteException | TransportException ignored) {
        }
    }

    @Nonnull
    static List<UIProperty> define(Collection<PropertyDescriptor> descriptors, Configuration parent,
                                   Configuration configuration) throws RemoteException {
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(descriptors.size());
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.isForServer()) {
                if (!Strings.isNullOrEmpty(configuration.get(descriptor.getShortName()))) {
                    UIProperty uiProperty = new UIProperty(descriptor, configuration.get(descriptor.getShortName()));
                    uiProperty.setOverridden("Overridden");
                    uiProperty.setInheritedValue(parent.get(descriptor.getShortName()));
                    uiProperties.add(uiProperty);
                } else {
                    UIProperty uiProperty = new UIProperty(descriptor, parent.get(descriptor.getShortName()));
                    uiProperty.setOverridden("Inherited");
                    uiProperties.add(uiProperty);
                }
            }
        }
        return uiProperties;
    }

    public ImmutableList<UITriggerConfiguration> getTriggers() {
        return triggers;
    }

    public void setTriggers(Collection<UITriggerConfiguration> triggers) {
        this.triggers = ImmutableList.copyOf(triggers);
    }

    @SuppressWarnings("Duplicates")
    public boolean defineProperties(InboundTransportConfiguration inboundTransport) throws RemoteException,
            TransportException {
        Map<String, PropertyDescriptor> transportParameters =
                TransportRegistryManager.getInstance().getProperties(inboundTransport.getTypeName());
        //Properties
        List<UIProperty> uiProperties;
        boolean transportDeployed = transportParameters != null;
        if (transportDeployed) {
            uiProperties = define(transportParameters.values(), inboundTransport.getReferencedConfiguration(),
                    inboundTransport);
        } else {
            uiProperties = defineNonDeployed(inboundTransport);
        }
        this.setProperties(uiProperties);
        return transportDeployed;
    }

    public void defineTriggers(Collection<TriggerConfiguration> triggerConfigurations) throws RemoteException {
        setTriggers(Collections2.transform(triggerConfigurations, TO_UI_TRIGGER_CONF));
    }

    public UIObject getTransport() {
        return transport;
    }

    public void setTransport(UIObject transport) {
        this.transport = transport;
    }

    public UITriggerConfiguration getEtalonTrigger() {
        return etalonTrigger;
    }

    public void setEtalonTrigger(UITriggerConfiguration etalonTrigger) {
        this.etalonTrigger = etalonTrigger;
    }
}
