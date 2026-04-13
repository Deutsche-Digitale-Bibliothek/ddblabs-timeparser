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

/** An element in a rule's input specification, filled with a value from the input string. Created by {@link InputParser}. */
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TokenWithValue extends Token {

    private final String inputValue;

    public TokenWithValue(Type type, String patternValue, String inputValue) {
        super(type, patternValue);
        this.inputValue = inputValue;
    }

    public TokenWithValue() {
        super(null, null);
        this.inputValue = null;
    }
}
