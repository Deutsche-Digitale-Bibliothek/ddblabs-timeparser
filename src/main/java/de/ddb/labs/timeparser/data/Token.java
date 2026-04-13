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

/** An element in a rule's input or output specification. Created by {@link PatternParser}. */
public class Token {

    public enum Type {
        GENERIC_VARIABLE, MONTH_REPLACEMENT_VARIABLE, WEEKDAY_REPLACEMENT_VARIABLE, TEXT
    }

    protected final String patternValue;
    protected final Type type;

    public Token(Type type, String patternValue) {
        this.patternValue = patternValue;
        this.type = type;
    }

    public String getPatternValue() {
        return this.patternValue;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.patternValue == null) ? 0 : this.patternValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( !(obj instanceof Token) ) {
            return false;
        }
        Token other = (Token) obj;
        if ( this.type != other.type ) {
            return false;
        }
        if ( this.patternValue == null ) {
            if ( other.patternValue != null ) {
                return false;
            }
        } else if ( !this.patternValue.equals(other.patternValue) ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Token[" + this.type + ", pattern=" + this.patternValue + "]";
    }

}
