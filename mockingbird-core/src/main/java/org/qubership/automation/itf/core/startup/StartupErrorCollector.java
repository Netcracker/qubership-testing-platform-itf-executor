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

package org.qubership.automation.itf.core.startup;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.Lists;

public class StartupErrorCollector {

    private static final StartupErrorCollector INSTANCE = new StartupErrorCollector();
    private LinkedList<Error> errors = Lists.newLinkedList();

    public static StartupErrorCollector getInstance() {
        return INSTANCE;
    }

    public void addError(@Nonnull String place, @Nonnull String message, @Nullable Throwable exception) {
        addError(new Error(place, message, exception));
    }

    public void addError(@Nonnull Error error) {
        errors.add(error);
    }

    public List<Error> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Deprecated //If this functional is necessary remove annotation and implement method.
    public void resolve(String id) {
        throw new NotImplementedException("Resolving startup errors is not implemented yet");
    }

    public void cleanUp() {
        errors.clear();
    }
}
