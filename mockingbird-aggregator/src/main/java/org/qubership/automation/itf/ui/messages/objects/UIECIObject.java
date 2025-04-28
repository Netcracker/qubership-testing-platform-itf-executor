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

package org.qubership.automation.itf.ui.messages.objects;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.eci.EciConfigurable;

public class UIECIObject extends UIObject {

    private String ecId;

    public UIECIObject() {
    }

    public UIECIObject(Storable storable) {
        super(storable);
        if (EciConfigurable.class.isAssignableFrom(storable.getClass())) {
            ecId = ((EciConfigurable) storable).getEcId();
        }
    }

    public UIECIObject(UIObject object) {
        super(object);
        if (object.getClass().isAssignableFrom(UIECIObject.class)) {
            ecId = ((UIECIObject) object).getEcId();
        }
    }

    public String getEcId() {
        return ecId;
    }

    public void setEcId(String ecId) {
        this.ecId = ecId;
    }
}
