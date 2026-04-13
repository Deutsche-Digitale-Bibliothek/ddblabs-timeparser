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

public class Facet {
    private final String id;
    private final String notation;
    private final Long earliestDate;
    private final Long latestDate;
    private final String prefLabelDe;
    private final String prefLabelEn;
    private final String sortOrder;

    public Facet(String id, String notation, Long earliestDate, Long latestDate, String prefLabelDe, String prefLabelEn, String sortOrder) {
        this.id = id;
        this.notation = notation;
        this.earliestDate = earliestDate;
        this.latestDate = latestDate;
        this.prefLabelDe = prefLabelDe;
        this.prefLabelEn = prefLabelEn;
        this.sortOrder = sortOrder;
    }

    public String getId() {
        return this.id;
    }

    public String getNotation() {
        return this.notation;
    }

    public Long getEarliestDate() {
        return this.earliestDate;
    }

    public Long getLatestDate() {
        return this.latestDate;
    }

    public String getPrefLabelDe() {
        return this.prefLabelDe;
    }

    public String getPrefLabelEn() {
        return this.prefLabelEn;
    }

    public String getSortOrder() {
        return this.sortOrder;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.earliestDate == null) ? 0 : this.earliestDate.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.latestDate == null) ? 0 : this.latestDate.hashCode());
        result = prime * result + ((this.notation == null) ? 0 : this.notation.hashCode());
        result = prime * result + ((this.prefLabelDe == null) ? 0 : this.prefLabelDe.hashCode());
        result = prime * result + ((this.prefLabelEn == null) ? 0 : this.prefLabelEn.hashCode());
        result = prime * result + ((this.sortOrder == null) ? 0 : this.sortOrder.hashCode());
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
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final Facet other = (Facet) obj;
        if ( this.earliestDate == null ) {
            if ( other.earliestDate != null ) {
                return false;
            }
        } else if ( !this.earliestDate.equals(other.earliestDate) ) {
            return false;
        }
        if ( this.id == null ) {
            if ( other.id != null ) {
                return false;
            }
        } else if ( !this.id.equals(other.id) ) {
            return false;
        }
        if ( this.latestDate == null ) {
            if ( other.latestDate != null ) {
                return false;
            }
        } else if ( !this.latestDate.equals(other.latestDate) ) {
            return false;
        }
        if ( this.notation == null ) {
            if ( other.notation != null ) {
                return false;
            }
        } else if ( !this.notation.equals(other.notation) ) {
            return false;
        }
        if ( this.prefLabelDe == null ) {
            if ( other.prefLabelDe != null ) {
                return false;
            }
        } else if ( !this.prefLabelDe.equals(other.prefLabelDe) ) {
            return false;
        }
        if ( this.prefLabelEn == null ) {
            if ( other.prefLabelEn != null ) {
                return false;
            }
        } else if ( !this.prefLabelEn.equals(other.prefLabelEn) ) {
            return false;
        }
        if ( this.sortOrder == null ) {
            if ( other.sortOrder != null ) {
                return false;
            }
        } else if ( !this.sortOrder.equals(other.sortOrder) ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Facet[id="
            + this.id
            + ", notation="
            + this.notation
            + ", earliestDate="
            + this.earliestDate
            + ", latestDate="
            + this.latestDate
            + ", prefLabelDe="
            + this.prefLabelDe
            + ", prefLabelEn="
            + this.prefLabelEn
            + ", sortOrder="
            + this.sortOrder
            + "]";
    }
}
