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
package de.ddb.labs.timeparser.replacement;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads typed replacements from a CSV resource.
 *
 * The first two columns define the source and target values. An optional third
 * column can be used as a category filter such as normalization, month, or
 * weekday.
 */
public final class ReplacementReader {

    private ReplacementReader() {
    }

    /**
     * Reads all configured regex replacements from the given classpath resource.
     * Longer source strings are applied first so that specific variants win over
     * shorter overlapping ones.
     */
    public static List<Replacement> read(final String path, final String charsetName)
            throws IOException, ParseException {
        return read(path, charsetName, true, null);
    }

    /**
     * Reads all configured replacements from the given classpath resource using
     * the requested replacement mode.
     */
    public static List<Replacement> read(final String path, final String charsetName, final boolean regex)
            throws IOException, ParseException {
        return read(path, charsetName, regex, null);
    }

    /**
     * Reads all configured replacements from the given classpath resource and
     * optionally filters them by category from the third CSV column.
     */
    public static List<Replacement> read(final String path, final String charsetName, final boolean regex,
            final String category)
            throws IOException, ParseException {
        final List<Replacement> replacements = new ArrayList<>();
        final CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (InputStream in = ReplacementReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Replacement file could not be found for the given path \"" + path + "\"");
            }
            try (CSVParser parser = CSVParser.parse(new InputStreamReader(in, charsetName), format)) {
                for (CSVRecord record : parser) {
                    final int lineNumber = Math.toIntExact(record.getRecordNumber());
                    if (record.size() < 2) {
                        throw new ParseException(
                                "Expected at least 2 columns in replacement file \"" + path + "\", line "
                                        + lineNumber + ": \"" + record + "\"",
                                lineNumber);
                    }
                    if (!matchesCategory(record, category)) {
                        continue;
                    }
                    replacements.add(new Replacement(record.get(0), record.get(1), regex));
                }
            }
        }

        return replacements;
    }

    private static boolean matchesCategory(final CSVRecord record, final String category) {
        if (category == null || category.isEmpty()) {
            return true;
        }
        return record.size() >= 3 && category.equalsIgnoreCase(record.get(2));
    }
}
