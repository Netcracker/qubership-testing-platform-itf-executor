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

package org.qubership.automation.itf.ui.messages.objects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.constants.Mep;
import org.qubership.automation.itf.ui.messages.objects.template.UITemplate;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.qubership.automation.itf.ui.util.UIHelper;

import com.google.common.collect.Lists;

public class UIOperation extends UIObject {

    private UIWrapper<UITransport> transport;
    private UIWrapper<UIDefinitionKey> definitionKey;
    private UIWrapper<List<UIParsingRule>> parsingRules;
    private UIWrapper<List<UITemplate>> templates;
    private UIWrapper<List<UISituation>> situations;
    private String mep;
    private UIWrapper<String> incoming;
    private UIWrapper<String> outgoing;

    public UIOperation() {
    }

    public UIOperation(Storable operation) {
        this((Operation) operation);
    }

    public UIOperation(Operation operation) {
        super(operation);
        TransportConfiguration transport = operation.getTransport();
        if (transport != null) {
            this.transport = new UIWrapper<>(new UITransport(transport));
        }
        wrapDefinitionKey(operation.getOperationDefinitionKey());
        wrapMep(operation.getMep());
    }

    public void fillForQuickDisplay(@NotNull Operation operation) {
        this.setId(operation.getID().toString());
        this.setName(operation.getName());
        this.setVersion(NumberUtils.toInt(String.valueOf(operation.getVersion()), -1));
        this.wrapDefinitionKey(operation.getOperationDefinitionKey());
        this.wrapMep(operation.getMep());
        TransportConfiguration transport = operation.getTransport();
        if (transport != null) {
            UITransport uiTransport = new UITransport();
            uiTransport.fillForQuickDisplay(transport);
            this.setTransport(new UIWrapper<>(uiTransport));
        }
    }

    @Override
    public void loadChildrenByClass(Class childClass, List<Storable> children) {
        if (childClass.isAssignableFrom(OperationParsingRule.class)) {
            List<UIParsingRule> uiParsingRules = Lists.newArrayList();
            for (Storable child : children) {
                uiParsingRules.add(new UIParsingRule((ParsingRule) child));
            }
            setParsingRules(new UIWrapper<>(uiParsingRules));
        }
        if (childClass.isAssignableFrom(Situation.class)) {
            List<UISituation> uiSituations = Lists.newArrayList();
            for (Storable child : children) {
                uiSituations.add(new UISituation((Situation) child));
            }
            setSituations(new UIWrapper<>(uiSituations));
        }
        if (childClass.isAssignableFrom(OperationTemplate.class)) {
            List<UITemplate> uiTemplates = Lists.newArrayList();
            for (Storable child : children) {
                uiTemplates.add(new UITemplate((Template) child));
            }
            setTemplates(new UIWrapper<>(uiTemplates));
        }
    }

    public UIWrapper<List<UISituation>> getSituations() {
        return situations;
    }

    public void setSituations(UIWrapper<List<UISituation>> situations) {
        this.situations = situations;
    }

    private String getContextDefinitionValue(String keyDefinition) {
        return UIHelper.getDefinitionValue(keyDefinition);
    }

    public UIWrapper<UITransport> getTransport() {
        return transport;
    }

    public void setTransport(UIWrapper<UITransport> transport) {
        this.transport = transport;
    }

    public UIWrapper<UIDefinitionKey> getDefinitionKey() {
        return definitionKey;
    }

    public void setDefinitionKey(UIWrapper<UIDefinitionKey> definitionKey) {
        this.definitionKey = definitionKey;
    }

    public void wrapDefinitionKey(String operationDefinitionKey) {
        if (!StringUtils.isEmpty(operationDefinitionKey)) {
            definitionKey = new UIWrapper<>(new UIDefinitionKey(operationDefinitionKey));
        }
    }

    public UIWrapper<List<UIParsingRule>> getParsingRules() {
        return parsingRules;
    }

    public void setParsingRules(UIWrapper<List<UIParsingRule>> parsingRules) {
        this.parsingRules = parsingRules;
    }

    public String getMep() {
        return mep;
    }

    public void setMep(String mep) {
        this.mep = mep;
    }

    public void wrapMep(Mep mep) {
        if (mep != null) {
            this.mep = mep.toString();
        }
    }

    public UIWrapper<List<UITemplate>> getTemplates() {
        return templates;
    }

    public void setTemplates(UIWrapper<List<UITemplate>> templates) {
        this.templates = templates;
    }

    public UIWrapper<String> getIncoming() {
        return incoming;
    }

    public void setIncoming(UIWrapper<String> incoming) {
        this.incoming = incoming;
    }

    public UIWrapper<String> getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(UIWrapper<String> outgoing) {
        this.outgoing = outgoing;
    }
}
