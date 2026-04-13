package de.ddb.labs.timeparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.ddb.labs.timeparser.data.InputParser;
import de.ddb.labs.timeparser.data.Outputter;
import de.ddb.labs.timeparser.data.PatternParser;
import de.ddb.labs.timeparser.data.Replacement;
import de.ddb.labs.timeparser.data.Token;
import de.ddb.labs.timeparser.data.TokenWithValue;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import de.ddb.labs.timeparser.timespan.TimeSpan;
import de.ddb.labs.timeparser.timespan.TimeSpanParser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression suite for parser semantics, fail-safe behavior, and rules.csv contract integrity.
 */
public class AppTest {

    @Test
    @DisplayName("Parses canonical ISO-like input into facet and day-range payload")
    public void parsesSimpleInput() {
        assertEquals("time_18000 -7304949|-7304949", TimeParser.getInstance().parseTime("-20000-02-21"));
    }

    @Test
    @DisplayName("Applies BC era suffix to both range boundaries")
    public void parsesBcDateSuffixForWholeRange() {
        TimeSpan timeSpan = new TimeSpanParser().parse("100/101 vor Christus");

        assertEquals(LocalDate.of(-99, 1, 1), timeSpan.getStartDate());
        assertEquals(LocalDate.of(-100, 12, 31), timeSpan.getEndDate());
    }

    @Test
    @DisplayName("Rejects disjoint spans instead of silently collapsing semantics")
    public void rejectsDisjointTimeSpans() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new TimeSpanParser().parse("1944/1945,1949"));

        assertTrue(exception.getMessage().startsWith("Disjoint time spans are not supported:"));
    }

    @Test
    @DisplayName("Returns empty output for null input on fail-safe API")
    public void returnsEmptyStringForNullInput() {
        assertEquals("", TimeParser.getInstance().parseTime(null));
    }

    @Test
    @DisplayName("Returns empty output for unsupported disjoint expressions")
    public void returnsEmptyStringForUnsupportedDisjointInput() {
        assertEquals("", TimeParser.getInstance().parseTime("1944-1945/1949", "solr-doc-42"));
    }

    @Test
    @DisplayName("Tracks aggregated parser errors with contextual metadata")
    public void exposesAggregatedErrorStats() {
        TimeParser parser = TimeParser.getInstance();
        Map<String, TimeParser.ParseErrorStats> beforeStats = parser.getErrorStats();
        int before = beforeStats.containsKey("DISJOINT_TIME_SPAN")
            ? beforeStats.get("DISJOINT_TIME_SPAN").getCount()
            : 0;

        parser.parseTime("1944-1945/1949", "solr-doc-stats");

        Map<String, TimeParser.ParseErrorStats> stats = parser.getErrorStats();
        TimeParser.ParseErrorStats disjointStats = stats.get("DISJOINT_TIME_SPAN");

        assertTrue(disjointStats != null);
        assertEquals(before + 1, disjointStats.getCount());
        assertEquals("solr-doc-stats", disjointStats.getLastContext());
    }

    @Test
    @DisplayName("Resets aggregated parser error counters to a clean state")
    public void resetsAggregatedErrorStats() {
        TimeParser parser = TimeParser.getInstance();
        parser.parseTime("1944-1945/1949", "solr-doc-reset");

        assertTrue(parser.getErrorStats().containsKey("DISJOINT_TIME_SPAN"));

        parser.resetErrorStats();

        assertTrue(parser.getErrorStats().isEmpty());
    }

    @Test
    @DisplayName("Validates every rules.csv input example against expected output example")
    public void allRuleInputExamplesMatchOutputExamples() throws Exception {
        List<RuleCsvEntry> entries = loadRulesCsvEntries();
        PatternParser patternParser = new PatternParser();
        int totalRules = entries.size();
        System.out.println("[rules.csv] validating " + totalRules + " rule examples");

        int validatedRules = 0;
        for (RuleCsvEntry entry : entries) {
            String actualOutput = null;
            try {
                List<Token> inputPattern = patternParser.parse(entry.inputMask, entry.inputPattern);
                InputParser inputParser = new InputParser(inputPattern, monthReplacements(), weekdayReplacements());
                List<TokenWithValue> parsedInputTokens = inputParser.parseInputString(entry.inputExample);

                List<Token> outputPattern = patternParser.parse(true, entry.outputMask, entry.outputPattern);
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

    private String buildRuleFailureMessage(RuleCsvEntry entry, String actualOutput, Exception exception) {
        StringBuilder builder = new StringBuilder();
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

    private List<RuleCsvEntry> loadRulesCsvEntries() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("conf/timeparser/rules.csv");
        assertNotNull(inputStream, "rules.csv not found on classpath");

        List<RuleCsvEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] columns = line.split("\\t", -1);
                assertTrue(columns.length >= 7, "Expected at least 7 columns in rules.csv at line " + lineNumber);

                entries.add(new RuleCsvEntry(
                    lineNumber,
                    columns[0],
                    columns[1],
                    columns[2],
                    columns[3],
                    columns[4],
                    columns[5]));
            }
        }
        return entries;
    }

    private List<Replacement> monthReplacements() {
        List<Replacement> replacements = new ArrayList<>();
        replacements.add(new Replacement("Januar", "01"));
        replacements.add(new Replacement("Februar", "02"));
        replacements.add(new Replacement("März", "03"));
        replacements.add(new Replacement("April", "04"));
        replacements.add(new Replacement("Mai", "05"));
        replacements.add(new Replacement("Juni", "06"));
        replacements.add(new Replacement("Juli", "07"));
        replacements.add(new Replacement("August", "08"));
        replacements.add(new Replacement("September", "09"));
        replacements.add(new Replacement("Oktober", "10"));
        replacements.add(new Replacement("November", "11"));
        replacements.add(new Replacement("Dezember", "12"));
        replacements.add(new Replacement("Jan.", "01"));
        replacements.add(new Replacement("Feb.", "02"));
        replacements.add(new Replacement("März", "03"));
        replacements.add(new Replacement("Apr.", "04"));
        replacements.add(new Replacement("Jun.", "06"));
        replacements.add(new Replacement("Jul.", "07"));
        replacements.add(new Replacement("Aug.", "08"));
        replacements.add(new Replacement("Sept.", "09"));
        replacements.add(new Replacement("Okt.", "10"));
        replacements.add(new Replacement("Nov.", "11"));
        replacements.add(new Replacement("Dez.", "12"));
        replacements.add(new Replacement("Nov", "11"));
        return replacements;
    }

    private List<Replacement> weekdayReplacements() {
        List<Replacement> replacements = new ArrayList<>();
        replacements.add(new Replacement("Montag", "GG"));
        replacements.add(new Replacement("Dienstag", "GG"));
        replacements.add(new Replacement("Mittwoch", "GG"));
        replacements.add(new Replacement("Donnerstag", "GG"));
        replacements.add(new Replacement("Freitag", "GG"));
        replacements.add(new Replacement("Samstag", "GG"));
        replacements.add(new Replacement("Sonntag", "GG"));
        return replacements;
    }

    private static final class RuleCsvEntry {
        private final int lineNumber;
        private final String inputMask;
        private final String inputPattern;
        private final String inputExample;
        private final String outputMask;
        private final String outputPattern;
        private final String outputExample;

        private RuleCsvEntry(int lineNumber, String inputMask, String inputPattern, String inputExample,
            String outputMask, String outputPattern, String outputExample) {
            this.lineNumber = lineNumber;
            this.inputMask = inputMask;
            this.inputPattern = inputPattern;
            this.inputExample = inputExample;
            this.outputMask = outputMask;
            this.outputPattern = outputPattern;
            this.outputExample = outputExample;
        }
    }
}
