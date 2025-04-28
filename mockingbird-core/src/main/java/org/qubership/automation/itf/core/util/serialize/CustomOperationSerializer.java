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

import org.qubership.automation.itf.core.model.jpa.storage.AbstractStorable;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomOperationSerializer extends StdSerializer<Operation> {

    protected CustomOperationSerializer() {
        this(null);
    }

    protected CustomOperationSerializer(Class<Operation> t) {
        super(t);
    }

    @Override
    public void serialize(Operation operation, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeObject(new SimpleOperation(operation));
    }

    private class SimpleOperation extends AbstractStorable {

        SimpleOperation(Operation operation) {
            setID(operation.getID());
            setParent(operation.getParent());
            setName(operation.getName());
            setDescription(operation.getDescription());
            setPrefix(operation.getPrefix());
            setVersion(operation.getVersion());
        }
    }
}
