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
import java.util.Objects;
import java.util.Set;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.SystemParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.constants.SystemMode;
import org.qubership.automation.itf.ui.messages.objects.template.UITemplate;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.qubership.automation.itf.ui.util.UIHelper;

import com.google.common.collect.Sets;

public class UISystem extends UIECIObject {

    private UIWrapper<Set<UITransport>> transports;
    private UIWrapper<Set<UIOperation>> operations;
    private UIWrapper<Set<UIParsingRule>> parsingRules;
    private UIWrapper<Set<UITemplate>> templates;
    private UIWrapper<String> incoming;
    private UIWrapper<String> outgoing;
    private UIWrapper<String> operationDefinition;
    private String mode;

    public UISystem() {
    }

    public UISystem(String id, String name) {
        this.setId(id);
        this.setName(name);
    }

    public UISystem(Storable system) {
        this((System) system);
    }

    public UISystem(System systemObject) {
        super(systemObject);
        defineTransports(systemObject.getTransports());
        defineOperations(systemObject.getOperations());
        defineParsingRules(systemObject.returnParsingRules());
        defineTemplates(systemObject.returnTemplates());
        setMode(Objects.toString(systemObject.getMode(), SystemMode.STUB.toString()));
        setIncoming(new UIWrapper<>(getKeyDefinitionValue(systemObject.getIncomingContextKeyDefinition())));
        setOutgoing(new UIWrapper<>(getKeyDefinitionValue(systemObject.getOutgoingContextKeyDefinition())));
        setOperationDefinition(new UIWrapper<>(getKeyDefinitionValue(systemObject.getOperationKeyDefinition())));
    }

    @Override
    public void loadChildrenByClass(Class childClass, List<Storable> children) {
        if (childClass.isAssignableFrom(TransportConfiguration.class)) {
            Set<UITransport> uiTransports = Sets.newHashSet();
            for (Storable child : children) {
                uiTransports.add(new UITransport((TransportConfiguration) child));
            }
            setTransports(new UIWrapper<>(uiTransports));
        }
        if (childClass.isAssignableFrom(SystemParsingRule.class)) {
            Set<UIParsingRule> uiParsingRules = Sets.newHashSet();
            for (Storable child : children) {
                uiParsingRules.add(new UIParsingRule((ParsingRule) child));
            }
            setParsingRules(new UIWrapper<>(uiParsingRules));
        }
        if (childClass.isAssignableFrom(SystemTemplate.class)) {
            Set<UITemplate> uiTemplates = Sets.newHashSet();
            for (Storable child : children) {
                uiTemplates.add(new UITemplate((Template) child));
            }
            setTemplates(new UIWrapper<>(uiTemplates));
        }
        if (childClass.isAssignableFrom(Operation.class)) {
            Set<UIOperation> uiOperations = Sets.newHashSet();
            for (Storable child : children) {
                uiOperations.add(new UIOperation((Operation) child));
            }
            setOperations(new UIWrapper<>(uiOperations));
        }
    }

    private String getKeyDefinitionValue(String keyDefinition) {
        return UIHelper.getDefinitionValue(keyDefinition);
    }

    public UIWrapper<Set<UITransport>> getTransports() {
        return transports;
    }

    public void setTransports(UIWrapper<Set<UITransport>> transports) {
        this.transports = transports;
    }

    public void defineTransports(Set<TransportConfiguration> transports) {
        if (this.transports == null) {
            this.transports = new UIWrapper<>();
            this.transports.setData(Sets.<UITransport>newHashSetWithExpectedSize(transports.size()));
        }
        if (transports != null) {
            for (TransportConfiguration entry : transports) {
                this.transports.getData().add(new UITransport(entry));
            }
        }
    }

    public UIWrapper<Set<UIOperation>> getOperations() {
        return operations;
    }

    public void setOperations(UIWrapper<Set<UIOperation>> operations) {
        this.operations = operations;
    }

    public void defineOperations(Set<Operation> operations) {
        if (this.operations == null) {
            this.operations = new UIWrapper<>();
            this.operations.setData(Sets.<UIOperation>newHashSetWithExpectedSize(operations.size()));
        }
        if (operations != null) {
            for (Operation entry : operations) {
                this.operations.getData().add(new UIOperation(entry));
            }
        }
    }

    public UIWrapper<Set<UIParsingRule>> getParsingRules() {
        return parsingRules;
    }

    public void setParsingRules(UIWrapper<Set<UIParsingRule>> parsingRules) {
        this.parsingRules = parsingRules;
    }

    public void defineParsingRules(Set<ParsingRule> parsingRules) {
        if (this.parsingRules == null) {
            this.parsingRules = new UIWrapper<>();
            this.parsingRules.setData(Sets.<UIParsingRule>newHashSetWithExpectedSize(parsingRules.size()));
        }
        if (parsingRules != null) {
            for (ParsingRule entry : parsingRules) {
                this.parsingRules.getData().add(new UIParsingRule(entry));
            }
        }
    }

    public UIWrapper<Set<UITemplate>> getTemplates() {
        return templates;
    }

    public void setTemplates(UIWrapper<Set<UITemplate>> templates) {
        this.templates = templates;
    }

    public void defineTemplates(Set<Template> templates) {
        if (this.templates == null) {
            this.templates = new UIWrapper<>();
            this.templates.setData(Sets.<UITemplate>newHashSetWithExpectedSize(templates.size()));
        }
        if (templates != null) {
            for (Template entry : templates) {
                this.templates.getData().add(new UITemplate(entry));
            }
        }
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

    public UIWrapper<String> getOperationDefinition() {
        return operationDefinition;
    }

    public void setOperationDefinition(UIWrapper<String> operationDefinition) {
        this.operationDefinition = operationDefinition;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
