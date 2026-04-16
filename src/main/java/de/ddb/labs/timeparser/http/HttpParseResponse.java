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

import de.ddb.labs.timeparser.TimeParser.IndexDaysMode;
import de.ddb.labs.timeparser.model.FacetNotation;
import de.ddb.labs.timeparser.model.ParseResult;
import de.ddb.labs.timeparser.rule.Rule;
import java.util.List;

/**
 * JSON-facing parse response for the embedded HTTP server.
 */
public record HttpParseResponse(
        boolean successful,
        String input,
        IndexDaysMode indexDaysMode,
        String normalizedInput,
        List<Rule> matchingRules,
        Rule matchedRule,
        String transformedInput,
        HttpTimeSpan timeSpan,
        List<FacetNotation> facetNotations,
        String facetString,
        Long startIndexDay,
        Long endIndexDay,
        String output,
        String errorType,
        String errorMessage) {

    public static HttpParseResponse from(final ParseResult result) {
        return new HttpParseResponse(
                result.isSuccessful(),
                result.getInput(),
                result.getIndexDaysMode(),
                result.getNormalizedInput(),
                result.getMatchingRules(),
                result.getMatchedRule(),
                result.getTransformedInput(),
                HttpTimeSpan.from(result.getTimeSpan()),
                result.getFacetNotations(),
                result.getFacetString(),
                result.getStartIndexDay(),
                result.getEndIndexDay(),
                result.getOutput(),
                result.getErrorType(),
                result.getErrorMessage());
    }
}
