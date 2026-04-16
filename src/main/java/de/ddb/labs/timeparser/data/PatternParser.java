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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses a rule's input or output specification, yielding a list of {@link Token}s.
 */
public class PatternParser {

    /**
     * Parses a mask/pattern pair and rejects duplicate variable names.
     */
    public List<Token> parse(final String mask, final String pattern) throws IllegalStateException {
        return parse(false, mask, pattern);
    }

    /**
     * Parses a mask/pattern pair into tokens.
     *
     * @param allowDuplicateVariableNames whether repeated variable names are allowed
     * @param mask mask describing token categories
     * @param pattern concrete pattern value
     * @return parsed token sequence
     * @throws IllegalStateException if mask and pattern do not align
     */
    public List<Token> parse(final boolean allowDuplicateVariableNames, final String mask, final String pattern)
            throws IllegalStateException {
        if (mask.length() != pattern.length()) {
            throw new IllegalStateException("Mask and pattern length not equal");
        }

        final List<Token> tokens = new ArrayList<>();

        for (int i = 0; i < mask.length(); i++) {
            Token token = readGenericVariableToken(mask, pattern, i);
            if (token == null) {
                token = readMonthReplacementToken(mask, pattern, i);
            }
            if (token == null) {
                token = readWeekdayReplacementToken(mask, pattern, i);
            }
            if (token == null) {
                token = readTextToken(mask, pattern, i);
            }

            if (token != null) {
                i += token.getPatternValue().length() - 1;
                tokens.add(token);
            }
        }

        if (!allowDuplicateVariableNames) {
            removeDuplicates(tokens, pattern);
        }

        return tokens;
    }

    private void removeDuplicates(final List<Token> tokens, final String pattern) throws IllegalStateException {
        final Set<Character> variableNamesInitials = new HashSet<>();
        for (final Token token : tokens) {
            if (token.getType() == Token.Type.GENERIC_VARIABLE
                    || token.getType() == Token.Type.MONTH_REPLACEMENT_VARIABLE
                    || token.getType() == Token.Type.WEEKDAY_REPLACEMENT_VARIABLE) {
                final Character currentInitial = token.getPatternValue().charAt(0);
                if (variableNamesInitials.contains(currentInitial)) {
                    throw new IllegalStateException(
                            "Duplicate variable names for " + currentInitial + " in pattern \"" + pattern + "\"");
                } else {
                    variableNamesInitials.add(currentInitial);
                }
            }
        }
    }

    private Token readGenericVariableToken(final String mask, final String pattern, final int start) {
        Token returnToken = null;
        final StringBuilder patternValue = getPatternValue(mask, pattern, start, '#');
        if (patternValue.length() > 0) {
            returnToken = new Token(Token.Type.GENERIC_VARIABLE, patternValue.toString());
        }

        return returnToken;
    }

    private Token readMonthReplacementToken(final String mask, final String pattern, final int start) {
        Token returnToken = null;
        final StringBuilder patternValue = getPatternValue(mask, pattern, start, 'M');
        if (patternValue.length() > 0) {
            if (patternValue.length() == 2) {
                returnToken = new Token(Token.Type.MONTH_REPLACEMENT_VARIABLE, patternValue.toString());
            } else if (patternValue.length() >= 2) {
                returnToken = new Token(Token.Type.MONTH_REPLACEMENT_VARIABLE, patternValue.substring(0, 2));
            }
        }

        return returnToken;
    }

    private Token readWeekdayReplacementToken(final String mask, final String pattern, final int start) {
        Token returnToken = null;
        final StringBuilder patternValue = getPatternValue(mask, pattern, start, 'G');
        if (patternValue.length() > 0) {
            if (patternValue.length() == 2) {
                returnToken = new Token(Token.Type.WEEKDAY_REPLACEMENT_VARIABLE, patternValue.toString());
            } else if (patternValue.length() >= 2) {
                returnToken = new Token(Token.Type.WEEKDAY_REPLACEMENT_VARIABLE, patternValue.substring(0, 2));
            }
        }

        return returnToken;
    }

    private Token readTextToken(final String mask, final String pattern, final int start) throws IllegalStateException {
        Token returnToken = null;

        final StringBuilder patternValue = new StringBuilder(pattern.length());
        for (int i = start; i < mask.length(); i++) {
            final char currentMaskChar = mask.charAt(i);
            final char currentPatternChar = pattern.charAt(i);

            if (currentMaskChar == '#') {
                break;
            }
            if (i < mask.length() - 1) {
                final char nextMaskChar = mask.charAt(i + 1);
                if (currentMaskChar == 'M' && nextMaskChar == 'M' || currentMaskChar == 'G' && nextMaskChar == 'G') {
                    break;
                }
            }
            if (currentMaskChar != currentPatternChar) {
                throw new IllegalStateException("Characters do not match at position " + i + " of mask (\""
                        + mask + "\") and pattern (\"" + pattern + "\")");
            }
            patternValue.append(currentPatternChar);
        }

        if (patternValue.length() > 0) {
            returnToken = new Token(Token.Type.TEXT, patternValue.toString());
        }

        return returnToken;
    }

    private StringBuilder getPatternValue(final String mask, final String pattern, final int start, final char maskChar) {
        final char firstPatternChar = pattern.charAt(start);
        final StringBuilder patternValue = new StringBuilder(pattern.length());
        for (int i = start; i < mask.length(); i++) {
            final char currentMaskChar = mask.charAt(i);
            final char currentPatternChar = pattern.charAt(i);

            if (currentMaskChar != maskChar || currentPatternChar != firstPatternChar) {
                break;
            } else {
                patternValue.append(currentPatternChar);
            }
        }
        return patternValue;
    }
}
