# Copilot Instructions for `trade-calc`

A Spring Boot 3.3.5 (Java 21, Maven) profit calculator for crafted items in Albion Online.
This file captures the conventions and architecture that are hard to discover from a single file.

## Build, Test, and Coverage Commands

The project is plain Maven — there is no separate linter step, but JaCoCo enforces coverage gates.

```bash
mvn verify              # Full build: unit tests + integration tests + JaCoCo coverage report
mvn test                # Unit tests only (Surefire, *Test)
mvn spring-boot:run     # Start the app on http://localhost:8080 (default profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # Verbose logging, all actuator endpoints
```

**Running a single test class** (replace `Foo` with the test class name, omit `Test` suffix):

```bash
mvn -Dtest=FooTest test                                # Surefire (unit)
mvn -Dit.test=FooIT verify                             # Failsafe (integration)
mvn -Dtest=ProfitCalculationServiceTest#specific_method test   # One method
```

**Coverage report:** `target/site/jacoco/index.html` (regenerated on every `mvn verify`).
**Coverage gates (fail the build):** ≥ 80% branch, ≥ 70% line on the `BUNDLE`. Authoring new
logic without meeting them will break `verify` even when tests pass.

## High-Level Architecture

The code follows a hexagonal-light layered structure rooted at `de.am.albion.tradecalc`:

- **`domain/`** — `CalculationMode` enum plus pure records (`Recipe`, `MarketPrice`,
  `CityBonus`, `ProfitResult`, `CraftingPlan`, `CraftingStep`, `Item`, `City`,
  `RecipeIngredient`). **No Spring annotations live here.** Treat as framework-free
  value types.
- **`service/`** — `PriceService`, `RecipeService`, `ProfitCalculationService`. The
  services wire domain code to Spring and orchestrate caching.
- **`service/calculator/`** — Strategy pattern for the three `CalculationMode`s.
  - `ProfitStrategy` (interface) + `AbstractProfitStrategy` (shared arithmetic).
  - One `@Component` per mode: `LocalOnlyStrategy`, `LocalBuyBestRestStrategy`,
    `BestOfAllStrategy`. They only decide *which city* to buy/refine/craft in;
    `AbstractProfitStrategy.calculate` does the money math.
  - `PriceLookup` and `CityBonusProvider` are read-only views used by the strategies
    so they remain unit-testable without Spring DI.
- **`dataprovider/`** — Adapters to outside sources:
  - `albion/` — `AlbionDataApiClient` (RestClient against `albion-online-data.com`,
    `GET /api/v2/stats/prices/{items}` — **v2**, v1 is not served on the `west` shard),
    `MarketJson` DTO, `MarketJsonMapper`.
  - `recipe/` — `RecipeLoader` reads `classpath:recipes/index.json` once into a
    `final` field at construction (no `@Cacheable`, no `exists()` checks, no double-IO);
    per-item look-ups happen via `service/RecipeService.findByItemId` which is
    `@Cacheable("recipes")`.
- **`repository/`** — `CityBonusRepository` loads `classpath:bonuses.json` once
  (`final` field), then serves look-ups via `@Cacheable("cityBonuses")`.
- **`api/`** — REST controllers. Currently `PriceController` at
  `GET /api/v1/prices/{itemId}`. There is **no** `RefreshController` yet — Phase 7
  will add the calculation endpoint together with a force-refresh action.
- **`config/`** — `AlbionApiProperties` (`albion.api.*`) and `CacheProperties`
  (`cache.market.*`), both registered via `@EnableConfigurationProperties` on the
  application class.

The data flow for one calculation: `PriceController` → `PriceService` →
`AlbionDataApiClient` (market data, cached under `marketPrices`) and
`CityBonusRepository` (cached under `cityBonuses`), fed into
`ProfitCalculationService`, which dispatches to the right `ProfitStrategy` via an
`EnumMap<CalculationMode, ProfitStrategy>` populated at construction.
**Adding a new `CalculationMode` requires only adding the enum constant and a
new `ProfitStrategy` `@Component`** — the service throws at construction if the
two get out of sync. The `CraftingPlan` / `CraftingStep` domain records exist
already; the `CraftingPlanService` that consumes them is pending (Phase 7).

## Codebase-Specific Conventions

### Test split is by suffix, not by annotation

- `*Test.java` → unit test, picked up by `maven-surefire-plugin` (~100 ms, no Spring).
- `*IT.java` → integration test, picked up by `maven-failsafe-plugin` (~5 s,
  `@SpringBootTest`, real HTTP via random port, `@ActiveProfiles("dev")`).
- Mixing them is a build error: do not name an integration test `FooTest`.
- Current count: **98** `@Test` / `@ParameterizedTest` methods across **22**
  classes — update the README's quoted count whenever you add or remove tests.

### `RecipeLoader` ≠ `@Cacheable("recipes")`

`RecipeLoader` reads `classpath:recipes/index.json` once into a `final` field at
construction (no `@Cacheable`, no `exists()` check, no double-IO). The
`recipes` cache wraps `RecipeService.findByItemId`, not the loader. Adding a
new `@Cacheable` on the loader would silently double-read on startup.

### Strategies are stateless and unit-testable without Spring

`ProfitStrategy` is `interface`-only, `AbstractProfitStrategy` does the math, and the
concrete strategies only decide city selection. Tests use `StubPriceLookup` and
`StubCityBonusProvider` from `src/test/java/.../service/calculator/`. **Do not
inject Spring beans into the strategies** — extend the stub helpers and the
interfaces (`PriceLookup`, `CityBonusProvider`) instead. Adapters
(`PriceServiceAdapter`, `CityBonusRepositoryAdapter`) are the only layer that
knows about Spring wiring on the calculator side.

### `domain/` must stay Spring-free

`domain` records and the `CalculationMode` enum must not import anything from
`org.springframework.*`. Tests in `domain` should construct values directly with
the record canonical constructor — there is no DI there.

### HTTP upstream is never hit

Tests stub the upstream Albion API via a recording `MockClientHttpRequest` /
`MockClientHttpResponse` pair (see `AlbionDataApiClientTest`). Never call
`west.albion-online-data.com` from a test. Production wiring uses the public
endpoint per `application.yml`; v1 is intentionally avoided.

### Monetary math uses `BigDecimal` with `RoundingMode.HALF_UP`

`ProfitResult` money fields are scaled to 2 decimal places (silver granularity);
ratios use 4. The shared constant is `AbstractProfitStrategy.SCALE = 2`. Do not
introduce `double` for prices or bonuses.

### License header on every Java file

Every Java file starts with the Apache 2.0 header used throughout
(`Copyright 2026 Martin Absmeier.`). Keep it — license check tooling assumes it.

### Lombok is used widely but not everywhere

Records use canonical constructors, not `@Builder`. Service / repository classes
use Lombok (`@Slf4j`, `@Getter`, etc.). Look at the closest sibling file before
introducing a new annotation.

### Caches are declared in `application.yml`, not via `@Bean`

`cache-names: [marketPrices, recipes, cityBonuses]` is the source of truth. Adding
a new cache requires updating that list **and** ensuring the `@Cacheable` annotation
references the new name; otherwise the call is silently no-op'd.

### Externalised config goes through `@ConfigurationProperties`

The README states this explicitly: "New configuration options are bound via a
`*Properties.java` class, not hard-coded." Match this when introducing a new setting.

### Known simplifications (ponytail-style)

- **Refining bonus is applied at recipe level (`recipe.refiningCategory()`) for
  every material.** Real Albion carries a per-material refining category.
  Upgrade path is to thread the category into `RecipeIngredient`. Mark any new
  shortcut of this kind with a `ponytail:` comment naming the ceiling.
- Bonus and category strings in `bonuses.json` / `recipes/index.json` are in
  German (`Häute, Lederwaren`, `Holz`, `Bögen, Stoff, Möbel`, …). Match the
  casing and umlauts exactly — they are matched case-insensitively but the
  JSON files must stay readable by hand.

## Roadmap Status (from README)

Phases 0–5 are complete. **Phases 6–9 are still in progress**: crafting plan
service, REST endpoint for profit calculation, Thymeleaf/HTMX web UI, polish.
If you start a new feature that touches an unfinished area, skim the
"Project Concept" / "Implementation Roadmap" section in `README.md` first so
the work slots into the planned phases rather than inventing a parallel design.