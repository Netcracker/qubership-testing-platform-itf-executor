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

package org.qubership.automation.itf.core.template.velocity.directives;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.util.engine.CounterEngine;
import org.qubership.automation.itf.core.util.engine.TemplateEngine;
import org.qubership.automation.itf.core.util.exception.CounterLimitIsExhaustedException;

import com.google.common.collect.Sets;

public class NextIndex extends Directive {

    @Override
    public String getName() {
        return "next_index";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node) throws IOException
            , ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) != null) {
                String counterFormat = String.valueOf(node.jjtGetChild(i).value(internalContextAdapter));
                try {
                    rendData(counterFormat, internalContextAdapter, writer);
                } catch (CounterLimitIsExhaustedException e) {
                    throw new IOException(e);
                }
            } else {
                rsvc.getLog().warn("next_index: format is null");
            }
        }
        return true;
    }

    private void rendData(String counterFormat, InternalContextAdapter internalContextAdapter, Writer writer) throws CounterLimitIsExhaustedException {
        Set<Object> owners = prepareOwners(internalContextAdapter);
        String index = CounterEngine.getInstance().nextIndex(owners, counterFormat);
        if (index != null) {
            rsvc.evaluate(internalContextAdapter, writer, TemplateEngine.LOG_TAG, index);
        } else {
            throw new VelocityException("Unable to next index of operation by counter format '" + counterFormat + "' "
                    + "counter not found.");
        }
    }

    //TODO the method is hardcode. Need use to iterate all owners on the context, but I don't know how to do it.
    //The implemented for NITP-4089
    private Set<Object> prepareOwners(InternalContextAdapter internalContextAdapter) {
        Set<Object> owners = Sets.newHashSet();
        Storable operation = Storable.class.cast(internalContextAdapter.getBaseContext().get(TemplateEngine.OPERATION));
        Storable environment =
                Storable.class.cast(internalContextAdapter.getBaseContext().get(TemplateEngine.ENVIRONMENT));
        if (Objects.nonNull(operation) && Objects.nonNull(environment)) {
            owners.add(operation.getID());
            owners.add(environment.getID());
        } else {
            throw new VelocityException("Unable to next index by operation. Operation or Environment is null ");
        }
        return owners;
    }
}
