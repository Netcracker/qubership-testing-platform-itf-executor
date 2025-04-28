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

package org.qubership.automation.itf.core.util.serialize;

import java.io.IOException;

import org.qubership.automation.itf.core.model.common.LabeledStorable;
import org.qubership.automation.itf.core.model.jpa.system.System;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomSystemSerializer extends StdSerializer<System> {

    protected CustomSystemSerializer() {
        this(null);
    }

    protected CustomSystemSerializer(Class<System> t) {
        super(t);
    }

    @Override
    public void serialize(System system, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeObject(new SimpleSystem(system));
    }

    private class SimpleSystem extends LabeledStorable {

        SimpleSystem(System system) {
            setID(system.getID());
            setParent(system.getParent());
            setName(system.getName());
            setDescription(system.getDescription());
            setPrefix(system.getPrefix());
            setVersion(system.getVersion());
            setLabels(system.getLabels());
        }
    }
}
