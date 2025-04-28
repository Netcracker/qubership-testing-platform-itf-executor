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

package org.qubership.automation.itf.transport.camel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Helper {

    public static final Gson GSON = new GsonBuilder().create();

    public static String setExtraProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> item : properties.entrySet()) {
            if (StringUtils.isBlank(item.getKey()) || item.getValue() == null) {
                continue;
            }
            String key = item.getKey().trim();
            if (!key.isEmpty()) {
                stringBuilder.append("&").append(key).append("=").append(processValue(item.getValue()));
            }
        }
        return stringBuilder.toString();
    }

    public static Map<String, Object> setExtraPropertiesMap(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> extraProps = Maps.newHashMapWithExpectedSize(properties.size());
        for (Map.Entry<String, Object> item : properties.entrySet()) {
            if (StringUtils.isBlank(item.getKey()) || item.getValue() == null) {
                continue;
            }
            String key = item.getKey().trim();
            if (!key.isEmpty()) {
                extraProps.put(key, item.getValue());
            }
        }
        return extraProps;
    }

    private static String processValue(Object objValue) {
        try {
            return URLEncoder.encode((String) objValue, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return (String) objValue;
        }
    }
}
