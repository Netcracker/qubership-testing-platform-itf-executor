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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Triple;
import org.qubership.automation.itf.core.util.constants.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public class PropertyHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyHelper.class);

    public static boolean meetsMatch(Object object, String property, Match match, Object er) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(object.getClass(), property);
        Class<?> propertyType = propertyDescriptor.getPropertyType();
        Object propertyValue = propertyDescriptor.getReadMethod().invoke(object);
        switch (match) {
            case EQUALS:
                return Objects.equals(propertyValue, er);
            case NOT_EQUALS:
                return !Objects.equals(propertyValue, er);
            case IN:
                if (propertyValue == null) {
                    return false;
                }
                Collection collection;
                if (er instanceof Collection) {
                    collection = (Collection) er;
                } else {
                    collection = Collections.singleton(er);
                }
                if (propertyType.isArray()) {
                    return collection.containsAll(Arrays.asList((Object[]) propertyValue));
                } else if (Collection.class.isAssignableFrom(propertyType)) {
                    return collection.containsAll((Collection) propertyValue);
                } else if (Map.class.isAssignableFrom(propertyType)) {
                    return collection.containsAll(((Map) propertyValue).keySet());
                } else {
                    return collection.contains(propertyValue);
                }
        }
        return false;
    }

    public static boolean meetsAllProperties(Object object, Triple<String, Match, ?>[] properties) {
        boolean meets;
        for (Triple<String, Match, ?> property : properties) {
            try {
                meets = meetsMatch(object, property.getLeft(), property.getMiddle(), property.getRight());
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LOGGER.error("Error getting property from object", e);
                meets = false;
            }
            if (!meets) {
                return false;
            }
        }
        return true;
    }
}
