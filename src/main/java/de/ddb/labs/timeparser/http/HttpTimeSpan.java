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
package de.ddb.labs.timeparser.http;

import de.ddb.labs.timeparser.timespan.TimeSpan;
import java.time.LocalDate;

/**
 * JSON-facing time span with explicit ISO date field names.
 */
public record HttpTimeSpan(
        String parsedInputString,
        LocalDate startISODate,
        LocalDate endISODate) {

    public static HttpTimeSpan from(final TimeSpan timeSpan) {
        if (timeSpan == null) {
            return null;
        }
        return new HttpTimeSpan(
                timeSpan.getParsedInputString(),
                timeSpan.getStartDate(),
                timeSpan.getEndDate());
    }
}
