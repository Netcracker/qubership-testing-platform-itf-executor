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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class UITriggerConfiguration extends UIConfiguration {

    private String state;
    private String error;

    public UITriggerConfiguration() {
    }

    public UITriggerConfiguration(Configuration configuration) {
        super(configuration);
    }

    @Nonnull
    static List<UIProperty> define(@Nonnull
                                   Collection<PropertyDescriptor> descriptors,
                                   InboundTransportConfiguration parent, TriggerConfiguration configuration) {
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(descriptors.size());
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.isForServer()) {
                if (!Strings.isNullOrEmpty(configuration.get(descriptor.getShortName()))) {
                    UIProperty uiProperty = new UIProperty(descriptor, configuration.get(descriptor.getShortName()));
                    uiProperty.setOverridden("Overridden");
                    if (Strings.isNullOrEmpty(parent.get(descriptor.getShortName()))) {
                        uiProperty.setInheritedValue(
                                parent.getReferencedConfiguration().get(descriptor.getShortName()));
                    } else {
                        uiProperty.setInheritedValue(parent.get(descriptor.getShortName()));
                    }
                    uiProperties.add(uiProperty);
                } else {
                    UIProperty uiProperty = Strings.isNullOrEmpty(parent.get(descriptor.getShortName()))
                            ? new UIProperty(descriptor,
                            parent.getReferencedConfiguration().get(descriptor.getShortName()))
                            : new UIProperty(descriptor, parent.get(descriptor.getShortName()));
                    uiProperty.setOverridden("Inherited");
                    uiProperties.add(uiProperty);
                }
            }
        }
        return uiProperties;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @SuppressWarnings("Duplicates")
    public boolean defineProperties(TriggerConfiguration triggerConfiguration) throws RemoteException,
            TransportException {
        Map<String, PropertyDescriptor> transportParameters =
                TransportRegistryManager.getInstance().getProperties(triggerConfiguration.getTypeName());
        //Properties
        List<UIProperty> uiProperties;
        boolean transportDeployed = transportParameters != null;
        if (transportDeployed) {
            uiProperties = define(transportParameters.values(), triggerConfiguration.getParent(), triggerConfiguration);
        } else {
            uiProperties = defineNonDeployed(triggerConfiguration);
        }
        this.setProperties(uiProperties);
        return transportDeployed;
    }
}
