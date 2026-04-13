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