# timeparser

Java 21 library for turning textual date expressions into machine-readable time spans, sortable day ranges, and DDB facet ids.

## What it does

Given input such as `Mai 2010`, `15. Jh.`, or `vor 500 Mio. Jahren`, the parser can produce:

- a compact legacy string like `time_62100|time_62110 2455318|2455348`
- a structured result object with normalization and error metadata
- an HTTP JSON response via the embedded demo server

The `time_XXX` identifiers refer to the DDB Zeitvokabular and are publicly browsable here:

- DDB Zeitvokabular: https://xtree-public.digicult-verbund.de/vocnet/?uriVocItem=http://ddb.vocnet.org/zeitvokabular/&startNode=dat00113&lang=de&d=n

## Requirements

- Java 21
- Maven Wrapper included, or Maven 3.9+

## Maven

```xml
<dependency>
    <groupId>de.ddb.labs</groupId>
    <artifactId>timeparser</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

## Quick start

```java
import de.ddb.labs.timeparser.TimeParser;
import de.ddb.labs.timeparser.TimeParser.IndexDaysMode;

TimeParser parser = TimeParser.getInstance();

String julian = parser.parseTime("Mai 2010");
String legacy = parser.parseTime("Mai 2010", IndexDaysMode.LEGACY);
```

`parseTime(...)` is the fail-safe API: it returns `""` on parse failure.
For debugging or integration code, prefer `parseTimeResult(...)`.

## Output model

The compact output format is:

```text
<facetString> <startIndexDay>|<endIndexDay>
```

Example:

```text
time_62100|time_62110 2455318|2455348
```

Meaning:

- `facetString`: pipe-separated DDB Zeitvokabular ids
- `startIndexDay` / `endIndexDay`: sortable numeric day bounds
- default index mode is `JULIAN_DAY`; `LEGACY` is kept for compatibility

## Parsing pipeline

Every input passes through six steps. Each step is now a public method on `TimeParser` so you can call, test, or inspect them individually.

```
Raw input string
       │
       ▼  Step 1: applyNormalizationRules(input)
       │  normalizations.csv — expands abbreviations, spelling variants,
       │  century/millennium expressions
       │  e.g. "200000000 v. Chr."  →  "-200000000"
       │       "15. Jh."            →  "15. Jahrhundert"
       ▼
       │  Step 2: tokenizeMonthsAndWeekdays(preprocessed)
       │  Replaces month and weekday names with match tokens
       │  e.g. "März 2010"  →  "MM 2010"
       ▼
       │  Step 3: findMatchingRules(tokenized)    ← rules.csv
       │  Selects exactly one input mask; returns empty list (→ error)
       │  or more than one (→ MULTIPLE_RULES error)
       ▼
       │  Step 4: applyRule(preprocessed, rule)
       │  Applies the matched output pattern
       │  e.g. "1923 ?"  →  "ca. 1923"
       │       "MM 2010" →  "2010-05"           (transformedInput)
       ▼
       │  Step 5: new TimeSpanParser().parse(transformedInput)
       │  Converts the canonical expression to a concrete date range
       │  e.g. startDate=2010-05-01, endDate=2010-05-31   (TimeSpan)
       ▼
       │  Step 6a: resolveFacetNotations(timeSpan) ← facets.csv
       │           buildFacetString(facetNotations)
       │           e.g. "time_62100|time_62110"
       │
       │  Step 6b: computeIndexDay(date, indexDaysMode)
       │           Julian Day Number (default) or legacy algorithm
       │           e.g. startIndexDay=2455318, endIndexDay=2455348
       ▼
  "time_62100|time_62110 2455318|2455348"
```

### Calling individual steps

```java
TimeParser p = TimeParser.getInstance();

// Step 1: applyNormalizationRules — normalizations.csv
String preprocessed = p.applyNormalizationRules("März 2010");
// → "März 2010"  (no normalization rule matches this input)

// Step 2: tokenizeMonthsAndWeekdays — month/weekday tokenization
String tokenized = p.tokenizeMonthsAndWeekdays(preprocessed);
// → "MM 2010"

// Step 3: findMatchingRules — rule matching
List<Rule> rules = p.findMatchingRules(tokenized);
Rule rule = rules.get(0);

// Step 4: applyRule — rule application (uses preprocessed, not tokenized)
String transformedInput = p.applyRule(preprocessed, rule);
// → "2010-03"

// Step 5: TimeSpanParser.parse — time span parsing
TimeSpan span = new TimeSpanParser().parse(transformedInput);
LocalDate start = span.getStartDate();  // 2010-03-01
LocalDate end   = span.getEndDate();    // 2010-03-31

// Step 6a: resolveFacetNotations + buildFacetString
List<FacetNotation> notations = p.resolveFacetNotations(span);
String facetString = p.buildFacetString(notations);
// → "time_62100|time_62110"

// Step 6b: computeIndexDay — index day
long startDay = p.computeIndexDay(start, TimeParser.IndexDaysMode.JULIAN_DAY);
long endDay   = p.computeIndexDay(end,   TimeParser.IndexDaysMode.JULIAN_DAY);
```

For end-to-end parsing without inspecting intermediate steps, use `parseTimeResult(...)` which returns all of the above fields pre-computed in a single `ParseResult`.

### Step 5: valid `transformedInput` expressions

Every output mask in rules.csv must produce a string that `TimeSpanParser` (Step 5) can parse. Valid expressions follow this grammar (verified against current source):

```
complex  :=  simple "/" complex         -- continuous span: start of first to end of last
          |  simple "," complex         -- throws DISJOINT_TIME_SPAN
          |  simple " oder " complex    -- throws DISJOINT_TIME_SPAN
          |  simple

simple   :=  rangeModifier " " date
          |  date

rangeModifier
         :=  "ab" | "seit"       -- from this date onwards
          |  "bis"                -- up to this date
          |  "vor"                -- before this date
          |  "nach"               -- after this date
          |  "um" | "ca."         -- around (± year-dependent delta, see getAroundDelta())
          |  "vermutlich"         -- presumably this date (no range expansion)

date     :=  [limitation " "] digit+ ". Jahrhundert" [" vor/nach Christus"]
          |  "-" yearMonthDay     -- BCE date
          |  yearMonthDay

yearMonthDay
         :=  digit+ "-" digit{2} "-" digit{2}   -- full date
          |  digit+ "-" digit{2}                -- year-month
          |  digit+                             -- year only

limitation (century qualifier)
         :=  digit+ ". Dekade" | digit+ ". Viertel"
          |  digit+ ". Drittel" | digit+ ". Hälfte"
          |  "Anfang" | "Mitte" | "Ende"
```

The `/` operator creates a **continuous span** from the start of the first date to the end of the last (e.g. `1911/1914` → 1911-01-01/1914-12-31). The `,` and ` oder ` operators are syntactically parsed but always throw `DISJOINT_TIME_SPAN` — they appear in rule outputs that represent forms the step-5 parser cannot combine (e.g. `1944/1945,1949` from the rule matching `1944-1945/1949`).

Not supported by `TimeSpanParser`: `Jahrtausend`, `zwischen X und Y`, `nicht datiert` / `undatiert` / `ohne Datum`.

## Structured API

Available overloads:

```java
String parseTime(String input)
String parseTime(String input, IndexDaysMode mode)
String parseTime(String input, String contextId)
String parseTime(String input, String contextId, IndexDaysMode mode)

ParseResult parseTimeResult(String input)
ParseResult parseTimeResult(String input, IndexDaysMode mode)
ParseResult parseTimeResult(String input, String contextId)
ParseResult parseTimeResult(String input, String contextId, IndexDaysMode mode)
```

Useful `ParseResult` fields:

- `normalizedInput` — after steps 1 and 2 (normalization + month/weekday tokenization)
- `matchingRules` / `matchedRule` — after step 3
- `transformedInput` — after step 4
- `timeSpan` — after step 5
- `facetString` / `facetNotations` — after step 6a
- `startIndexDay` / `endIndexDay` — after step 6b
- `successful` / `errorType` / `errorMessage`

Errors are aggregated internally and can be inspected via `getErrorStats()`.

## CSV-driven knowledge base

The parser behavior is data-driven. Code provides the parsing engine; the CSV files provide vocabulary and transformation knowledge.

| File | Role |
|------|------|
| [rules.csv](src/main/resources/conf/timeparser/rules.csv) | 346 rules mapping input masks and patterns to normalized parser expressions |
| [normalizations.csv](src/main/resources/conf/timeparser/normalizations.csv) | Regex replacements (Step 1) and literal month/weekday token substitutions (Step 2) |
| [facets.csv](src/main/resources/conf/timeparser/facets.csv) | 110 entries mapping year ranges to DDB Zeitvokabular facet ids and labels |

All `rules.csv` input examples are regression-tested in [src/test/java/de/ddb/labs/timeparser/TimeParserTest.java](src/test/java/de/ddb/labs/timeparser/TimeParserTest.java). For CSV file formats, design principles, and guidance on extending the knowledge base, see [src/main/resources/conf/timeparser/README.md](src/main/resources/conf/timeparser/README.md).

### Mask and pattern syntax

Every rule defines its input and output through a *mask* and a *pattern* of identical length.
The mask character at each position determines the token type; the pattern character at the same position names the variable or reproduces the literal text.

**Token types**

| Mask char | Pattern char | Token type | Description |
|-----------|-------------|------------|-------------|
| `#` | any letter (except `M`, `G`) | Generic variable | Matches exactly one digit in the input. All consecutive `#` positions with the same pattern letter form one variable. No two variables in the same input specification may share the same initial letter. |
| `M` + `M` | two identical letters | Month variable | A two-character pair of `M` in the mask captures the two-character month token produced by Step 2. |
| `G` + `G` | two identical letters | Weekday variable | A two-character pair of `G` in the mask captures the two-character weekday token produced by Step 2. |
| any other char | same char | Literal text | Mask and pattern character must be identical. The mask character is used verbatim during input matching. |

> **Key constraint — mask matching:** `#` accepts any digit; literal characters must match exactly; spaces must align with spaces. This is enforced by `isMatching()` in `TimeParser`.

> **Key constraint — duplicate variable initials:** Within a single input specification, no two variables may begin with the same letter. This is enforced when the pattern is parsed (output specifications may repeat variable initials, e.g. the same year variable `JJJJ` used in both start and end of a range).

**Variable naming conventions** (the specific letters are free to choose, but the following names are used consistently throughout `rules.csv`):

| Pattern letters | Meaning |
|-----------------|---------|
| `JJJJ` | 4-digit year |
| `XXXX`, `ZZZZ`, `YYYY` | Second/third year in a range |
| `TT` | 2-digit day |
| `XX` | Second day in a range |
| `MM` | Month variable (2-char month token, must use mask `MM`) |
| `YY` | Second month in a range (must use mask `MM`) |
| `GG` | Weekday variable (2-char weekday token, must use mask `GG`) |

**Example**

Input `März 2010` is tokenized by Step 2 to `MM 2010`. The matching rule is:

```
input  mask:    MM ####
input  pattern: MM JJJJ
output mask:    ####-##
output pattern: JJJJ-MM
```

The parser reads two tokens from the input: `MM` → month variable with value `März` (resolved to `03` during output), `JJJJ` → year variable with value `2010`.
The output template produces `2010-03`.

## Semantics and limits

A few important caveats:

- the parser is optimized for historical and catalog-style date strings, not arbitrary natural language
- exactly one rule must match after normalization; ambiguous inputs are rejected
- the `/` operator creates a continuous span from the start of the first date to the end of the last (e.g. `1911/1914` = 1911 to 1914); `,` and ` oder ` always throw `DISJOINT_TIME_SPAN`
- year range is `-1000000000` to `999999999` (bounded by `java.time.LocalDate`); larger magnitudes return `""` through the fail-safe API
- the fail-safe `parseTime(...)` methods return `""` on errors; if you need diagnostics, use `parseTimeResult(...)`
- request input is intentionally capped at 2048 characters to protect the parser from excessive memory and CPU pressure

## HTTP demo server

The project ships with a minimal Javalin-based HTTP wrapper in `de.ddb.labs.timeparser.TimeParserHttpServer`.

Build and run:

```bash
./mvnw -q -DskipTests package
java -jar target/timeparser-2.0.0-SNAPSHOT-shaded.jar
```

Optional environment variables:

- `TIMEPARSER_HOST` — default `127.0.0.1`
- `TIMEPARSER_PORT` — default `8080`

Request:

```text
GET /?date=Mai%202010&indexDaysMode=JULIAN_DAY
```

Successful response shape:

```json
{
  "successful": true,
  "input": "Mai 2010",
  "indexDaysMode": "JULIAN_DAY",
  "normalizedInput": "MM 2010",
  "transformedInput": "2010-05",
  "timeSpan": {
    "parsedInputString": "2010-05",
    "startISODate": "2010-05-01",
    "endISODate": "2010-05-31"
  },
  "facetString": "time_62100|time_62110",
  "startIndexDay": 2455318,
  "endIndexDay": 2455348,
  "output": "time_62100|time_62110 2455318|2455348"
}
```

Notes:

- `startISODate` and `endISODate` make the wire format explicit: these are ISO-serialized calendar dates
- empty strings and empty arrays are omitted from failure JSON
- `errorType` and `errorMessage` are only present on failures

## Build, test, package

```bash
./mvnw test
./mvnw -DskipTests package
```

The `package` goal also produces a runnable shaded jar:

- `target/timeparser-2.0.0-SNAPSHOT-shaded.jar`

## Docker

```bash
docker build -t timeparser .
docker run --rm -p 8080:8080 -e TIMEPARSER_HOST=0.0.0.0 -e TIMEPARSER_PORT=8080 timeparser
```

