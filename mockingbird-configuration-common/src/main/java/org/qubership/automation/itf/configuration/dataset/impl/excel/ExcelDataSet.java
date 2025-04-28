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

package org.qubership.automation.itf.configuration.dataset.impl.excel;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.usermodel.Cell;
import org.qubership.automation.configuration.dataset.excel.core.DS;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;

import com.google.common.base.Supplier;

public class ExcelDataSet implements IDataSet {

    private static final String PREFIX = "[Modified Dataset]";
    private DS<String, Collection<Triple<String, String, String>>> ds;
    private String name;

    public ExcelDataSet(@Nonnull DS<String, Collection<Triple<String, String, String>>> ds) {
        this.name = ds.getName();
        this.ds = ds;
    }

    @Override
    public JsonContext read(Object projectId) {
        JsonContext jsonContext = read(ds::getVariables);
        ds = null;
        return jsonContext;
    }

    @Override
    public String getIdDs() {
        return name;
    }

    @Override
    public String getDsServiceUri() {
        return "";
    }

    @Override
    public JsonContext read(final @Nonnull JsonContext overriddenValues, Object projectId) {
        JsonContext jsonContext = read(() -> ds.getVariables((entity, param, convertedParam, value) -> {
            if (!overriddenValues.containsKey(convertedParam)) {
                return;
            }
            //So, if we found param, and replace it, let's remove it
            //for avoid duplication in merge plain params which not in DataSet.
            Object overrideParam = overriddenValues.get(convertedParam);
            if (value.getCellType() == Cell.CELL_TYPE_FORMULA) {
                // Text values (in case of formulas) should be enclosed by "" - 20180606, Alexander Kapustin,
                // Alexander Kolosov
                value.setCellValue("\"" + overrideParam.toString() + "\"");
            } else {
                value.setCellValue(overrideParam.toString());
            }
            overriddenValues.remove(overrideParam);
        }));
        ds = null;
        return jsonContext;
    }

    private JsonContext read(@Nonnull Supplier<Collection<Triple<String, String, String>>> triples) {
        final JsonContext context = new JsonContext();
        for (Triple<String, String, String> entry : triples.get()) {
            String group = entry.getLeft();
            String param = entry.getMiddle();
            String var = entry.getRight();
            if (context.get(group) == null) {
                JsonContext value = new JsonContext();
                value.put(param, var);
                context.put(group, value);
            } else {
                context.put(group + '.' + param, var);
            }
        }
        return context;
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
        return null;
    }
}
