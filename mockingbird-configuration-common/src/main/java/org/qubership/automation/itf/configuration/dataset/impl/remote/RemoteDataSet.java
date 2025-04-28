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

package org.qubership.automation.itf.configuration.dataset.impl.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.json.simple.JSONObject;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;

public class RemoteDataSet implements IDataSet {

    private static final String PREFIX = "[Modified Dataset]";
    private final Supplier<JsonContext> contextSup;
    private String name;
    private String dsId;
    private List<String> labels;

    /**
     * TODO Add JavaDoc.
     */
    public RemoteDataSet(@Nonnull String name, @Nonnull Supplier<JsonContext> contextSup, String id) {
        this.name = name;
        this.contextSup = contextSup;
        this.dsId = id;
    }

    /**
     * TODO Add JavaDoc.
     */
    public RemoteDataSet(@Nonnull String name, @Nonnull Supplier<JsonContext> contextSup,
                         String id, List<String> labels) {
        this.name = name;
        this.contextSup = contextSup;
        this.dsId = id;
        this.labels = labels;
    }

    @Override
    public JsonContext read(Object projectId) {
        return contextSup.get();
    }

    @Override
    public JsonContext read(@Nonnull JsonContext overriddenValues, Object projectId) {
        JsonContext context = read(projectId);
        if (overriddenValues != null && !overriddenValues.isEmpty()) {
            List<Object> keysToDelete = new ArrayList<>();
            overriddenValues.forEach((key, value) -> {
                Object contextValue = context.get(key);
                if (contextValue != null) {
                    if (value instanceof JSONObject && contextValue instanceof JSONObject) {
                        for (Map.Entry<String, String> entry : ((Map<String, String>) value).entrySet()) {
                            ((JSONObject) contextValue).put(entry.getKey(), entry.getValue());
                        }
                    } else {
                        context.put(key, value);
                    }
                    keysToDelete.add(key);
                }
            });
            for (Object key : keysToDelete) {
                overriddenValues.remove(key);
            }
        }
        return context;
    }

    @Override
    public String getIdDs() {
        return dsId;
    }

    @Override
    public String getDsServiceUri() {
        return "";
    }

    @Override
    public void addModifiedToName(boolean flag) {
        if (flag) {
            name = PREFIX + name;
        } else {
            if (name.startsWith(PREFIX)) {
                name = name.substring(PREFIX.length());
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getLabels() {
        return labels;
    }
}
