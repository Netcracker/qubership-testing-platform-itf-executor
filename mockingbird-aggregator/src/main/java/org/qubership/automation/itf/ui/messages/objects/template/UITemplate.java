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

package org.qubership.automation.itf.ui.messages.objects.template;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.helper.Comparators;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;
import org.qubership.automation.itf.core.util.transport.access.AccessTransport;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class UITemplate extends UIObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(UITemplate.class);

    private String content;
    private ImmutableList<UITransportPropsForTemplate> transportProperties;
    private ImmutableList<UIProperty> headers;
    private Collection<UIInterceptor> transportInterceptors;

    public UITemplate(Storable template) {
        this((Template) template);
    }

    public UITemplate(Template template) {
        try {
            prepareUiTemplate(template);
        } catch (TransportException | RemoteException | IllegalAccessException | ClassNotFoundException
                 | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("UI Templete dosn't create:", e);
            throw new IllegalArgumentException(e);
        }
    }

    public UITemplate() {
    }

    public ImmutableList<UIProperty> getHeaders() {
        return headers;
    }

    public void setHeaders(List<UIProperty> headers) {
        this.headers = ImmutableList.copyOf(headers);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ImmutableList<UITransportPropsForTemplate> getTransportProperties() {
        return transportProperties;
    }

    public void setTransportProperties(List<UITransportPropsForTemplate> transportProperties) {
        this.transportProperties = ImmutableList.copyOf(transportProperties);
    }

    public Collection<UIInterceptor> getTransportInterceptors() {
        return transportInterceptors;
    }

    public void setTransportInterceptors(Collection<UIInterceptor> transportInterceptors) {
        this.transportInterceptors = transportInterceptors;
    }
    //TODO refactoring this methods at the bottom

    private void prepareUiTemplate(Template<? extends TemplateProvider> template) throws TransportException,
            RemoteException,
            ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException,
            InvocationTargetException {
        if (template != null) {
            defineObjectParam(template);
            setContent(template.getText());
            if (template.getHeaders() != null) {
                List<UIProperty> headers = Lists.newArrayList();
                for (Map.Entry<String, String> entry : (template.getHeaders().entrySet())) {
                    UIProperty header = new UIProperty();
                    header.setName(entry.getKey());
                    Object value = entry.getValue();
                    if (value != null) {
                        header.setValue(value.toString());
                    }
                    headers.add(header);
                }
                setHeaders(headers);
            }
            setTransportProperties(buildUITransportProperties(template));
            setTransportInterceptors(getUIInterceptors(template));
        }
    }

    private List<UITransportPropsForTemplate> buildUITransportProperties(Template<? extends TemplateProvider> template)
            throws RemoteException,
            TransportException {
        List<UITransportPropsForTemplate> transportProperties = Lists.newArrayListWithCapacity(10);
        for (Map.Entry<String, AccessTransport> transportEntry :
                TransportRegistryManager.getInstance().getTransports().entrySet()) {
            LinkedList<UIProperty> uiProperties = Lists.newLinkedList();
            getPropertiesFromDescriptor(transportEntry, uiProperties);
            if (!uiProperties.isEmpty()) {
                transportProperties.add(new UITransportPropsForTemplate(transportEntry.getKey(),
                        transportEntry.getValue().getUserName(), mergeWithExisting(uiProperties,
                        template.getTransportProperties(transportEntry.getKey()))));
            }
        }
        return transportProperties;
    }

    private Collection<UIInterceptor> getUIInterceptors(Template template) {
        Collection<UIInterceptor> result = new ArrayList<UIInterceptor>();
        List<Interceptor> interceptors = template.getInterceptors();
        interceptors.sort(Comparators.INTERCEPTOR_COMPARATOR);
        for (Interceptor interceptor : interceptors) {
            try {
                result.add(new UIInterceptor(interceptor));
            } catch (Exception e) {
                LOGGER.error("Cannot instantiate the \"{}\" interceptor. Check that the appropriate interceptor's "
                        + "implementation wad added and interceptor successfully registered.", interceptor.getName());
                LOGGER.error("Stacktrace: ", e);
            }
        }
        return result;
    }

    private @Nonnull
    LinkedList<UIProperty> mergeWithExisting(
            @Nonnull LinkedList<UIProperty> uiProperties, @Nullable Map<String, String> propertiesFromTemplate) {
        if (propertiesFromTemplate == null) {
            return uiProperties;
        }
        for (UIProperty uiProperty : uiProperties) {
            uiProperty.setValue(propertiesFromTemplate.get(uiProperty.getName()));
        }
        return uiProperties;
    }

    private void getPropertiesFromDescriptor(Map.Entry<String, AccessTransport> transportEntry,
                                             LinkedList<UIProperty> uiProperties) throws TransportException {
        try {
            for (PropertyDescriptor propertyDescriptor : transportEntry.getValue().getProperties()) {
                if (propertyDescriptor.isForTemplate()) {
                    uiProperties.add(new UIProperty(propertyDescriptor));
                }
            }
        } catch (RemoteException e) {
            throw new TransportException(e);
        }
    }
}
