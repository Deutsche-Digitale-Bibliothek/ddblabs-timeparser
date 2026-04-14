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
package de.ddb.labs.timeparser.data;

/**
 * Contains a string that needs to be replaced and a string by which it should
 * be replaced. Used to convert textual months and weekdays to numbers.
 */
public class Replacement {
    public final String from;
    public final String to;

    public Replacement(String from, String to) {
        this.from = from;
        this.to = to;
    }
}
