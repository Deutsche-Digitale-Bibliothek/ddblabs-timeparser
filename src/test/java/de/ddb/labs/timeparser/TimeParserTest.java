package de.ddb.labs.timeparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ddb.labs.timeparser.http.HttpParseResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import de.ddb.labs.timeparser.data.InputParser;
import de.ddb.labs.timeparser.data.Outputter;
import de.ddb.labs.timeparser.data.PatternParser;
import de.ddb.labs.timeparser.data.Token;
import de.ddb.labs.timeparser.data.TokenWithValue;
import de.ddb.labs.timeparser.model.ParseErrorStats;
import de.ddb.labs.timeparser.model.ParseResult;
import de.ddb.labs.timeparser.replacement.Replacement;
import de.ddb.labs.timeparser.replacement.ReplacementReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import de.ddb.labs.timeparser.timespan.TimeSpan;
import de.ddb.labs.timeparser.timespan.TimeSpanParser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression suite for parser semantics, fail-safe behavior, and rules.csv contract integrity.
 */
public class TimeParserTest {

    @Test
    @DisplayName("Parses canonical ISO-like input into facet and day-range payload")
    public void parsesSimpleInput() {
        assertEquals("time_18000 -5583373|-5583373", TimeParser.getInstance().parseTime("-20000-02-21"));
    }

    @Test
    @DisplayName("Supports legacy day-index output via explicit parse overload")
    public void parsesSimpleInputWithLegacyIndexDaysMode() {
        assertEquals(
            "time_18000 -7304949|-7304949",
            TimeParser.getInstance().parseTime("-20000-02-21", TimeParser.IndexDaysMode.LEGACY));
    }

    @Test
    @DisplayName("Applies BC era suffix to both range boundaries")
    public void parsesBcDateSuffixForWholeRange() {
        final TimeSpan timeSpan = new TimeSpanParser().parse("100/101 vor Christus");

        assertEquals(LocalDate.of(-99, 1, 1), timeSpan.getStartDate());
        assertEquals(LocalDate.of(-100, 12, 31), timeSpan.getEndDate());
    }

    @Test
    @DisplayName("Exposes structured parse result metadata for successful parsing")
    public void exposesStructuredParseResult() {
        final ParseResult result = TimeParser.getInstance().parseTimeResult("-20000-02-21");

        assertTrue(result.isSuccessful());
        assertEquals("-20000-02-21", result.getInput());
        assertEquals(TimeParser.IndexDaysMode.JULIAN_DAY, result.getIndexDaysMode());
        assertEquals("time_18000 -5583373|-5583373", result.getOutput());
        assertEquals("time_18000", result.getFacetString());
        assertEquals(-5583373L, result.getStartIndexDay());
        assertEquals(-5583373L, result.getEndIndexDay());
        assertNotNull(result.getTimeSpan());
        assertTrue(result.getFacetNotations().stream().anyMatch(facet -> "time_18000".equals(facet.getNotation())
                && "Quartär".equals(facet.getPrefLabelDe())
                && "Quaternary".equals(facet.getPrefLabelEn())));
        assertTrue(result.getErrorType() == null);
        assertTrue(result.getErrorMessage() == null);
    }

    @Test
    @DisplayName("Exposes structured failure metadata instead of only an empty string")
    public void exposesStructuredFailureResult() {
        final ParseResult result = TimeParser.getInstance().parseTimeResult("1000000000");

        assertFalse(result.isSuccessful());
        assertEquals("1000000000", result.getInput());
        assertEquals("", result.getOutput());
        assertEquals("DateTimeException", result.getErrorType());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Uses explicit ISO field names in successful HTTP JSON")
    public void usesExplicitIsoFieldNamesInHttpJson() throws Exception {
        final ParseResult result = TimeParser.getInstance().parseTimeResult("Mai 2010");
        final ObjectMapper objectMapper = TimeParserHttpServer.createObjectMapper();

        final String json = objectMapper.writeValueAsString(HttpParseResponse.from(result));

        assertTrue(json.contains("\"startISODate\":\"2010-05-01\""));
        assertTrue(json.contains("\"endISODate\":\"2010-05-31\""));
        assertFalse(json.contains("\"startDate\""));
        assertFalse(json.contains("\"endDate\""));
    }

    @Test
    @DisplayName("Omits empty metadata fields from HTTP failure JSON")
    public void omitsEmptyFieldsFromHttpFailureJson() throws Exception {
        final ParseResult result = TimeParser.getInstance().parseTimeResult("200 V.Vh");
        final ObjectMapper objectMapper = TimeParserHttpServer.createObjectMapper();

        final String json = objectMapper.writeValueAsString(HttpParseResponse.from(result));

        assertFalse(json.contains("\"matchingRules\""));
        assertFalse(json.contains("\"transformedInput\""));
        assertFalse(json.contains("\"facetNotations\""));
        assertFalse(json.contains("\"facetString\""));
        assertFalse(json.contains("\"output\""));
        assertTrue(json.contains("\"errorType\":\"INVALID_TIME_EXPRESSION\""));
    }

    @Test
    @DisplayName("Normalizes BCE abbreviation variants before rule matching")
    public void normalizesBceAbbreviationVariants() {
        final TimeParser parser = TimeParser.getInstance();
        final String expected = parser.parseTime("-200000000");

        assertEquals(expected, parser.parseTime("200000000 v.Chr."));
        assertEquals(expected, parser.parseTime("200000000 v. Chr."));
        assertEquals(expected, parser.parseTime("200000000 v. Chr"));
        assertEquals(expected, parser.parseTime("200000000 v Chr."));
        assertEquals(expected, parser.parseTime("200000000 v Chr"));
    }

    @Test
    @DisplayName("Parses and generates the largest supported positive year")
    public void parsesMaximumSupportedPositiveYear() {
        final TimeSpan timeSpan = new TimeSpanParser().parse("999999999");

        assertEquals(LocalDate.of(999999999, 1, 1), timeSpan.getStartDate());
        assertEquals(LocalDate.of(999999999, 12, 31), timeSpan.getEndDate());
        assertTrue(TimeParser.getInstance().parseTime("999999999").endsWith(expectedIndexRange(timeSpan)));
    }

    @Test
    @DisplayName("Parses and generates the largest supported BCE year in current negative notation")
    public void parsesMaximumSupportedNegativeYearOfEra() {
        final TimeSpan timeSpan = new TimeSpanParser().parse("-1000000000");

        assertEquals(LocalDate.of(-999999999, 1, 1), timeSpan.getStartDate());
        assertEquals(LocalDate.of(-999999999, 12, 31), timeSpan.getEndDate());
        assertTrue(TimeParser.getInstance().parseTime("-1000000000").endsWith(expectedIndexRange(timeSpan)));
    }

    @Test
    @DisplayName("Normalizes large-number unit variants before rule matching")
    public void normalizesLargeNumberUnitVariants() {
        final TimeParser parser = TimeParser.getInstance();

        final ParseResult million = parser.parseTimeResult("vor 500 Millionen Jahren");
        assertEquals("vor 500 Mio. Jahren", million.getNormalizedInput());
        assertEquals(parser.parseTime("-500000000"), million.getOutput());

        final ParseResult milliard = parser.parseTimeResult("vor 1 Milliarde Jahren");
        assertEquals("vor 1 Mrd. Jahren", milliard.getNormalizedInput());
        assertEquals(parser.parseTime("-1000000000"), milliard.getOutput());

        final ParseResult shorthandMilliard = parser.parseTimeResult("vor 1 Mrd Jahren");
        assertEquals("vor 1 Mrd. Jahren", shorthandMilliard.getNormalizedInput());
        assertEquals(parser.parseTime("-1000000000"), shorthandMilliard.getOutput());

        final ParseResult billion = parser.parseTimeResult("vor 1 Bill. Jahren");
        assertEquals("vor 1 Billionen Jahren", billion.getNormalizedInput());
        assertEquals("NumberFormatException", billion.getErrorType());
    }

    @Test
    @DisplayName("Parses million-year past expressions through rules.csv normalization")
    public void parsesMillionYearPastExpressions() {
        final TimeParser parser = TimeParser.getInstance();

        assertEquals(parser.parseTime("-500000000"), parser.parseTime("vor 500 Mio. Jahren"));
        assertEquals(parser.parseTime("-1000000000"), parser.parseTime("vor 1000 Millionen Jahren"));
    }

    @Test
    @DisplayName("Parses million-year future expressions while resulting year stays within LocalDate range")
    public void parsesMillionYearFutureExpressionsWithinRange() {
        final TimeParser parser = TimeParser.getInstance();

        assertEquals(parser.parseTime("250000000"), parser.parseTime("in 250 Mio Jahren"));
        assertEquals(parser.parseTime("250000000"), parser.parseTime("in 250 Millionen Jahren"));
    }

    @Test
    @DisplayName("Returns empty output for years outside LocalDate range")
    public void returnsEmptyStringForYearsOutsideSupportedRange() {
        assertEquals("", TimeParser.getInstance().parseTime("1000000000"));
        assertEquals("", TimeParser.getInstance().parseTime("-1000000001"));
    }

    @Test
    @DisplayName("Returns empty output when million-year future expressions exceed LocalDate range")
    public void returnsEmptyStringForUnsupportedFutureMillionYearExpressions() {
        assertEquals("", TimeParser.getInstance().parseTime("in 1000 Mio Jahren"));
        assertEquals("", TimeParser.getInstance().parseTime("in 1000 Millionen Jahren"));
    }

    @Test
    @DisplayName("Returns empty output for billion-year expressions because the resulting year exceeds LocalDate range")
    public void returnsEmptyStringForUnsupportedBillionYearExpressions() {
        assertEquals("", TimeParser.getInstance().parseTime("vor 1 Billion Jahren"));
        assertEquals("", TimeParser.getInstance().parseTime("in 1 Billionen Jahren"));
    }

    @Test
    @DisplayName("Rejects disjoint spans instead of silently collapsing semantics")
    public void rejectsDisjointTimeSpans() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new TimeSpanParser().parse("1944/1945,1949"));

        assertTrue(exception.getMessage().startsWith("Disjoint time spans are not supported:"));
    }

    @Test
    @DisplayName("Returns empty output for null input on fail-safe API")
    public void returnsEmptyStringForNullInput() {
        assertEquals("", TimeParser.getInstance().parseTime(null));
    }

    @Test
    @DisplayName("Rejects oversized inputs before expensive normalization and parsing")
    public void rejectsOversizedInputEarly() {
        final String input = "x".repeat(5000);

        final ParseResult result = TimeParser.getInstance().parseTimeResult(input);

        assertFalse(result.isSuccessful());
        assertEquals("", result.getOutput());
        assertEquals("INPUT_TOO_LONG", result.getErrorType());
    }

    @Test
    @DisplayName("Returns empty output for unsupported disjoint expressions")
    public void returnsEmptyStringForUnsupportedDisjointInput() {
        assertEquals("", TimeParser.getInstance().parseTime("1944-1945/1949", "solr-doc-42"));
    }

    @Test
    @DisplayName("Tracks aggregated parser errors with contextual metadata")
    public void exposesAggregatedErrorStats() {
        final TimeParser parser = TimeParser.getInstance();
        final Map<String, ParseErrorStats> beforeStats = parser.getErrorStats();
        final int before = beforeStats.containsKey("DISJOINT_TIME_SPAN")
            ? beforeStats.get("DISJOINT_TIME_SPAN").getCount()
            : 0;

        parser.parseTime("1944-1945/1949", "solr-doc-stats");

        final Map<String, ParseErrorStats> stats = parser.getErrorStats();
        final ParseErrorStats disjointStats = stats.get("DISJOINT_TIME_SPAN");

        assertTrue(disjointStats != null);
        assertEquals(before + 1, disjointStats.getCount());
        assertEquals("solr-doc-stats", disjointStats.getLastContext());
    }

    @Test
    @DisplayName("Resets aggregated parser error counters to a clean state")
    public void resetsAggregatedErrorStats() {
        final TimeParser parser = TimeParser.getInstance();
        parser.parseTime("1944-1945/1949", "solr-doc-reset");

        assertTrue(parser.getErrorStats().containsKey("DISJOINT_TIME_SPAN"));

        parser.resetErrorStats();

        assertTrue(parser.getErrorStats().isEmpty());
    }

    @Test
    @DisplayName("Captures parse error stats safely during concurrent parsing")
    public void capturesErrorStatsDuringConcurrentParsing() throws Exception {
        final TimeParser parser = TimeParser.getInstance();
        parser.resetErrorStats();

        final int taskCount = 24;
        final ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    parser.parseTime("1944-1945/1949", "ctx-" + index);
                    final ParseErrorStats stats = parser.getErrorStats().get("DISJOINT_TIME_SPAN");
                    assertTrue(stats == null || stats.getCount() >= 1);
                    return null;
                }));
            }

            for (final Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        final ParseErrorStats stats = parser.getErrorStats().get("DISJOINT_TIME_SPAN");
        assertNotNull(stats);
        assertEquals(taskCount, stats.getCount());
        assertNotNull(stats.getFirstContext());
        assertNotNull(stats.getLastContext());
    }

    @Test
    @DisplayName("Validates every rules.csv input example against expected output example")
    public void allRuleInputExamplesMatchOutputExamples() throws Exception {
        final List<RuleCsvEntry> entries = loadRulesCsvEntries();
        final PatternParser patternParser = new PatternParser();
        final int totalRules = entries.size();
        System.out.println("[rules.csv] validating " + totalRules + " rule examples");

        int validatedRules = 0;
        for (final RuleCsvEntry entry : entries) {
            String actualOutput = null;
            try {
            final List<Token> inputPattern = patternParser.parse(entry.inputMask, entry.inputPattern);
            final InputParser inputParser = new InputParser(inputPattern, monthReplacements(), weekdayReplacements());
            final List<TokenWithValue> parsedInputTokens = inputParser.parseInputString(entry.inputExample);

            final List<Token> outputPattern = patternParser.parse(true, entry.outputMask, entry.outputPattern);
                actualOutput = new Outputter(outputPattern).createOutputString(parsedInputTokens);

                assertEquals(entry.outputExample, actualOutput,
                    buildRuleFailureMessage(entry, actualOutput, null));
                validatedRules++;
                if (validatedRules % 100 == 0 || validatedRules == totalRules) {
                    System.out.println("[rules.csv] validated " + validatedRules + "/" + totalRules);
                }
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                throw new AssertionError(buildRuleFailureMessage(entry, actualOutput, e), e);
            }
        }

        System.out.println("[rules.csv] validation complete: " + validatedRules + " rules checked");
        assertTrue(validatedRules > 0);
    }

    private String buildRuleFailureMessage(final RuleCsvEntry entry, final String actualOutput, final Exception exception) {
        final StringBuilder builder = new StringBuilder();
        builder.append("rules.csv validation failed").append(System.lineSeparator());
        builder.append("line: ").append(entry.lineNumber).append(System.lineSeparator());
        builder.append("input example: ").append(entry.inputExample).append(System.lineSeparator());
        builder.append("input mask/pattern: ").append(entry.inputMask).append(" / ").append(entry.inputPattern).append(System.lineSeparator());
        builder.append("expected output: ").append(entry.outputExample).append(System.lineSeparator());
        builder.append("actual output: ").append(actualOutput == null ? "<not produced>" : actualOutput).append(System.lineSeparator());
        builder.append("output mask/pattern: ").append(entry.outputMask).append(" / ").append(entry.outputPattern);
        if (exception != null) {
            builder.append(System.lineSeparator()).append("error: ")
                .append(exception.getClass().getSimpleName())
                .append(": ")
                .append(exception.getMessage());
        }
        return builder.toString();
    }

    private String expectedIndexRange(final TimeSpan timeSpan) {
        return timeSpan.getStartDate().getLong(JulianFields.JULIAN_DAY)
                + "|"
                + timeSpan.getEndDate().getLong(JulianFields.JULIAN_DAY);
    }

    private List<RuleCsvEntry> loadRulesCsvEntries() throws Exception {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("conf/timeparser/rules.csv");
        assertNotNull(inputStream, "rules.csv not found on classpath");

        final List<RuleCsvEntry> entries = new ArrayList<>();
        final CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        try (CSVParser parser = CSVParser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8), format)) {
            for (final CSVRecord record : parser) {
                final int lineNumber = Math.toIntExact(record.getRecordNumber() + 1);
                assertTrue(record.size() >= 7, "Expected at least 7 columns in rules.csv at line " + lineNumber);

                entries.add(new RuleCsvEntry(
                    lineNumber,
                    record.get(0),
                    record.get(1),
                    record.get(2),
                    record.get(3),
                    record.get(4),
                    record.get(5)));
            }
        }
        return entries;
    }

    private List<Replacement> monthReplacements() throws Exception {
        return ReplacementReader.read("conf/timeparser/normalizations.csv", StandardCharsets.UTF_8.name(), false, "month");
    }

    private List<Replacement> weekdayReplacements() throws Exception {
        return ReplacementReader.read("conf/timeparser/normalizations.csv", StandardCharsets.UTF_8.name(), false, "weekday");
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class RuleCsvEntry {
        private final int lineNumber;
        private final String inputMask;
        private final String inputPattern;
        private final String inputExample;
        private final String outputMask;
        private final String outputPattern;
        private final String outputExample;
    }
}
