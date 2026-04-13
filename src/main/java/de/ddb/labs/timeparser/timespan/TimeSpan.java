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

import java.time.LocalDate;

/** A time period between two points in time. Contains the input string that was parsed to generate this period. */
public class TimeSpan {
    private final String parsedInputString;
    private final LocalDate start;
    private final LocalDate end;

    public TimeSpan(String parsedInputString, LocalDate start, LocalDate end) {
        this.parsedInputString = parsedInputString;
        this.start = start;
        this.end = end;
    }

    public String getParsedInputString() {
        return this.parsedInputString;
    }

    public LocalDate getStartDate() {
        return this.start;
    }

    public LocalDate getEndDate() {
        return this.end;
    }

    @Override
    public String toString() {
        String startEra = this.start.getYear() <= 0 ? "BC" : "AD";
        String endEra = this.end.getYear() <= 0 ? "BC" : "AD";
        return "TimeSpan[" + this.start + " (" + startEra + ") - " + this.end + " (" + endEra + "), \"" + this.parsedInputString + "\"]";
    }
}
