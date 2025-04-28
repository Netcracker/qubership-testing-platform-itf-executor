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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.project.IntegrationConfig;
import org.qubership.automation.itf.core.model.jpa.transport.Configuration;
import org.qubership.automation.itf.core.util.descriptor.Extractor;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.exception.NoDeployedTransportException;
import org.qubership.automation.itf.core.util.provider.PropertyProvider;
import org.qubership.automation.itf.core.util.registry.EngineIntegrationRegistry;
import org.qubership.automation.itf.core.util.transport.access.AccessTransport;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UITypedObject;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class UIConfiguration extends UITypedObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIConfiguration.class);

    private String userTypeName;
    private List<UIProperty> properties = Lists.newArrayList();

    public UIConfiguration() {
    }

    public UIConfiguration(Storable storable) {
        super(storable);
    }

    public UIConfiguration(UIObject object) {
        super(object);
    }

    public UIConfiguration(Configuration configuration) {
        super(configuration);
        userTypeName = configuration.getTypeName();
        setType(configuration.getTypeName());
    }

    public UIConfiguration(IntegrationConfig integrationConfig) {
        super(integrationConfig);
        setType(integrationConfig.getTypeName());
        userTypeName = integrationConfig.getTypeName();
        defineProperties(integrationConfig);
    }

    @Nonnull
    public static List<UIProperty> defineNonDeployed(@Nonnull Configuration transport) {
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(transport.size());
        for (Map.Entry<String, String> entry : transport.entrySet()) {
            UIProperty uiProperty = new UIProperty();
            uiProperty.setName(entry.getKey());
            uiProperty.setUserName(entry.getKey() + "[Not deployed]");
            uiProperty.setDescription("Transport implementation for type " + transport.getTypeName() + " was not "
                    + "deployed correctly");
            uiProperty.setValue(entry.getValue());
            uiProperty.setOptional(Boolean.FALSE.toString());
            uiProperties.add(uiProperty);
        }
        return uiProperties;
    }

    public void defineProperties(Configuration configuration) {
        try {
            Class<?> aClass = Class.forName(configuration.getTypeName());
            if (PropertyProvider.class.isAssignableFrom(aClass)) {
                defineProperties(configuration, Extractor.extractProperties((PropertyProvider) aClass.newInstance()));
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Can't extract properties from %s for configuration %s",
                    configuration.getTypeName(), configuration.getID()), e);
        }
    }

    public void fillUserTypeNameFromConfiguration(Configuration configuration) {
        try {
            AccessTransport remoteTransport;
            try {
                remoteTransport = TransportRegistryManager.getInstance().find(configuration.getTypeName());
            } catch (NoDeployedTransportException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            if (remoteTransport == null) {
                setUserTypeName(configuration.getTypeName());
            } else {
                setUserTypeName(remoteTransport.getUserName());
            }
        } catch (Exception ignored) {
        }
        setType(configuration.getTypeName());
        if (configuration.getParent() instanceof Environment) {
            setParent(new UIEnvironment((Environment) configuration.getParent()));
        }
    }

    public void defineProperties(Configuration configuration, List<PropertyDescriptor> properties) {
        this.properties = Lists.newArrayListWithExpectedSize(configuration.size());
        if (properties != null) {
            for (PropertyDescriptor descriptor : properties) {
                UIProperty uiProperty = new UIProperty(descriptor, configuration.get(descriptor.getShortName()));
                this.properties.add(uiProperty);
            }
        } else {
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                UIProperty uiProperty = new UIProperty();
                uiProperty.setName(entry.getKey());
                uiProperty.setUserName(entry.getKey() + " [unavailable]");
                uiProperty.setValue(entry.getValue());
                this.properties.add(uiProperty);
            }
        }
        Collections.sort(this.properties);
    }

    public void defineProperties(IntegrationConfig integrationConfig) {
        List<PropertyDescriptor> properties =
                EngineIntegrationRegistry.getInstance().getProperties(integrationConfig.getTypeName());
        List<UIProperty> uiProperties = Lists.newArrayListWithExpectedSize(integrationConfig.size());
        if (properties != null) {
            for (PropertyDescriptor descriptor : properties) {
            /*for (Map.Entry<String, PropertyDescriptor> property : properties.entrySet()) {
                PropertyDescriptor descriptor = property.getValue();*/
                UIProperty uiProperty = new UIProperty(descriptor, integrationConfig.get(descriptor.getShortName()));
                uiProperties.add(uiProperty);
            }
        } else {
            for (Map.Entry<String, String> entry : integrationConfig.entrySet()) {
                UIProperty uiProperty = new UIProperty();
                uiProperty.setName(entry.getKey());
                uiProperty.setUserName(entry.getKey() + " [Integration not initialized]");
                uiProperty.setValue(entry.getValue());
                uiProperties.add(uiProperty);
            }
        }
        this.properties = uiProperties;
    }

    public String getUserTypeName() {
        return userTypeName;
    }

    public void setUserTypeName(String userTypeName) {
        this.userTypeName = userTypeName;
    }

    public List<UIProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<UIProperty> properties) {
        this.properties = properties;
    }

    public Optional<UIProperty> getProperty(String name) {
        return properties.stream()
                .filter(uiProperty -> name.equals(uiProperty.getName()))
                .findFirst();
    }
}
