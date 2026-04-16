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
package de.ddb.labs.timeparser.rule;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Transformation rule mapping one textual input pattern to a normalized output
 * pattern.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Rule {

    private final String inputMask;
    private final String inputPattern;
    private final String inputExample;
    private final String outputMask;
    private final String outputPattern;
    private final String outputExample;
    private final String test;

    public Rule(final String inputMask, final String inputPattern, final String inputExample, final String outputMask,
            final String outputPattern, final String outputExample, final String test) {
        this.inputMask = inputMask;
        this.inputPattern = inputPattern;
        this.inputExample = inputExample;
        this.outputMask = outputMask;
        this.outputPattern = outputPattern;
        this.outputExample = outputExample;
        this.test = test;
    }
}
