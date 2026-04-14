# timeparser

Java library for parsing textual time expressions into a compact machine-readable representation.

## Requirements

- Java 21
- Maven Wrapper or Maven 3.9+

## Maven

```xml
<dependency>
    <groupId>de.ddb.labs</groupId>
    <artifactId>timeparser</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

```java
import de.ddb.labs.timeparser.TimeParser;
import de.ddb.labs.timeparser.TimeParser.IndexDaysMode;

public class Example {
    public static void main(String[] args) {
        TimeParser parser = TimeParser.getInstance();

        // Default: Julian Day output index
        String julian = parser.parseTime("-20000-02-21");

        // Optional: legacy day-index behavior for compatibility
        String legacy = parser.parseTime("-20000-02-21", IndexDaysMode.LEGACY);

        System.out.println(julian);
        System.out.println(legacy);
    }
}
```

Expected output (Julian default):

```text
time_18000 -5583373|-5583373
```

## IndexDaysMode

`parseTime(...)` supports two day-index strategies:

- `JULIAN_DAY` (default): based on `JulianFields.JULIAN_DAY`.
- `LEGACY`: historical compatibility mode.

Available overloads:

```java
String parseTime(String input)
String parseTime(String input, IndexDaysMode mode)
String parseTime(String input, String contextId)
String parseTime(String input, String contextId, IndexDaysMode mode)
```

## Error Handling and Logging

Important behavior:

- `parseTime(...)` never throws; on failure it returns `""`.
- Errors are aggregated by type and rate-limited in logs:
  - first occurrence: detailed warning,
  - then summary every 100 occurrences.
- Stack traces are only logged on debug level when `-Dtimeparser.logStacktraces=true` is set.

Monitoring API:

```java
TimeParser parser = TimeParser.getInstance();
Map<String, TimeParser.ParseErrorStats> stats = parser.getErrorStats();
parser.resetErrorStats();
```

## HTTP Demo Server

The project contains a minimal embedded HTTP server in `de.ddb.labs.timeparser.TimeParserHttpServer`.

Build and start it locally:

```bash
./mvnw -q -DskipTests package
TIMEPARSER_HOST=127.0.0.1 TIMEPARSER_PORT=8080 java -jar target/timeparser-2.0.0-SNAPSHOT.jar
```

Configuration:

- Host: `TIMEPARSER_HOST`, default `127.0.0.1`
- Port: `TIMEPARSER_PORT`, default `8080`

Optional JVM properties are still supported:

```bash
java -Dserver.host=127.0.0.1 -Dserver.port=8080 -jar target/timeparser-2.0.0-SNAPSHOT.jar
```

Request:

```text
GET /?date=Mai%202010&indexDaysMode=JULIAN_DAY
```

Query parameters:

- `date`: required input string to parse
- `indexDaysMode`: optional, one of `JULIAN_DAY` or `LEGACY`
- `indexDayMode`: optional legacy alias for `indexDaysMode`

Example response:

```json
{
    "input": "Mai 2010",
    "output": "time_62100|time_62110 2455318|2455348"
}
```

The server responds with `application/json`, sets `Vary: Accept-Encoding`, and enables gzip compression.

## Docker

Build the image:

```bash
docker build -t timeparser .
```

Run it:

```bash
docker run --rm -p 8080:8080 \
    -e TIMEPARSER_HOST=0.0.0.0 \
    -e TIMEPARSER_PORT=8080 \
    timeparser
```

The container runs as a non-root user.

Example request:

```bash
curl --compressed 'http://127.0.0.1:8080/?date=Mai%202010&indexDaysMode=JULIAN_DAY'
```
