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

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.qubership.automation.itf.core.util.config.Config;

public class SetConfig extends Directive {
    @Override
    public String getName() {
        return "set_config";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        if (node.jjtGetNumChildren() != 2) {
            writer.append("Incorrect #set_config directive format. Check parameters: Parameter#1 - config property "
                    + "name, Parameter#2 - property value");
            return true;
        }

        //TODO: revise the below code in the multi-project ITF. Currently this functionality is turned OFF, due to
        // cross-project and config safety emergencies
        boolean isSingleProjectInstance = true;
        if (isSingleProjectInstance) {
            writer.append("#set_config directive is turned off in the single-project instance. Please contact dev "
                    + "team.");
        } else {
            String property = String.valueOf(node.jjtGetChild(0).value(internalContextAdapter));
            String propertyValue = String.valueOf(node.jjtGetChild(1).value(internalContextAdapter));
            Config.getConfig().addProperty("project." + property, propertyValue);
        }
        return true;
    }
}
