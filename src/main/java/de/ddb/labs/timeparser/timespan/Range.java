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

public class Range {

    private final String parsedInputString;
    private final RangeType type;

    public enum RangeType {
        FROM, BEFORE, UNTIL, AFTER, AROUND, PRESUMABLY
    }

    public Range(String parsedInputString, RangeType type) {
        this.parsedInputString = parsedInputString;
        this.type = type;
    }

    public String getParsedInputString() {
        return this.parsedInputString;
    }

    public RangeType getType() {
        return this.type;
    }
}
