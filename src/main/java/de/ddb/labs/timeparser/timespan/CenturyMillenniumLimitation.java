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

import lombok.Getter;

/**
 * Qualifier narrowing a century expression to a subrange such as a half or
 * quarter.
 */
@Getter
final class CenturyMillenniumLimitation {

    private final String parsedInputString;
    private final Integer number;
    private final LimitationType limitation;

    /**
     * Supported subrange categories for century expressions.
     */
    enum LimitationType {
        QUARTER, THIRD, HALF, DECADE, START, MIDDLE, END
    }

    CenturyMillenniumLimitation(final String parsedInputString, final Integer number,
            final LimitationType limitation) {
        this.parsedInputString = parsedInputString;
        this.number = number;
        this.limitation = limitation;
    }
}
