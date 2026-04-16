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
package de.ddb.labs.timeparser.timespan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tries to read desired strings from an input string. The input string is a
 * normalized string given to {@link TimeSpanParser}. The {@code tryTo*()}
 * methods are used to attempt to read certain strings ({@code char}s,
 * {@link String}s or {@link Pattern}s) from a given position within the input
 * string. The success of each attempt is indicated by an {@link AcceptResult}. If
 * the attempt is successful, the position within the input string is immediately
 * moved to the position after the string just read.
 */
final class InputStringReader {

    private final String inputString;

    InputStringReader(final String inputString) {
        this.inputString = inputString;
    }

    AcceptResult tryToAccept(final Position position, final char character) {
        final boolean isAccepted = position.getPos() < this.inputString.length()
                && this.inputString.charAt(position.getPos()) == character;
        String parsedInputString = null;
        if (isAccepted) {
            parsedInputString = String.valueOf(character);
            position.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, null);
    }

    AcceptResult tryToAccept(final Position position, final String string) {
        final int startIndex = position.getPos();
        final int endIndex = position.getPos() + string.length();
        final boolean isAccepted = endIndex <= this.inputString.length()
                && this.inputString.substring(startIndex, endIndex).equals(string);
        String parsedInputString = null;
        if (isAccepted) {
            parsedInputString = string;
            position.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, null);
    }

    AcceptResult tryToAccept(final Position position, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(this.inputString);
        matcher.region(position.getPos(), this.inputString.length());
        final boolean isAccepted = matcher.lookingAt();
        String parsedInputString = null;
        if (isAccepted) {
            parsedInputString = matcher.group(0);
            position.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, matcher);
    }
}
