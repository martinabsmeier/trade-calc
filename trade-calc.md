# trade-calc

Profitrechner für hergestellte Gegenstände in **Albion Online** (Europa-Server).

- **Stack:** Spring Boot 3.3.5, Maven, Java 21
- **Datensstrategie:** Rezepte & Boni lokal (JSON), Marktdaten via Cache (Caffeine) + API-Refresh
- **UI:** Thymeleaf + Bootstrap 5 + HTMX

> Dieses Dokument ist gleichzeitig **Projektbeschreibung**, **Konzept** und **Implementierungs-Roadmap**.
> Jede Aussage ist eine Anforderung an den Code, jede Phase ein lauffähiger Meilenstein.

---

## Inhalt

1. [Vorbemerkung](#vorbemerkung)
2. [Glossar](#glossar)
3. [Beispiel-Durchlauf](#beispiel-durchlauf)
4. [Beschreibung](#beschreibung)
5. [Datenquellen](#datenquellen)
6. [User Interface](#user-interface)
7. [Konzept](#konzept)
8. [Datenhaltung — Entscheidung](#datenhaltung--entscheidung)
9. [Implementierungs-Roadmap](#implementierungs-roadmap)
10. [Teststrategie](#teststrategie)

---

## Vorbemerkung

- Wir beziehen uns auf das Spiel **Albion Online**.
- Der Name der Applikation lautet `trade-calc`.
- Die Maven-Koordinaten lauten `de.am.albion:trade-calc:1.0.0-SNAPSHOT`.

---

## Glossar

| Begriff | Bedeutung |
|---|---|
| **Item** | Ein Gegenstand oder Material (z. B. `T4_BOW`, `T4_PLANK`, `T4_WOOD`). |
| **Tier** | Seltenheitsstufe. `T4` = Sehr selten, `T8` = Göttlich. Die App fokussiert sich auf **T4** … **T8**. |
| **Rezept** | Stückliste: Liste der Materialien, aus denen ein Item hergestellt wird. |
| **Veredelung (Refining)** | Rohmaterial → Zwischenprodukt (z. B. `WOOD` → `PLANK`). |
| **Herstellung (Crafting)** | Material(ien) → Endprodukt (z. B. `PLANK` → `BOW`). |
| **Royal City** | Eine der sechs großen Städte: Martlock, Fort Sterling, Lymhurst, Bridgewatch, Thetford, Caerleon. |
| **Outland** | `Brecilien` — keine Boni, dafür Schwarzmarkt-Zugang. |
| **Bonus** | Prozentuale Vergünstigung auf Herstell-/Veredelungs-Kosten in einer Stadt. |
| **buyPriceMax** | Höchster aktueller Kaufauftrag (Buy-Order) — relevant, wenn wir Material *kaufen* wollen. |
| **sellPriceMin** | Niedrigster aktueller Verkaufsauftrag (Sell-Order) — relevant, wenn wir das Endprodukt *verkaufen* wollen. |
| **Profit** | `Verkaufserlös − Gesamtkosten`. |
| **ROI** | Return on Investment in Prozent: `Profit / Gesamtkosten × 100`. |
| **Modus (a)/(b)/(c)** | Die drei Berechnungsstrategien (siehe [Berechnungsmodi](#berechnungsmodi)). |

---

## Beispiel-Durchlauf

> *Ein Spieler öffnet das Dashboard, wählt **Lymhurst** als Heimatstadt, Modus **(c) BEST_OF_ALL** und klickt auf „Berechnen".*

1. Die App lädt aktuelle Marktpreise aus dem Cache (alle 5 Min von `albion-online-data.com` aktualisiert).
2. Für jedes craftbare Item prüft sie:
   - Wo ist das billigste Rohmaterial?
   - Wo bekommt sie den größten Veredelungs-Bonus?
   - Wo bekommt sie den größten Herstellungs-Bonus?
3. Das Ergebnis: sortierte Liste der Items mit höchstem Profit/ROI.
4. Klick auf ein Item → Schritt-für-Schritt-Anleitung:

   > **Bogen (T4_BOW) — ROI 63,9 %**
   > 1. Kaufe `T4_WOOD` in **Lymhurst** zu `780` Silber/Stück (`16 × 780 = 12.480`).
   > 2. Reise nach **Fort Sterling** und veredle `T4_WOOD` → `T4_PLANK` (Bonus 9,8 %, Effektivkosten `703,56/Stück`).
   > 3. Reise zurück nach **Lymhurst** und stelle `T4_BOW` her (Crafting-Bonus 18,3 %).
   > 4. Verkaufe `T4_BOW` in **Lymhurst** zu `sellPriceMin = 18.450` Silber.
   >
   > **Profit:** `7.193` Silber | **ROI:** `63,9 %`

---

## Beschreibung

- Erstelle ein Programm mit Spring Boot, Maven und Java 21.
- Es berechnet Gewinne aus dem Verkauf hergestellter Gegenstände und maximiert sie.
- **Rohmaterialien** werden in Städten mit dem niedrigsten Preis gekauft (modusabhängig).
- **Veredelung** (z. B. Holz → Planken) findet in der Stadt mit dem höchsten Refining-Bonus statt.
- **Herstellung** (z. B. Planken → Bogen) findet in der Stadt mit dem höchsten Crafting-Bonus statt.
- **Verkauf** erfolgt in der Herstellungsstadt (höchster `sellPriceMin`).

---

## Datenquellen

| Quelle | Inhalt | Zugriff |
|---|---|---|
| `albion-online-data.com` (Europa) | Live-Marktpreise (`buyPriceMax`, `sellPriceMin`) | REST, gecached via Caffeine |
| `resources/bonuses.json` | Stadt-Boni (statisch) | Klassenpfad |
| `resources/recipes/*.json` | Stücklisten (statisch) | Klassenpfad |

API-Dokumentation: <https://www.albion-online-data.com/api/>

---

## User Interface

### Ablauf

1. **Stadt wählen** — Royal City oder Brecilien (für Modus a/b relevant).
2. **Berechnungsmodus wählen** — drei Optionen (siehe unten).
3. **Ergebnis anzeigen** — Tabelle der profitabelsten Items, sortiert nach `ROI`.
4. **Anleitung öffnen** — Klick auf eine Zeile zeigt die Schritt-für-Schritt-Anleitung.

### Berechnungsmodi

| Modus | Kurzname | Rohmaterial kaufen | Veredeln | Herstellen & Verkauf |
|---:|---|---|---|---|
| (a) | `LOCAL_ONLY` | In der vom User gewählten Stadt | In der vom User gewählten Stadt | In der vom User gewählten Stadt |
| (b) | `LOCAL_BUY_BEST_REST` | In der vom User gewählten Stadt | In der Stadt mit dem höchsten Refining-Bonus | In der Stadt mit dem höchsten Crafting-Bonus |
| (c) | `BEST_OF_ALL` | In der Stadt mit dem **niedrigsten Preis** | In der Stadt mit dem höchsten Refining-Bonus | In der Stadt mit dem höchsten Crafting-Bonus |

> **Hinweis:** Modus (a) wirkt nur, wenn die gewählte Stadt *beide* Boni (Refining + Crafting) für die jeweiligen Kategorien bietet. Andernfalls entfallen die Boni und die Berechnung wird ineffizient.

### UI-Technologie

- **Server-Rendering** mit Thymeleaf (bereits in `pom.xml` enthalten).
- **HTMX** für dynamische Tabellen-Updates ohne Seiten-Reload.
- **Bootstrap 5** für Layout & Styling.

### Seitenstruktur

| Seite | URL | Zweck |
|---|---|---|
| Dashboard | `/` | Stadt wählen, Modus wählen, Filter setzen, Berechnung triggern |
| Ergebnisliste | `/results` | Sortierbare Tabelle der Top-Items |
| Anleitung-Drawer | (Modal/Drawer) | Schritt-für-Schritt-Anleitung pro Item |
| Markt-Übersicht | `/market` | Aktuelle Preise je Stadt für ein Item (Bonus-Feature) |

#### Dashboard-Details

- Stadt-Dropdown: Royal Cities + Brecilien.
- Modus-Auswahl: Radio-Buttons für (a), (b), (c) mit Kurzbeschreibungen.
- Filter: Tier (T4–T8), Item-Kategorie, Mindest-ROI in %.
- Button **„Berechnen"** → POST an `/api/v1/profit/calculate`, Tabelle wird befüllt.
- Button **„Daten aktualisieren"** → POST an `/api/v1/refresh`, leert Cache und zeigt neuen Zeitstempel.

#### Ergebnisliste-Details

Sortierbare Tabelle: Gegenstand | Tier | Modus | Buy-City | Ref-City | Sell-City | Kosten | Profit | ROI.

#### Anleitung-Drawer-Details

Nummerierte Liste pro Item:

1. Kaufe `T4_WOOD` in **Lymhurst** zu `X` Silber/Stück.
2. Reise nach **Fort Sterling** und veredle zu `T4_PLANK` (Bonus 9,8 %, Effektivkosten `Y`).
3. Reise nach **Lymhurst** und stelle `T4_BOW` her (Bonus 18,3 %, Effektivkosten `Z`).
4. Verkaufe `T4_BOW` in **Lymhurst** zu `sellPriceMin`.

Plus Zusammenfassung: Gesamtkosten, Verkaufserlös, Profit, ROI.

### Tabellenkomponente (Beispiel)

```
┌─────────────┬─────┬────────┬───────────┬───────────┬────────────┬───────┬───────┬────────┐
│ Gegenstand  │Tier │ Modus  │ Buy-City  │ Ref-City  │ Sell-City  │ Kosten│ Profit│  ROI   │
├─────────────┼─────┼────────┼───────────┼───────────┼────────────┼───────┼───────┼────────┤
│ T4_BURN_BOW │ T4  │ (c)    │ Lymhurst  │ Fort St.  │ Lymhurst   │ 12.390│  6.060│ 48,9 % │
│ T4_BURN_BOW │ T4  │ (b)    │ Martlock  │ Fort St.  │ Lymhurst   │ 13.100│  5.350│ 40,8 % │
│ T4_BURN_BOW │ T4  │ (a)    │ Lymhurst  │ Lymhurst  │ Lymhurst   │ 12.390│  4.220│ 34,1 % │
└─────────────┴─────┴────────┴───────────┴───────────┴────────────┴───────┴───────┴────────┘
```

### Interaktionen

- **Auto-Refresh:** Tabelle aktualisiert sich alle 5 Minuten via HTMX (`hx-trigger="every 5m"`).
- **Filter:** Tier-Dropdown, Kategorie-Dropdown, ROI-Mindestwert als Schieberegler.
- **Sortierung:** Spaltenüberschriften sind klickbar (HTMX-Sort).
- **Detail-Drawer:** Klick auf eine Zeile öffnet seitlich die Anleitung.

---

## Konzept

### Domänenmodell (Kernentitäten)

| Entität | Zweck | Attribute |
|---|---|---|
| `Item` | Ein Gegenstand oder Material | `id`, `name`, `tier`, `category` |
| `City` | Eine Spielstadt | `name`, `region` |
| `CityBonus` | Bonus-Wert einer Stadt für eine Kategorie | `city`, `bonusType` (`CRAFTING`/`REFINING`), `category`, `bonusFactor` (z. B. `0.152`) |
| `Recipe` | Stückliste für ein Item | `resultItem`, `requiredItems[]`, `craftingCityCategory`, `refiningCityCategory` |
| `MarketPrice` | Marktdaten pro Item/Stadt | `item`, `city`, `sellPriceMin`, `buyPriceMax`, `sellPriceMinDate`, `buyPriceMaxDate` |
| `ProfitResult` | Berechnungsergebnis | `item`, `city`, `sellPrice`, `totalCost`, `profit`, `roi`, `craftingFee` (optional) |
| `CalculationMode` | Enum der drei Berechnungsmodi | `LOCAL_ONLY`, `LOCAL_BUY_BEST_REST`, `BEST_OF_ALL` |
| `CraftingPlan` | Schritt-für-Schritt-Anleitung | `item`, `buySteps[]`, `refineSteps[]`, `craftCity`, `sellCity`, `totalCost`, `profit`, `roi` |

### Städte und Boni

Die Boni sind im Spiel statisch und werden hier lokal vorgehalten (kein API-Endpoint verfügbar). Ein Strich (`—`) bedeutet: *kein Bonus in dieser Stadt für diese Kategorie*.

| Stadt | Crafting-Kategorie | Refining-Kategorie | Bonus |
|---|---|---|---:|
| Martlock | Häute, Lederwaren | Häute | 15,2 % |
| Fort Sterling | Holz, Schilde, Mörser | Holz | 9,8 % |
| Lymhurst | Bögen, Stoff, Möbel | — | 18,3 % |
| Bridgewatch | Fackeln, Speere | Stein | 24,0 % |
| Thetford | Klingen, Rüstungen | Metall | 22,5 % |
| Caerleon | Alle Kategorien (Food) | — | 7,2 % |
| Brecilien | — (keine Boni) | — | 0,0 % |

> Die tatsächlichen Boni variieren pro Marktzyklus. Die hier angegebenen Werte sind eine Momentaufnahme und werden aktualisiert, sobald eine verlässliche Quelle verfügbar ist.

### Berechnungslogik

**Eingabe:** `selectedCity`, `mode`, `recipe`, `marketPrices[]`

1. **Einkaufskosten ermitteln** (modusabhängig):
   - `(a)`: `buyPriceMax(material, selectedCity)`
   - `(b)`: `buyPriceMax(material, selectedCity)`
   - `(c)`: `min über alle Städte von buyPriceMax(material, city)`

2. **Veredelungsstadt wählen** (modusabhängig):
   - `(a)`: `selectedCity` (kein Bonus, falls dort kein Refining-Bonus existiert)
   - `(b)`: `Stadt mit maximalem Refining-Bonus für material.category`
   - `(c)`: `Stadt mit maximalem Refining-Bonus für material.category`

3. **Herstellungsstadt wählen** (modusabhängig):
   - `(a)`: `selectedCity` (kein Bonus, falls dort kein Crafting-Bonus existiert)
   - `(b)`: `Stadt mit maximalem Crafting-Bonus für result.category`
   - `(c)`: `Stadt mit maximalem Crafting-Bonus für result.category`

4. **Verkaufspreis:** `sellPriceMin(result, craftCity)`

5. **Berechnung:**

   ```
   materialCostEffective = materialCost × (1 − refiningBonus)
   craftCostEffective    = (summe aller materialCostEffective + craftingFee) × (1 − craftingBonus)
   totalCost             = craftCostEffective
   profit                = sellPrice − totalCost
   roi                   = profit / totalCost × 100
   ```

6. **Sortierung:** Alle Items absteigend nach `ROI`.

7. **CraftingPlan generieren:** Schritt-für-Schritt-Liste für die UI erstellen.

### Beispiel-Rechnung (Modus c, T4_BOW)

| Schritt | Stadt | Berechnung | Ergebnis |
|---|---|---|---:|
| Kauf `T4_WOOD` (16×) | Lymhurst (günstigster) | `16 × 780` | `12.480` Silber |
| Veredelung `T4_WOOD` → `T4_PLANK` | Fort Sterling (Refining-Bonus 9,8 %) | `780 × (1 − 0,098) = 703,56` → `16 × 703,56` | `11.256,96` Silber |
| Herstellung `T4_BOW` | Lymhurst (Crafting-Bonus 18,3 %) | `0` Basis-Fee angenommen | `0` Silber |
| Verkauf `T4_BOW` | Lymhurst | `sellPriceMin` | `18.450` Silber |
| **Profit** | | `18.450 − 11.256,96` | **`7.193,04` Silber** |
| **ROI** | | `7.193 / 11.257 × 100` | **`63,9 %`** |

> **Vereinfachung:** Die Berechnung ignoriert die Basis-Herstellgebühr (`Crafting Fee` ohne Bonus) und Transportkosten. Diese müssen ergänzt werden, sobald eine Quelle verfügbar ist.

### Paketstruktur

Eine klassische Spring-Boot-Layer-Architektur (Hexagonal-light):

```
de.am.albion.tradecalc
├── TradeCalcApplication.java
│
├── config/                            # Spring-Konfiguration
│   ├── AlbionApiProperties.java        # @ConfigurationProperties("albion.api")
│   ├── CacheProperties.java            # @ConfigurationProperties("cache.market")
│   ├── CacheConfig.java                # CaffeineCacheManager
│   └── WebConfig.java
│
├── api/                               # REST-Endpunkte
│   ├── PriceController.java            # GET  /api/v1/prices/{itemId}
│   ├── ProfitController.java           # POST /api/v1/profit/calculate
│   └── RefreshController.java          # POST /api/v1/refresh
│
├── ui/                                # Web-UI (Thymeleaf)
│   ├── controller/
│   │   ├── DashboardController.java    # GET /
│   │   └── ResultsController.java      # GET /results
│   └── dto/
│       ├── CalculationRequestDto.java  # city, mode, tier, category, minRoi
│       ├── ProfitTableDto.java
│       └── CraftingPlanDto.java
│   # templates/ in resources/templates/
│
├── dataprovider/                      # Adapter für externe Quellen
│   ├── albion/
│   │   ├── AlbionDataApiClient.java    # RestClient gegen albion-online-data.com
│   │   ├── dto/                        # API-Records
│   │   └── mapper/                     # API → Domain
│   └── recipe/
│       └── RecipeLoader.java           # Liest resources/recipes/*.json
│
├── repository/                        # In-Memory-Indizes + Cache-Wrapper
│   ├── MarketPriceRepository.java
│   ├── RecipeRepository.java
│   └── CityBonusRepository.java
│
├── service/                           # Anwendungslogik
│   ├── PriceService.java               # Marktdaten
│   ├── RecipeService.java              # Recipe-Lookup
│   ├── ProfitCalculationService.java   # Kern-Berechnung
│   ├── CraftingPlanService.java        # Anleitungs-Erzeugung
│   └── BonusService.java               # Bonus-Lookup
│
├── domain/                            # Framework-agnostische Records/POJOs
│   ├── model/
│   │   ├── Item.java
│   │   ├── City.java
│   │   ├── CityBonus.java
│   │   ├── Recipe.java
│   │   ├── MarketPrice.java
│   │   ├── ProfitResult.java
│   │   └── CraftingPlan.java
│   └── CalculationMode.java            # Enum
│
└── exception/                         # Fehlerbehandlung
    ├── GlobalExceptionHandler.java
    └── TradeCalcException.java
```

**Begründung der Struktur:**

- **`domain`** enthält nur POJOs/Records ohne Spring-Abhängigkeiten — einfach zu mocken, keine Kopplung.
- **`service`** orchestriert die Domain-Logik. `ProfitCalculationService` und `CraftingPlanService` sind getrennt, damit die UI direkt einen `CraftingPlan` rendern kann.
- **`dataprovider`** ist ein klar abgegrenzter Adapter zur externen API. Ein späterer Wechsel auf eine andere Quelle ändert nur diesen Layer.
- **`ui`** (Thymeleaf) und **`api`** (REST) sind getrennt, damit später z. B. ein SPA-Frontend (React/Vue) nur den `api`-Layer benötigt.
- **`config`** hält externe Konfiguration (API-URL, Cache-Einstellungen).
- **`domain/CalculationMode`** ist als Enum zentral, damit UI, API und Service dieselben Werte verwenden.

---

## Datenhaltung — Entscheidung

### Empfehlung

**Hybrid-Ansatz:** Rezepte und Boni lokal (Klassenpfad), Marktdaten via Cache (Caffeine) + On-Demand-Refresh.

### Pro/Contra-Tabelle

| Aspekt | A: Immer API | B: Vollständig lokal (SQLite) | C: **Hybrid** (Empfehlung) |
|---|:---:|:---:|:---:|
| Daten-Aktualität | 🟢 Immer aktuell | 🔴 Nur bei manuellem Import | 🟢 Nahezu aktuell (TTL 5–15 Min) |
| Performance / Latenz | 🟡 Jede Anfrage = Netzwerk | 🟢 Sehr schnell (lokal) | 🟢 Schnell, Hintergrund-Refresh |
| API-Last / Rate-Limit | 🔴 Hoch — Risk Limit | 🟢 Keine | 🟡 Gering, Cache schützt |
| Offline-Fähigkeit | 🔴 Ohne Netz unbrauchbar | 🟢 Voll funktionsfähig | 🟡 Eingeschränkt (mit Cache) |
| Komplexität | 🟢 Einfach (kein State) | 🟡 Mittel (DB, Sync, Migrationen) | 🟡 Mittel |
| Konsistenz | 🟢 Immer konsistent | 🟡 Risiko veralteter Daten | 🟢 Konsistent innerhalb TTL |
| Robustheit bei API-Ausfall | 🔴 Komplettausfall | 🟢 Keine Abhängigkeit | 🟢 Fallback auf Cache |
| Speicherverbrauch | 🟢 Minimal | 🟡 Wachsend mit Historie | 🟡 Begrenzt (konfigurierbar) |
| Backup / Recovery | 🟢 Keine | 🟡 DB muss gesichert werden | 🟢 Kein Persistenz-Zwang |
| Entwicklungsaufwand | 🟢 Niedrig | 🟡 Hoch (DB-Layer, ETL) | 🟡 Mittel (Cache, Scheduler) |
| Skalierbarkeit (Last) | 🟡 Direkt von API-Limits abhängig | 🟢 Unabhängig | 🟢 Gut (Cache entkoppelt) |
| Rezept-Daten-Quelle | 🔴 Nicht verfügbar | 🟢 Lokal gepflegt | 🟢 Lokal gepflegt |
| Boni-Daten-Quelle | 🔴 Nicht verfügbar | 🟢 Lokal gepflegt | 🟢 Lokal gepflegt |
| Ersteinrichtung für Spieler | 🟢 Null (nur URL) | 🟡 Initialer Import nötig | 🟢 Minimal (nur Cache-Aufbau) |

**Legende:** 🟢 = positiv | 🟡 = neutral | 🔴 = negativ

### Begründung der Empfehlung (Variante C)

- **Rezepte und Boni** sind nicht über die API verfügbar und müssen ohnehin lokal gepflegt werden (`resources/recipes/*.json`, `resources/bonuses.json`).
- **Marktdaten** ändern sich im Minuten-Takt — ein vollständig lokaler Ansatz wäre innerhalb von Minuten veraltet.
- **Caffeine-Cache** (bereits als Dependency vorhanden) lässt sich mit minimalem Aufwand einbinden.
- **Fallback auf Cache** bei API-Ausfall sorgt für Robustheit.
- **Hintergrund-Refresh** (alle 5 Min) reduziert API-Calls und hält die Daten frisch.

### Konfiguration

Die zentralen Werte sind in `application.yml` unter `cache.market.*` ausgelagert und ohne Code-Änderung anpassbar:

```yaml
albion:
  api:
    base-url: https://west.albion-online-data.com
    server: europe                # Europa-Server-Endpoint
    locations:                    # Städte, für die wir Preise abfragen
      - Lymhurst
      - Fort Sterling
      - Martlock
      - Bridgewatch
      - Thetford
    qualities:                    # Item-Qualitätsstufen (1 = Normal, höhere Stufen kosten mehr)
      - 1

cache:
  market:
    refresh-interval: PT5M        # Scheduler-Refresh-Intervall (alle 5 Min)
    ttl: PT15M                    # Cache-Eintrag läuft nach 15 Min ab
    max-items: 5000               # Maximale Anzahl Items im Cache

spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=${cache.market.max-items},expireAfterWrite=${cache.market.ttl}
    cache-names:
      - marketPrices
      - recipes
      - cityBonuses

management:
  endpoints:
    web:
      exposure:
        include: caches,metrics
  endpoint:
    caches:
      enabled: true
```

| Parameter | Wert | Bedeutung |
|---|---|---|
| `cache.market.refresh-interval` | `PT5M` | Scheduler ruft `RefreshService.warmUp()` alle 5 Min auf. |
| `cache.market.ttl` | `PT15M` | Cache-Eintrag wird 15 Min nach Schreiben ungültig. |
| `cache.market.max-items` | `5000` | Hard-Limit; bei Überlauf werden alte Einträge evicted. |

### Entscheidungen (Offene Punkte geklärt)

| Thema | Entscheidung | Begründung / Implementierungsnotiz |
|---|:---:|---|
| **Cache-TTL** | **5 Minuten** | Ausreichend für Marktdaten; zu kurz = unnötige API-Calls, zu lang = veraltete Preise. |
| **Force-Refresh-Endpoint** | **Ja, einbauen** | `POST /api/v1/refresh` leert `marketPrices`-Cache und lädt synchron neu. Optional `?async=true`. Wird vom UI-Button „Daten aktualisieren" getriggert. |
| **Cache-Statistik via Actuator** | **Ja, einbauen** | `spring-boot-starter-actuator` einbinden; liefert Hit-Rate, Größe, Eviction-Rate → Tuning-Daten in Produktion. |

### Wichtige Endpunkte

- `GET /actuator/caches` — Liste aller Caches.
- `GET /actuator/caches/marketPrices` — Größe und Konfiguration des Marktdaten-Caches.
- `GET /actuator/metrics/cache.gets?tag=cache:marketPrices` — Hit/Miss-Rate.
- `POST /api/v1/refresh` — leert `marketPrices` und lädt synchron neu (UI-Button).

---

## Implementierungs-Roadmap

Das Projekt wird in **10 Phasen** umgesetzt. Jede Phase liefert etwas Lauffähiges (vertikale Slices).

### Phase 0 — Projekt-Setup verifizieren

Bevor wir Code schreiben, prüfen wir, dass die Basis steht.

| Komponente | Erwartet | Aktion bei Abweichung |
|---|:---:|---|
| `pom.xml` mit Spring Boot 3.x, Java 21 | ✅ | ggf. `spring-boot-starter-parent` anpassen |
| `TradeCalcApplication.java` mit `@SpringBootApplication` | ✅ | Klasse anlegen |
| Paket `de.am.albion.tradecalc` in `src/main/java` | ✅ | Struktur anlegen |
| `src/main/resources/application.yml` | ⚠️ existiert, aber unvollständig | in Phase 1 ergänzen |
| `.gitignore` (`target/`, `.idea/`, …) | ✅ | prüfen |
| `README.md` | ✅ | prüfen / erweitern |

**Verifikation:** `mvn -v` zeigt Java 21+ und Maven 3.9+. `mvn dependency:resolve` läuft fehlerfrei.

→ Nächste Phase: [Phase 1 — Fundament](#phase-1--fundament)

### Phase 1 — Fundament

- `application.yml` mit Cache-Config (`maximumSize=${cache.market.max-items}`, `expireAfterWrite=${cache.market.ttl}`, `cache-names`) und `management.endpoints.web.exposure.include=caches,metrics`.
- `application-dev.yml` (Dev-Profil mit ausführlicheren Logs).
- `TradeCalcApplication` mit `@SpringBootApplication`, `@EnableCaching` und `@EnableScheduling`.
- `config/AlbionApiProperties` (`@ConfigurationProperties("albion.api")`).
- `config/CacheProperties` (`@ConfigurationProperties("cache.market")`).
- `pom.xml` ergänzen: `spring-boot-starter-cache`, `spring-boot-starter-actuator`.

**Verifikation:** `mvn spring-boot:run` startet, `/actuator/health` antwortet `UP`.

→ Nächste Phase: [Phase 2 — Statische Daten](#phase-2--statische-daten)

### Phase 2 — Statische Daten

- `domain/CalculationMode` als Enum.
- `domain/model/Item`, `City`, `CityBonus`, `Recipe`, `MarketPrice`, `ProfitResult`, `CraftingPlan` als Records.
- `resources/bonuses.json` mit den 7 Städten und Boni (Werte aus der Boni-Tabelle oben).
- `resources/recipes/` mit 5–10 Beispiel-Recipes (T4 Bow, T4 Plank, T4 Sword, …).
- `dataprovider/recipe/RecipeLoader` (liest JSON beim Start, cached via `@Cacheable("recipes")`).
- `repository/CityBonusRepository` (In-Memory-Index).
- `service/BonusService`, `RecipeService` als Wrapper mit Cache.

**Verifikation:** Unit-Test lädt 3 Recipes, prüft Indizes.

→ Nächste Phase: [Phase 3 — Externe API](#phase-3--externe-api)

### Phase 3 — Externe API

- `dataprovider/albion/AlbionDataApiClient` (RestClient gegen `albion-online-data.com`, Items per ID/Locations).
- `dataprovider/albion/dto/*` für `MarketJson`.
- `dataprovider/albion/mapper/*` für API → Domain.
- `service/PriceService` lädt `MarketPrice`-Daten pro Item, cached via `@Cacheable("marketPrices")`.
- `api/PriceController` mit `GET /api/v1/prices/{itemId}`.

**Verifikation:** Live-Call gegen echte API (z. B. `T4_WOOD`), Wert landet im Cache, zweiter Call <10 ms.

→ Nächste Phase: [Phase 4 — Refresh & Actuator](#phase-4--refresh--actuator)

### Phase 4 — Refresh & Actuator

- `api/RefreshController` (`POST /api/v1/refresh` leert `marketPrices`-Cache und ruft `PriceService.warmUp()`).
- `RefreshService` orchestriert Bulk-Reload.
- `config/CacheConfig` mit `CaffeineCacheManager` Bean.
- Integration-Test ruft Refresh, prüft neue Werte.

**Verifikation:** `/actuator/caches/marketPrices` zeigt Größe und Hit-Rate.

→ Nächste Phase: [Phase 5 — Kern-Berechnung](#phase-5--kern-berechnung)

### Phase 5 — Kern-Berechnung

- `service/ProfitCalculationService` als Strategie pro `CalculationMode` (Strategy-Pattern oder `switch`).
- `service/ProfitCalculator` mit reinen Berechnungsmethoden (testbar ohne Spring).
- Unit-Tests pro Modus mit fixen Preisen → deterministische Erwartungswerte.

**Verifikation:** `T4_BOW` mit Modus (c) liefert bekannten Profit (siehe [Beispiel-Rechnung](#beispiel-rechnung-modus-c-t4_bow)).

→ Nächste Phase: [Phase 6 — Anleitung](#phase-6--anleitung)

### Phase 6 — Anleitung

- `service/CraftingPlanService` iteriert Recipe-Baum und erzeugt `BuyStep`/`RefineStep`/`CraftStep`/`SellStep`.
- Tests mit Mock-`MarketPrice`, prüfen Reihenfolge und Städte.

**Verifikation:** Service gibt für `T4_BOW` (c) korrekt „Holz in Lymhurst, Planken in Fort Sterling, Bogen in Lymhurst verkaufen" zurück.

→ Nächste Phase: [Phase 7 — REST-API für Berechnung](#phase-7--rest-api-für-berechnung)

### Phase 7 — REST-API für Berechnung

- `api/ProfitController` (`POST /api/v1/profit/calculate`, Body: `CalculationRequestDto`).
- DTOs für Request/Response (Mode, City, Tier, MinRoi, Filter).
- Validierung mit `jakarta.validation`.
- Fehlerbehandlung in `GlobalExceptionHandler`.

**Verifikation:** `curl -X POST` liefert JSON-Liste der Top-Items inkl. `CraftingPlan`.

→ Nächste Phase: [Phase 8 — Web-UI](#phase-8--web-ui)

### Phase 8 — Web-UI

- `ui/controller/DashboardController` (`GET /` zeigt Stadt-/Modus-Auswahl).
- `ui/controller/ResultsController` (`GET /results` zeigt Tabelle).
- `templates/dashboard.html`, `results.html`, `_plan-drawer.html` (Thymeleaf).
- `static/css/style.scss`, `static/js/app.js` (Bootstrap 5, HTMX, ggf. kleine Helper).
- DTOs: `CalculationRequestDto`, `ProfitTableDto`, `CraftingPlanDto`.

**Verifikation:** Browser-Flow: Stadt wählen → Modus → Berechnen → Tabelle → Klick auf Zeile → Anleitung sichtbar.

→ Nächste Phase: [Phase 9 — Polish & Qualität](#phase-9--polish--qualität)

### Phase 9 — Polish & Qualität

- Tests: `application-test.yml` mit WireMock für API, Spring Boot Test für End-to-End.
- Resilience: Retry mit Resilience4j (optional), Circuit-Breaker für API-Ausfall.
- Caching-Strategie: Bulk-Warmup beim Start (Scheduler), nur Items aus Recipes.
- Logging: strukturierte Logs mit MDC für Request-ID.
- README.md mit Run-Anleitung und Screenshots.

**Verifikation:** App läuft 1 Stunde ohne Speicher-Leak, alle Tests grün.

### Begründung der Reihenfolge

- **Phase 0 zuerst.** Bestätigt, dass das Skelett steht (pom.xml, Application-Klasse, Paketstruktur), bevor Code hinzukommt.
- **Bottom-up, aber funktional.** Erst Daten (statisch), dann externe API, dann Berechnung. Nach Phase 5 ist die Logik schon korrekt — auch ohne UI.
- **Cache von Anfang an.** In Phase 1 mit `spring-boot-starter-cache` aktiviert, damit Tests realistisch sind.
- **Testbarkeit vor UI.** `ProfitCalculationService` ohne Spring-Kontext testbar (Phase 5).
- **Vertical Slices.** Jede Phase liefert etwas Sichtbares/Aufrufbares.

---

## Teststrategie

Das Projekt folgt einer zweistufigen Test-Pyramide: **schnelle Unit-Tests** für isolierte Logik und **Integration-Tests** für Spring-Boot-Spezifika.

### Namenskonvention

| Suffix | Ausführungs-Plugin | Zweck |
|---|---|---|
| `*Test` | `maven-surefire-plugin` | Schnell, kein Spring-Kontext, isolierte Klassen |
| `*IT` | `maven-failsafe-plugin` | Brauchen Spring-Kontext oder HTTP (`mvn verify`) |

**Befehle:**

```bash
mvn test        # nur Unit-Tests (schnell, ~100 ms)
mvn verify      # Unit + Integration-Tests (~5 s)
```

### Tests pro Phase

| Phase | Tests | Abdeckung |
|---|---|---|
| **1 Fundament** | `*Test` + `*IT` | `*Properties` (Defaults, Setters, ISO-8601-Parsing), Kontext-Start, `CacheManager`-Bohnen, Actuator-Endpoints. |
| **2 Statische Daten** | `*Test` | JSON-Loader lädt 3 Recipes, Indizes sind konsistent, Enum-Werte. |
| **3 Externe API** | `*Test` + `*IT` | `RestClient`-Mock (WireMock), Mapping API→Domain, Cache-Behavior bei 2. Call. |
| **4 Refresh & Actuator** | `*IT` | `POST /api/v1/refresh` leert Cache, `/actuator/caches` zeigt neue Stats. |
| **5 Kern-Berechnung** | `*Test` | Pro Modus fixe Preise → erwarteter Profit/ROI (deterministisch). |
| **6 Anleitung** | `*Test` | Mock-`MarketPrice`, Reihenfolge und Städte geprüft. |
| **7 REST-API** | `*IT` | `MockMvc`: Happy-Path + Validierungsfehler. |
| **8 Web-UI** | `*IT` | Selenium oder Playwright (Stadt wählen → Berechnen → Drawer). |
| **9 Polish** | `*IT` | End-to-End mit WireMock, 1-h-Memory-Profil manuell. |

### Was wird getestet?

| Schicht | Was | Warum |
|---|---|---|
| `domain` | Records/Enums | Wertgleichheit, Immutability, Enum-Werte. |
| `config.*Properties` | YAML-Bindung, Defaults | Fehler in Konfiguration sind schwer zu debuggen — Bindung muss getestet werden. |
| `service` (reine Logik) | `ProfitCalculator`, `BonusService` | Komplex, ändert sich oft — Regression-Risiko. |
| `service` (Spring-abhängig) | `PriceService` mit WireMock | Cache-Treffer, Retry-Logik. |
| `dataprovider` | `RecipeLoader`, API-Mapper | Datei- und Schema-Änderungen brechen das. |
| `api` | `MockMvc`/`TestRestTemplate` | HTTP-Status, Response-Schema, Validierung. |
| `ui` | UI-Smoke-Test | Klickflow rendert korrekt. |

### Was wird *nicht* getestet?

- **Triviale Getter/Setter** in Records.
- **Konfigurationsdateien selbst** (nur ihre Bindung).
- **Externe API** in Echtzeit — stattdessen WireMock.
- **Build-/Dependency-Mechanik** — das macht Maven.

### Coverage-Ziel

- **Branchenabdeckung: ≥ 80 %** *(einklagbar — der Build bricht ab, wenn das Ziel unterschritten wird)*.
- **Zeilenabdeckung: ≥ 70 %** *(einklagbar)*.
- **Coverage-Report:** `target/site/jacoco/index.html` (JaCoCo).
- **Coverage-Grenze** ist im `pom.xml` als JaCoCo-Regel hinterlegt und wird bei `mvn verify` automatisch geprüft.
- *Nicht erzwingen* — Coverage ist Mittel, nicht Zweck. Lieber ehrliche 60 % mit guten Assertions als 100 % ohne Lesbarkeit.