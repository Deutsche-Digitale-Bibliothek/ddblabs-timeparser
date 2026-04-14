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

/**
 * Indicates whether an attempt to read a certain string from the input string
 * by {@link InputStringReader} was successful or not. If successful, the part
 * of
 * the input string just read can be returned, as well as the groups from the
 * regular expression, if used.
 */
class AcceptResult {

    private final boolean isAccepted;
    private final String parsedInputString;
    private final Matcher matcher;

    public AcceptResult(boolean isAccepted, String parsedInputString, Matcher matcher) {
        this.isAccepted = isAccepted;
        this.parsedInputString = parsedInputString;
        this.matcher = matcher;
    }

    public boolean isAccepted() {
        return this.isAccepted;
    }

    public String getParsedInputString() {
        return this.parsedInputString;
    }

    public String group(int group) {
        return this.matcher.group(group);
    }

}
