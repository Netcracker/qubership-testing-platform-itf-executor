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

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link VelocityVariablesParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface VelocityVariablesVisitor<T> extends ParseTreeVisitor<T> {

    /**
     * Visit a parse tree produced by {@link VelocityVariablesParser#variables}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitVariables(VelocityVariablesParser.VariablesContext ctx);

    /**
     * Visit a parse tree produced by {@link VelocityVariablesParser#variable}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitVariable(VelocityVariablesParser.VariableContext ctx);
}
