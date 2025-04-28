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

package org.qubership.automation.itf.ui.messages.objects.transport.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.interceptor.ContentInterceptor;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.interceptor.ParametersInterceptor;
import org.qubership.automation.itf.core.model.interceptor.TransportInterceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.ApplicabilityParams;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.loader.InterceptorClassLoader;
import org.qubership.automation.itf.core.util.transport.access.AccessTransport;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIResult;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;

import com.google.common.collect.Lists;

public class UIInterceptor extends UIObject {

    private List<UIProperty> parameters = new ArrayList<>();
    private int order;
    private boolean active;
    private String transportName;
    private String interceptorGroup;
    private List<UIApplicabilityParams> applicabilityParams = new ArrayList<>();

    public UIInterceptor(Storable interceptor) throws TransportException, InvocationTargetException,
            IllegalAccessException, InstantiationException, NoSuchMethodException {
        this((Interceptor) interceptor);
    }

    public UIInterceptor(Interceptor interceptor) throws IllegalArgumentException, TransportException {
        super(interceptor);
        setOrder(interceptor.getOrder());
        setActive(interceptor.isActive());
        setTransportName(interceptor.getTransportName());
        setInterceptorGroup(interceptor.getInterceptorGroup());
        TransportInterceptor transportInterceptor = null;
        try {
            transportInterceptor = InterceptorClassLoader.getInstance().getInstanceClass(interceptor.getTypeName(),
                    interceptor);
        } catch (ClassNotFoundException e) {
            throw new TransportException(e);
        }
        List<InterceptorPropertyDescriptor> parameters;
        if (ParametersInterceptor.class.isAssignableFrom(transportInterceptor.getClass())) {
            List<PropertyDescriptor> redefinedParameters =
                    getRedefinedParametersFromTransport(interceptor.getTransportName());
            parameters = ((ParametersInterceptor) transportInterceptor).getParameters(redefinedParameters);
        } else {
            parameters = ((ContentInterceptor) transportInterceptor).getParameters();
        }
        for (InterceptorPropertyDescriptor parameter : parameters) {
            UIProperty uiProperty = new UIProperty(parameter);
            getParameters().add(uiProperty);
        }
        List<ApplicabilityParams> applicabilityParams = interceptor.getApplicabilityParams();
        for (ApplicabilityParams params : applicabilityParams) {
            UIApplicabilityParams uiApplicabilityParams = new UIApplicabilityParams(params);
            getApplicabilityParams().add(uiApplicabilityParams);
        }
    }

    public UIInterceptor() {
        this.active = false;
    }

    public List<UIProperty> getParameters() {
        return parameters;
    }

    public void setParameters(List<UIProperty> parameters) {
        this.parameters = parameters;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UIResult validate() {
        for (UIProperty parameter : parameters) {
            if (!Boolean.valueOf(parameter.getOptional()) && StringUtils.isEmpty(parameter.getValue())) {
                return new UIResult(false,
                        String.format("Parameter %s can not be empty. Please, fill the parameter.",
                                parameter.getName()));
            }
        }
        return new UIResult();
    }

    public String getTransportName() {
        return transportName;
    }

    public void setTransportName(String transportName) {
        this.transportName = transportName;
    }

    public String getInterceptorGroup() {
        return interceptorGroup;
    }

    public void setInterceptorGroup(String interceptorGroup) {
        this.interceptorGroup = interceptorGroup;
    }

    public List<UIApplicabilityParams> getApplicabilityParams() {
        return applicabilityParams;
    }

    public void setApplicabilityParams(List<UIApplicabilityParams> applicabilityParams) {
        this.applicabilityParams = applicabilityParams;
    }

    private List<PropertyDescriptor> getRedefinedParametersFromTransport(String transportTypeName)
            throws TransportException {
        List<PropertyDescriptor> redefinedProperties = Lists.newArrayList();
        AccessTransport transport = TransportRegistryManager.getInstance().find(transportTypeName);
        if (transport != null) {
            try {
                for (PropertyDescriptor propertyDescriptor : transport.getProperties()) {
                    if (propertyDescriptor.isRedefined()) {
                        redefinedProperties.add(propertyDescriptor);
                    }
                }
            } catch (RemoteException e) {
                throw new TransportException(e);
            }
        }
        return redefinedProperties;
    }
}
