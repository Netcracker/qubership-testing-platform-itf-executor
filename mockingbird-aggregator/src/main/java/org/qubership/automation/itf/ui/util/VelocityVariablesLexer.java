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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VelocityVariablesLexer extends Lexer {
    public static final int
            Variable = 1;
    public static final String[] ruleNames = {
            "Variable", "VariableRef", "VARLINK", "LETTER", "OPTIONAL", "DELIMITER",
            "WHITESPACE", "DIGIT", "IDENTIFIER", "Namepart"
    };
    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    public static final String _serializedATN =
            "\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\3L\b\1\4\2\t\2\4"
                    + "\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"
                    + "\13\3\2\3\2\5\2\32\n\2\3\2\3\2\3\2\3\2\3\2\5\2!\n\2\3\3\6\3$\n\3\r\3\16"
                    + "\3%\3\3\7\3)\n\3\f\3\16\3,\13\3\3\3\5\3/\n\3\3\3\7\3\62\n\3\f\3\16\3\65"
                    + "\13\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\5\nE\n\n"
                    + "\3\13\3\13\6\13I\n\13\r\13\16\13J\2\2\f\3\3\5\2\7\2\t\2\13\2\r\2\17\2"
                    + "\21\2\23\2\25\2\3\2\4\4\2C\\c|\3\2\62;K\2\3\3\2\2\2\3\27\3\2\2\2\5#\3"
                    + "\2\2\2\7\66\3\2\2\2\t8\3\2\2\2\13:\3\2\2\2\r<\3\2\2\2\17>\3\2\2\2\21@"
                    + "\3\2\2\2\23D\3\2\2\2\25H\3\2\2\2\27\31\5\7\4\2\30\32\5\13\6\2\31\30\3"
                    + "\2\2\2\31\32\3\2\2\2\32 \3\2\2\2\33\34\7}\2\2\34\35\5\5\3\2\35\36\7\177"
                    + "\2\2\36!\3\2\2\2\37!\5\5\3\2 \33\3\2\2\2 \37\3\2\2\2!\4\3\2\2\2\"$\5\23"
                    + "\n\2#\"\3\2\2\2$%\3\2\2\2%#\3\2\2\2%&\3\2\2\2&*\3\2\2\2\')\5\25\13\2("
                    + "\'\3\2\2\2),\3\2\2\2*(\3\2\2\2*+\3\2\2\2+\63\3\2\2\2,*\3\2\2\2-/\5\r\7"
                    + "\2.-\3\2\2\2./\3\2\2\2/\60\3\2\2\2\60\62\5\25\13\2\61.\3\2\2\2\62\65\3"
                    + "\2\2\2\63\61\3\2\2\2\63\64\3\2\2\2\64\6\3\2\2\2\65\63\3\2\2\2\66\67\7"
                    + "&\2\2\67\b\3\2\2\289\t\2\2\29\n\3\2\2\2:;\7#\2\2;\f\3\2\2\2<=\7\60\2\2"
                    + "=\16\3\2\2\2>?\7a\2\2?\20\3\2\2\2@A\t\3\2\2A\22\3\2\2\2BE\5\t\5\2CE\5"
                    + "\17\b\2DB\3\2\2\2DC\3\2\2\2E\24\3\2\2\2FI\5\23\n\2GI\5\21\t\2HF\3\2\2"
                    + "\2HG\3\2\2\2IJ\3\2\2\2JH\3\2\2\2JK\3\2\2\2K\26\3\2\2\2\f\2\31 %*.\63D"
                    + "HJ\2";
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
    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

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

    public VelocityVariablesLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
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
    public String[] getModeNames() {
        return modeNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }
}
