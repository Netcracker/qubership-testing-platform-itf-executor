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

package org.qubership.automation.itf.core.monitor;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;

import com.google.common.collect.ImmutableList;

public class PausedSituationExtension {

    private LinkedHashMap<String, Situation> data = new LinkedHashMap<>();

    public void add(Situation situation) {
        if (!this.data.containsKey(situation.getID().toString())) {
            this.data.put(situation.getID().toString(), situation);
        }
    }

    public Situation get(String id) {
        return this.data.get(id);
    }

    public Collection<Object> getList() {
        return ImmutableList.copyOf(data.values().toArray());
    }

    public boolean isPaused(String id) {
        return this.data.containsKey(id);
    }

    public int size() {
        return this.data.size();
    }

    public void clear() {
        this.data.clear();
    }

    public void removeById(String id) {
        this.data.remove(id);
    }
}
