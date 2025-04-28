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

package org.qubership.automation.itf.transport.sql.outbound;

import static org.qubership.automation.itf.transport.sql.SqlTransportConstants.DATA_SOURCE;

import org.apache.camel.builder.RouteBuilder;

import com.google.common.base.Strings;

public class SqlRouteBuilder extends RouteBuilder {

    private final String options;

    public SqlRouteBuilder(String options) {
        this.options = options;
    }

    @Override
    public void configure() {
        if ((Strings.isNullOrEmpty(options))) {
            from("direct:start").to("jdbc:" + DATA_SOURCE);
        } else {
            from("direct:start").to("jdbc:" + DATA_SOURCE + options);
        }
    }
}
