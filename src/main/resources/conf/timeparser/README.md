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

Schreibvarianten **ohne semantischen Unterschied** → Normalisierungszeile in normalizations.csv. Varianten **mit unterschiedlichem Ausgabeverhalten** → separate Regel in rules.csv.

Praxis: 11 Synonyme für `circa` (etwa, wohl, vermutlich, …) werden durch 11 Normalisierungszeilen abgedeckt — die 21 bestehenden `circa`-Regeln greifen damit automatisch für alle Synonyme, ohne eine einzige neue Zeile in rules.csv.

**Eingabemaske muss die kanonische Post-Normalisierungs-Form enthalten.** Wenn Step 1 `vermutlich` → `circa` wandelt, lautet die Eingabemaske `s.a. [circa ####]`. Das Eingabebeispiel (Spalte 2) darf die Originalform `s.a. [vermutlich 1850]` zeigen; Spalte 3 (tokenized) zeigt `s.a. [circa 1850]`.

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

| Eingabe (nach Step 1+2) | rules.csv-Output | TimeSpanParser-Token | Effekt |
|-------------------------|------------------|----------------------|--------|
| `[ca. ####]` | `ca. ####` (Klammern entfernt) | `ca.` → AROUND | Zeitraum ausgedehnt (±N Jahre) |
| `circa ####` | `ca. ####` (`circa` → `ca.`) | `ca.` → AROUND | Zeitraum ausgedehnt |
| `[circa ####]` | `ca. ####` | `ca.` → AROUND | Zeitraum ausgedehnt |
| `um ####` | `um ####` | `um` → AROUND | Zeitraum ausgedehnt |
| `[####?]`, `[i.e. ####?]`, `[um ####?]` | `vermutlich ####` | `vermutlich` → PRESUMABLY | **kein Ausdehnen** — exakte Jahreszahl |

Ausdehnung: `ca.` und `um` verwenden dieselbe `getAroundDelta()`-Methode (±1/2/5/10 Jahre je nach Zeitalter — siehe Tabelle oben). Die eckigen Klammern in der Eingabe sind Quellmaterial-Konventionen; rules.csv entfernt sie.

**Hinweis: zwei verschiedene Rollen von `vermutlich`**

`vermutlich` erscheint im System in zwei unabhängigen Kontexten:

1. **Als Wort im Eingabe-String** (Step 1, normalizations.csv): `vermutlich 1850` → normalisiert zu `circa 1850` → Regel `circa ####` greift → Output `ca. 1850` → AROUND → Zeitraum wird ausgedehnt.

2. **Als Output-Token in rules.csv**: Eingaben wie `[1868?]` oder `[um 1470?]` enthalten das Wort `vermutlich` **nicht** — es entsteht erst als Regelausgabe (`vermutlich 1868`). TimeSpanParser erkennt `vermutlich` als PRESUMABLY-Marker → **keine Ausdehnung** → exakt ein Jahreszeitraum (`1868-01-01/1868-12-31`). Das Fragezeichen in Klammern drückt quellkritische Unsicherheit ohne implizierte Zeitbreite aus.

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

### Nicht normalisiert: `um` und `ca.`

`um` und `ca.` bleiben eigenständige Regelgruppen — unterschiedliche Output-Token, unterschiedliche Ausgabedarstellung (`um 1865` vs. `ca. 1865`). AROUND-Näherungsbereich ist epochenabhängig und für beide identisch (beide → `getAroundDelta()`).

Neues Synonym zu `circa` hinzufügen — nur eine normalizations.csv-Zeile, keine rules.csv-Änderung nötig:

```csv
(?i)\bmöglicherweise\b,circa,normalization,normalize uncertainty marker 'möglicherweise' to canonical form 'circa'
```

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

**Ausnahme:** Die Regex greift nur auf isolierte Nummern (1–3 Stellen, Wortgrenze). Regeln mit `Nr.` als Format-Bestandteil (z. B. `Nr. #.#### -`) sind nicht betroffen.

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

**Masken- und Mustersyntax:** → [„Mask and pattern syntax" in der Root-README](../../../../../README.md#mask-and-pattern-syntax).

**Spalte 3 (`tokenized example`):** Zustand der Eingabe nach Step 1+2; leer wenn identisch mit Spalte 2. Wird durch den Test `allRuleTokenizedExamplesMatchStep2` geprüft.

---

## Noch nicht unterstützt

Folgende Datumsformen sind in `TimeSpanParser` nicht implementiert:

| Form | Beschreibung | Workaround |
|------|--------------|------------|
| `Jahrtausend` | z. B. „2. Jahrtausend" | — |
| `zwischen X und Y` | Explizite `zwischen`-Form | Output `X/Y` in rules.csv |
| `nicht datiert`, `undatiert`, `ohne Datum` | Explizit undatierte Objekte | — |
| `DD.MM.YYYY`, `YYYY/MM/DD` | Weitere Datumsformat-Varianten | rules.csv-Regel mit Output `YYYY-MM-DD` |

Gegenüber der ursprünglichen Spezifikation **inzwischen über rules.csv implementiert**:

- `?`-Notation: z. B. `1965?` → `ca. 1965`, `[1868?]` → `vermutlich 1868`, `1923 ?` → `ca. 1923`
- Monat- und Wochentagserkennung: früher im Java-Code hardcodiert, jetzt konfigurierbar über normalizations.csv

---

## facets.csv

### Format

CSV, 7 Spalten: `ID`, `notation`, `earliestDate`, `latestDate`, `prefLabel@de`, `prefLabel@en`, `sortOrder`

Enthält das **DDB-Zeitvokabular** (110 Einträge) von geologischen Zeitskalen (Präkambrium, ca. −4,6 Mrd. Jahre) bis zur jüngsten Gegenwart. Step 6 der Pipeline ordnet einem geparsten Zeitraum die zutreffenden Zeitfacetten zu (`time_XXXXX`-IDs für das DDB-Suchportal).

`earliestDate`/`latestDate` sind in ganzen Jahren angegeben (negativ = v. Chr.).
