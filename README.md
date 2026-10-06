# trade-calc

Profit calculator for crafted items in **Albion Online** (Europe server).

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-orange.svg)](https://maven.apache.org/)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

`trade-calc` is a Spring Boot application that determines the most profitable crafting route
for every craftable item in Albion Online. It takes the Royal City bonuses for refining and
crafting into account, along with the latest `buyPriceMax` and `sellPriceMin` values from
the Albion Online market (Europe server).

For the full project concept, glossary, calculation modes, and the 10-phase
implementation roadmap, see the [Project Concept](#project-concept) section below.

**Highlights:**

- **Three calculation modes** — from "home city only" to "optimal city per stage".
- **Live market data** — cached via Caffeine (15 min TTL, force-refresh via REST).
- **Static configuration** — bonuses and recipes loaded from JSON files on the classpath.
- **Web UI** — Thymeleaf + HTMX (planned from Phase 8 onward).

---

## Quick Start

### Prerequisites

| Tool | Version | Verify with |
|---|---|---|
| Java | 21+ | `java -version` |
| Maven | 3.9+ | `mvn -v` |

### Build and Run

```bash
mvn verify                # Compile + Tests + Coverage Report
mvn spring-boot:run       # Start the app (default profile)
```

The app will be available at <http://localhost:8080>.

### First Steps

1. **Check health:** <http://localhost:8080/actuator/health> → `{"status":"UP"}`
2. **View cache stats:** <http://localhost:8080/actuator/caches>
3. **Open coverage report:** `target/site/jacoco/index.html`

---

## Configuration

All values live in `src/main/resources/application.yml`:

```yaml
albion:
  api:
    base-url: https://west.albion-online-data.com
    server: europe
    locations: [Lymhurst, Fort Sterling, Martlock, Bridgewatch, Thetford]
    qualities: [1]

cache:
  market:
    refresh-interval: PT5M   # Background refresh every 5 minutes
    ttl: PT15M               # Cache entries expire after 15 minutes
    max-items: 5000
```

**Dev profile** (`application-dev.yml`) enables verbose logging and exposes all
Actuator endpoints. Activate with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Live market price endpoint

```bash
curl http://localhost:8080/api/v1/prices/T4_BOW
# {"Lymhurst":{"itemId":"T4_BOW","city":"Lymhurst",...},...}
```

Results are cached under `marketPrices` (Caffeine, 15-min TTL by default);
repeat calls within the TTL are served from cache and complete in single-digit ms.

---

## Tests

| Test Type | Suffix | Plugin | Command | Duration |
|---|---|---|---|---|
| Unit Test | `*Test` | `maven-surefire-plugin` | `mvn test` | ~100 ms |
| Integration Test | `*IT` | `maven-failsafe-plugin` | `mvn verify` | ~5 s |

**Coverage Target:** **> 80 %** branch coverage, **> 70 %** line coverage (rule-based
enforcement via JaCoCo). Running `mvn verify` will fail the build if the target is not met.

```bash
mvn test       # Unit tests only
mvn verify     # Unit + Integration + Coverage report (target/site/jacoco/)
```

> Current unit-test count: **65** across the domain, config, service and
> dataprovider packages (no Spring context required).

### Test Strategy

- **Unit tests** (`*Test`) exercise isolated logic: records, enums, properties binding,
  repositories, `MarketJsonMapper`, and `PriceService` / `AlbionDataApiClient` with a
  recording `ClientHttpRequestFactory` (no Spring context).
- **Integration tests** (`*IT`) start the full Spring context (`@SpringBootTest`) and
  exercise REST endpoints and Actuator over a random port.
- **HTTP upstream** is never hit in tests — a stub `MockClientHttpRequest` /
  `MockClientHttpResponse` pair serves the JSON payload and captures the outbound URI
  for assertions.

| Layer | What | Why |
|---|---|---|
| `domain` | Records / enums | Value equality, immutability, enum names. |
| `config.*Properties` | YAML binding, defaults | Config bugs are hard to debug — binding must be tested. |
| `service` (pure logic) | `PriceService`, `MarketJsonMapper` | Regression risk, complex mapping rules. |
| `dataprovider` | `AlbionDataApiClient`, `RecipeLoader` | API / file schema changes break the contract. |
| `api` | `MockMvc` / `TestRestTemplate` | HTTP status, response schema, validation. |

### What is *not* tested?

- Trivial getters in records.
- Configuration files themselves (only their bindings).
- External API in real-time (stubbed via `MockClientHttpRequest`).
- Build / dependency mechanics — that is Maven's job.

---

## Project Structure

```
trade-calc/
├── pom.xml                          # Maven build
├── README.md                        # This file
└── src/
    ├── main/
    │   ├── java/de/am/albion/tradecalc/
    │   │   ├── TradeCalcApplication.java    # @SpringBootApplication
    │   │   ├── config/                      # Properties classes
    │   │   ├── api/                         # REST endpoints (Phase 3+)
    │   │   ├── ui/                          # Web UI (Phase 8)
    │   │   ├── dataprovider/                # API adapter (Phase 3)
    │   │   ├── repository/                  # In-memory indexes (Phase 2+)
    │   │   ├── service/                     # Application logic (Phase 5+)
    │   │   │   ├── ProfitCalculationService.java    # Strategy dispatch (Phase 5)
    │   │   │   └── calculator/             # Profit strategies (Phase 5)
    │   │   ├── domain/                      # Records / enums (Phase 2)
    │   │   └── exception/                   # Error handling (Phase 7)
    │   └── resources/
    │       ├── application.yml              # Main configuration
    │       ├── application-dev.yml          # Dev profile
    │       ├── bonuses.json                 # Static city bonuses
    │       └── recipes/                     # Static recipe JSON files
    └── test/java/de/am/albion/tradecalc/    # Tests (mirrors main packages)
```

---

## Implementation Progress

| Phase | Content | Status |
|---|---|:---:|
| 0 | Verify project setup | ✅ |
| 1 | Foundation (config, properties, actuator, cache) | ✅ |
| 2 | Static data (domain records, JSON recipes, CityBonusRepository) | ✅ |
| 3 | External API (AlbionDataApiClient + MarketJsonMapper) | ✅ |
| 4 | Refresh & Actuator stats | ✅ |
| 5 | Core calculation (ProfitCalculationService — Strategy pattern) | ✅ |
| 6 | Crafting plan (CraftingPlanService) | ⏳ |
| 7 | REST-API for calculation | ⏳ |
| 8 | Web UI (Thymeleaf + HTMX) | ⏳ |
| 9 | Polish & Quality | ⏳ |

---

## Project Concept

`trade-calc` implements a profit calculator for crafted items in **Albion Online** (Europe
server). The full concept, glossary, calculation logic, data sources, user-interface
spec, package structure, and phased roadmap are documented below.

### Glossary

| Term | Meaning |
|---|---|
| **Item** | A tradeable object (e.g. `T4_BOW`, `T4_PLANK`, `T4_WOOD`). |
| **Tier** | Rarity tier. `T4` = Excellent, `T8` = Epic+. App focuses on **T4** … **T8**. |
| **Recipe** | Bill of materials: list of materials that produce one item. |
| **Refining** | Raw material → intermediate product (e.g. `WOOD` → `PLANK`). |
| **Crafting** | Material(s) → final product (e.g. `PLANK` → `BOW`). |
| **Royal City** | One of the six main cities: Martlock, Fort Sterling, Lymhurst, Bridgewatch, Thetford, Caerleon. |
| **Outland** | `Brecilien` — no bonuses, but access to the black market. |
| **Bonus** | Percentage discount on crafting / refining costs in a city. |
| **buyPriceMax** | Highest active buy-order — relevant when *buying* materials. |
| **sellPriceMin** | Lowest active sell-order — relevant when *selling* the final product. |
| **Profit** | `Sale revenue − Total cost`. |
| **ROI** | Return on Investment in percent: `Profit / Total cost × 100`. |
| **Mode (a)/(b)/(c)** | The three calculation strategies (see [Calculation Modes](#calculation-modes)). |

### Calculation Modes

| Mode | Short Name | Buy Material | Refine | Craft & Sell |
|---:|---|---|---|---|
| (a) | `LOCAL_ONLY` | In the user-selected city | In the user-selected city | In the user-selected city |
| (b) | `LOCAL_BUY_BEST_REST` | In the user-selected city | In the city with the highest refining bonus | In the city with the highest crafting bonus |
| (c) | `BEST_OF_ALL` | In the city with the **lowest price** | In the city with the highest refining bonus | In the city with the highest crafting bonus |

> **Note:** Mode (a) only helps if the chosen city offers *both* bonuses (refining +
> crafting) for the relevant categories. Otherwise the bonuses drop out and the calculation
> becomes inefficient.

### Calculation Logic

Given `selectedCity`, `mode`, `recipe`, `marketPrices[]`:

1. **Material cost** (mode-dependent):
   - `(a)` / `(b)`: `buyPriceMax(material, selectedCity)`
   - `(c)`: `min over all cities of buyPriceMax(material, city)`
2. **Refining city** (mode-dependent):
   - `(a)`: `selectedCity` (no bonus if no refining bonus exists there)
   - `(b)` / `(c)`: city with the highest refining bonus for `material.category`
3. **Crafting city** (mode-dependent):
   - `(a)`: `selectedCity` (no bonus if no crafting bonus exists there)
   - `(b)` / `(c)`: city with the highest crafting bonus for `result.category`
4. **Sell price:** `sellPriceMin(result, craftCity)`
5. **Profit:**
   ```
   materialCostEffective = materialCost × (1 − refiningBonus)
   craftCostEffective    = (sum(materialCostEffective) + craftingFee) × (1 − craftingBonus)
   totalCost             = craftCostEffective
   profit                = sellPrice − totalCost
   roi                   = profit / totalCost × 100
   ```
6. **Sort:** all items descending by `ROI`.
7. **CraftingPlan:** generate a step-by-step instruction for the UI.

### Data Sources

| Source | Content | Access |
|---|---|---|
| `albion-online-data.com` (Europe) | Live market prices (`buyPriceMax`, `sellPriceMin`) | REST, cached via Caffeine |
| `resources/bonuses.json` | City bonuses (static) | Classpath |
| `resources/recipes/*.json` | Recipes (static) | Classpath |

API documentation: <https://www.albion-online-data.com/api/>

### Package Structure

Classic Spring Boot layered architecture (hexagonal-light):

```
de.am.albion.tradecalc
├── TradeCalcApplication.java
│
├── config/                            # Spring configuration
│   ├── AlbionApiProperties.java        # @ConfigurationProperties("albion.api")
│   └── CacheProperties.java            # @ConfigurationProperties("cache.market")
│
├── api/                               # REST endpoints
│   └── PriceController.java            # GET /api/v1/prices/{itemId}
│
├── ui/                                # Web UI (Thymeleaf) — Phase 8
│   ├── controller/
│   └── dto/
│
├── dataprovider/                      # Adapter for external sources
│   ├── albion/
│   │   ├── AlbionDataApiClient.java    # RestClient against albion-online-data.com
│   │   ├── dto/                        # API records
│   │   └── mapper/                     # API → Domain
│   └── recipe/
│       ├── RecipeLoader.java           # Reads resources/recipes/*.json
│       ├── RecipeJsonMapper.java
│       └── RecipeLoadException.java
│
├── repository/                        # In-memory indexes + cache wrappers
│   └── CityBonusRepository.java        # Loaded once from bonuses.json
│
├── service/                           # Application logic
│   ├── PriceService.java               # Market data
│   ├── RecipeService.java              # Recipe lookup
│   ├── ProfitCalculationService.java   # Strategy dispatch (Phase 5)
│   └── calculator/                     # Profit strategies (Phase 5)
│       ├── ProfitStrategy.java         # Interface — one impl per CalculationMode
│       ├── PriceLookup.java            # Read-only view on PriceService
│       ├── CityBonusProvider.java      # Read-only view on CityBonusRepository
│       ├── AbstractProfitStrategy.java  # Shared arithmetic
│       ├── LocalOnlyStrategy.java       # Mode (a)
│       ├── LocalBuyBestRestStrategy.java   # Mode (b)
│       └── BestOfAllStrategy.java      # Mode (c)
│
└── domain/                            # Framework-agnostic records / POJOs
    ├── CalculationMode.java            # Enum
    └── model/
        ├── Item.java
        ├── City.java
        ├── CityBonus.java
        ├── Recipe.java
        ├── MarketPrice.java
        ├── ProfitResult.java
        ├── CraftingPlan.java
        ├── CraftingStep.java
        └── RecipeIngredient.java
```

**Rationale:**

- **`domain`** contains only POJOs / records without Spring dependencies — easy to mock,
  no coupling.
- **`service`** orchestrates domain logic.
- **`dataprovider`** is a clearly isolated adapter for the external API. A future switch
  to another source only changes this layer.
- **`ui`** (Thymeleaf) and **`api`** (REST) are separated, so a later SPA frontend (React/Vue)
  only needs the `api` layer.
- **`config`** holds external configuration (API URL, cache settings).
- **`domain/CalculationMode`** is a central enum so UI, API and service use the same values.

### Implementation Roadmap

The project is delivered in **10 phases**. Each phase produces a runnable vertical slice.

#### Phase 0 — Verify project setup
Confirm the skeleton is intact (`pom.xml`, `TradeCalcApplication`, package structure)
before writing any code. *Verify:* `mvn -v` shows Java 21+ and Maven 3.9+.

#### Phase 1 — Foundation
- `application.yml` with cache config and `management.endpoints.web.exposure.include=caches,metrics`
- `application-dev.yml` for verbose dev logs
- `TradeCalcApplication` with `@SpringBootApplication`, `@EnableCaching`, `@EnableScheduling`
- `config/AlbionApiProperties`, `config/CacheProperties`
- `pom.xml` adds `spring-boot-starter-cache`, `spring-boot-starter-actuator`

*Verify:* `mvn spring-boot:run` starts; `/actuator/health` returns `UP`.

#### Phase 2 — Static data
- `domain/CalculationMode` enum
- `domain/model/*` records (`Item`, `City`, `CityBonus`, `Recipe`, `MarketPrice`,
  `ProfitResult`, `CraftingPlan`, `CraftingStep`, `RecipeIngredient`)
- `resources/bonuses.json` (7 cities) and `resources/recipes/` (5–10 example recipes)
- `dataprovider/recipe/RecipeLoader` (cached via `@Cacheable("recipes")`)
- `repository/CityBonusRepository` (in-memory index)

*Verify:* unit test loads 3 recipes and asserts the indexes are consistent.

#### Phase 3 — External API
- `dataprovider/albion/AlbionDataApiClient` (`RestClient` against `albion-online-data.com`)
- `dataprovider/albion/dto/MarketJson`
- `dataprovider/albion/mapper/MarketJsonMapper`
- `service/PriceService` caches `MarketPrice` per item under `marketPrices`
- `api/PriceController` with `GET /api/v1/prices/{itemId}`

*Verify:* live call against the real API (e.g. `T4_WOOD`) lands in the cache; a second
call returns in single-digit ms.

#### Phase 4 — Refresh & Actuator
- `api/RefreshController` (`POST /api/v1/refresh` clears the `marketPrices` cache and
  calls `PriceService.warmUp()`)
- `config/CacheConfig` with `CaffeineCacheManager` bean

*Verify:* `/actuator/caches/marketPrices` shows size and hit rate.

#### Phase 5 — Core calculation ✅
- `service/ProfitCalculationService` dispatches per `CalculationMode` via an
  `EnumMap` populated from the Spring-injected `ProfitStrategy` beans.
- One `@Component` per mode in `service/calculator/`: `LocalOnlyStrategy`,
  `LocalBuyBestRestStrategy`, `BestOfAllStrategy`. They share the arithmetic
  in `AbstractProfitStrategy` and only decide which cities to use for buying
  and crafting.
- Adding a new mode = adding a new strategy class — no changes to the service.
- `PriceServiceAdapter` and `CityBonusRepositoryAdapter` are the only places
  that know about Spring wiring on the calculator side.
- `ProfitStrategy`, `PriceLookup`, `CityBonusProvider` are pure interfaces so
  the strategies are unit-testable without any DI context.

*Verify:* `mvn test` runs all strategy unit tests with deterministic prices
against a builder-driven stub. Adding a fourth mode without registering a
strategy fails fast at service construction.

**Worked example (`T4_BOW`, mode `BEST_OF_ALL`, fixed prices):**

| Step | Value |
|---|---|
| Materials | `T4_PLANK × 8` |
| Buy price (`FortSterling`) | 720 |
| Refining bonus (`FortSterling`, Holz) | 9.8 % |
| Effective unit cost | 720 × (1 − 0.098) = 649.44 |
| Material cost | 649.44 × 8 = 5 195.52 |
| Crafting bonus (`Martlock`, Bögen, Stoff, Möbel) | 25 % |
| `totalCost` | 5 195.52 × (1 − 0.25) = 3 896.64 |
| Sell price (`Martlock`) | 18 450 |
| `profit` | 18 450 − 3 896.64 = 14 553.36 |
| `profitRatio` | 14 553.36 / 3 896.64 ≈ 3.7348 |

> **Known simplification:** the refining bonus is currently applied at recipe
> level (`recipe.refiningCategory()`) for every material. The real game carries
> a refining category *per material*; the upgrade path is to thread the
> category into `RecipeIngredient`.

#### Phase 6 — Crafting plan
- `service/CraftingPlanService` iterates the recipe tree and produces
  `BuyStep` / `RefineStep` / `CraftStep` / `SellStep`
- Tests with mock `MarketPrice`, asserting order and cities

*Verify:* for `T4_BOW` (c) the plan returns "wood in Lymhurst → planks in Fort Sterling →
sell bow in Lymhurst".

#### Phase 7 — REST API for calculation
- `api/ProfitController` (`POST /api/v1/profit/calculate`, body: `CalculationRequestDto`)
- Request / response DTOs with `jakarta.validation`
- `GlobalExceptionHandler` for error responses

*Verify:* `curl -X POST` returns a JSON list of top items including `CraftingPlan`.

#### Phase 8 — Web UI
- `ui/controller/DashboardController` (`GET /` for city / mode selection)
- `ui/controller/ResultsController` (`GET /results` for the table)
- Thymeleaf templates (`dashboard.html`, `results.html`, `_plan-drawer.html`)
- Bootstrap 5 + HTMX for styling and dynamic updates

*Verify:* browser flow — pick city → pick mode → calculate → click row → see plan.

#### Phase 9 — Polish & quality
- WireMock for API in tests, Spring Boot Test for end-to-end
- Optional Resilience4j retry / circuit-breaker for API outages
- Bulk warm-up on start (scheduler) for only the items that appear in recipes
- Structured logs with MDC for request IDs
- This README with run instructions

*Verify:* app runs for 1 hour without memory leak; all tests green.

### Roadmap Ordering

- **Phase 0 first** — checks the skeleton (pom, application class, packages) before any code is added.
- **Bottom-up but functional** — data first (static), then external API, then calculation. After Phase 5 the logic is already correct — even without UI.
- **Caching from the start** — `spring-boot-starter-cache` is enabled in Phase 1 so tests stay realistic.
- **Testability before UI** — `ProfitCalculationService` is testable without Spring (Phase 5).
- **Vertical slices** — every phase ships something visible or callable.

---

## Contributing

1. Pick a phase from the roadmap above or open a new issue.
2. Create a feature branch using the schema `feature/phase-X-short-name`.
3. Open a PR referencing the phase and any roadmap changes.

**Code conventions:**

- Use Java 21 features (records, pattern matching, `var`).
- Domain code (package `domain`) stays free of Spring annotations.
- Every new service ships with at least one unit test achieving ≥ 80 % branch coverage.
- New configuration options are bound via a `*Properties.java` class, not hard-coded.

---

## Further Reading

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) — License terms
- [Albion Online Data API](https://www.albion-online-data.com/api/) — Market data source
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/3.3.x/reference/) — Framework reference