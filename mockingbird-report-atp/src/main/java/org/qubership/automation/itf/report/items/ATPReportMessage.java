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

package org.qubership.automation.itf.report.items;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;
import org.qubership.automation.itf.core.util.constants.Status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class ATPReportMessage extends ATPReportItem {

    protected Throwable exception;
    protected TcContext tcContext;
    protected Status status;
    protected Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ATPReportMessage(AbstractInstance object) {
        super(object);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public void setTcContext(TcContext tcContext) {
        this.tcContext = tcContext;
    }

    public String buildSimpleSnapShotRam2(Message message, Collection<MessageParameter> messageParameters,
                                          String contextMessage) {
        return buildSimpleSnapShotRam2(message, messageParameters, contextMessage, null);
    }

    public String buildSimpleSnapShotRam2(Message message, Collection<MessageParameter> messageParameters,
                                          String contextMessage, Message responseMessage) {
        HtmlBuilder builder = new HtmlBuilder();
        builder.beginDiv();
        builder.beginTable("Properties");
        builder.addHeaders("Key", "Value");
        builder.addRow("Message",
                "<a onclick='atp_ram_toggle(this)'>Collapse</a><pre><div>"
                        + StringEscapeUtils.escapeXml10(message == null ? "" : message.getText()) + "</div></pre>");
        if (message != null) {
            appendPropertiesFromMessage(message, builder, "Headers");
            appendMessageParameters(messageParameters, builder);
        }
        if (responseMessage != null) {
            builder.addRow("Response Message",
                    "<a onclick='atp_ram_toggle(this)'>Collapse</a><pre><div>"
                            + StringEscapeUtils.escapeXml10(responseMessage.getText()) + "</div></pre>");
            appendPropertiesFromMessage(responseMessage, builder, "Response Headers");
        }
        builder.addRow("Step Context", "<pre>" + StringEscapeUtils.escapeXml10(contextMessage) + "</pre>");
        String tcJson = GSON.toJson(tcContext); //for pretty-print
        builder.addRow("Testcase Context",
                "<a onclick='atp_ram_toggle(this)'>Collapse</a><pre><div>" + StringEscapeUtils.escapeXml10(tcJson)
                        + "</div></pre>");
        builder.endTable();
        builder.endDiv();
        return builder.build();
    }

    private void appendMessageParameters(Collection<MessageParameter> messageParameters, HtmlBuilder builder) {
        builder.beginRow().cell("Message Parameters").beginCell().beginTable();
        addMessageParameters(builder, messageParameters);
        builder.endTable().endCell().endRow();
    }

    private void appendPropertiesFromMessage(Message message, HtmlBuilder builder, String tableTitle) {
        addProperties(builder, message.getConnectionProperties());
        builder.beginRow().cell(tableTitle).beginCell().beginTable();
        addProperties(builder, message.getHeaders());
        builder.endTable().endCell().endRow();
    }

    private void addProperties(HtmlBuilder builder, Map<String, ?> properties) {
        Object value;
        for (Map.Entry<String, ?> property : properties.entrySet()) {
            value = property.getValue();
            builder.addRow(property.getKey(), value != null ? value.toString() : "null");
        }
    }

    private void addMessageParameters(HtmlBuilder builder, Collection<MessageParameter> properties) {
        if (properties == null) {
            return;
        }
        for (MessageParameter property : properties) {
            List<String> multipleValue = property.getMultipleValue();
            if (multipleValue != null) {
                for (String value : multipleValue) {
                    builder.addRow(property.getParamName(), value);
                }
            }
        }
    }
}
