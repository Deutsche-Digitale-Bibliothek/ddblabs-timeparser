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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ddb.labs.timeparser.http.HttpParseResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.compression.CompressionStrategy;
import io.javalin.http.Header;
import io.javalin.json.JavalinJackson;
import lombok.extern.slf4j.Slf4j;

/**
 * Minimal HTTP wrapper exposing {@link TimeParser} via query parameters.
 */
@Slf4j
public final class TimeParserHttpServer {

    private static final String HOST_ENV = "TIMEPARSER_HOST";
    private static final String PORT_ENV = "TIMEPARSER_PORT";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final TimeParser PARSER = TimeParser.getInstance();

    private TimeParserHttpServer() {
    }

    /**
     * Starts the HTTP endpoint.
     */
    public static void main(final String[] args) {
        final String host = firstNonBlank(
                System.getenv(HOST_ENV),
                System.getProperty("server.host"),
                DEFAULT_HOST);
        final int port = Integer.parseInt(firstNonBlank(
                System.getenv(PORT_ENV),
                System.getProperty("server.port"),
                String.valueOf(DEFAULT_PORT)));

        createApp().start(host, port);
        log.info("Server gestartet auf http://{}:{}/?date=Mai%202010&indexDaysMode=JULIAN_DAY", host, port);
    }

    private static Javalin createApp() {
        return Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(createObjectMapper(), false));
            config.http.defaultContentType = JSON_CONTENT_TYPE;
            config.http.compressionStrategy = CompressionStrategy.GZIP;
            config.http.compressionStrategy.setDefaultMinSizeForCompression(0);
            config.routes.get("/", ctx -> {
                ctx.res().setCharacterEncoding(StandardCharsets.UTF_8.name());
                ctx.header(Header.VARY, Header.ACCEPT_ENCODING);

                final String input = ctx.queryParam("date");
                if (input == null || input.isBlank()) {
                    ctx.status(400)
                            .json(Map.of("error", "Missing query parameter 'date'"));
                    return;
                }

                try {
                    final TimeParser.IndexDaysMode indexDaysMode = resolveIndexDaysMode(ctx.queryParam("indexDaysMode"));
                    ctx.json(HttpParseResponse.from(PARSER.parseTimeResult(input, indexDaysMode)));
                } catch (IllegalArgumentException exception) {
                    ctx.status(400).json(Map.of(
                            "error", exception.getMessage(),
                            "supportedValues", Arrays.stream(TimeParser.IndexDaysMode.values())
                                    .map(Enum::name)
                                    .toList()));
                }
            });
        });
    }

    protected static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
    }

    private static TimeParser.IndexDaysMode resolveIndexDaysMode(final String primaryValue) {
        final String rawValue = firstNonBlank(primaryValue, TimeParser.IndexDaysMode.JULIAN_DAY.name());
        try {
            return TimeParser.IndexDaysMode.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid query parameter 'indexDaysMode': " + rawValue);
        }
    }

    private static String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }


}