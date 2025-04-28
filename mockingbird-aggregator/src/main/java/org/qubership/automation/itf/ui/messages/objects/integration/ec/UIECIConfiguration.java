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

package org.qubership.automation.itf.ui.messages.objects.integration.ec;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.transport.EciConfiguration;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIConfiguration;

public class UIECIConfiguration<T extends EciConfiguration> extends UIConfiguration {

    private String ecId;

    public UIECIConfiguration() {
    }

    public UIECIConfiguration(Storable storable) {
        super(storable);
    }

    public UIECIConfiguration(UIObject uiObject) {
        super(uiObject);
    }

    public UIECIConfiguration(T entity) {
        super(entity);
        ecId = entity.getEcId();
    }

    public String getEcId() {
        return ecId;
    }

    public void setEcId(String ecId) {
        this.ecId = ecId;
    }
}
