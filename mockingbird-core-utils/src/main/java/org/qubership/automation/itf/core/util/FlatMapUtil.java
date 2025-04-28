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

package org.qubership.automation.itf.core.util;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class FlatMapUtil {

    private static final String leadingChar = "";          // 1st version value is: "/";
    private static final String keyDelimiter = ".";        // 1st version value is: "/";
    private static final String arrayElementLeft = "[";    // 1st version value is: "/";
    private static final String arrayElementRight = "]";   // 1st version value is: "";

    private FlatMapUtil() {
        throw new AssertionError("No instances for you!");
    }

    public static Map<String, Object> flatten(Map<String, Object> map) {
        return map.entrySet().stream().flatMap(FlatMapUtil::flatten).collect(LinkedHashMap::new,
                (m, e) -> m.put(leadingChar + e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private static Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {
        if (entry == null) {
            return Stream.empty();
        }
        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?, ?>) entry.getValue()).entrySet()
                    .stream()
                    .flatMap(e -> flatten(
                            new AbstractMap.SimpleEntry<>(entry.getKey() + keyDelimiter + e.getKey(), e.getValue())));
        }
        if (entry.getValue() instanceof List<?>) {
            List<?> list = (List<?>) entry.getValue();
            return IntStream.range(0, list.size())
                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + arrayElementLeft + i + arrayElementRight, list.get(i)))
                    .flatMap(FlatMapUtil::flatten);
        }
        return Stream.of(entry);
    }
}
