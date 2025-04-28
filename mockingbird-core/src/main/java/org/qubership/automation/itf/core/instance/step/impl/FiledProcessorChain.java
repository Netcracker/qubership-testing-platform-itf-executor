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

package org.qubership.automation.itf.core.instance.step.impl;

import java.util.Set;

import org.qubership.automation.itf.core.instance.step.impl.chain.AbstractChainUnit;
import org.qubership.automation.itf.core.instance.step.impl.chain.LoadTemplate;
import org.qubership.automation.itf.core.instance.step.impl.chain.UserSettings;
import org.qubership.automation.itf.core.model.jpa.context.InstanceContext;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.descriptor.PropertyDescriptor;

import com.google.common.collect.Sets;

public class FiledProcessorChain {

    private static final FiledProcessorChain FILED_PROCESSOR_CHAIN = new FiledProcessorChain();
    private final Set<AbstractChainUnit> chain = Sets.newLinkedHashSet();

    private FiledProcessorChain() {
        this.chain.add(new UserSettings());
        this.chain.add(new LoadTemplate());
    }

    public static FiledProcessorChain getInstance() {
        return FILED_PROCESSOR_CHAIN;
    }

    public void process(PropertyDescriptor descriptor, Message message, InstanceContext context) {
        chain.forEach(unit -> {
            if (unit.isAcceptable(descriptor)) {
                unit.accept(descriptor, message, context);
            }
        });
    }
}
