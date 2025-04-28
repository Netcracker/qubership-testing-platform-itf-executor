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

package org.qubership.automation.itf.core.report.impl;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.report.LinkCollectorConfiguration;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.util.annotation.Parameter;
import org.qubership.automation.itf.core.util.engine.TemplateEngineFactory;
import org.qubership.automation.itf.core.util.report.LinkCollector;

public class TemplateBasedLinkCollector implements LinkCollector {

    @Parameter(shortName = "system",
            longName = "System",
            description = "System in this environment to collect reference",
            optional = true)
    private System system;

    @Parameter(shortName = "template",
            longName = "Template to render",
            description = "Template which will be rendered as url",
            optional = true)
    private String template;

    @Parameter(shortName = "bv.conf.path",
            longName = "BV configuration path",
            description = "bv_environment_name/bv_system_name/bv_connection_name",
            optional = true)
    private String path;

    @Override
    public Pair<String, String> collect(TcContext context, LinkCollectorConfiguration configuration) {
        String url = TemplateEngineFactory.process(context.getEnvironmentById(), configuration.get("template"),
                InstanceContext.from(context, null));

        /* url can contain server:port or be without it.
            1. Deutsche_Telecom variant:
                - url like '/solutions/integration/logging/integration_sessions_logs.jsp?correlationKey=$tc.Params.CRM'
                - server is computed from environment --> getOutbounds --> find outbound by system --> get
                corresponding server --> get server url
         */
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return Pair.of(configuration.getName(), url);
        }
        String base = "unknownhost/";
        String strSystem = configuration.get("system");
        for (Map.Entry<System, Server> entry : context.getEnvironmentById().getOutbound().entrySet()) {
            if (entry.getKey().getID().toString().equals(strSystem)) {
                base = entry.getValue().getUrl();
                if (!url.startsWith("/") && !base.endsWith("/")) {
                    url = "/" + url;
                }
                break;
            }
        }
        return Pair.of(configuration.getName(), base + url);
    }

    @Override
    public boolean common() {
        return false;
    }
}
