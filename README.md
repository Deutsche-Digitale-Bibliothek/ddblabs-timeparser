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

- `successful`
- `normalizedInput`
- `matchingRules` / `matchedRule`
- `transformedInput`
- `timeSpan`
- `facetString`
- `startIndexDay` / `endIndexDay`
- `errorType` / `errorMessage`

Errors are aggregated internally and can be inspected via `getErrorStats()`.

## CSV-driven knowledge base

The parser behavior is data-driven:

- [src/main/resources/conf/timeparser/rules.csv](src/main/resources/conf/timeparser/rules.csv) — maps input masks and patterns to normalized parser expressions
- [src/main/resources/conf/timeparser/normalizations.csv](src/main/resources/conf/timeparser/normalizations.csv) — regex pre-normalization plus literal month/weekday token replacements
- [src/main/resources/conf/timeparser/facets.csv](src/main/resources/conf/timeparser/facets.csv) — maps year ranges to DDB facet ids and labels

In short: code provides the parsing engine, CSV files provide most of the vocabulary and transformation knowledge.

All `rules.csv` examples are regression-tested in [src/test/java/de/ddb/labs/timeparser/TimeParserTest.java](src/test/java/de/ddb/labs/timeparser/TimeParserTest.java).

## Semantics and limits

A few important caveats:

- the parser is optimized for historical and catalog-style date strings, not arbitrary natural language
- exactly one rule must match after normalization; ambiguous inputs are rejected
- disjoint expressions such as `1944/1945,1949` are currently rejected
- very large years are bounded by `java.time.LocalDate`
- inputs up to `999999999` and `-1000000000` are supported in the current implementation; larger magnitudes return `""` through the fail-safe API
- the fail-safe `parseTime(...)` methods return `""` on errors; if you need diagnostics, use `parseTimeResult(...)`
- request input is intentionally capped at 2048 characters to protect the parser from excessive memory and CPU pressure
- diagnostic logging and stored error-stat values are abbreviated to keep monitoring data bounded

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

