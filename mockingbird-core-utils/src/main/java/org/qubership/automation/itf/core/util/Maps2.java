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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Maps2 {

    public static <T, U> MapBuilder<T, U> map(T key, U value) {
        return new MapBuilder<T, U>().hashMap().val(key, value);
    }

    public static class MapBuilder<T, U> {

        private Map<T, U> map;

        public MapBuilder<T, U> hashMap() {
            map = new HashMap<>();
            return this;
        }

        public MapBuilder<T, U> val(T key, U value) {
            map.put(key, value);
            return this;
        }

        public Map<T, U> build() {
            return map;
        }

        public Map<T, U> ro() {
            return Collections.unmodifiableMap(map);
        }
    }
}
