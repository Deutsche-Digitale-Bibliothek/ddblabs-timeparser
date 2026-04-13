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
package de.ddb.labs.timeparser;

import de.ddb.labs.timeparser.data.Outputter;
import de.ddb.labs.timeparser.data.Token;
import de.ddb.labs.timeparser.data.TokenWithValue;
import de.ddb.labs.timeparser.data.Replacement;
import de.ddb.labs.timeparser.data.PatternParser;
import de.ddb.labs.timeparser.data.InputParser;
import de.ddb.labs.timeparser.facet.Facet;
import de.ddb.labs.timeparser.facet.FacetReader;
import de.ddb.labs.timeparser.rule.Rule;
import de.ddb.labs.timeparser.rule.RuleReader;
import de.ddb.labs.timeparser.timespan.TimeSpan;
import de.ddb.labs.timeparser.timespan.TimeSpanParser;
import java.time.LocalDate;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Converts a textual representation of a point or period in time into:
 * </p>
 * <ul>
 * <li>a value, that can be used to sort documents by (starting) date, and</li>
 * <li>a list of time periods in which it lies.</li>
 * </ul>
 * <p>
 * The process consists of four steps:
 * </p>
 * <ol>
 * <li>Rule selection</li>
 * <li>Input normalization</li>
 * <li>Time span calculation</li>
 * <li>Final output string generation</li>
 * </ol>
 */
@Slf4j
public class TimeParser {

    /**
     * Output day-index strategy used by {@code parseTime(...)}.
     */
    public enum IndexDaysMode {
        /**
         * Use Julian Day Number via {@link JulianFields#JULIAN_DAY}.
         */
        JULIAN_DAY,
        /**
         * Use the legacy era/year-of-era indexing algorithm.
         */
        LEGACY
    }

    private static final String RULES_RESOURCE = "conf/timeparser/rules.csv";
    private static final String FACETS_RESOURCE = "conf/timeparser/facets.csv";
    private static final String EMPTY_RESULT = "";

    private static final TimeZone timezone = TimeZone.getTimeZone("UTC");
    private static final int DETAILED_LOG_LIMIT_PER_ERROR = 1;
    private static final int SUMMARY_LOG_EVERY_NTH_ERROR = 100;
    private static final boolean LOG_STACKTRACES = Boolean.getBoolean("timeparser.logStacktraces");

    private final static List<Character> AUXILIAR_CHARS = stringToCharList(",=?/()-–.[]0acuorcfAMJhDVIZX"); // Achtung!!
                                                                                                            // different
                                                                                                            // kind of
                                                                                                            // slashes -
                                                                                                            // vs –
    private final static List<Character> DYNAMIC_CHARS = stringToCharList("#");
    private final static List<String> PERIOD_RULES = Arrays.asList("Ottonisch", "Römisch", "Karolingisch",
            "Klassizistisch");

    private static final List<Replacement> MONTH_REPLACEMENTS = new ArrayList<>();

    static {
        MONTH_REPLACEMENTS.add(new Replacement("Januar", "01"));
        MONTH_REPLACEMENTS.add(new Replacement("Februar", "02"));
        MONTH_REPLACEMENTS.add(new Replacement("März", "03"));
        MONTH_REPLACEMENTS.add(new Replacement("April", "04"));
        MONTH_REPLACEMENTS.add(new Replacement("Mai", "05"));
        MONTH_REPLACEMENTS.add(new Replacement("Juni", "06"));
        MONTH_REPLACEMENTS.add(new Replacement("Juli", "07"));
        MONTH_REPLACEMENTS.add(new Replacement("August", "08"));
        MONTH_REPLACEMENTS.add(new Replacement("September", "09"));
        MONTH_REPLACEMENTS.add(new Replacement("Oktober", "10"));
        MONTH_REPLACEMENTS.add(new Replacement("November", "11"));
        MONTH_REPLACEMENTS.add(new Replacement("Dezember", "12"));
        MONTH_REPLACEMENTS.add(new Replacement("Jan.", "01"));
        MONTH_REPLACEMENTS.add(new Replacement("Feb.", "02"));
        MONTH_REPLACEMENTS.add(new Replacement("März", "03"));
        MONTH_REPLACEMENTS.add(new Replacement("Apr.", "04"));
        MONTH_REPLACEMENTS.add(new Replacement("Jun.", "06"));
        MONTH_REPLACEMENTS.add(new Replacement("Jul.", "07"));
        MONTH_REPLACEMENTS.add(new Replacement("Aug.", "08"));
        MONTH_REPLACEMENTS.add(new Replacement("Sept.", "09"));
        MONTH_REPLACEMENTS.add(new Replacement("Okt.", "10"));
        MONTH_REPLACEMENTS.add(new Replacement("Nov.", "11"));
        MONTH_REPLACEMENTS.add(new Replacement("Dez.", "12"));
        MONTH_REPLACEMENTS.add(new Replacement("Nov", "11"));
    }

    private static final List<Replacement> WEEKDAY_REPLACEMENTS = new ArrayList<>();

    static {
        WEEKDAY_REPLACEMENTS.add(new Replacement("Montag", "GG"));
        WEEKDAY_REPLACEMENTS.add(new Replacement("Dienstag", "GG"));
        WEEKDAY_REPLACEMENTS.add(new Replacement("Mittwoch", "GG"));
        WEEKDAY_REPLACEMENTS.add(new Replacement("Donnerstag", "GG"));
        WEEKDAY_REPLACEMENTS.add(new Replacement("Freitag", "GG"));
        WEEKDAY_REPLACEMENTS.add(new Replacement("Samstag", "GG"));
        WEEKDAY_REPLACEMENTS.add(new Replacement("Sonntag", "GG"));
    }

    private static final PatternParser patternParser = new PatternParser();
    private static final TimeSpanParser timeSpanParser = new TimeSpanParser();

    private static List<Rule> rules;
    private static List<Facet> facets;

    private static final ConcurrentMap<String, ParseErrorCounter> parseErrorCounters = new ConcurrentHashMap<>();

    private static class Holder {

        private static final TimeParser INSTANCE = new TimeParser();
    }

    /**
     * Private Constructor
     */
    private TimeParser() {
        try {
            rules = new RuleReader().read(RULES_RESOURCE, "UTF-8");
            facets = new FacetReader().read(FACETS_RESOURCE, "UTF-8");

            parseErrorCounters.clear();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns the shared parser instance.
     *
     * @return singleton parser instance
     */
    public static TimeParser getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns all loaded transformation rules.
     *
     * @return loaded rules
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Returns a snapshot of aggregated parse error counters.
     *
     * @return immutable map keyed by error type
     */
    public Map<String, ParseErrorStats> getErrorStats() {
        Map<String, ParseErrorStats> snapshot = new HashMap<>();
        for (Map.Entry<String, ParseErrorCounter> entry : parseErrorCounters.entrySet()) {
            ParseErrorCounter counter = entry.getValue();
            snapshot.put(entry.getKey(), new ParseErrorStats(
                    counter.getCount(),
                    counter.getFirstContext(),
                    counter.getFirstInput(),
                    counter.getLastContext(),
                    counter.getLastInput(),
                    counter.getLastMessage()));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Clears all aggregated parse error counters.
     */
    public void resetErrorStats() {
        parseErrorCounters.clear();
    }

    /**
     * Parses input using default settings (no context id, Julian Day output index).
     *
     * @param input textual date/time expression
     * @return parsed output or empty string on parse failure
     */
    public String parseTime(String input) {
        return parseTime(input, null, IndexDaysMode.JULIAN_DAY);
    }

    /**
     * Parses input with an explicit day-index mode and no context id.
     *
     * @param input         textual date/time expression
     * @param indexDaysMode output day-index strategy
     * @return parsed output or empty string on parse failure
     */
    public String parseTime(String input, IndexDaysMode indexDaysMode) {
        return parseTime(input, null, indexDaysMode);
    }

    /**
     * Parses input with optional context id using Julian Day output index.
     *
     * @param input     textual date/time expression
     * @param contextId optional correlation id for logging/monitoring
     * @return parsed output or empty string on parse failure
     */
    public String parseTime(String input, String contextId) {
        return parseTime(input, contextId, IndexDaysMode.JULIAN_DAY);
    }

    /**
     * Parses input with optional context id and explicit day-index mode.
     *
     * @param input         textual date/time expression
     * @param contextId     optional correlation id for logging/monitoring
     * @param indexDaysMode output day-index strategy
     * @return parsed output or empty string on parse failure
     */
    public String parseTime(String input, String contextId, IndexDaysMode indexDaysMode) {
        if (input == null || input.isEmpty()) {
            return EMPTY_RESULT;
        }

        IndexDaysMode mode = indexDaysMode == null ? IndexDaysMode.JULIAN_DAY : indexDaysMode;

        try {
            return doParseTime(input, contextId, mode);
        } catch (Exception e) {
            logParseFailure(contextId, input, e.getMessage(), e);
        }
        return EMPTY_RESULT;
    }

    private String doParseTime(String input, String contextId, IndexDaysMode indexDaysMode) throws Exception {
        final List<Rule> matchingRules = findRulesForInput(input);
        if (matchingRules.size() > 1) {
            logParseFailure(contextId, input, "Multiple rules found for input string \"" + input + "\"", null);
            return EMPTY_RESULT;
        }

        // Transform the input string into a normalized date string.
        String transformedInput = input;
        if (matchingRules.size() == 1) {
            Rule rule = matchingRules.get(0);
            transformedInput = transform(input, rule);
        }

        // Calculate a time span for the output string.
        final TimeSpan timeSpan = timeSpanParser.parse(transformedInput);
        // LOG.info("Timespan: {}", timeSpan);

        // Create the final output string.
        final String finalOutput = createFacetString(timeSpan) + " "
                + createDaysFromZeroString(timeSpan, indexDaysMode);

        return finalOutput;
    }

    private void logParseFailure(String contextId, String input, String message, Exception exception) {
        String errorType = classifyErrorType(message, exception);
        ParseErrorCounter counter = parseErrorCounters.computeIfAbsent(errorType, key -> new ParseErrorCounter());
        int count = counter.incrementAndTrack(contextId, input, message);

        boolean shouldLogDetailed = count <= DETAILED_LOG_LIMIT_PER_ERROR;
        boolean shouldLogSummary = count % SUMMARY_LOG_EVERY_NTH_ERROR == 0;

        if (shouldLogDetailed) {
            if (contextId == null || contextId.isEmpty()) {
                log.warn("TimeParser failed (type={}, count={}) for input [{}]: {}", errorType, count, input, message);
            } else {
                log.warn("TimeParser failed (type={}, count={}) for context [{}], input [{}]: {}", errorType, count,
                        contextId, input, message);
            }
        } else if (shouldLogSummary) {
            log.warn(
                    "TimeParser error summary (type={}, count={}): firstContext=[{}], firstInput=[{}], lastContext=[{}], lastInput=[{}], lastMessage=[{}]",
                    errorType,
                    count,
                    counter.getFirstContext(),
                    counter.getFirstInput(),
                    counter.getLastContext(),
                    counter.getLastInput(),
                    counter.getLastMessage());
        }

        if (exception != null && LOG_STACKTRACES && log.isDebugEnabled() && (shouldLogDetailed || shouldLogSummary)) {
            log.debug("TimeParser parse failure details", exception);
        }
    }

    private String classifyErrorType(String message, Exception exception) {
        if (message != null) {
            if (message.startsWith("Disjoint time spans are not supported:")) {
                return "DISJOINT_TIME_SPAN";
            }
            if (message.startsWith("Multiple rules found for input string")) {
                return "MULTIPLE_RULES";
            }
            if (message.contains("could not be parsed")) {
                return "INVALID_TIME_EXPRESSION";
            }
        }

        if (exception == null) {
            return "UNKNOWN";
        }
        return exception.getClass().getSimpleName();
    }

    @Getter
    private static final class ParseErrorCounter {
        @Getter(AccessLevel.NONE)
        private final AtomicInteger count = new AtomicInteger();
        private volatile String firstContext = "-";
        private volatile String firstInput = "-";
        private volatile String lastContext = "-";
        private volatile String lastInput = "-";
        private volatile String lastMessage = "-";

        int incrementAndTrack(String contextId, String input, String message) {
            int currentCount = count.incrementAndGet();
            String normalizedContext = (contextId == null || contextId.isEmpty()) ? "-" : contextId;
            String normalizedInput = (input == null || input.isEmpty()) ? "-" : input;
            String normalizedMessage = (message == null || message.isEmpty()) ? "-" : message;

            if (currentCount == 1) {
                firstContext = normalizedContext;
                firstInput = normalizedInput;
            }

            lastContext = normalizedContext;
            lastInput = normalizedInput;
            lastMessage = normalizedMessage;
            return currentCount;
        }

        int getCount() {
            return count.get();
        }
    }

    /**
     * Immutable snapshot entry for aggregated parse errors.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ParseErrorStats {
        /** Number of occurrences for this error type. */
        private final int count;
        /** First observed context id, or {@code -} if none. */
        private final String firstContext;
        /** First observed input, or {@code -} if none. */
        private final String firstInput;
        /** Last observed context id, or {@code -} if none. */
        private final String lastContext;
        /** Last observed input, or {@code -} if none. */
        private final String lastInput;
        /** Last observed error message, or {@code -} if none. */
        private final String lastMessage;
    }

    /**
     * Does the first step of the transformation, i.e. the transformation into a
     * normalized string according to pattern rules.
     */
    private String transform(String input, Rule rule) throws Exception {
        // Compile the input mask and input pattern into an (also) input pattern.
        final List<Token> inputPattern = patternParser.parse(rule.getInputMask(), rule.getInputPattern());

        // Parse the input string.
        final InputParser inputParser = new InputParser(inputPattern, TimeParser.MONTH_REPLACEMENTS,
                TimeParser.WEEKDAY_REPLACEMENTS);
        final List<TokenWithValue> parsedInputTokens = inputParser.parseInputString(input);

        // Compile the output mask and output pattern into an (also) output pattern.
        final List<Token> outputPattern = patternParser.parse(true, rule.getOutputMask(), rule.getOutputPattern());

        // Create the output string.
        final Outputter outputter = new Outputter(outputPattern);
        return outputter.createOutputString(parsedInputTokens);
    }

    private List<Rule> findRulesForInput(String input) {
        for (Replacement replacement : TimeParser.MONTH_REPLACEMENTS) {
            input = input.replace(replacement.from, "MM");
        }

        for (Replacement replacement : TimeParser.WEEKDAY_REPLACEMENTS) {
            input = input.replace(replacement.from, "GG");
        }

        List<Rule> foundRules = getRules(input);
        foundRules = cleanupRules(foundRules);

        return foundRules;
    }

    private List<Rule> getRules(String input) {
        final List<Rule> foundRules = new ArrayList<>();
        for (Rule rule : rules) {
            final String ruleInputMask = rule.getInputMask();

            if (!isInputPassingBasicChecks(input, ruleInputMask)) {
                continue;
            }

            boolean correct = true;
            for (int i = 0; i < ruleInputMask.length(); i++) {
                char maskChar = ruleInputMask.charAt(i);
                char inputChar = input.charAt(i);
                if (!isMatching(maskChar, inputChar)) {
                    correct = false;
                    break;
                }
            }

            if (correct) {
                foundRules.add(rule);
            }
        }

        return foundRules;
    }

    private boolean isMatching(char maskChar, char inputChar) {
        return !((maskChar == '#' && !Character.isDigit(inputChar))
                || (Character.isSpaceChar(maskChar) && !Character.isSpaceChar(inputChar)) // Spaces should match
                || (!Character.isSpaceChar(maskChar) && Character.isSpaceChar(inputChar)) // Spaces should match
                || (AUXILIAR_CHARS.contains(maskChar) && maskChar != inputChar) // auxiliar chars are expected to be the
                                                                                // same
                || (DYNAMIC_CHARS.contains(maskChar) && maskChar == inputChar)); // dynamic chars are expected to be a
                                                                                 // value
    }

    private boolean isInputPassingBasicChecks(String input, String ruleInputMask) {
        // Lengths should be the same with and without blank spaces
        if (ruleInputMask.length() != input.length()
                || ruleInputMask.replaceAll("\\s", "").length() != input.replaceAll("\\s", "").length()) {
            return false;
        }
        // If the rule is one of the period literals, we expect the input to be exactly
        // the same
        return !(PERIOD_RULES.contains(ruleInputMask) && !input.equals(ruleInputMask));
    }

    private List<Rule> cleanupRules(List<Rule> foundRules) {
        // Keep rules with the least hashes.
        if (foundRules.size() > 1) {
            int smallestNumberOfHashes = getSmallestNumberOfHashes(foundRules);
            final Iterator<Rule> it = foundRules.iterator();
            while (it.hasNext()) {
                Rule rule = it.next();
                int n = countCharacterOccurrences(rule.getInputMask(), '#');
                if (n > smallestNumberOfHashes) {
                    it.remove();
                }
            }
        }
        // Remove duplicate rules.
        return new ArrayList<>(new LinkedHashSet<>(foundRules));
    }

    private int getSmallestNumberOfHashes(List<Rule> foundRules) {
        int smallestNumberOfHashes = -1;
        for (Rule rule : foundRules) {
            if (smallestNumberOfHashes == -1) {
                smallestNumberOfHashes = countCharacterOccurrences(rule.getInputMask(), '#');
            } else {
                int n = countCharacterOccurrences(rule.getInputMask(), '#');
                if (n < smallestNumberOfHashes) {
                    smallestNumberOfHashes = n;
                }
            }
        }
        return smallestNumberOfHashes;
    }

    private int countCharacterOccurrences(String string, char c) {
        int n = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    private String createFacetString(TimeSpan timeSpan) {
        StringBuilder facetString = new StringBuilder("");
        List<String> facetTokens = new ArrayList<>();
        int startYear = toFacetYear(timeSpan.getStartDate());
        int endYear = toFacetYear(timeSpan.getEndDate());
        for (Facet facet : facets) {
            if ((startYear <= facet.getLatestDate()) && (endYear >= facet.getEarliestDate())
                    && !facetTokens.contains(facet.getNotation())) {
                facetTokens.add(facet.getNotation());
            }
        }
        if (facetTokens.isEmpty()) {
            return EMPTY_RESULT;
        } else {
            for (String facetToken : facetTokens) {
                if (facetString.length() > 0) {
                    facetString.append("|");
                }
                facetString.append(facetToken);
            }
        }
        return facetString.toString();
    }

    private String createDaysFromZeroString(TimeSpan timeSpan, IndexDaysMode indexDaysMode) {
        final long startDays = toIndexDays(timeSpan.getStartDate(), indexDaysMode);
        // LOG.info("Timespan (Start): {} - {}",
        // DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(timeSpan.getStart()),
        // startDays);
        final long endDays = toIndexDays(timeSpan.getEndDate(), indexDaysMode);
        // LOG.info("Timespan (End): {} - {}",
        // DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(timeSpan.getEnd()),
        // endDays);
        return startDays + "|" + endDays;
    }

    private long toIndexDays(LocalDate date, IndexDaysMode indexDaysMode) {
        if (indexDaysMode == IndexDaysMode.LEGACY) {
            return getDateAsIndexDays(date);
        }
        return getJulianDayAsIndexDays(date);
    }

    private int toFacetYear(LocalDate date) {
        int year = date.getYear();
        return year > 0 ? year : year - 1;
    }

    private long getJulianDayAsIndexDays(LocalDate date) {
        return date.getLong(JulianFields.JULIAN_DAY);
    }

    /**
     * @deprecated Kept for compatibility and reference only. Uses the historical
     *             era/year-of-era indexing model.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    private long getDateAsIndexDays(LocalDate date) {
        final Calendar temp = new GregorianCalendar(timezone);
        temp.clear();
        temp.setLenient(false);

        int prolepticYear = date.getYear();
        boolean isAnnoDomini = prolepticYear > 0;
        int yearOfEra = isAnnoDomini ? prolepticYear : (1 - prolepticYear);

        // Keep historical day indexing stable by using the same era/year-of-era model
        // as before.
        temp.set(Calendar.ERA, isAnnoDomini ? GregorianCalendar.AD : GregorianCalendar.BC);
        temp.set(Calendar.YEAR, yearOfEra);
        temp.set(Calendar.MONTH, date.getMonthValue() - 1);
        temp.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());

        long days = temp.getTimeInMillis() / 86400000L; // 1000*60*60*24 (approximate number of milliseconds per day)
        days += 719164; // approximate number of days from 0001-01-01 to 1970-01-01 (this is where
                        // getTimeInMillis
        return days;
    }

    private static List<Character> stringToCharList(String input) {
        final List<Character> result = new ArrayList<>();

        for (char c : input.toCharArray()) {
            result.add(c);
        }
        return result;
    }
}
