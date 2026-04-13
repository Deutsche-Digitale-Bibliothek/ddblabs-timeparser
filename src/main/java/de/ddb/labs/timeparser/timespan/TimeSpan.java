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

import java.util.Calendar;
import java.util.GregorianCalendar;

/** A time period between two points in time. Contains the input string that was parsed to generate this period. */
public class TimeSpan {
    private final String parsedInputString;
    private final Calendar start;
    private final Calendar end;

    public TimeSpan(String parsedInputString, Calendar start, Calendar end) {
        this.parsedInputString = parsedInputString;
        this.start = start;
        this.end = end;
    }

    public String getParsedInputString() {
        return this.parsedInputString;
    }

    public Calendar getStart() {
        return this.start;
    }

    public Calendar getEnd() {
        return this.end;
    }

    @Override
    public String toString() {
        String startEra = "AD";
        if ( this.start.get(Calendar.ERA) == GregorianCalendar.BC ) {
            startEra = "BC";
        }
        String endEra = "AD";
        if ( this.end.get(Calendar.ERA) == GregorianCalendar.BC ) {
            endEra = "BC";
        }
        return "TimeSpan[" + this.start.getTime() + " (" + startEra + ") - " + this.end.getTime() + " (" + endEra + "), \"" + this.parsedInputString + "\"]";
    }
}
