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
package de.ddb.labs.timeparser.timespan;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Interprets a normalized string and determines what time period it corresponds
 * to. The implemented syntax is documented elsewhere. Time periods are represented
 * as {@link TimeSpan}s.
 */
public class TimeSpanParser {

    /**
     * Lightweight holder for calculated start and end dates.
     */
    private record DateBounds(LocalDate start, LocalDate end) {
    }

    private static final Pattern PATTERN_CENTURY = Pattern.compile("^(\\d+)\\. Jahrhundert");
    private static final Pattern PATTERN_CENTURY_MILLENNIUM_LIMITATION = Pattern
            .compile("^([1-9]|10)\\. (Dekade)"
                    + "|([1-3])\\. (Drittel)"
                    + "|([1-4])\\. (Viertel)"
                    + "|([1-2])\\. (Hälfte)"
                    + "|Anfang|Mitte|Ende");
    private static final Pattern PATTERN_RANGE = Pattern
            .compile("^(ab|seit|bis|vor|um|ca\\.|nach|vermutlich)");
    private static final Pattern PATTERN_YMD = Pattern.compile("^(-?)(\\d+)-(\\d{2})-(\\d{2})");
    private static final Pattern PATTERN_YM = Pattern.compile("^(-?)(\\d+)-(\\d{2})");
    private static final Pattern PATTERN_Y = Pattern.compile("^(-?)(\\d+)");

    /**
     * Parses a normalized input string into a concrete {@link TimeSpan}.
     *
     * @param inputString normalized parser input
     * @return parsed time span
     * @throws IllegalStateException if parsing fails or leaves trailing input
     */
    public TimeSpan parse(final String inputString) throws IllegalStateException {
        final InputStringReader input = new InputStringReader(inputString);
        final Position position = new Position();
        final TimeSpan timeSpan = parseComplex(input, position, true);

        if (timeSpan == null) {
            throw new IllegalStateException("The input string \"" + inputString + "\" could not be parsed");
        }
        if (timeSpan.getParsedInputString().length() != inputString.length()) {
            throw new IllegalStateException("The input string \"" + inputString + "\" could not be parsed entirely");
        }
        return timeSpan;
    }

    /**
     * Parses a full expression, including chained operators and optional era suffixes.
     */
    private TimeSpan parseComplex(final InputStringReader input, final Position startingPosition,
            final boolean allowEraSuffix) {
        Position cursor = startingPosition.copy();
        TimeSpan timeSpan = parseSimple(input, cursor);

        if (timeSpan == null) {
            return null;
        }

        final Position operatorCursor = cursor.copy();
        final Operator operator = parseOperator(input, operatorCursor);
        if (operator != null) {
            final TimeSpan nextTimeSpan = parseComplex(input, operatorCursor, false);
            if (nextTimeSpan != null) {
                if (operator.getType() == Operator.OperatorType.OR) {
                    throw new IllegalStateException("Disjoint time spans are not supported: \""
                            + timeSpan.getParsedInputString()
                            + operator.getParsedInputString()
                            + nextTimeSpan.getParsedInputString()
                            + "\"");
                }

                timeSpan = new TimeSpan(timeSpan.getParsedInputString()
                        + operator.getParsedInputString()
                        + nextTimeSpan.getParsedInputString(), timeSpan.getStartDate(), nextTimeSpan.getEndDate());
                cursor = operatorCursor;
            }
        }

        if (allowEraSuffix) {
            timeSpan = applyEraSuffix(input, cursor, timeSpan);
        }
        startingPosition.update(cursor);
        return timeSpan;
    }

    /**
     * Parses binary operators between two date expressions.
     */
    private Operator parseOperator(final InputStringReader input, final Position startingPosition) {
        Position cursor = startingPosition.copy();
        AcceptResult acceptResult = input.tryToAccept(cursor, ',');
        if (acceptResult.isAccepted()) {
            startingPosition.update(cursor);
            return new Operator(acceptResult.getParsedInputString(), Operator.OperatorType.OR);
        }

        cursor = startingPosition.copy();
        acceptResult = input.tryToAccept(cursor, '/');
        if (acceptResult.isAccepted()) {
            startingPosition.update(cursor);
            return new Operator(acceptResult.getParsedInputString(), Operator.OperatorType.BETWEEN);
        }

        cursor = startingPosition.copy();
        acceptResult = input.tryToAccept(cursor, " oder ");
        if (acceptResult.isAccepted()) {
            startingPosition.update(cursor);
            return new Operator(acceptResult.getParsedInputString(), Operator.OperatorType.OR);
        }

        return null;
    }

    /**
     * Parses a single date expression with an optional range qualifier.
     */
    private TimeSpan parseSimple(final InputStringReader input, final Position startingPosition) {
        final Position cursor = startingPosition.copy();
        final Range range = parseRange(input, cursor);
        if (range != null) {
            final AcceptResult separator = input.tryToAccept(cursor, ' ');
            if (separator.isAccepted()) {
                final TimeSpan originalDate = parseDate(input, cursor);
                if (originalDate != null) {
                    final DateBounds modifiedStartEnd = getModifiedStartEnd(originalDate, range);
                    startingPosition.update(cursor);
                    return new TimeSpan(range.getParsedInputString()
                            + separator.getParsedInputString()
                            + originalDate.getParsedInputString(), modifiedStartEnd.start(), modifiedStartEnd.end());
                }
            }
        }

        return parseDate(input, startingPosition);
    }

    /**
     * Parses range qualifiers such as "ab", "bis" or "vermutlich".
     */
    private Range parseRange(final InputStringReader input, final Position startingPosition) {
        final Position cursor = startingPosition.copy();
        final AcceptResult acceptResult = input.tryToAccept(cursor, PATTERN_RANGE);
        if (acceptResult.isAccepted()) {
            startingPosition.update(cursor);
            if (acceptResult.group(1) != null) {
                switch (acceptResult.group(1)) {
                    case "ab":
                    case "seit":
                        return new Range(acceptResult.getParsedInputString(), Range.RangeType.FROM);
                    case "bis":
                        return new Range(acceptResult.getParsedInputString(), Range.RangeType.UNTIL);
                    case "vor":
                        return new Range(acceptResult.getParsedInputString(), Range.RangeType.BEFORE);
                    case "um":
                    case "ca.":
                        return new Range(acceptResult.getParsedInputString(), Range.RangeType.AROUND);
                    case "nach":
                        return new Range(acceptResult.getParsedInputString(), Range.RangeType.AFTER);
                    case "vermutlich":
                        return new Range(acceptResult.getParsedInputString(), Range.RangeType.PRESUMABLY);
                    default:
                        break;
                }
            }
        }
        return null;
    }

    /**
     * Parses the actual date expression once optional range prefixes were handled.
     */
    private TimeSpan parseDate(final InputStringReader input, final Position startingPosition) {
        TimeSpan timeSpan = parseCenturyOrMillennium(input, startingPosition);
        if (timeSpan == null) {
            timeSpan = parseYMD(input, startingPosition);
        }
        return timeSpan;
    }

    /**
     * Parses century-based expressions such as "3. Jahrhundert" or
     * "Mitte 3. Jahrhundert".
     */
    private TimeSpan parseCenturyOrMillennium(final InputStringReader input, final Position startingPosition) {
        Position cursor = startingPosition.copy();
        final CenturyMillenniumLimitation limitation = parseCenturyMillenniumLimitation(input, cursor);
        if (limitation != null) {
            final AcceptResult separator = input.tryToAccept(cursor, ' ');
            if (separator.isAccepted()) {
                final AcceptResult century = input.tryToAccept(cursor, PATTERN_CENTURY);
                if (century.isAccepted()) {
                    final DateBounds startEnd = getStartAndEnd(limitation, Integer.parseInt(century.group(1)));
                    startingPosition.update(cursor);
                    return new TimeSpan(limitation.getParsedInputString()
                            + separator.getParsedInputString()
                            + century.getParsedInputString(), startEnd.start(), startEnd.end());
                }
            }
        }

        cursor = startingPosition.copy();
        final AcceptResult century = input.tryToAccept(cursor, PATTERN_CENTURY);
        if (century.isAccepted()) {
            final int centuryNumber = Integer.parseInt(century.group(1));
            final LocalDate start = LocalDate.of((centuryNumber - 1) * 100 + 1, 1, 1);
            final LocalDate end = LocalDate.of(centuryNumber * 100, 12, 31);

            startingPosition.update(cursor);
            return new TimeSpan(century.getParsedInputString(), start, end);
        }

        return null;
    }

    /**
     * Parses numeric date forms: year, year-month, and year-month-day.
     */
    private TimeSpan parseYMD(final InputStringReader input, final Position startingPosition) {
        Position cursor = startingPosition.copy();
        AcceptResult acceptResult = input.tryToAccept(cursor, PATTERN_YMD);
        if (acceptResult.isAccepted()) {
            final boolean isAnnoDomini = acceptResult.group(1) == null || "".equals(acceptResult.group(1));
            final LocalDate start = createGregorianCalendar(
                    isAnnoDomini,
                    Integer.parseInt(acceptResult.group(2)),
                    Integer.parseInt(acceptResult.group(3)) - 1,
                    Integer.parseInt(acceptResult.group(4)));

            startingPosition.update(cursor);
            return new TimeSpan(acceptResult.getParsedInputString(), start, start);
        }

        cursor = startingPosition.copy();
        acceptResult = input.tryToAccept(cursor, PATTERN_YM);
        if (acceptResult.isAccepted()) {
            final boolean isAnnoDomini = acceptResult.group(1) == null || "".equals(acceptResult.group(1));
            final int year = Integer.parseInt(acceptResult.group(2));
            final int month = Integer.parseInt(acceptResult.group(3));
            final LocalDate start = createGregorianCalendar(isAnnoDomini, year, month - 1, 1);
            final LocalDate end = LocalDate.of(start.getYear(), start.getMonthValue(), start.lengthOfMonth());

            startingPosition.update(cursor);
            return new TimeSpan(acceptResult.getParsedInputString(), start, end);
        }

        cursor = startingPosition.copy();
        acceptResult = input.tryToAccept(cursor, PATTERN_Y);
        if (acceptResult.isAccepted()) {
            final boolean isAnnoDomini = acceptResult.group(1) == null || "".equals(acceptResult.group(1));
            final int year = Integer.parseInt(acceptResult.group(2));
            final LocalDate start = createGregorianCalendar(isAnnoDomini, year, 0, 1);
            final LocalDate end = createGregorianCalendar(isAnnoDomini, year, 11, 31);

            startingPosition.update(cursor);
            return new TimeSpan(acceptResult.group(0), start, end);
        }

        return null;
    }

    /**
     * Appends an explicit era suffix and converts the span to BCE when required.
     */
    private TimeSpan applyEraSuffix(final InputStringReader input, final Position position, final TimeSpan timeSpan) {
        AcceptResult acceptResult = input.tryToAccept(position, " nach Christus");
        if (acceptResult.isAccepted()) {
            return new TimeSpan(timeSpan.getParsedInputString() + acceptResult.getParsedInputString(),
                    timeSpan.getStartDate(), timeSpan.getEndDate());
        }

        acceptResult = input.tryToAccept(position, " vor Christus");
        if (acceptResult.isAccepted()) {
            return new TimeSpan(timeSpan.getParsedInputString() + acceptResult.getParsedInputString(),
                    copyDateWithEra(timeSpan.getStartDate(), false),
                    copyDateWithEra(timeSpan.getEndDate(), false));
        }

        return timeSpan;
    }

    /**
     * Expands a concrete date span according to range qualifiers such as "ab" or "um".
     */
    private DateBounds getModifiedStartEnd(final TimeSpan originalDate, final Range range) {
        final LocalDate originalStart = originalDate.getStartDate();
        final LocalDate originalEnd = originalDate.getEndDate();
        final int yearOfEra = getYearOfEra(originalStart);

        switch (range.getType()) {
            case FROM:
                return new DateBounds(originalStart, originalEnd.plusYears(getDateDelta(yearOfEra)));
            case UNTIL:
                return new DateBounds(originalStart.minusYears(getDateDelta(yearOfEra)), originalEnd);
            case BEFORE:
                return new DateBounds(originalStart.minusYears(getDateDelta(yearOfEra)), originalStart.minusDays(1));
            case AROUND:
                final int aroundDelta = getAroundDelta(yearOfEra);
                return new DateBounds(originalStart.minusYears(aroundDelta), originalEnd.plusYears(aroundDelta));
            case AFTER:
                return new DateBounds(originalStart, originalEnd.plusYears(25));
            case PRESUMABLY:
                return new DateBounds(originalStart, originalEnd);
            default:
                return new DateBounds(null, null);
        }
    }

    /**
     * Resolves a limitation like "Mitte" or "2. Viertel" against a century.
     */
    private DateBounds getStartAndEnd(final CenturyMillenniumLimitation limitation, final int century) {
        switch (limitation.getLimitation()) {
            case DECADE:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 10, 1, 1),
                        LocalDate.of((century - 1) * 100 + limitation.getNumber() * 10, 12, 31));
            case QUARTER:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 25, 1, 1),
                        LocalDate.of((century - 1) * 100 + limitation.getNumber() * 25, 12, 31));
            case THIRD:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 33, 1, 1),
                        LocalDate.of((century - 1) * 100 + limitation.getNumber() * 33, 12, 31));
            case HALF:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 50, 1, 1),
                        LocalDate.of((century - 1) * 100 + limitation.getNumber() * 50, 12, 31));
            case START:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 1, 1, 1),
                        LocalDate.of((century - 1) * 100 + 15, 12, 31));
            case MIDDLE:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 45, 1, 1),
                        LocalDate.of((century - 1) * 100 + 55, 12, 31));
            case END:
                return new DateBounds(
                        LocalDate.of((century - 1) * 100 + 85, 1, 1),
                        LocalDate.of((century - 1) * 100 + 100, 12, 31));
            default:
                return new DateBounds(null, null);
        }
    }

    /**
     * Parses qualifiers such as "Anfang", "2. Viertel" or "1. Dekade".
     */
    private CenturyMillenniumLimitation parseCenturyMillenniumLimitation(
            final InputStringReader input,
            final Position startingPosition) {
        final Position cursor = startingPosition.copy();
        final AcceptResult acceptResult = input.tryToAccept(cursor, PATTERN_CENTURY_MILLENNIUM_LIMITATION);
        if (!acceptResult.isAccepted()) {
            return null;
        }

        Integer number = null;
        CenturyMillenniumLimitation.LimitationType limitationType = null;
        if ("Dekade".equals(acceptResult.group(2))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.DECADE;
            number = Integer.valueOf(acceptResult.group(1));
        } else if ("Viertel".equals(acceptResult.group(6))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.QUARTER;
            number = Integer.valueOf(acceptResult.group(5));
        } else if ("Drittel".equals(acceptResult.group(4))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.THIRD;
            number = Integer.valueOf(acceptResult.group(3));
        } else if ("Hälfte".equals(acceptResult.group(8))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.HALF;
            number = Integer.valueOf(acceptResult.group(7));
        } else if ("Anfang".equals(acceptResult.group(0))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.START;
        } else if ("Mitte".equals(acceptResult.group(0))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.MIDDLE;
        } else if ("Ende".equals(acceptResult.group(0))) {
            limitationType = CenturyMillenniumLimitation.LimitationType.END;
        }

        startingPosition.update(cursor);
        return new CenturyMillenniumLimitation(acceptResult.getParsedInputString(), number, limitationType);
    }

    private int getAroundDelta(final int yearOfEra) {
        if (yearOfEra >= 1900) {
            return 1;
        }
        if (yearOfEra >= 1700) {
            return 2;
        }
        if (yearOfEra >= 1000) {
            return 5;
        }
        return 10;
    }

    private int getDateDelta(final int yearOfEra) {
        if (yearOfEra >= 0 && yearOfEra <= 999) {
            return 100;
        }
        if (yearOfEra >= 1000 && yearOfEra <= 1499) {
            return 50;
        }
        if (yearOfEra >= 1500 && yearOfEra <= 1799) {
            return 25;
        }
        if (yearOfEra >= 1800 && yearOfEra <= 1899) {
            return 10;
        }
        if (yearOfEra >= 1900 && yearOfEra <= 1945) {
            return 3;
        }
        if (yearOfEra >= 1946) {
            return 1;
        }
        return 0;
    }

    private int getYearOfEra(final LocalDate date) {
        return date.getYear() > 0 ? date.getYear() : (1 - date.getYear());
    }

    private LocalDate copyDateWithEra(final LocalDate source, final boolean isAnnoDomini) {
        final int yearOfEra = getYearOfEra(source);
        final int prolepticYear = isAnnoDomini ? yearOfEra : (1 - yearOfEra);
        return LocalDate.of(prolepticYear, source.getMonthValue(), source.getDayOfMonth());
    }

    private LocalDate createGregorianCalendar(final boolean isAnnoDomini, final int year, final int month,
            final int day) {
        final int prolepticYear = isAnnoDomini ? year : (1 - year);
        return LocalDate.of(prolepticYear, month + 1, day);
    }
}
