# trade-calc

A profit calculator for crafted items in **Albion Online**.

Pick a Royal City and a crafting category — the app fetches the 20 most-sold items
in that combination, applies each item's crafting chain against the city bonus,
and ranks them by expected profit using the 4-week average sell price and volume
from `albion-online-data.com` (Europe server).

## Status

| Phase | Topic                                | State      |
|-------|--------------------------------------|------------|
| 0     | Maven skeleton, Java 21, Spring Boot | done       |
| 1     | Recipe data ingestion                | done       |
| 2     | Domain model + JSON loader           | done       |
| 3     | Log4j2 wiring                        | done       |
| 4     | CI build (GitHub Actions)            | done       |
| 5     | Coverage gates (JaCoCo)              | done       |
| 6     | Crafting plan service                | in progress |
| 7     | REST endpoint for profit calculation | planned    |
| 8     | Thymeleaf / HTMX web UI              | planned    |
| 9     | Polish                               | planned    |

Current test counts: 7 unit tests in 3 `*Test` classes + 1 integration test in
`TradeCalcApplicationIT`.

## Build, test, run

Plain Maven — no extra linter step. JaCoCo enforces the coverage gates on `verify`.

```bash
mvn verify              # Unit tests + integration test + JaCoCo coverage gate
mvn test                # Unit tests only
mvn spring-boot:run     # App on http://localhost:8080
```

Run a single test class:

```bash
mvn -Dtest=RecipeServiceTest test
```

Coverage report: `target/site/jacoco/index.html` (regenerated on every `mvn verify`).
Coverage gates (fail the build): ≥ 80% branch and ≥ 70% line coverage on the BUNDLE.

## Architecture

Hexagonal-lite layering rooted at `de.am.albion.tradecalc`:

- **`domain/`** — `Recipe` and `RecipeIngredient` records plus the
  `CalculationMode` enum. Pure value types, no Spring annotations.
- **`dataprovider/recipe/`** — `RecipeLoader` reads `classpath:data/recipes.json` once
  at construction into a `final` field. No `@Cacheable` here.
- **`service/`** — `RecipeService` wraps the loader with a precomputed index and
  exposes `findByItemId` (annotated `@Cacheable("recipes")`).
- **`config/`** — `AlbionApiProperties` and `CacheProperties` (planned).

New configuration values go through a `*Properties` class bound via
`@ConfigurationProperties` — never hard-coded.

## Recipe data

`src/main/resources/data/recipes.json` (4 218 recipes, ≈ 2.1 MB) is produced by
`scripts/build_recipes.py` from the community-maintained
[`ao-data/ao-bin-dumps`](https://github.com/ao-data/ao-bin-dumps) dump. An item
appears in the file iff it has a non-empty `craftingrequirements.craftresource`
and is market-tradable.

The script is part of the CI pipeline and runs before `mvn verify`.

## Logging

Log4j 2 via `spring-boot-starter-log4j2` (the default Logback starter is
excluded). The configuration lives in `src/main/resources/log4j2.xml`.

## License

Apache 2.0 — see the per-file headers.
