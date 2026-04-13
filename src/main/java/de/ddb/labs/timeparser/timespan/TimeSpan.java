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
import java.util.Calendar;
import java.util.GregorianCalendar;

/** A time period between two points in time. Contains the input string that was parsed to generate this period. */
public class TimeSpan {
    private final String parsedInputString;
    private final LocalDate start;
    private final LocalDate end;

    public TimeSpan(String parsedInputString, Calendar start, Calendar end) {
        this(parsedInputString, toLocalDate(start), toLocalDate(end));
    }

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

    public Calendar getStart() {
        return toGregorianCalendar(this.start);
    }

    public Calendar getEnd() {
        return toGregorianCalendar(this.end);
    }

    @Override
    public String toString() {
        String startEra = this.start.getYear() <= 0 ? "BC" : "AD";
        String endEra = this.end.getYear() <= 0 ? "BC" : "AD";
        return "TimeSpan[" + this.start + " (" + startEra + ") - " + this.end + " (" + endEra + "), \"" + this.parsedInputString + "\"]";
    }

    private static LocalDate toLocalDate(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        if (calendar.get(Calendar.ERA) == GregorianCalendar.BC) {
            year = 1 - year;
        }
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return LocalDate.of(year, month, day);
    }

    private static GregorianCalendar toGregorianCalendar(LocalDate date) {
        int prolepticYear = date.getYear();
        boolean isAD = prolepticYear > 0;
        int yearOfEra = isAD ? prolepticYear : (1 - prolepticYear);

        GregorianCalendar calendar = new GregorianCalendar(yearOfEra, date.getMonthValue() - 1, date.getDayOfMonth());
        if (!isAD) {
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }
        return calendar;
    }
}
