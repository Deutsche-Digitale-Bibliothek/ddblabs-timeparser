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
    <version>0.1.0</version>
</dependency>
```

## Usage

```java
import de.ddb.labs.timeparser.TimeParser;

public class Example {
    public static void main(String[] args) {
        String value = TimeParser.getInstance().parseTime("-20000-02-21");
        System.out.println(value);
    }
}
```

Expected output:

```text
time_18000 -7304949|-7304949
```

## Parsing, Error Handling, and Logging

`TimeParser` is optimized for indexing pipelines (for example Solr indexing):

- `parseTime(...)` never throws to the caller.
- On parse failure it returns an empty string (`""`).
- Failures are logged with rate-limiting and per-error aggregation.

### API

```java
TimeParser parser = TimeParser.getInstance();

// Fast path without context id
String value1 = parser.parseTime("-20000-02-21");

// Preferred in batch/indexing pipelines: include a context id (e.g. document id)
String value2 = parser.parseTime("1944-1945/1949", "solr-doc-42");
```

### Failure Return Value

If parsing fails, the parser returns `""`.

Recommended indexing behavior:

1. If the returned value is not empty, index it.
2. If the returned value is empty, skip time fields for that document.

### Logging Behavior

Logging is grouped by error type:

- First 3 occurrences per error type: detailed warning log with context and input.
- After that: summary warning every 100 occurrences.
- Stacktraces are emitted only in debug mode for detailed/summary points.

Example detailed log:

```text
TimeParser failed (type=DISJOINT_TIME_SPAN, count=1) for context [solr-doc-42], input [1944-1945/1949]: ...
```

### Error Statistics for Monitoring

You can read aggregated error counters without parsing logs:

```java
TimeParser parser = TimeParser.getInstance();
Map<String, TimeParser.ParseErrorStats> stats = parser.getErrorStats();

TimeParser.ParseErrorStats disjoint = stats.get("DISJOINT_TIME_SPAN");
if (disjoint != null) {
    System.out.println(disjoint.getCount());
    System.out.println(disjoint.getLastContext());
}
```

For batch jobs, reset counters at the beginning of each run:

```java
TimeParser.getInstance().resetErrorStats();
```

## Build

```bash
./mvnw verify
```

Windows:

```powershell
.\mvnw.cmd verify
```

## Release Automation

- [.github/workflows/release.yml](.github/workflows/release.yml) builds on `main` and pull requests, and publishes to Maven Central on GitHub releases.
- [.github/workflows/prepare-next-snapshot.yml](.github/workflows/prepare-next-snapshot.yml) bumps the project to the next patch `-SNAPSHOT` after a successful release publish.

Required repository secrets:

- `CENTRAL_TOKEN_USERNAME`
- `CENTRAL_TOKEN_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

Release flow:

1. Set [pom.xml](pom.xml) to a non-`-SNAPSHOT` version.
2. Merge to `main`.
3. Create a GitHub release.
4. Let the workflows publish the release and move `main` to the next development version.