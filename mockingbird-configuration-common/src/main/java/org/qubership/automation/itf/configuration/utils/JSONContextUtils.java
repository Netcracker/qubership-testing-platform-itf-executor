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

package org.qubership.automation.itf.configuration.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.automation.itf.core.model.jpa.context.JsonContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONContextUtils {

    public static JsonContext convert(@Nonnull ObjectNode from, @Nonnull ObjectMapper mapper) throws IOException {
        JsonContext to = new JsonContext();
        LeafsIterator<Map.Entry<String, JsonNode>> leafsIter =
                new LeafsIterator<Map.Entry<String, JsonNode>>(from.fields()) {
                    @Nullable
                    @Override
                    protected Iterator<? extends Map.Entry<String, JsonNode>> getChildren(
                            @Nonnull Map.Entry<String, JsonNode> parent) {
                        JsonNode parentNode = parent.getValue();
                        if (parentNode.isObject()) {
                            return ObjectNode.class.cast(parentNode).fields();
                        }
                        return null;
                    }
                };
        while (leafsIter.hasNext()) {
            List<Map.Entry<String, JsonNode>> path = leafsIter.next();
            Map.Entry<String, JsonNode> leaf = path.get(path.size() - 1);//last one
            Stream<Map.Entry<String, JsonNode>> pathToLeaf = path.stream().limit(path.size() - 1);//except leaf
            JsonNode leafNode = leaf.getValue();
            String paramKey = leaf.getKey();
            List<String> groupNames = pathToLeaf.map(Map.Entry::getKey).collect(Collectors.toList());
            if (leafNode.isValueNode()) {
                String paramValue = leafNode.textValue();
                getGroup(groupNames.iterator(), to).put(paramKey, paramValue);
            } else if (leafNode.isObject() && leafNode.isEmpty()) {
                JsonContext paramValue = new JsonContext();
                getGroup(groupNames.iterator(), to).put(paramKey, paramValue);
            } else {
                throw new IOException("Can not accept [" + mapper.writeValueAsString(leaf) + "] parameter value");
            }
        }
        return to;
    }

    private static JsonContext getGroup(@Nonnull Iterator<String> path, @Nonnull JsonContext to) {
        JsonContext result = to;
        while (path.hasNext()) {
            result = getGroup(path.next(), result);
        }
        return result;
    }

    @Nonnull
    private static JsonContext getGroup(@Nonnull String key, @Nonnull JsonContext to) {
        Object targetGroup = to.get(key);
        JsonContext validTargetGroup;
        if (targetGroup == null) {
            validTargetGroup = new JsonContext();
            to.put(key, validTargetGroup);
        } else {
            validTargetGroup = JsonContext.class.cast(targetGroup);
        }
        return validTargetGroup;
    }
}
