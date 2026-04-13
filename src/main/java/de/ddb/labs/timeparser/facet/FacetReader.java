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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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
public class FacetReader {

    private boolean skipHashes = true;

    public List<Facet> read(String path, String charsetName) throws IOException, ParseException {
        ArrayList<Facet> facets = new ArrayList<>();

        try (final InputStream in = FacetReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Facet file does could not be found for the given path \"" + path + "\"");
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in, charsetName))) {
                int i = 0;
                String line;
                boolean skippedFirstLine = false;
                while ((line = reader.readLine()) != null) {
                    if (skippedFirstLine) {
                        line = line.trim();
                        if (line.length() > 0) {
                            String[] columns = line.split("\t");
                            if (columns.length < 7) {
                                final String errorMsg = "Expected 7 columns instead of " + columns.length + " in facet file \"" + path + "\", line " + i + ": \"" + line + "\"";
                                throw new ParseException(errorMsg, i);
                            }

                            try {
                                final Facet facet = new Facet(columns[0], columns[1], Long.valueOf(columns[2]), Long.valueOf(columns[3]), columns[4], columns[5], columns[6]);
                                facets.add(facet);
                            } catch (Exception x) {
                                if (!this.skipHashes) {
                                    final String errorMsg = "Incorrect number format in facet file \"" + path + "\", line " + i + ": \"" + line + "\"";
                                    throw new ParseException(errorMsg, i);
                                }
                            }
                        }
                    } else {
                        skippedFirstLine = true;
                    }
                    i++;
                }
            }
        }

        return facets;
    }

    public List<Facet> readSkipHashFacets(String path, String charsetName) throws IOException, ParseException {
        this.skipHashes = true;
        return read(path, charsetName);
    }
}
