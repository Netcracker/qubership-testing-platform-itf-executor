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

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.interceptor.ApplicabilityParams;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIApplicabilityParams extends UIObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIApplicabilityParams.class);
    private UIObject environment;
    private UIObject system;

    public UIApplicabilityParams(Storable applicabilityParams) {
        this((ApplicabilityParams) applicabilityParams);
    }

    public UIApplicabilityParams(ApplicabilityParams applicabilityParams) {
        super(applicabilityParams);
        getApplicableParameterForUI(applicabilityParams, PropertyConstants.Applicability.ENVIRONMENT);
        getApplicableParameterForUI(applicabilityParams, PropertyConstants.Applicability.SYSTEM);
    }

    public UIApplicabilityParams() {
    }

    public UIObject getEnvironment() {
        return environment;
    }

    public void setEnvironment(UIObject environment) {
        this.environment = environment;
    }

    public UIObject getSystem() {
        return system;
    }

    public void setSystem(UIObject system) {
        this.system = system;
    }

    private void getApplicableParameterForUI(ApplicabilityParams applicabilityParams, String parameter) {
        String storableId = applicabilityParams.get(parameter);
        if (StringUtils.isNotEmpty(storableId)) {
            if (parameter.equals(PropertyConstants.Applicability.ENVIRONMENT)) {
                Storable storable = CoreObjectManager.getInstance().getManager(Environment.class).getById(storableId);
                if (storable != null) {
                    this.environment = new UIObject(storable);
                } else {
                    LOGGER.error(String.format("Environment with id = %s was not found. Please, import the "
                            + "environment or use another.", storableId));
                }
            }
            if (parameter.equals(PropertyConstants.Applicability.SYSTEM)) {
                Storable storable = CoreObjectManager.getInstance().getManager(System.class).getById(storableId);
                if (storable != null) {
                    this.system = new UIObject(storable);
                } else {
                    LOGGER.error(String.format("System with id = %s was not found. Please, import the system or use "
                            + "another.", storableId));
                }
            }
        }
    }
}
