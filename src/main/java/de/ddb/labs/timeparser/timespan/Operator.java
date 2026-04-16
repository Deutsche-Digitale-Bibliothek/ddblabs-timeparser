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
 * Binary operator between two parsed time expressions.
 */
@Getter
final class Operator {

    private final String parsedInputString;
    private final OperatorType type;

    /**
     * Supported operator categories in normalized expressions.
     */
    enum OperatorType {
        OR, BETWEEN
    }

    Operator(final String parsedInputString, final OperatorType type) {
        this.parsedInputString = parsedInputString;
        this.type = type;
    }
}
