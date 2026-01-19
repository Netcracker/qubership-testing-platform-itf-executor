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

package org.qubership.automation.itf.ui.controls;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.reflections.Reflections;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

public class EndpointDocBuilder {

    private PrintStream out = System.out;

    @Test
    public void export() throws IOException {
        FileUtils.forceMkdir(new File("./docs/"));
        Reflections reflections = new Reflections("org.qubership.automation.itf.ui.controls");
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(RestController.class);
        PrintStream printStream = null;
        for (Class<?> clazz : typesAnnotatedWith) {
            try {
                printStream = new PrintStream(Paths.get("./docs/", clazz.getSimpleName() + ".txt").toFile());
                out = printStream;
                printRequestMethodsForClass(clazz);
            } finally {
                IOUtils.closeQuietly(printStream);
            }
        }
    }

    public void printRequestMethodsForClass(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                getOut().println("======================================");
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                getOut().printf("Endpoint %s, method %s. Java method: %s\n", Arrays.toString(annotation.value()),
                        Arrays.toString(annotation.method()), method.getName());
                getOut().printf("Params: %s\n", getParmTypesAsString(method));
                getOut().printf("Returns: %s\n", method.getReturnType().getSimpleName());
                getOut().println("======================================\n");
            }
        }
    }

    private PrintStream getOut() {
        return out;
    }

    private String getParmTypesAsString(Method method) {
        Set<String> set = Sets.newLinkedHashSet();
        for (Parameter parameter : method.getParameters()) {
            set.add(String.format("%s %s %s", getAnnotations(parameter), parameter.getType().getSimpleName(),
                    parameter.getName()));
        }
        return Arrays.toString(set.toArray());
    }

    private String getAnnotations(Parameter parameter) {
        Set<String> set = Sets.newLinkedHashSet();
        for (Annotation annotation : parameter.getAnnotations()) {
            set.add(annotation.annotationType().getSimpleName());
            if (RequestParam.class.isAssignableFrom(annotation.getClass())) {
                set.add(((RequestParam) annotation).value());
            }
        }
        return Arrays.toString(set.toArray());
    }
}
