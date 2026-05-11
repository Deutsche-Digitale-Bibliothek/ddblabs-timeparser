# conf/timeparser — Konfigurationsdateien

Konfigurationsdateien für den Java-Timeparser. Diese CSV-Dateien werden beim Programmstart geladen; Anpassungen erfordern keinen Neucompile. API-Dokumentation, Pipeline-Details und Verwendungsbeispiele: [README.md](../../../../../README.md).

## CSV-Dateien und Pipeline-Schritte

| Pipeline-Schritt | CSV-Datei | Beschreibung |
|------------------|-----------|--------------|
| Step 1 | `normalizations.csv` (`type=normalization`) | Regex-Ersetzungen: Schreibvarianten → kanonische Form |
| Step 2 | `normalizations.csv` (`type=month` / `type=weekday`) | Monatsnamen → `01`–`12`, Wochentage → `GG` |
| Step 3–4 | `rules.csv` | Regel-Matching und Transformation |
| Step 6a | `facets.csv` | Zeitspanne → DDB-Zeitfacetten (`time_XXXXX`) |

Steps 5 und 6b verwenden keine CSV-Datei.

---

## normalizations.csv

### Format

CSV, 4 Spalten:

| Spalte    | Beschreibung |
|-----------|--------------|
| `from`    | **Regex** (bei `type=normalization`) oder **Literaltext** (bei `type=month`/`weekday`) |
| `to`      | Ersetzungstext; leer = Ausdruck entfernen |
| `type`    | `normalization` / `month` / `weekday` |
| `comment` | Erläuterung und Begründung |

Die Einträge werden **sequenziell von oben nach unten** angewendet.

### Typen

#### `normalization` (Step 1)

Regex-Ersetzungen. Die Regex kann `(?i)` für Groß-/Kleinschreibungsunempfindlichkeit und `\b` für Wortgrenzen enthalten.

Ziel: Eingabevarianten ohne semantischen Unterschied auf eine **kanonische Form** reduzieren, bevor die Regelsuche in rules.csv beginnt. Dadurch braucht man in rules.csv nur eine Regel statt vieler gleichbedeutender Varianten.

#### `month` (Step 2)

Literal-Ersetzungen: Monatsbezeichnungen → zweistellige Monatsnummern (`01`–`12`). Wird nach Step 1 angewendet; die Zahlen dienen als Monat-Token `MM` für den Masken-Matcher in rules.csv.

#### `weekday` (Step 2)

Literal-Ersetzungen: Wochentagsbezeichnungen → Token `GG`. Regeln können so auf Wochentage in Datumsangaben matchen, ohne die konkrete Tagsangabe zu kennen.

### Normalisierungsgruppen (Überblick)

Alle aktiven Step-1-Gruppen in der Reihenfolge, in der sie in normalizations.csv erscheinen:

| Gruppe | Beispiel-Eingaben | Kanonische Form | Zweck |
|--------|-------------------|-----------------|-------|
| BCE/CE-Schreibvarianten | `v.Chr.`, `v. Chr`, `vChr.` | `v. Chr.` / `n. Chr.` | Einheitliche Schreibweise für TimeSpanParser |
| Größenordnungen | `Mio`, `Million`, `Milliarden`, `Bill.` | `Mio.` / `Mrd.` / `Billionen` | Einheitliche Abkürzungen für TimeSpanParser |
| Unsicherheitsmarker | `etwa`, `wohl`, `vermutlich`, … | `circa` | Synonyme → gleiche Regelgruppe (→ Details unten) |
| Band-/Nummern-Annotationen | `Bd. 1`, `Nr. 3`, `Teil 2` | *(entfernt)* | Irrelevant für Datumserkennung |

---

## Design-Prinzip: Normalisierung statt Regeln

**Grundregel:**

- **Normalisierung** verwenden bei reinen Schreibvarianten **ohne semantischen Unterschied** — z. B. `Febr.` und `Feb.` bedeuten dasselbe, `wahrscheinlich` und `circa` stehen für denselben Näherungsbereich.
- **Separate Regel** in rules.csv verwenden, wenn Varianten **unterschiedliche Ausgaben** erzeugen sollen — z. B. `ca. 1850` (±1 Jahr) vs. `um 1850` (±5 Jahre).

**Konkret:** Statt 11 separate Regeleinträge für `wohl ####`, `etwa ####`, `vermutlich ####` usw. anzulegen, normalisieren 11 Zeilen in normalizations.csv alle diese Formen zu `circa`. Damit greifen die bestehenden 21 `circa`-Regeln automatisch für alle Synonyme — ohne eine einzige neue Zeile in rules.csv.

### Konsequenz für rules.csv: Eingabemaske muss kanonische Form verwenden

Wenn eine Normalisierung einen Begriff verändert, **muss die Eingabemaske der Regel die kanonische Form** (also den Wert nach der Normalisierung) enthalten. Das Eingabebeispiel (Spalte 2) darf weiterhin die Originalform zeigen; das tokenisierte Beispiel (Spalte 3) zeigt den Zustand nach der Normalisierung.

Beispiel: Die Regel für `s.a. [vermutlich 1850]`:
- Normalisierung Step 1: `vermutlich` → `circa`
- Eingabemaske (Spalte 0): `s.a. [circa ####]` ← kanonische Form
- Eingabebeispiel (Spalte 2): `s.a. [vermutlich 1850]` ← Originalform, unverändert
- Tokenisiertes Beispiel (Spalte 3): `s.a. [circa 1850]` ← Zustand nach Step 1+2

---

## Unsicherheitsmarker

Der Timeparser kennt zwei kanonische Marker-Token, die rules.csv als Ausgabe erzeugt: `ca.` und `um`. Beide werden von TimeSpanParser identisch behandelt: sie lösen denselben `AROUND`-Modus aus und rufen dieselbe `getAroundDelta()`-Methode auf.

### Näherungsbereich: jahreszahlabhängig, nicht markerabhängig

Die tatsächliche Breite der Zeitspanne hängt vom **Zeitalter der Jahreszahl** ab, nicht vom verwendeten Marker. Festgelegt ist dies in `TimeSpanParser.getAroundDelta()`:

| Jahreszahl (n. Chr.) | Näherungsbereich (±) | Beispiel ISO-Ausgabe |
|----------------------|----------------------|----------------------|
| ≥ 1900               | ±1 Jahr              | `ca. 1965` → `1964-01-01/1966-12-31` |
| 1700–1899            | ±2 Jahre             | `ca. 1865` → `1863-01-01/1867-12-31` |
| 1000–1699            | ±5 Jahre             | `ca. 1533` → `1528-01-01/1538-12-31` |
| < 1000               | ±10 Jahre            | `ca. 800` → `790-01-01/810-12-31` |

`um 1965` ergibt also ±1 Jahr — genau wie `ca. 1965`. `ca. 1550` ergibt ±5 Jahre — genau wie `um 1550`.

### Wie Eingabeformen auf Ausgabe-Token abgebildet werden

| Eingabe (nach Normalisierung) | rules.csv-Output | TimeSpanParser-Token |
|-------------------------------|------------------|----------------------|
| `[ca. ####]` | `ca. ####` (Klammern entfernt) | `ca.` → AROUND |
| `circa ####` | `ca. ####` (`circa` → `ca.`) | `ca.` → AROUND |
| `[circa ####]` | `ca. ####` | `ca.` → AROUND |
| `um ####` | `um ####` | `um` → AROUND |

**Die eckigen Klammern in der Eingabe** (z. B. `[ca. 1965]`, `[circa 1533]`) sind reine Eingabe-Konventionen im Quellmaterial; rules.csv entfernt sie. Sie haben keinen Einfluss auf den Näherungsbereich — der hängt allein vom Jahr ab.

### Cluster `circa` — normalisierte Synonyme

Die folgenden Schreibvarianten werden in Step 1 zu `circa` normalisiert. Rules.csv wandelt `circa` dann in `ca.` um, sodass alle Synonyme denselben TimeSpanParser-Pfad nehmen:

| Eingabe                                  | wird zu  | Begründung |
|------------------------------------------|----------|------------|
| `etwa`                                   | `circa`  | Synonym, gleiche semantische Breite (~±2 Jahre) |
| `wohl`                                   | `circa`  | Synonym |
| `vermutlich`, `vermutl.`                 | `circa`  | Synonym |
| `wahrscheinlich`, `wahrscheinl.`, `wahrsch.` | `circa` | Synonym |
| `ungefähr`, `ungef.`                     | `circa`  | Synonym |
| `vorwiegend`, `vorw.`                    | `circa`  | Synonym |

### Nicht normalisiert

- **`um`** bleibt eigenständig — eigene Regelgruppe in rules.csv, produziert `um X`-Output statt `ca. X`.
- **`ca.`** bleibt eigenständig — eigene Regelgruppe.

Der Näherungsbereich von `um` und `ca.` ist bei gleichem Jahreszahlbereich identisch (beide → AROUND → `getAroundDelta()`). Sie sind trotzdem separate Regelgruppen, weil die Eingabeformen in Quellmaterialien semantisch unterschiedlich benutzt werden und die Ausgabedarstellung sich unterscheidet (`ca. 1865` vs. `um 1865`).

### Neue Synonyme hinzufügen

Soll ein weiteres Synonym zu `circa` hinzugefügt werden (z. B. `möglicherweise`):

```csv
(?i)\bmöglicherweise\b,circa,normalization,normalize uncertainty marker 'möglicherweise' to canonical form 'circa'
```

Keine Änderung in rules.csv nötig.

---

## Monatsabkürzungen

Die `month`-Einträge decken folgende Varianten ab (Groß-/Kleinschreibung wird literal gematcht — Varianten sind explizit einzutragen):

**Vollformen:** `Januar`, `Februar`, `März`, `April`, `Mai`, `Juni`, `Juli`, `August`, `September`, `Oktober`, `November`, `Dezember`

**Abkürzungen mit Punkt:** `Jan.`, `Feb.`, `Febr.`, `Mrz.`, `Mär.`, `Mar.`, `Apr.`, `Jun.`, `Jul.`, `Aug.`, `Sept.`, `Sep.`, `Okt.`, `Oct.`, `Nov.`, `Dez.`, `Dec.`

**Abkürzungen ohne Punkt:** analoge Liste ohne `.`

Neue Abkürzungen können als `month`-Zeile hinzugefügt werden — kein Java-Code nötig.

---

## Band-/Nummern-Annotation entfernen

Angaben wie `Bd. 1`, `Nr. 3`, `Teil 2`, `Nummer 1` vor einer Jahreszahl werden durch die Normalisierung entfernt:

```
Bd. 1 1950    →  1950
Nr. 3 1960    →  1960
Teil 2 1975   →  1975
```

Das Muster greift für `Band`, `Bd.`, `Bd`, `Nummer`, `Nr.`, `Nr`, `Teil` gefolgt von einer 1–3-stelligen Zahl.

**Hinweis:** Regeln, deren Eingabemaske mit `Nr.` als strukturellem Bestandteil eines Formats beginnt (z. B. `Nr. #.#### -`), sind davon nicht betroffen, sofern die Zahl nach `Nr.` im Eingabebeispiel Teil eines Datumscodes ist (z. B. `Nr. 1.2011 -`, wo `1.2011` eine laufende-Nummer-plus-Jahr-Struktur darstellt).

---

## Wann Normalisierung, wann neue Regel?

| Szenario | Empfehlung |
|----------|------------|
| Neue Schreibvariante eines Monats (z. B. `Jän.` für Januar) | Neue `month`-Zeile in normalizations.csv |
| Neues Synonym für `circa` (z. B. `möglicherweise`) | Neue `normalization`-Zeile → `circa` |
| Neue Datumsstruktur ohne bestehende Entsprechung (z. B. `MM/YYYY`) | Neue Regel in rules.csv |
| Neuer Unsicherheitsmarker, der `ca.` oder `um` als Ausgabe-Token nutzt | Normalisierung zu `circa` (→ `ca.`-Regeln) oder direkte `um`-Regel |
| Bracket-Variante einer bestehenden Form ohne eigene Semantik | Normalisierung (z. B. `{ca. X}` → `[ca. X]`) |
| Bracket-Variante mit eigenständiger Darstellung in Quellmaterial | Separate Regel in rules.csv |

---

## rules.csv

### Format

CSV, 8 Spalten:

| Spalte | Name                  | Beschreibung |
|--------|-----------------------|--------------|
| 0      | `input mask`          | Muster aus Variablen (`####`, `##`, `MM`, `GG`) und Literaltext; muss **kanonische Form** nach Normalisierung verwenden |
| 1      | `input pattern`       | Wie `input mask`, aber mit benannten Variablen (`JJJJ`, `MM`, `TT`, …) |
| 2      | `input example`       | Konkretes Beispiel für die Eingabe; darf **Originalform** vor Normalisierung zeigen |
| 3      | `tokenized example`   | Beispiel nach Step 1+2 (nach Normalisierung und Tokenisierung); **leer** wenn identisch mit Spalte 2 |
| 4      | `output mask`         | Ausgabemuster (Variablen + Literaltext) |
| 5      | `output pattern`      | Ausgabemuster mit benannten Variablen |
| 6      | `output example`      | Konkretes Ausgabebeispiel |
| 7      | `output example ISO`  | ISO-8601-Ausgabe (`YYYY-MM-DD/YYYY-MM-DD`); leer wenn keine direkte ISO-Entsprechung (z. B. Kommalisten) |

### Masken- und Mustersyntax

- `#` = eine beliebige Ziffer; **gleicher Buchstabe = gleiche Zahl**: z. B. `##` steht für zwei Ziffern, die zusammen eine Zahl bilden
- `MM` = Monat-Token (zweistellige Nummer `01`–`12`, aus Step 2)
- `GG` = Wochentag-Token (aus Step 2)
- Alle anderen Zeichen = Literal (muss exakt übereinstimmen)

Variablennamen (Großbuchstaben) im Pattern sind frei wählbar. Im **Eingabemuster** darf ein Buchstabe nicht mehrfach verwendet werden (jede Variable ist eindeutig und repräsentiert eine eigenständige Zahl). Im **Ausgabemuster** sind Wiederholungen erlaubt — z. B. erscheint `JJJJ` zweimal, wenn Anfangs- und Endjahr identisch sein sollen.

### Spalte 3: `tokenized example`

Zeigt, wie die Eingabe nach Step 1 (Normalisierung) und Step 2 (Tokenisierung) aussieht. Dient als maschinenlesbare Prüfgrundlage: Der Test `allRuleTokenizedExamplesMatchStep2` wendet die Pipeline bis Step 2 an und vergleicht mit diesem Feld.

Das Feld ist **leer**, wenn die normalisierte/tokenisierte Form identisch mit Spalte 2 (`input example`) ist.

---

## facets.csv

### Format

CSV, 7 Spalten: `ID`, `notation`, `earliestDate`, `latestDate`, `prefLabel@de`, `prefLabel@en`, `sortOrder`

Enthält das **DDB-Zeitvokabular** (110 Einträge) von geologischen Zeitskalen (Präkambrium, ca. −4,6 Mrd. Jahre) bis zur jüngsten Gegenwart. Step 6 der Pipeline ordnet einem geparsten Zeitraum die zutreffenden Zeitfacetten zu (`time_XXXXX`-IDs für das DDB-Suchportal).

`earliestDate`/`latestDate` sind in ganzen Jahren angegeben (negativ = v. Chr.).
