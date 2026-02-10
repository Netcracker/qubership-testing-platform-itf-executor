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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.transport.http.HTTPConstants;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;
import org.qubership.automation.itf.core.util.exception.TransportException;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.transport.manager.TransportRegistryManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.integration.ec.UIECIConfiguration;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UITransport extends UIECIConfiguration<TransportConfiguration> {

    private static final String INPUT_TYPE_REFERENCE = "reference";
    private static final Logger LOGGER = LoggerFactory.getLogger(UITransport.class);
    private String endpoint;
    private String status;
    private String mep;
    private String endpointPrefix;
    private Collection<UIInterceptor> transportInterceptors;

    public UITransport() {
    }

    public UITransport(Storable storable) {
        this((TransportConfiguration) storable);
    }

    public UITransport(TransportConfiguration transport) {
        super(transport);
        fillUserTypeNameFromConfiguration(transport);
        endpoint = StringUtils.defaultIfEmpty(transport.get(HTTPConstants.ENDPOINT), "");
        if (transport.getMep() != null) {
            mep = transport.getMep().toString();
        }
        endpointPrefix = transport.getEndpointPrefix();
        try {
            defineProperties(transport);
        } catch (Exception ignored) {

        }
    }

    public void fillForQuickDisplay(@Nonnull TransportConfiguration transport) {
        fillForQuickDisplay(transport, false);
    }

    public void fillForQuickDisplay(@Nonnull TransportConfiguration transport, boolean toTableDisplay) {
        this.setId(transport.getID().toString());
        this.setClassName(transport.getClass().getName());
        this.setName(transport.getName());
        this.setType(transport.getTypeName());
        this.setMep(transport.getMep().toString());
        this.setVersion(NumberUtils.toInt(String.valueOf(transport.getVersion()), -1));
        if (toTableDisplay) {
            this.setEndpoint(StringUtils.defaultIfEmpty(transport.get(HTTPConstants.ENDPOINT), ""));
            this.setEndpointPrefix(transport.getEndpointPrefix());
            this.setDescription(transport.getDescription());
            fillUserTypeNameFromConfiguration(transport);
        }
    }

    public boolean defineProperties(TransportConfiguration transport) throws TransportException {
        Map<String, PropertyDescriptor> transportParameters =
                TransportRegistryManager.getInstance().getProperties(transport.getTypeName());
        //Properties
        List<UIProperty> uiProperties;
        boolean transportDeployed = transportParameters != null;
        if (transportDeployed) {
            uiProperties = Lists.newArrayListWithExpectedSize(transportParameters.size());
            for (PropertyDescriptor descriptor : transportParameters.values()) {
                if (!descriptor.isFromServer()) {
                    UIProperty uiProperty = new UIProperty(descriptor, transport.get(descriptor.getShortName()));
                    if (INPUT_TYPE_REFERENCE.equals(uiProperty.getInputType())
                            && !StringUtils.EMPTY.equals(uiProperty.getValue())) {
                        String id = uiProperty.getValue();
                        if (StringUtils.isBlank(id)) {
                            uiProperty.setReferenceValue(null);
                            LOGGER.debug("Empty id of reference value for '{}' transport property",
                                    uiProperty.getUserName());
                        } else {
                            try {
                                Storable storable;
                                if (Template.class.getName().equals(uiProperty.getReferenceClass())) {
                                    storable = TemplateHelper.getById(id);
                                } else {
                                    Class<?> propertyType = Class.forName(uiProperty.getReferenceClass());
                                    storable = CoreObjectManager.getInstance()
                                            .getManager(propertyType.asSubclass(Storable.class)).getById(id);
                                }
                                uiProperty.setReferenceValue(new UIObject(storable));
                            } catch (Exception e) {
                                uiProperty.setReferenceValue(null);
                                LOGGER.debug("Error while getting the reference value for transport property '{}' by "
                                        + "id {}", uiProperty.getUserName(), id, e);
                            }
                        }
                    }
                    uiProperties.add(uiProperty);
                }
            }
        } else {
            uiProperties = defineNonDeployed(transport);
        }
        this.setProperties(uiProperties);
        return transportDeployed;
    }

}
