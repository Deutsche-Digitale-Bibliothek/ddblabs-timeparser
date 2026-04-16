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
package de.ddb.labs.timeparser.model;

import de.ddb.labs.timeparser.TimeParser.IndexDaysMode;
import de.ddb.labs.timeparser.rule.Rule;
import de.ddb.labs.timeparser.timespan.TimeSpan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Structured parse result for callers that need full parser metadata or JSON
 * serialization.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParseResult {
    private final boolean successful;
    private final String input;
    private final String contextId;
    private final IndexDaysMode indexDaysMode;
    private final String normalizedInput;
    private final List<Rule> matchingRules;
    private final Rule matchedRule;
    private final String transformedInput;
    private final TimeSpan timeSpan;
    private final List<FacetNotation> facetNotations;
    private final String facetString;
    private final Long startIndexDay;
    private final Long endIndexDay;
    private final String output;
    private final String errorType;
    private final String errorMessage;

    public static ParseResult success(final String input, final String contextId, final IndexDaysMode indexDaysMode,
            final String normalizedInput, final List<Rule> matchingRules, final Rule matchedRule,
            final String transformedInput, final TimeSpan timeSpan, final List<FacetNotation> facetNotations,
            final String facetString, final Long startIndexDay, final Long endIndexDay, final String output) {
        return new ParseResult(true, input, contextId, indexDaysMode, normalizedInput,
                Collections.unmodifiableList(new ArrayList<>(matchingRules)), matchedRule, transformedInput, timeSpan,
                Collections.unmodifiableList(new ArrayList<>(facetNotations)), facetString, startIndexDay,
                endIndexDay, output, null, null);
    }

    public static ParseResult failure(final String input, final String contextId, final IndexDaysMode indexDaysMode,
            final String normalizedInput, final List<Rule> matchingRules, final Rule matchedRule,
            final String transformedInput, final TimeSpan timeSpan, final List<FacetNotation> facetNotations,
            final String facetString, final Long startIndexDay, final Long endIndexDay, final String output,
            final String errorType, final String errorMessage) {
        return new ParseResult(false, input, contextId, indexDaysMode, normalizedInput,
                Collections.unmodifiableList(new ArrayList<>(matchingRules)), matchedRule, transformedInput, timeSpan,
                Collections.unmodifiableList(new ArrayList<>(facetNotations)), facetString, startIndexDay,
                endIndexDay, output, errorType, errorMessage);
    }
}
