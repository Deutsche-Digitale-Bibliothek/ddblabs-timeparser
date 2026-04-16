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
package de.ddb.labs.timeparser.internal;

import de.ddb.labs.timeparser.rule.Rule;
import java.util.List;
import lombok.Getter;

/**
 * Precomputed rule matching metadata used for fast candidate lookup.
 */
@Getter
public final class RuleCandidate {
    private final Rule rule;
    private final String inputMask;
    private final int compactLength;
    private final int hashCount;
    private final boolean periodLiteral;

    public RuleCandidate(final Rule rule, final List<String> periodRules) {
        this.rule = rule;
        this.inputMask = rule.getInputMask();
        this.compactLength = countNonWhitespace(this.inputMask);
        this.hashCount = countCharacterOccurrences(this.inputMask, '#');
        this.periodLiteral = periodRules.contains(this.inputMask);
    }

    private static int countCharacterOccurrences(final String string, final char character) {
        int count = 0;
        for (int index = 0; index < string.length(); index++) {
            if (string.charAt(index) == character) {
                count++;
            }
        }
        return count;
    }

    private static int countNonWhitespace(final String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isWhitespace(value.charAt(index))) {
                count++;
            }
        }
        return count;
    }
}
