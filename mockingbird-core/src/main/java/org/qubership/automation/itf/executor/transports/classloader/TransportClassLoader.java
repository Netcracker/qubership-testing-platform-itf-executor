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

package org.qubership.automation.itf.executor.transports.classloader;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Set;

import org.qubership.automation.itf.core.util.loader.base.AbstractLoader;
import org.qubership.automation.itf.core.util.provider.MeansCommunication;
import org.qubership.automation.itf.executor.transports.holder.TransportHolder;

public class TransportClassLoader extends AbstractLoader<MeansCommunication> {

    private static final TransportClassLoader INSTANCE = new TransportClassLoader();

    private TransportClassLoader() {
        LIB = "/lib";
        PATH_PATTERN = "(mockingbird-transport+[\\w-]*)";
    }

    public static TransportClassLoader getInstance() {
        return INSTANCE;
    }

    @Override
    public MeansCommunication getInstanceClass(String className, Object... paramForConstructor)
            throws ClassNotFoundException {
        try {
            MeansCommunication meansCommunication = TransportHolder.getInstance().checkAvailability(className);
            if (Objects.nonNull(meansCommunication)) {
                return meansCommunication;
            }
            return getClass(className).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                 | InvocationTargetException e) {
            throw new ClassNotFoundException("Classloader not found for class", e);
        }
    }

    @Override
    public Class<? extends MeansCommunication> getClass(String typeName) throws ClassNotFoundException {
        try {
            return getClassLoaderHolder().computeIfAbsent(typeName, className -> {
                throw new IllegalArgumentException("Classloader not found for class " + typeName);
            }).loadClass(typeName).asSubclass(MeansCommunication.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class with type name '" + typeName + "' is not found in classloader",
                    e);
        }
    }

    @Override
    protected Class<MeansCommunication> getGenericType() {
        return MeansCommunication.class;
    }

    @Override
    protected void validateClasses(Set<Class<? extends MeansCommunication>> classes) {
        if (!classes.iterator().hasNext()) {
            throw new IllegalArgumentException("No one class found");
        }
    }
}
