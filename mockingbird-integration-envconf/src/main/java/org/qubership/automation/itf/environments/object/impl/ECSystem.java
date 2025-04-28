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

package org.qubership.automation.itf.environments.object.impl;

import java.util.List;

import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.system.System;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class ECSystem extends ConvertableECEntity<System> {

    private List<ECConnection> connections;
    @SerializedName("serverITF")
    @JsonProperty("serverITF")
    private ECServer server;
    @SerializedName("mergeByName")
    @JsonProperty("mergeByName")
    private String mergeByName;

    public ECSystem() {
        setGenericType(System.class);
        setParentClass(Folder.class);
    }

    public ECSystem(String ecId) {
        setEcId(ecId);
        setGenericType(System.class);
        setParentClass(Folder.class);
    }

    public List<ECConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<ECConnection> connections) {
        this.connections = connections;
    }

    public ECServer getServer() {
        return server;
    }

    public void setServer(ECServer server) {
        this.server = server;
    }

    public String getMergeByName() {
        return mergeByName;
    }

    public void setMergeByName(String mergeByName) {
        this.mergeByName = mergeByName;
    }
}
