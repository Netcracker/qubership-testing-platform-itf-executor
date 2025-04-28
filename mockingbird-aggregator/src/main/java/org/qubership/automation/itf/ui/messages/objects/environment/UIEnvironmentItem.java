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

package org.qubership.automation.itf.ui.messages.objects.environment;

import java.util.Objects;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UISystem;
import org.slf4j.LoggerFactory;

public class UIEnvironmentItem {

    private UIObject system;
    private UIObject server;

    public UIEnvironmentItem() {
    }

    public UIObject getSystem() {
        return system;
    }

    public void setSystem(UIObject system) {
        this.system = system;
    }

    public void defineSystem(System system) {
        this.system = getUiObject(system);
    }

    private UIObject getUiObject(Storable storable) {
        try {
            return new UIObject(storable);
        } catch (NullPointerException e) {
            LoggerFactory.getLogger(UIEnvironmentItem.class).error("Failed to getting UI Object from storable", e);
        }
        return new UIObject();
    }

    public void defineSystem(UISystem system) {
        this.system = new UIObject(system);
    }

    public UIObject getServer() {
        return server;
    }

    public void setServer(UIObject server) {
        this.server = server;
    }

    public void defineServer(Server server) {
        this.server = new UIServer(server);
    }

    public void defineServer(UIServer server) {
        this.server = new UIObject(server);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UIEnvironmentItem that = (UIEnvironmentItem) o;
        if ((system == null && that.system != null)
                || (system != null && that.system == null)
                || (server == null && that.server != null)
                || (server != null && that.server == null)) {
            return false;
        }
        return ((system == null && that.system == null) || system.getId().equals(that.system.getId()))
                && ((server == null && that.server == null) || server.getId().equals(that.server.getId()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, server);
    }
}
