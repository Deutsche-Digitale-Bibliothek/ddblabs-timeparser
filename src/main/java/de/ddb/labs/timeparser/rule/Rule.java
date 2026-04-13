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

import java.util.Objects;

/**
 * A transformation rule consists of an input specification, and an output specification.
 */
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

    public String getInputMask() {
        return this.inputMask;
    }

    public String getInputPattern() {
        return this.inputPattern;
    }

    public String getInputExample() {
        return inputExample;
    }

    public String getOutputMask() {
        return this.outputMask;
    }

    public String getOutputPattern() {
        return this.outputPattern;
    }

    public String getOutputExample() {
        return outputExample;
    }

    public String getTest() {
        return test;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Rule rule = (Rule) o;
        return Objects.equals(inputMask, rule.inputMask) && Objects.equals(inputPattern, rule.inputPattern) && Objects.equals(inputExample, rule.inputExample) && Objects.equals(outputMask, rule.outputMask) && Objects.equals(outputPattern, rule.outputPattern) && Objects.equals(outputExample, rule.outputExample) && Objects.equals(test, rule.test);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputMask, inputPattern, inputExample, outputMask, outputPattern, outputExample, test);
    }

    @Override
    public String toString() {
        return "Rule[\"" + this.inputMask + "\", \"" + this.inputPattern + "\", \"" + this.inputExample + "\" -> \"" + this.outputMask + "\", \"" + this.outputPattern + "\", \"" + this.outputExample + "\"]";
    }
}
