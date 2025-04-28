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

import static org.qubership.automation.itf.core.template.velocity.VelocityTemplateEngine.extractProjectIdFromContextAdapter;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Objects;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.UserDataManager;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;

public class SetUserData extends Directive {

    @Override
    public String getName() {
        return "set_userdata";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        if (node.jjtGetNumChildren() < 2 || node.jjtGetNumChildren() > 3) {
            writer.append("Incorrect #set_userdata directive format.\n" + " Please check parameters:\n"
                    + "       where \"UPSERT\" is for PostgreSql only (see PostgreSql documentation),\n"
                    + "  1. Action: \"SELECT\" / \"INSERT\" / \"UPDATE\" / \"DELETE\" / \"UPSERT\" ,\n"
                    + "  2. Key: Unique row identifier (String(50)),\n"
                    + "  3. Text: Text to store (String) - only for INSERT/UPDATE actions");
            return true;
        }

        String action = String.valueOf(node.jjtGetChild(0).value(internalContextAdapter));
        String key = String.valueOf(node.jjtGetChild(1).value(internalContextAdapter));
        String value = "";
        if (node.jjtGetNumChildren() == 3) {
            value = String.valueOf(node.jjtGetChild(2).value(internalContextAdapter));
        }
        BigInteger projectId = extractProjectIdFromContextAdapter(internalContextAdapter);
        if (Objects.nonNull(projectId)) {
            writer.append(CoreObjectManager.getInstance().getSpecialManager(StubProject.class, UserDataManager.class)
                    .setUserData(action.toUpperCase(), key, value, projectId));
        } else {
            writer.append("Cannot execute #set_userdata directive, due to projectId identification failed.");
        }
        return true;
    }

}
