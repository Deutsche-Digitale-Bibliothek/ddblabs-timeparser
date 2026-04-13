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
 * A transformation rule consists of an input specification, and an output specification.
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

    public Rule(String inputMask, String inputPattern, String inputExample, String outputMask, String outputPattern, String outputExample, String test) {
        this.inputMask = inputMask;
        this.inputPattern = inputPattern;
        this.inputExample = inputExample;
        this.outputMask = outputMask;
        this.outputPattern = outputPattern;
        this.outputExample = outputExample;
        this.test = test;
    }

}
