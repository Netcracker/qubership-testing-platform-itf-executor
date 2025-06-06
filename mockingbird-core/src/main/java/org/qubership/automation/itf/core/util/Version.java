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

import java.io.IOException;
import java.util.Properties;

public class Version {

    public static final String MOCKINGBIRD_VERSION;

    static {
        Properties properties = new Properties();
        String version;
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));
            version = properties.getProperty("mockingbird.version");
        } catch (IOException e) {
            version = "Unknown";
        }
        MOCKINGBIRD_VERSION = version;
    }
}
