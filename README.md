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

For the full project concept, glossary and calculation modes, see the
[Project Concept](#project-concept) section below.

**Highlights:**

- **Three calculation modes** — from "home city only" to "optimal city per stage".
- **Live market data** — cached via Caffeine (15 min TTL, force-refresh via REST).
- **Static configuration** — bonuses and recipes loaded from JSON files on the classpath.
- **Web UI** — Thymeleaf + HTMX dashboard with a lazy-loaded plan drawer.
- **REST API + observability** — `POST /api/v1/profit/calculate`, request-id filter
  with MDC, exponential-backoff retry on upstream outages, recipe-driven warmup
  on startup.

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

> Current unit-test count: **102** unit tests across **24** `*Test` classes,
> plus **19** `*IT` integration tests across **6** `*IT` classes. JaCoCo
> enforces ≥ 80 % branch and ≥ 70 % line coverage on the `BUNDLE`.

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
    │   │   ├── api/                         # REST endpoints
    │   │   ├── dataprovider/                # API adapter
    │   │   ├── repository/                  # In-memory indexes
    │   │   ├── service/                     # Application logic
    │   │   │   ├── PriceService.java                # Market data
    │   │   │   ├── RecipeService.java               # Recipe lookup
    │   │   │   ├── ProfitCalculationService.java    # Strategy dispatch
    │   │   │   ├── CraftingPlanService.java         # Step-by-step plan
    │   │   │   ├── PriceServiceAdapter.java         # PriceLookup adapter
    │   │   │   └── calculator/             # Profit strategies
    │   │   └── domain/                      # Records / enums
    │   └── resources/
    │       ├── application.yml              # Main configuration
    │       ├── application-dev.yml          # Dev profile
    │       ├── bonuses.json                 # Static city bonuses
    │       └── recipes/                     # Static recipe JSON files
    └── test/java/de/am/albion/tradecalc/    # Tests (mirrors main packages)
```

---

## Project Concept

`trade-calc` implements a profit calculator for crafted items in **Albion Online** (Europe
server). The full concept, glossary, calculation logic, data sources and package
structure are documented below.

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
   effectiveUnitCost(material) = buyPriceMax(material, buyCity) × (1 − refiningBonus(buyCity, recipe.refiningCategory))
   materialCost                = Σ effectiveUnitCost(material) × quantity
   totalCost                   = materialCost × (1 − craftingBonus(craftCity, recipe.craftingCategory))
   profit                      = sellPrice − totalCost
   roi                         = profit / totalCost × 100
   ```

   > **Known simplification:** the refining bonus is currently read from
> `recipe.refiningCategory()` for every material (the recipe-level category).
> The real game carries a refining category *per material*; the upgrade path
> is to thread the category into `RecipeIngredient` and use it instead.
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
├── ui/                                # Web UI (Thymeleaf)
│   ├── controller/
│   │   └── DashboardController.java    # GET/POST / + GET /results + GET /plan
│   └── dto/
│       ├── DashboardForm.java          # Form backing bean
│       └── ResultsView.java            # Results page model
│   ├── controller/                     # planned
│   └── dto/                            # planned
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
│   ├── CityBonusRepository.java        # Loaded once from bonuses.json
│   └── CityBonusRepositoryAdapter.java # CityBonusProvider for calculator package
│
├── observability/                     # Cross-cutting concerns
│   ├── MdcKeys.java                   # requestId key
│   ├── RequestIdFilter.java             # OncePerRequestFilter — X-Request-Id + MDC
│   └── MarketPriceWarmupService.java  # @EventListener(ApplicationReadyEvent)
│
├── service/                           # Application logic
│   ├── PriceService.java               # Market data
│   ├── PriceServiceAdapter.java        # PriceLookup adapter for calculator
│   ├── RecipeService.java              # Recipe lookup
│   ├── ProfitCalculationService.java   # Strategy dispatch
│   ├── ProfitQueryService.java         # Ranked top-N query
│   ├── CraftingPlanService.java        # Step-by-step plan
│   └── calculator/                     # Profit strategies
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

## Features

| Feature | Where | What it gives you |
|---|---|---|
| **Config layer** | `config/AlbionApiProperties`, `config/CacheProperties` | Externalised config via `@ConfigurationProperties`; no hard-coded URLs or magic strings. |
| **Domain records** | `domain/model/*` (`Item`, `City`, `CityBonus`, `Recipe`, `MarketPrice`, `ProfitResult`, `CraftingPlan`, `CraftingStep`, `RecipeIngredient`) | Framework-agnostic value types — no Spring annotations on this package. |
| **Static data** | `resources/bonuses.json`, `resources/recipes/index.json` | Royal City bonuses and crafting recipes, loaded once into a `final` field. |
| **Live market data** | `dataprovider/albion/AlbionDataApiClient`, `service/PriceService` | `RestClient` against `albion-online-data.com` (v2 stats/prices endpoint, west shard), with hand-rolled exponential-backoff retry on transient failures. Per-item caching with 15 min TTL. |
| **Profit calculation** | `service/ProfitCalculationService`, `service/calculator/*Strategy` | Three calculation modes (`LOCAL_ONLY`, `LOCAL_BUY_BEST_REST`, `BEST_OF_ALL`) dispatched via an `EnumMap` of Spring-injected strategies. Strategies are unit-testable without Spring. |
| **Crafting plan** | `service/CraftingPlanService` | Walks the recipe tree bottom-up; emits `BUY` / `REFINE` / `CRAFT` / `SELL` steps that agree with the `ProfitResult`'s cities. |
| **REST API** | `api/PriceController`, `api/ProfitController`, `api/dto/*`, `api/GlobalExceptionHandler` | `GET /api/v1/prices/{itemId}` and `POST /api/v1/profit/calculate` with validated DTOs and a `@RestControllerAdvice` for error responses. |
| **Web UI** | `ui/controller/DashboardController`, `templates/*`, `static/css/site.css` | Thymeleaf + HTMX dashboard. Inline HTMX on the results table with a vanilla-JS fetch backup. |
| **Caching** | `application.yml` (`cache-names: marketPrices, recipes, cityBonuses`), `@Cacheable` on the relevant services | Caffeine via Spring Cache abstraction; `/actuator/caches/{name}` exposes size, hit rate and evictions. |
| **Observability** | `observability/MdcKeys`, `RequestIdFilter`, `MarketPriceWarmupService`, `logback-spring.xml` | `X-Request-Id` honoured and echoed, MDC propagation into logs, startup-time warmup of market prices for every recipe item. |
| **CI** | `.github/workflows/build.yml` | `mvn -B verify` on Temurin JDK 21 for every push and PR to `main`; JaCoCo report uploaded as a build artifact. |

### Test Counters

The project ships with **102** unit tests across **24** `*Test` classes, plus
**19** `*IT` integration tests across **6** `*IT` classes. JaCoCo gates enforce
≥ 80 % branch and ≥ 70 % line coverage on the `BUNDLE`.

## Contributing

1. Pick a feature from the table above or open a new issue.
2. Create a feature branch using the schema `feature/<short-name>`.
3. Open a PR referencing the feature.

**Code conventions:**

- Use Java 21 features (records, pattern matching, `var`).
- Domain code (package `domain`) stays free of Spring annotations.
- Every new service ships with at least one unit test achieving ≥ 80 % branch coverage.
- New configuration options are bound via a `*Properties.java` class, not hard-coded.

---

## Further Reading

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) — License terms
- [Albion Online Data API](https://www.albion-online-data.com/api/) — Market data source
- [Spring Boot Docs](https://docs.spring.io/spring-boot/) — Framework reference
  (3.3.x is now served from the canonical `/spring-boot/` URL)
