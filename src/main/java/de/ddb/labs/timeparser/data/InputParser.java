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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ddb.labs.timeparser.replacement.Replacement;

/**
 * Parses an input string using a list of {@link Token}s, created from a rule's
 * input specification by
 * {@link PatternParser}, yielding a list of {@link TokenWithValue}s.
 */
public class InputParser {

    private static final Pattern PATTERN_DIGITS = Pattern.compile("\\d+");
    private static final String LOGEXPR_WITH = "\" with ";

    private final List<Token> patternTokens;
    private final List<Replacement> monthReplacements;
    private final List<Replacement> weekdayReplacements;

    public InputParser(
            final List<Token> pattern,
            final List<Replacement> monthReplacements,
            final List<Replacement> weekdayReplacements) {
        this.patternTokens = List.copyOf(pattern);
        this.monthReplacements = List.copyOf(monthReplacements);
        this.weekdayReplacements = List.copyOf(weekdayReplacements);
    }

    /**
     * Applies the configured token pattern to a normalized input string.
     *
     * @param inputString normalized input string
     * @return tokens enriched with the concrete input values
     * @throws IllegalStateException if the input does not match the configured pattern
     */
    public List<TokenWithValue> parseInputString(String inputString) throws IllegalStateException {
        final List<TokenWithValue> tokens = new ArrayList<>();

        inputString = Replacement.applyAll(inputString, this.monthReplacements);
        inputString = Replacement.applyAll(inputString, this.weekdayReplacements);

        int currentInputStringPosition = 0;
        for (final Token patternToken : this.patternTokens) {
            final int patternValueLength = patternToken.getPatternValue().length();
            ensureInputHasRemainingCharacters(inputString, currentInputStringPosition, patternValueLength);

            final String tokenInputValue = inputString.substring(currentInputStringPosition,
                    currentInputStringPosition + patternValueLength);

            validateTokenInput(patternToken, inputString, currentInputStringPosition, tokenInputValue);

            tokens.add(new TokenWithValue(patternToken.getType(), patternToken.getPatternValue(), tokenInputValue));
            currentInputStringPosition += patternValueLength;
        }
        if (currentInputStringPosition < inputString.length()) {
            throw new IllegalStateException("Input string too long; cannot parse \""
                    + inputString
                    + LOGEXPR_WITH
                    + this.patternTokens.toString());
        }

        return tokens;
    }

    private void ensureInputHasRemainingCharacters(final String inputString, final int currentInputStringPosition,
            final int patternValueLength) {
        if (currentInputStringPosition + patternValueLength > inputString.length()) {
            throw new IllegalStateException("Input string too short; cannot parse \""
                    + inputString
                    + LOGEXPR_WITH
                    + this.patternTokens.toString());
        }
    }

    private void validateTokenInput(final Token patternToken, final String inputString,
            final int currentInputStringPosition, final String tokenInputValue) {
        if (patternToken.getType() == Token.Type.TEXT
                && !patternToken.getPatternValue().equals(tokenInputValue)) {
            throw new IllegalStateException(
                    "Input string's text does not match pattern's text at position "
                            + currentInputStringPosition
                            + "; cannot parse \""
                            + inputString
                            + LOGEXPR_WITH
                            + this.patternTokens.toString());
        }

        if (patternToken.getType() == Token.Type.GENERIC_VARIABLE) {
            final Matcher matcher = PATTERN_DIGITS.matcher(tokenInputValue);
            if (!matcher.lookingAt()) {
                throw new IllegalStateException("Number expected in input string at position "
                        + currentInputStringPosition
                        + "; cannot parse \""
                        + inputString
                        + LOGEXPR_WITH
                        + this.patternTokens.toString());
            }
        }
    }
}
