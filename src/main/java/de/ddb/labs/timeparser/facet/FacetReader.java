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
package de.ddb.labs.timeparser.facet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Reads facets from a file.
 * </p>
 * <ul>
 * <li>The first line in the file is ignored and can be used as a header
 * line.</li>
 * <li>Each line must consist of seven columns, separated by a tab character
 * (\t). The columns must correspond to the following, in the specified order:
 * <ol>
 * <li>ID</li>
 * <li>Notation</li>
 * <li>Earliest date in years</li>
 * <li>Latest date in years</li>
 * <li>German description</li>
 * <li>English description</li>
 * <li>A sorting value to sort facets</li>
 * </ol>
 * </li>
 * </ul>
 */
@Slf4j
public class FacetReader {

    public List<Facet> read(String path, String charsetName) throws IOException, ParseException {
        ArrayList<Facet> facets = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (final InputStream in = FacetReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Facet file does could not be found for the given path \"" + path + "\"");
            }
            try (final CSVParser parser = CSVParser.parse(new InputStreamReader(in, charsetName), format)) {
                for (CSVRecord record : parser) {
                    int lineNumber = Math.toIntExact(record.getRecordNumber());
                    if (record.size() < 7) {
                        final String errorMsg = "Expected 7 columns instead of " + record.size() + " in facet file \""
                                + path + "\", line " + lineNumber + ": \"" + record + "\"";
                        throw new ParseException(errorMsg, lineNumber);
                    }

                    try {
                        final Facet facet = new Facet(record.get(0), record.get(1), Long.valueOf(record.get(2)),
                                Long.valueOf(record.get(3)), record.get(4), record.get(5), record.get(6));
                        facets.add(facet);
                    } catch (NumberFormatException x) {
                        log.warn("Skipping facet row with invalid numeric value in facet file \"{}\", line {}: {} ({})",
                                path,
                                lineNumber,
                                record,
                                x.getMessage());
                    }
                }
            }
        }

        return facets;
    }
}
