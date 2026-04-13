/* 
 * Copyright 2012-2026 Deutsche Digitale Bibliothek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.labs.timeparser.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a normalized output string using a list of {@link TokenWithValue}s,
 * created from a rule's input specification and an input string by
 * {@link PatternParser} and {@link InputParser}, and a list of {@link Token}s,
 * created from a rule's output specification by {@link PatternParser}.
 */
public class Outputter {

    private final List<Token> outputPatternTokens;

    public Outputter(List<Token> outputPatternTokens) {
        this.outputPatternTokens = outputPatternTokens;
    }

    public String createOutputString(List<TokenWithValue> tokensWithValues) throws IllegalStateException {
        Map<Character, TokenWithValue> tokensWithValuesByVariableName = new HashMap<>();
        for (TokenWithValue tokenWithValue : tokensWithValues) {
            if (tokenWithValue.getType() != Token.Type.TEXT) {
                tokensWithValuesByVariableName.put(tokenWithValue.getPatternValue().charAt(0), tokenWithValue);
            }
        }

        StringBuilder output = new StringBuilder();
        for (Token outputPatternToken : this.outputPatternTokens) {
            if (outputPatternToken.getType() == Token.Type.TEXT) {
                output.append(outputPatternToken.getPatternValue());
            } else {
                char variableName = outputPatternToken.getPatternValue().charAt(0);

                if (tokensWithValuesByVariableName.get(variableName) == null) {
                    throw new IllegalStateException("No input token found for variable name " + variableName);
                }

                if (outputPatternToken.getPatternValue().length() > tokensWithValuesByVariableName.get(variableName).getPatternValue().length()) {
                    throw new IllegalStateException("Output length of variable " + variableName + " greater than input length");
                }

                output.append(tokensWithValuesByVariableName.get(variableName).getInputValue().substring(0, outputPatternToken.getPatternValue().length()));
            }
        }
        return output.toString();
    }

}
