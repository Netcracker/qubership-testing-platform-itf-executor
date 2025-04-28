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

package org.qubership.automation.itf.ui.util;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link VelocityVariablesParser}.
 */
public interface VelocityVariablesListener extends ParseTreeListener {

    /**
     * Enter a parse tree produced by {@link VelocityVariablesParser#variables}.
     *
     * @param ctx the parse tree
     */
    void enterVariables(VelocityVariablesParser.VariablesContext ctx);

    /**
     * Exit a parse tree produced by {@link VelocityVariablesParser#variables}.
     *
     * @param ctx the parse tree
     */
    void exitVariables(VelocityVariablesParser.VariablesContext ctx);

    /**
     * Enter a parse tree produced by {@link VelocityVariablesParser#variable}.
     *
     * @param ctx the parse tree
     */
    void enterVariable(VelocityVariablesParser.VariableContext ctx);

    /**
     * Exit a parse tree produced by {@link VelocityVariablesParser#variable}.
     *
     * @param ctx the parse tree
     */
    void exitVariable(VelocityVariablesParser.VariableContext ctx);
}
