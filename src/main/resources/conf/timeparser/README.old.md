# Time Parsing

The time parser converts a textual representation of a point or period in time into:

- a list of time periods, called facets, in which it lies, and
- two values, that can be used to sort documents by start or end date.

The conversion process consists of four steps:

1. Rule selection
2. Input normalization
3. Time span calculation
4. Final output string generation

## Limitations

Due to technical limitations, the time parser is only able to represent time periods between the year 292.269.053 (BC) and the year 292.278.994 (AD).

## Input Normalization

Input normalization uses a set of transformation rules to transform heterogeneous input formats into a normalized format.

A transformation rule consists of:

- an input specification, and
- an output specification.

When an input string is processed, the appropriate rule is sought first.
Once a rule is found, the input string is interpreted according to the input specification. The values from the variables defined in the input specification will be stored.
Then, the values from the input are put into the output specification. The result is the normalized input string, from which a time span will be calculated next.

Example:

```text
Input specification: ##.##.#### TT.MM.JJJJ
Input string: 05.04.2014
Output specification: ######## JJJJ-MM-TT
Output string: 2014-04-05
```

## Input and Output Specification Syntax

An input or output specification (short: specification) describes the structure of a string.
A specification consists of:

- a pattern, and
- a mask.

The mask specifies the type of each element in the specification, while the pattern assigns names to the elements. Together, they describe the structure of the string.

There are four types of elements in a specification. Each type may be used as often as desired, in any order.

### Generic variables

Generic variables hold values extracted from the input string, which are then put into the output string.

- Pattern: A letter (usually upper-case), occurring 1 or more times. The letter forms the name of the variable.
- Mask: The symbol "#" (without quotes), occurring the same number of times as the pattern's letter.

Example:

```text
Input specification: pattern: This year is 'YY. | mask: This year is '##.
Input string: This year is '14.
Output specification: pattern: 20YY | mask: 20##
Output string: 2014
```

### Month replacement variables

In input specifications, month replacement variables indicate that a textual month (e.g. "April") is required in the input string. When the textual month is parsed, the variable's value will be a pre-defined alternative value. The list of possible textual months and the corresponding variable values are defined in code, in TimeParser.java.
In output specifications, month replacement variables function like generic variables.

- Pattern: A letter, occurring exactly twice. The letter forms the name of the variable.
- Mask: The exact string "MM".

Example:

```text
Input specification: pattern: This is PP. | mask: This is MM.
Input string: This month is April.
Output specification: pattern: 2014-PP | mask: 2014-MM
Output string: 2014-04
```

### Weekday replacement variables

Like month replacement variables, except for weekdays (e.g. "Mittwoch") and with "GG" instead of "MM".

Example:

```text
Input specification: pattern: Heute ist PP, TT.MM.JJJJ. | mask: Heute ist, GG, TT.MM.JJJJ.
Input string: Heute ist Mittwoch, 01.01.2014.
Output specification: pattern: JJJJ-MM-TT | mask: ########
Output string: 2014-01-01
```

### Text

Indicates fixed text that is required in the input string (in input specifications) or that is put into output strings (in output specifications).

- Pattern: Any string that does not contain, "#", "MM", "GG" or any of the month or weekday replacement strings.
- Mask: Exactly the same string as the pattern.

Example:

```text
Input specification: pattern: Hello, world! | mask: Hello, world!
Input string: Hello, world!
Output specification: pattern: Auf wiedersehen, Welt! | mask: Auf wiedersehen, Welt!
Output string: Auf wiedersehen, Welt!
```

In input specifications, variable names may occur only once. For example, the following pattern is invalid, as it uses the name "J" three times: JJJJ, JJJ, JJ
In output specifications, variable names may be used as often as desired. The variable's value will be repeated each time. Filling out a value starts from the left of the string.

Example:

```text
Input specification: pattern: JJJJ/KK | mask: ####/##
Input string: 1983/84
Output specification: pattern: JJJJ/JJKK | mask: ####/####
Output string: 1983/1984
```

## Rule File Syntax

The rule file contains transformation rules and an example transformation for each rule.

The file must be encoded with the Windows-1252 character encoding.
The first line in the file is ignored and can be used as a header line.
Each line must consist of six columns, separated by a tab character (`\t`). The columns must correspond to the following, in the specified order:

1. Input specification mask
2. Input specification pattern
3. Example input string
4. Output specification mask
5. Output specification pattern
6. Required output string for the example input string

Tip: In Western Europe, exporting the rules from the Excel worksheet "Time_patterns_list_rules" as a tab-delimited file will result in a valid rule file.

Example:

```text
pattern	transform_rule	example	pattern_result	transform_result	example_result	result
- ####/####	- JJJJ/ZZZZ	- 2002/2003	bis ####	bis ZZZZ	bis 2003	bis 2003
#	J	5	000#	000J	0005	0005
##	JJ	68	00##	00JJ	0068	0068
###	JJJ	753	0###	0JJJ	0753	0753
### MM	JJJ MM	753 Juni	0###-##	0JJJ-MM	0753-06	0753-06
```

## Time Span Calculation

After an input string has been normalized, the time span calculation interprets the normalized string and determines what time period it corresponds to. The normalized strings must adhere to the following syntax:

```text
complex
 := simple ws "oder" ws complex
 |  simple "/" complex
 |  simple "," complex
 |  simple

simple
 := rangeModifier ws date
 |  date

rangeModifier
 := "ab"
 |  "seit"
 |  "bis"
 |  "vor"
 |  "nach"
 |  "um"
 |  "ca."
 |  "vermutlich"

date
 := century ws "vor Christus"
 |  century ws "nach Christus"
 |  century
 |  yearMonthDay ws "vor Christus"
 |  yearMonthDay ws "nach Christus"
 |  "-" yearMonthDay
 |  yearMonthDay

century
 := centuryMillenniumLimitation ws digit{+} "." ws "Jahrhundert"
 |  digit{+} "." ws "Jahrhundert"

centuryMillenniumLimitation
 := oneToTen   "." ws "Dekade"
 |  oneToFour  "." ws "Viertel"
 |  oneToThree "." ws "Drittel"
 |  oneToTwo   "." ws "Hälfte"
 |  "Anfang"
 |  "Mitte"
 |  "Ende"

yearMonthDay
 := digit{+} "-" digit{2} "-" digit{2}
 |  digit{+} "-" digit{2}
 |  digit{+}

oneToTwo   := "1" | "2"
oneToThree := "1" | "2" | "3"
oneToFour  := "1" | "2" | "3" | "4"
oneToTen   := "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" | "10"

digit := "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"

ws := " "

rule{?} means: rule occurring at most once.
rule{+} means: rule occurring at least once.
rule{2} means: rule occurring exactly twice.
```

The syntax was based on the Excel worksheet "Formale_Regeln". In general, the normalized strings are interpreted as specified in that worksheet.

A few differences between the worksheet and the implementation are (state of 2014-01-15):

- Half, third and quarter centuries and decades start at a year ending with "1", not "0".
- The operator "," (meaning "or") functions like the operator "/" (meaning "between"). That is, the string "1912,1914" results in one period, starting in 1912 and ending 1914.

The worksheet defines several features that have not been implemented. Not implemented are (state of 2014-01-15):

- The notation "y-45000"
- The notations DD.MM.YYYY, DD-MM-YYYY and YYYY/MM/DD.
- The notation "?", for "Datierung ungesichert, Fragezeichen ohne Leerzeichen direkt nach der Datierung" (can be done with "um" or "ca.")
- The notation "zwischen #### und ####", for "zwischen zwei Jahren" (can be done with "/")
- "nicht datiert"
- "undatiert"
- "ohne Datum"
- "Jahrtausend"

## Rule Selection

Before an input string can be normalized, the appropriate transformation rule needs to be selected.

The algorithm to select transformation rules for an input string is the following:

1. Replace all textual months in the input string by "MM".
2. Replace all textual weekdays in the input string by "GG".
3. For each possible rule:
   - Skip the rule if any of the following conditions do not hold:
     - The length of the rule's input mask and the input string are equal.
     - Every character in the input mask that is "#" has a digit at the corresponding position in the input string.
     - Every character in the input mask that is not "#" has exactly the same character in the input string.
   - Add the rule to the list of found rules.
4. If more than one rule has been found:
   - Find the rule that has the least number of "#" characters in its input mask.
   - Remove all rules whose input masks contain more "#" characters than that rule does.
5. If more than one rule remains:
   - Remove duplicate rules.

This algorithm will function properly for input strings like "2001-00-00", "04.05.00" and "2001-04-04T15:45:00+01:00", amongst others.

If no rules match an input string, then the input normalization will be skipped and the time parser will continue with the time span calculation.
If, for some reason, more than one rule matches the input string, this is an error and the time parser will output an empty string.

## Final Output String Generation

Once a time period has been calculated for an input string, the final output string will be generated.

The format of the output string is as follows:

```text
time_1|time_2|...|time_n start|end
```

This is a list of facets separated by "|", followed by a space, followed by a sorting start value, "|" and a sorting end value. The sorting start and end values can be used to sort documents by their start or end date.

The facet file contains a list of possible facets.

The file must be encoded with the Windows-1252 character encoding.
The first line in the file is ignored and can be used as a header line.
Each line must consist of seven columns, separated by a tab character (`\t`). The columns must correspond to the following, in the specified order:

1. ID
2. Notation
3. Earliest date in years
4. Latest date in years
5. German description
6. English description
7. A sorting value to sort facets

Note: Facets with an earliest date or a latest date of "-####" or "####" will be ignored.

Example:

```text
ID	notation	earliestDate	latestDate	prefLabel@de	prefLabel@en	sortOrder
dat00106	time_15000	-210000000	-140000000	Jura	Jurassic	15000
dat00102	time_33000	-7000	-6001	7. Jahrtausend vor Christus	7th millenium BC	33000
dat00101	time_34000	-6000	-5001	6. Jahrtausend vor Christus	6th millenium BC	34000
```
