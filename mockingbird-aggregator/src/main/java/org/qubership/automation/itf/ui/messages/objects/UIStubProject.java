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

import java.util.UUID;

import org.qubership.automation.itf.core.model.jpa.project.StubProject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UIStubProject extends UIObject {

    private UUID uuid;
    private UIObject systemFolder;
    private UIObject callChainFolder;
    private UIObject serverFolder;
    private UIObject environmentFolder;

    public UIStubProject() {
    }

    public UIStubProject(StubProject stubProject) {
        super(stubProject);
        this.uuid = stubProject.getUuid();
        this.systemFolder = new UIObject(stubProject.getSystems());
        this.callChainFolder = new UIObject(stubProject.getCallchains());
        this.serverFolder = new UIObject(stubProject.getServers());
        this.environmentFolder = new UIObject(stubProject.getEnvironments());
    }
}
