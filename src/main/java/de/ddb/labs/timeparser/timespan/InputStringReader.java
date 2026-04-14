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
 * methods are
 * used to attempt to read certain strings ({@code char}s, {@link String}s or
 * {@link Pattern}s) from a given position within the input string. The success
 * of
 * each attempt is indicated by an {@link AcceptResult}. If the attempt is
 * successful, the position within the input string is immediately moved to the
 * position
 * after the string just read.
 */
class InputStringReader {

    private final String inputString;

    public InputStringReader(String inputString) {
        this.inputString = inputString;
    }

    public AcceptResult tryToAccept(Position p, char c) {
        boolean isAccepted = p.get() < this.inputString.length() && this.inputString.charAt(p.get()) == c;
        String parsedInputString = null;
        if (isAccepted) {
            parsedInputString = String.valueOf(c);
            p.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, null);
    }

    public AcceptResult tryToAccept(Position p, String string) {
        int startIndex = p.get();
        int endIndex = p.get() + string.length();
        boolean isAccepted = endIndex <= this.inputString.length()
                && this.inputString.substring(startIndex, endIndex).equals(string);
        String parsedInputString = null;
        if (isAccepted) {
            parsedInputString = string;
            p.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, null);
    }

    public AcceptResult tryToAccept(Position p, Pattern pattern) {
        final Matcher m = pattern.matcher(this.inputString);
        m.region(p.get(), this.inputString.length());
        boolean isAccepted = m.lookingAt();
        String parsedInputString = null;
        if (isAccepted) {
            parsedInputString = m.group(0);
            p.move(parsedInputString.length());
        }

        return new AcceptResult(isAccepted, parsedInputString, m);
    }

}
