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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VelocityVariablesParser extends Parser {
    public static final int
            Variable = 1;
    public static final int RULE_variables = 0;
    public static final int RULE_variable = 1;
    public static final String[] ruleNames = {
            "variables", "variable"
    };
    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    public static final String _serializedATN =
            "\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\3\22\4\2\t\2\4\3"
                    + "\t\3\3\2\3\2\3\2\7\2\n\n\2\f\2\16\2\r\13\2\3\3\3\3\3\3\3\3\2\2\4\2\4\2"
                    + "\2\20\2\13\3\2\2\2\4\16\3\2\2\2\6\7\5\4\3\2\7\b\b\2\1\2\b\n\3\2\2\2\t"
                    + "\6\3\2\2\2\n\r\3\2\2\2\13\t\3\2\2\2\13\f\3\2\2\2\f\3\3\2\2\2\r\13\3\2"
                    + "\2\2\16\17\7\3\2\2\17\20\b\3\1\2\20\5\3\2\2\2\3\13";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    private static final String[] _LITERAL_NAMES = {
    };
    private static final String[] _SYMBOLIC_NAMES = {
            null, "Variable"
    };
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    static {
        RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION);
    }

    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }

    public VelocityVariablesParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "VelocityVariables.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public final VariablesContext variables() throws RecognitionException {
        VariablesContext _localctx = new VariablesContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_variables);

        ((VariablesContext) _localctx).variablesList = new ArrayList<String>();

        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(9);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Variable) {
                    {
                        {
                            setState(4);
                            ((VariablesContext) _localctx).variable = variable();

                            if (!_localctx.variablesList.contains((((VariablesContext) _localctx).variable != null
                                    ? _input.getText(((VariablesContext) _localctx).variable.start,
                                            ((VariablesContext) _localctx).variable.stop) : null))) {
                                _localctx.variablesList.add((((VariablesContext) _localctx).variable != null
                                        ? _input.getText(((VariablesContext) _localctx).variable.start,
                                                ((VariablesContext) _localctx).variable.stop) : null));
                            }

                        }
                    }
                    setState(11);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
            _ctx.stop = _input.LT(-1);
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public final VariableContext variable() throws RecognitionException {
        VariableContext _localctx = new VariableContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_variable);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(12);
                ((VariableContext) _localctx).Variable = match(Variable);
                _localctx.varName = (((VariableContext) _localctx).Variable != null
                        ? ((VariableContext) _localctx).Variable.getText() : null);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class VariablesContext extends ParserRuleContext {
        public List<String> variablesList;
        public VariableContext variable;

        public VariablesContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        public List<VariableContext> variable() {
            return getRuleContexts(VariableContext.class);
        }

        public VariableContext variable(int i) {
            return getRuleContext(VariableContext.class, i);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variables;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VelocityVariablesListener) {
                ((VelocityVariablesListener) listener).enterVariables(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VelocityVariablesListener) {
                ((VelocityVariablesListener) listener).exitVariables(this);
            }
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VelocityVariablesVisitor) {
                return ((VelocityVariablesVisitor<? extends T>) visitor).visitVariables(this);
            } else {
                return visitor.visitChildren(this);
            }
        }
    }

    public static class VariableContext extends ParserRuleContext {
        public String varName;
        public Token Variable;

        public VariableContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        public TerminalNode Variable() {
            return getToken(VelocityVariablesParser.Variable, 0);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variable;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VelocityVariablesListener) {
                ((VelocityVariablesListener) listener).enterVariable(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VelocityVariablesListener) {
                ((VelocityVariablesListener) listener).exitVariable(this);
            }
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VelocityVariablesVisitor) {
                return ((VelocityVariablesVisitor<? extends T>) visitor).visitVariable(this);
            } else {
                return visitor.visitChildren(this);
            }
        }
    }
}
