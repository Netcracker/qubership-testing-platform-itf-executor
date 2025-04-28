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

package org.qubership.automation.itf.core.report.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class AbstractSetter implements Setter {

    protected final String property;
    protected final int sqlType;

    protected AbstractSetter(String property, int sqlType) {
        this.property = property;
        this.sqlType = sqlType;
    }

    @Override
    public int set(JsonObject json, PreparedStatement statement, int index) throws SQLException {
        Optional<Object> value = getValid(json).transform(new Function<JsonElement, Object>() {
            @Override
            public Object apply(JsonElement input) {
                return convertJsonValue(input);
            }
        });
        statement.setObject(index, value.orNull(), sqlType);
        return ++index;
    }

    @Override
    public String getProperty() {
        return property;
    }

    protected Optional<? extends JsonElement> getValid(JsonObject json) {
        JsonElement element = json.get(property);
        if (element == null || element.isJsonNull()) {
            return Optional.absent();
        } else {
            return Optional.of(element);
        }
    }

    protected abstract Object convertJsonValue(JsonElement element);
}
