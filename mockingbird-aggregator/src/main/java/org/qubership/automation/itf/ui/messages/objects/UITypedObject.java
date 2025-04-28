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

public class UITypedObject extends UIObject {

    private String type;

    public UITypedObject() {
        super();
    }

    public UITypedObject(Storable storable) {
        super(storable);
    }

    public UITypedObject(Storable storable, boolean isFullWithParent) {
        super(storable, isFullWithParent);
    }

    public UITypedObject(UIObject uiObject) {
        super(uiObject);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
