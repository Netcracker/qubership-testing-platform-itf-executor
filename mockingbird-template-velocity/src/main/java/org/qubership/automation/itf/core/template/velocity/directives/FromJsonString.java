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

package org.qubership.automation.itf.core.template.velocity.directives;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FromJsonString extends Directive {

    @Override
    public String getName() {
        return "fromJson";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node)
            throws ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        int count = node.jjtGetNumChildren();
        if (count < 2) {
            throw new IllegalArgumentException("Directive '#" + getName() + "' arguments are missed.\n" + "The 1st "
                    + "argument is $jsonString (to be parsed into Json object),\n" + "The 2nd argument is Context "
                    + "Variable Name (without leading $, but with tc. or sp.) - where to put Json object.");
        }
        Object jsonObject = node.jjtGetChild(0).value(internalContextAdapter);
        Object fullKeyName = node.jjtGetChild(1).value(internalContextAdapter);
        String fullKeyString;
        String contextKey;
        String keyString;
        if (fullKeyName != null) {
            fullKeyString = fullKeyName.toString();
        } else {
            throw new IllegalArgumentException("Directive '#" + getName() + "': 2nd argument should not be empty!");
        }
        JsonContext contextToManage;
        if (fullKeyString.isEmpty()) {
            throw new IllegalArgumentException("Directive '#" + getName() + "': 2nd argument should not be empty!");
        } else if (fullKeyString.length() < 4) {
            throw new IllegalArgumentException("Directive '#" + getName() + "': 2nd argument (" + fullKeyString + ") "
                    + "should be like sp.somekey or tc.somekey!");
        } else {
            contextKey = fullKeyString.substring(0, 2);
            keyString = fullKeyString.substring(3);
            if (!(contextKey.equals("tc") || contextKey.equals("sp")) || !fullKeyString.substring(2, 3).equals(".")
                    || keyString.startsWith(".")) {
                throw new IllegalArgumentException("Directive '#" + getName()
                        + "': 2nd argument (" + fullKeyString + ") format is invalid!");
            }
            contextToManage = (JsonContext) internalContextAdapter.get(contextKey);
            if (contextToManage == null) {
                throw new IllegalArgumentException("Directive '#" + getName()
                        + "': Context object by key (" + contextKey + ") is not found!");
            }
            if (jsonObject instanceof String) {
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = jsonObject.toString();
                try {
                    Map mp = mapper.readerFor(Map.class).readValue(jsonString);
                    contextToManage.put(keyString, (mp == null || mp.isEmpty()) ? jsonString : mp);
                } catch (Exception ex) {
                    try {
                        List lst = mapper.readerFor(List.class).readValue(jsonString);
                        contextToManage.put(keyString, lst);
                    } catch (Exception ex2) {
                        contextToManage.put(keyString, jsonString);
                    }
                }
            } else {
                contextToManage.put(keyString, jsonObject);
            }
        }
        return true;
    }
}
