# trade-calc – Spezifikation

Gewinnrechner für hergestellte Gegenstände in **Albion Online** 

Die Anwendung berechnet den erwarteten Gewinn, der durch die Verarbeitung von Rohstoffen zu Gegenständen – unter Nutzung der offiziellen Rezeptketten für Veredelung und Herstellung sowie unter Berücksichtigung der Boni der „Royal Cities“ – erzielt wird. 

Sie bietet folgende Funktion:
Der Benutzer wählt eine Stadt Kategorie und Unterkategorie aus. Es werden für diese Stadt, Kategorie und Unterkategorie 25
Artikel eingelesen und sortiert angezeigt. Der am meisten verkaufte Artikel steht oben.

Wählbare Städte: Fort Sterling, Lymhurst, Bridgewatch, Martlock, Thetford, Brecilien, Caerleon und der Schwarze Markt.

Beispiel:
Auswahl: Lymhurst
Kategorie: Waffen
Unterkategorie: Bögen
Ausgabe in Tabellenform

| Bogen       | Anzahl | Preis | Stufe | Verzauberung |     Qualität | 
| Langbogen   |    345 |  2650 |   4   |         0    |       Normal |
| Langbogen   |    336 |  4999 |   4   |         1    |       Normal |
| Langbogen   |    319 |  5249 |   5   |         0    |          Gut |
| Bogen       |    302 |  9878 |   4   |         0    | Hervorragend |
| Kriegsbogen |    287 |  1987 |   4   |         2    |       Normal |
| Langbogen   |    271 |  5781 |   5   |         0    |       Normal |
Und so weiter bis alle bögen aufgelistet sind.

Anzahl = die durchschnittlich verkaufte Anzahl pro Tag in den letzten 4 Wochen
Preis = der durchschnittliche Preis in den letzten 4 Wochen

Rundung durchgängig **kaufmännisch** (`RoundingMode.HALF_UP`): Anzahl mit 1 Nachkommastelle,
Preise und Geldbeträge mit 2 Nachkommastellen (`BigDecimal` in der Umsetzung).

Für Kategorie bzw. Unterkategorie kann auch "Alle" gewählt werden
Per default werden die ersten 25 Artikel in Listenform angezeigt, über eine Drop Down box kann die angezeigte Anzahl 
eingestellt werden. Zu Auswahl steht 25 (Default), 50 und 100

Über eine weitere Drop Down box wird die Qualität des Markts gewählt: Normal (Default), Gut, Außergewöhnlich, 
Hervorragend. 
Anzahl = die durchschnittlich verkaufte Anzahl pro Tag in den letzten 4 Wochen, **summiert über alle Qualitäten**. 
Preis = der durchschnittliche Preis **der gewählten Qualität** in den letzten 4 Wochen. 
Default „Normal", da selbst gefertigte Gegenstände standardmäßig diese Qualität haben.

## Berechnen
Der Benutzer wählt einen Gegenstand aus der liste (siehe oben) aus, für diesen Gegenstand wird folgendes angezeigt.

### Variante 1
Der Kaufpreis der Materialien wenn sie in der Stadt gekauft werden die ausgewählt wurde.
z.B. Langbogen Stufe 5 wird aus 32 Planken Stufe 5 hergestellt. 

### Variante 2
Der Kaufpreis der Materialien (diesmal Rohmeterial), es wird in der Stadt mit dem besten Bonus veredelt.
z.B. Langbogen Stufe 5 wird aus 
- 32 T2 Holz -> T2 Planken
- 32 T2 Planken + 64 T3 Holz -> 32 T3 Planken
- 32 T3 Planken + 64 T4 Holz -> 32 T4 Planken
- 32 T4 Planken + 96 T5 Holz -> 32 T5 Planken
Mit diesen 32 T5 Planken kann der Langbogen hergestellt werden

Die Berechnung erfolgt **ohne Crafting Focus** (Rücklaufrate 36,7 % in der Spezialitätsstadt, siehe README).
Ein Vermerk weist im UI darauf hin, dass mit Fokus die Ausbeute höher (bis 53,9 %) und der Materialbedarf
somit niedriger sein kann.

### Gewinn

**Gewinn = Erlös − Materialkosten − Marktgebühren.**
- **Erlös** = Preis der gewählten Qualität in der **gewählten Stadt** (unabhängig davon, wo veredelt wurde).
- **Marktgebühren** (verifiziert, identisch in allen Städten): Verkaufsteuer **4 %** mit Premium
  (**8 %** ohne), Setup-Fee **2,5 %** nur bei Sell-Order. Über Einstellungen steuerbar:
  Premium ja/nein (Default ja), Sell-Order/Direktverkauf an Kauforder (Default Sell-Order).
- **Werkstatt-/Stationsgebühren bleiben unberücksichtigt** (nutzungsbasiert, je Kraftwerk unterschiedlich) —
  Upgrade-Pfad: freier Parameter.
- Die Gewinnberechnung rechnet mit `BigDecimal` (Scale 2, kaufmännisch `RoundingMode.HALF_UP`).


## Datenquellen (Extern)

Quelle: [`albion-online-data.com`](https://www.albion-online-data.com/api/).
Die Daten werden vom Europa Server geholt.

Endpunkt für Preis- und Verkaufsmengen-Historie (live verifiziert 2026-10-09):
`GET https://europe.albion-online-data.com/api/v2/stats/history/{item_ids}?locations=…&qualities=1,2,3,4&time-range=28`

Antwortform: je `(location, item_id, quality)` ein Objekt mit stündlichen Einträgen
`{"item_count": <verkaufte Stück dieser Stunde>, "avg_price": <Ø Preis dieser Stunde>, "timestamp": …}`.

Daraus berechnet die Anwendung (alle vier Qualitäten kommen in **einer** Anfrage):
- **Anzahl** = Σ(item_count) ÷ 28 → durchschnittlich verkaufte Stück pro Tag, summiert über alle Qualitäten.
- **Preis** = Σ(item_count × avg_price) ÷ Σ(item_count) → verkaufsvolumengewichteter Durchschnittspreis
  der gewählten Qualität. Ein ungewichteter Stunden-Schnitt würde ruhige Nachtstunden genauso einbeziehen
  wie Umsatzspitzen und den realen Marktpreis verzerrn.
- Die Anfrage bündelt alle Listitems einer Seite (25/50/100); Zwischenergebnisse werden gecacht.

Wichtig: Die API akzeptiert nur die exakten Item-IDs der ao-bin-dumps (z. B. `T4_2H_BOW`, nicht `T4_BOW`) —
`recipes.json` nutzt dieselben IDs aus demselben Dump, daher passen sie 1:1.

