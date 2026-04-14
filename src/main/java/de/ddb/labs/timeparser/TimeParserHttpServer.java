package de.ddb.labs.timeparser;

import io.javalin.Javalin;
import io.javalin.compression.CompressionStrategy;
import io.javalin.http.Header;
import io.javalin.json.JavalinJackson;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeParserHttpServer {
    private static final String HOST_ENV = "TIMEPARSER_HOST";
    private static final String PORT_ENV = "TIMEPARSER_PORT";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final TimeParser PARSER = TimeParser.getInstance();

    public static void main(String[] args) {
        String host = firstNonBlank(
                System.getenv(HOST_ENV),
                System.getProperty("server.host"),
                DEFAULT_HOST);
        int port = Integer.parseInt(firstNonBlank(
                System.getenv(PORT_ENV),
                System.getProperty("server.port"),
                String.valueOf(DEFAULT_PORT)));

        Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson());
            config.http.defaultContentType = JSON_CONTENT_TYPE;
            config.http.compressionStrategy = CompressionStrategy.GZIP;
            config.http.compressionStrategy.setDefaultMinSizeForCompression(0);
            config.routes.get("/", ctx -> {
                ctx.res().setCharacterEncoding(StandardCharsets.UTF_8.name());
                ctx.header(Header.VARY, Header.ACCEPT_ENCODING);

                String input = ctx.queryParam("date");
                if (input == null || input.isBlank()) {
                    ctx.status(400)
                            .json(Map.of("error", "Missing query parameter 'date'"));
                    return;
                }

                try {
                    TimeParser.IndexDaysMode indexDaysMode = resolveIndexDaysMode(ctx.queryParam("indexDaysMode"),
                            ctx.queryParam("indexDayMode"));
                    ctx.json(new ParseResponse(input, PARSER.parseTime(input, indexDaysMode)));
                } catch (IllegalArgumentException exception) {
                    ctx.status(400).json(Map.of(
                            "error", exception.getMessage(),
                            "supportedValues", Arrays.stream(TimeParser.IndexDaysMode.values())
                                    .map(Enum::name)
                                    .toList()));
                }
            });
        }).start(host, port);

        log.info("Server gestartet auf http://{}:{}/?date=Mai%202010&indexDaysMode=JULIAN_DAY", host, port);
    }

    private static TimeParser.IndexDaysMode resolveIndexDaysMode(String primaryValue, String legacyAliasValue) {
        String rawValue = firstNonBlank(primaryValue, legacyAliasValue, TimeParser.IndexDaysMode.JULIAN_DAY.name());
        try {
            return TimeParser.IndexDaysMode.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid query parameter 'indexDaysMode': " + rawValue);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record ParseResponse(String input, String output) {
    }
}