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

    public String getInputValue() {
        return this.inputValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.inputValue == null) ? 0 : this.inputValue.hashCode());
        result = prime * result + ((this.patternValue == null) ? 0 : this.patternValue.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !super.equals(obj) ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        TokenWithValue other = (TokenWithValue) obj;
        if ( this.inputValue == null ) {
            if ( other.inputValue != null ) {
                return false;
            }
        } else if ( !this.inputValue.equals(other.inputValue) ) {
            return false;
        }
        if ( this.patternValue == null ) {
            if ( other.patternValue != null ) {
                return false;
            }
        } else if ( !this.patternValue.equals(other.patternValue) ) {
            return false;
        }
        return this.type == other.type;
    }

    @Override
    public String toString() {
        return "TokenWithValue[" + this.type + ", pattern=" + this.patternValue + ", input=" + this.inputValue + "]";
    }

}
