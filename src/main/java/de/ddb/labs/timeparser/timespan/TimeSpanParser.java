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

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interprets a normalized string and determines what time period it corresponds to. The implemented syntax is
 * documented elsewhere. Time periods are represented as {@link TimeSpan}s.
 */
public class TimeSpanParser
{

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

    public TimeSpan parse(String inputString) throws IllegalStateException {
        InputStringReader input = new InputStringReader(inputString);
        Position position = new Position();
        TimeSpan timeSpan = parseComplex(input, position, true);

        if ( timeSpan == null ) {
            throw new IllegalStateException("The input string \"" + inputString + "\" could not be parsed");
        } else if ( timeSpan.getParsedInputString().length() != inputString.length() ) {
            throw new IllegalStateException("The input string \"" + inputString + "\" could not be parsed entirely");
        } else {
            return timeSpan;
        }
    }

    private TimeSpan parseComplex(InputStringReader input, Position startingPosition, boolean allowEraSuffix) {
        Position p = startingPosition.copy();
        TimeSpan timeSpan = parseSimple(input, p);

        if ( timeSpan == null ) {
            return null;
        }

        Position p2 = p.copy();
        Operator operator = parseOperator(input, p2);
        if ( operator != null ) {
            TimeSpan nextTimeSpan = parseComplex(input, p2, false);
            if ( nextTimeSpan != null ) {
                if ( operator.getType() == Operator.OperatorType.OR ) {
                    throw new IllegalStateException("Disjoint time spans are not supported: \""
                        + timeSpan.getParsedInputString()
                        + operator.getParsedInputString()
                        + nextTimeSpan.getParsedInputString()
                        + "\"");
                }

                timeSpan = new TimeSpan(timeSpan.getParsedInputString()
                    + operator.getParsedInputString()
                    + nextTimeSpan.getParsedInputString(), timeSpan.getStart(), nextTimeSpan.getEnd());
                p = p2;
            }
        }

        if ( allowEraSuffix ) {
            timeSpan = applyEraSuffix(input, p, timeSpan);
        }
        startingPosition.update(p);
        return timeSpan;
    }

    private TimeSpan applyEraSuffix(InputStringReader input, Position position, TimeSpan timeSpan) {
        AcceptResult acceptResult = input.tryToAccept(position, " nach Christus");
        if ( acceptResult.isAccepted() ) {
            return new TimeSpan(timeSpan.getParsedInputString() + acceptResult.getParsedInputString(), timeSpan.getStart(), timeSpan.getEnd());
        }

        acceptResult = input.tryToAccept(position, " vor Christus");
        if ( acceptResult.isAccepted() ) {
            return new TimeSpan(timeSpan.getParsedInputString() + acceptResult.getParsedInputString(),
                copyCalendarWithEra(timeSpan.getStart(), GregorianCalendar.BC),
                copyCalendarWithEra(timeSpan.getEnd(), GregorianCalendar.BC));
        }

        return timeSpan;
    }

    private Calendar copyCalendarWithEra(Calendar source, int era) {
        GregorianCalendar calendar = new GregorianCalendar(source.get(Calendar.YEAR), source.get(Calendar.MONTH), source.get(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.ERA, era);
        return calendar;
    }

    private Operator parseOperator(InputStringReader input, Position startingPosition) {
        Position p;
        AcceptResult a;

        p = startingPosition.copy();
        a = input.tryToAccept(p, ',');
        if ( a.isAccepted() ) {
            startingPosition.update(p);
            return new Operator(a.getParsedInputString(), Operator.OperatorType.OR);
        }

        p = startingPosition.copy();
        a = input.tryToAccept(p, '/');
        if ( a.isAccepted() ) {
            startingPosition.update(p);
            return new Operator(a.getParsedInputString(), Operator.OperatorType.BETWEEN);
        }

        p = startingPosition.copy();
        a = input.tryToAccept(p, " oder ");
        if ( a.isAccepted() ) {
            startingPosition.update(p);
            return new Operator(a.getParsedInputString(), Operator.OperatorType.OR);
        }

        return null;
    }

    private TimeSpan parseSimple(InputStringReader input, Position startingPosition) {
        Position p = startingPosition.copy();
        Range range = parseRange(input, p);
        if ( range != null ) {
            AcceptResult a = input.tryToAccept(p, ' ');
            if ( a.isAccepted() ) {
                TimeSpan originalDate = parseDate(input, p);
                if ( originalDate != null ) {
                    List<Calendar> modifiedStartEnd = getModifiedStartEnd(originalDate, range);
                    startingPosition.update(p);
                    return new TimeSpan(range.getParsedInputString()
                        + a.getParsedInputString()
                        + originalDate.getParsedInputString(), modifiedStartEnd.get(0), modifiedStartEnd.get(1));
                }
            }
        }

        return parseDate(input, startingPosition);
    }
    
    private List<Calendar> getModifiedStartEnd(TimeSpan originalDate, Range range) {
        Calendar modifiedStart = null;
        Calendar modifiedEnd = null;

        if ( range.getType() == Range.RangeType.FROM ) { // ab, seit                  
            int date = originalDate.getStart().get(Calendar.YEAR);
            int delta = getDateDelta(date);

            modifiedStart =
                new GregorianCalendar(
                    originalDate.getStart().get(Calendar.YEAR),
                    originalDate.getStart().get(Calendar.MONTH),
                    originalDate.getStart().get(Calendar.DAY_OF_MONTH));

            modifiedEnd =
                new GregorianCalendar(
                    originalDate.getEnd().get(Calendar.YEAR) + delta,
                    originalDate.getEnd().get(Calendar.MONTH),
                    originalDate.getEnd().get(Calendar.DAY_OF_MONTH));

        } else if ( range.getType() == Range.RangeType.UNTIL ) { // bis
            int date = originalDate.getStart().get(Calendar.YEAR);
            int delta = getDateDelta(date);

            modifiedStart =
                new GregorianCalendar(
                    originalDate.getStart().get(Calendar.YEAR) - delta,
                    originalDate.getStart().get(Calendar.MONTH),
                    originalDate.getStart().get(Calendar.DAY_OF_MONTH));
            
            modifiedEnd =
                new GregorianCalendar(
                    originalDate.getEnd().get(Calendar.YEAR),
                    originalDate.getEnd().get(Calendar.MONTH),
                    originalDate.getEnd().get(Calendar.DAY_OF_MONTH));

        } else if ( range.getType() == Range.RangeType.BEFORE ) { // vor
            int date = originalDate.getStart().get(Calendar.YEAR);
            int delta = getDateDelta(date);

            modifiedStart =
                new GregorianCalendar(
                    originalDate.getStart().get(Calendar.YEAR) - delta,
                    originalDate.getStart().get(Calendar.MONTH),
                    originalDate.getStart().get(Calendar.DAY_OF_MONTH));
            
            modifiedEnd =
                new GregorianCalendar(
                    originalDate.getStart().get(Calendar.YEAR),
                    originalDate.getStart().get(Calendar.MONTH),
                    originalDate.getStart().get(Calendar.DAY_OF_MONTH) - 1);

        } else if ( range.getType() == Range.RangeType.AROUND ) {
            int delta = 0;
            if ( originalDate.getStart().get(Calendar.YEAR) >= 1900 ) {
                delta = 1;
            } else if ( originalDate.getStart().get(Calendar.YEAR) >= 1700 ) {
                delta = 2;
            } else if ( originalDate.getStart().get(Calendar.YEAR) >= 1000 ) {
                delta = 5;
            } else {
                delta = 10;
            }

            modifiedStart =
                new GregorianCalendar(
                    originalDate.getStart().get(Calendar.YEAR) - delta,
                    originalDate.getStart().get(Calendar.MONTH),
                    originalDate.getStart().get(Calendar.DAY_OF_MONTH));
            modifiedEnd =
                new GregorianCalendar(
                    originalDate.getEnd().get(Calendar.YEAR) + delta,
                    originalDate.getEnd().get(Calendar.MONTH),
                    originalDate.getEnd().get(Calendar.DAY_OF_MONTH));
        } else if ( range.getType() == Range.RangeType.AFTER ) {
            modifiedStart = originalDate.getStart();
            modifiedEnd =
                new GregorianCalendar(originalDate.getEnd().get(Calendar.YEAR) + 25, originalDate
                    .getEnd()
                    .get(Calendar.MONTH), originalDate.getEnd().get(Calendar.DAY_OF_MONTH));
        } else if ( range.getType() == Range.RangeType.PRESUMABLY ) {
            modifiedStart = originalDate.getStart();
            modifiedEnd = originalDate.getEnd();
        }
        return Arrays.asList(modifiedStart, modifiedEnd);
    }

    private Range parseRange(InputStringReader input, Position startingPosition) {
        Position p = startingPosition.copy();
        AcceptResult a = input.tryToAccept(p, PATTERN_RANGE);
        if ( a.isAccepted() ) {
            startingPosition.update(p);
            if ( null != a.group(1) ) switch (a.group(1)) {
                case "ab":
                    return new Range(a.getParsedInputString(), Range.RangeType.FROM);
                case "seit":
                    return new Range(a.getParsedInputString(), Range.RangeType.FROM);
                case "bis":
                    return new Range(a.getParsedInputString(), Range.RangeType.UNTIL);
                case "vor":
                    return new Range(a.getParsedInputString(), Range.RangeType.BEFORE);
                case "um":
                    return new Range(a.getParsedInputString(), Range.RangeType.AROUND);
                case "ca.":
                    return new Range(a.getParsedInputString(), Range.RangeType.AROUND);
                case "nach":
                    return new Range(a.getParsedInputString(), Range.RangeType.AFTER);
                case "vermutlich":
                    return new Range(a.getParsedInputString(), Range.RangeType.PRESUMABLY);
                default:
                    break;
            }
        }
        return null;
    }

    private TimeSpan parseDate(InputStringReader input, Position startingPosition) {
        TimeSpan timeSpan = null;

        timeSpan = parseCenturyOrMillennium(input, startingPosition);
        if ( timeSpan == null ) {
            timeSpan = parseYMD(input, startingPosition);
        }

        return timeSpan;

    }

    private TimeSpan parseCenturyOrMillennium(InputStringReader input, Position startingPosition) {
        Calendar start;
        Calendar end;

        Position p;
        AcceptResult a;

        p = startingPosition.copy();
        CenturyMillenniumLimitation limitation = parseCenturyMillenniumLimitation(input, p);
        if ( limitation != null ) {
            a = input.tryToAccept(p, ' ');
            if ( a.isAccepted() ) {
                AcceptResult a2 = input.tryToAccept(p, PATTERN_CENTURY);
                if ( a2.isAccepted() ) {
                    List<Calendar> startEnd = getStartAndEnd(limitation, Integer.parseInt(a2.group(1)));
                    start = startEnd.get(0);
                    end = startEnd.get(1);

                    startingPosition.update(p);
                    return new TimeSpan(limitation.getParsedInputString()
                        + a.getParsedInputString()
                        + a2.getParsedInputString(), start, end);
                }
            }
        }

        p = startingPosition.copy();
        a = input.tryToAccept(p, PATTERN_CENTURY);
        if ( a.isAccepted() ) {
            int century = Integer.parseInt(a.group(1));

            start = new GregorianCalendar((century - 1) * 100 + 1, 0, 1);
            end = new GregorianCalendar(century * 100, 11, 31);

            startingPosition.update(p);
            return new TimeSpan(a.getParsedInputString(), start, end);
        }
        return null;
    }
    
    private List<Calendar> getStartAndEnd(CenturyMillenniumLimitation limitation, int century) {
        Calendar start = null;
        Calendar end = null;
        if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.DECADE ) {
            start =
                new GregorianCalendar(
                    (century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 10,
                    0,
                    1);
            end =
                new GregorianCalendar((century - 1) * 100 + limitation.getNumber() * 10, 11, 31);
        } else if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.QUARTER ) {
            start =
                new GregorianCalendar(
                    (century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 25,
                    0,
                    1);
            end =
                new GregorianCalendar((century - 1) * 100 + limitation.getNumber() * 25, 11, 31);
        } else if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.THIRD ) {
            start =
                new GregorianCalendar(
                    (century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 33,
                    0,
                    1);
            end =
                new GregorianCalendar((century - 1) * 100 + limitation.getNumber() * 33, 11, 31);
        } else if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.HALF ) {
            start =
                new GregorianCalendar(
                    (century - 1) * 100 + 1 + (limitation.getNumber() - 1) * 50,
                    0,
                    1);
            end =
                new GregorianCalendar((century - 1) * 100 + limitation.getNumber() * 50, 11, 31);
        } else if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.START ) {
            start = new GregorianCalendar((century - 1) * 100 + 1, 0, 1);
            end = new GregorianCalendar((century - 1) * 100 + 15, 11, 31);
        } else if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.MIDDLE ) {
            start = new GregorianCalendar((century - 1) * 100 + 45, 0, 1);
            end = new GregorianCalendar((century - 1) * 100 + 55, 11, 31);
        } else if ( limitation.getLimitation() == CenturyMillenniumLimitation.LimitationType.END ) {
            start = new GregorianCalendar((century - 1) * 100 + 85, 0, 1);
            end = new GregorianCalendar((century - 1) * 100 + 100, 11, 31);
        } else {
            start = null;
            end = null;
        }
        return Arrays.asList(start, end);
    }

    private TimeSpan parseYMD(InputStringReader input, Position startingPosition) {
        Calendar start;
        Calendar end;

        Position p;
        AcceptResult a;

        p = startingPosition.copy();
        a = input.tryToAccept(p, PATTERN_YMD);
        if ( a.isAccepted() ) {
            boolean isAD = a.group(1) == null || "".equals(a.group(1));

            start =
                createGregorianCalendar(
                    isAD,
                    Integer.parseInt(a.group(2)),
                    Integer.parseInt(a.group(3)) - 1,
                    Integer.parseInt(a.group(4)));
            end = start;

            startingPosition.update(p);
            return new TimeSpan(a.getParsedInputString(), start, end);
        }

        p = startingPosition.copy();
        a = input.tryToAccept(p, PATTERN_YM);
        if ( a.isAccepted() ) {
            boolean isAD = a.group(1) == null || "".equals(a.group(1));
            int year = Integer.parseInt(a.group(2));
            int month = Integer.parseInt(a.group(3));

            start = createGregorianCalendar(isAD, year, month - 1, 1);
            end = createGregorianCalendar(isAD, year, month - 1, 1);
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));

            startingPosition.update(p);
            return new TimeSpan(a.getParsedInputString(), start, end);
        }

        p = startingPosition.copy();
        a = input.tryToAccept(p, PATTERN_Y);
        if ( a.isAccepted() ) {
            boolean isAD = a.group(1) == null || "".equals(a.group(1));
            int year = Integer.parseInt(a.group(2));
            start = createGregorianCalendar(isAD, year, 0, 1);
            end = createGregorianCalendar(isAD, year, 11, 31);

            startingPosition.update(p);
            return new TimeSpan(a.group(0), start, end);
        }

        return null;
    }
    
    private int getDateDelta(int date) {
        int delta = 0;
        if ( date >= 0 && date <= 999 ) {
            delta = 100;
        } else if ( date >= 1000 && date <= 1499 ) {
            delta = 50;
        } else if ( date >= 1500 && date <= 1799 ) {
            delta = 25;
        } else if ( date >= 1800 && date <= 1899 ) {
            delta = 10;
        } else if ( date >= 1900 && date <= 1945 ) {
            delta = 3;
        } else if ( date >= 1946 ) {
            delta = 1;
        }
        return delta;
    }

    private CenturyMillenniumLimitation parseCenturyMillenniumLimitation(
        InputStringReader input,
        Position startingPosition) {
        Position p = startingPosition.copy();
        AcceptResult a = input.tryToAccept(p, PATTERN_CENTURY_MILLENNIUM_LIMITATION);
        if ( a.isAccepted() ) {
            Integer number = null;
            CenturyMillenniumLimitation.LimitationType limitationType = null;
            if ( "Dekade".equals(a.group(2)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.DECADE;
                number = Integer.valueOf(a.group(1));
            } else if ( "Viertel".equals(a.group(6)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.QUARTER;
                number = Integer.valueOf(a.group(5));
            } else if ( "Drittel".equals(a.group(4)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.THIRD;
                number = Integer.valueOf(a.group(3));
            } else if ( "Hälfte".equals(a.group(8)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.HALF;
                number = Integer.valueOf(a.group(7));
            } else if ( "Anfang".equals(a.group(0)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.START;
            } else if ( "Mitte".equals(a.group(0)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.MIDDLE;
            } else if ( "Ende".equals(a.group(0)) ) {
                limitationType = CenturyMillenniumLimitation.LimitationType.END;
            }

            startingPosition.update(p);
            return new CenturyMillenniumLimitation(a.getParsedInputString(), number, limitationType);
        }
        return null;
    }

    private GregorianCalendar createGregorianCalendar(boolean isAnnoDomini, int year, int month, int day) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day);
        if ( !isAnnoDomini ) {
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }
        return calendar;
    }
}
