# trade-calc

Profit calculator for crafted items in **Albion Online** (Europe server).

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-orange.svg)](https://maven.apache.org/)

---

## Overview

`trade-calc` is a Spring Boot application that determines the most profitable crafting route
for every craftable item in Albion Online. It takes the Royal City bonuses for refining and
crafting into account, along with the latest `buyPriceMax` and `sellPriceMin` values from
the Albion Online market (Europe server).

The project is currently in **Phase 1 of 10** of the implementation roadmap
(see [`./trade-calc.md`](./trade-calc.md#implementierungs-roadmap)).

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

For the detailed per-phase test strategy, see
[`./trade-calc.md#teststrategie`](./trade-calc.md#teststrategie).

---

## Project Structure

```
trade-calc/
├── pom.xml                          # Maven build
├── README.md                        # This file
├── trade-calc.md                    # Design doc (DMD) + 10-phase roadmap
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
    │   │   ├── domain/                      # Records / enums (Phase 2)
    │   │   └── exception/                   # Error handling (Phase 7)
    │   └── resources/
    │       ├── application.yml              # Main configuration
    │       └── application-dev.yml          # Dev profile
    └── test/java/de/am/albion/tradecalc/    # Tests (mirrors main packages)
```

---

## Implementation Progress

| Phase | Content | Status |
|---|---|:---:|
| 0 | Verify project setup | ✅ |
| 1 | Foundation (config, properties, actuator, cache) | ✅ |
| 2 | Static data (domain records, JSON recipes) | ✅ |
| 3 | External API (AlbionDataApiClient) | ✅ |
| 4 | Refresh & Actuator stats | ⏳ |
| 5 | Core calculation (ProfitCalculationService) | ⏳ |
| 6 | Crafting plan (CraftingPlanService) | ⏳ |
| 7 | REST-API for calculation | ⏳ |
| 8 | Web UI (Thymeleaf + HTMX) | ⏳ |
| 9 | Polish & Quality | ⏳ |

---

## Contributing

1. Pick a phase from the design doc or open a new issue.
2. Create a feature branch using the schema `feature/phase-X-short-name`.
3. Open a PR referencing the phase and any DMD changes.

**Code conventions:**

- Use Java 21 features (records, pattern matching, `var`).
- Domain code (package `domain`) stays free of Spring annotations.
- Every new service ships with at least one unit test achieving ≥ 80 % branch coverage.
- New configuration options are bound via a `*Properties.java` class, not hard-coded.

---

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
You are free to use, modify, and distribute the code under the terms of that license.
See the [`LICENSE`](LICENSE) file for the full text and copyright information.

---

## Further Reading

- [`./trade-calc.md`](./trade-calc.md) — Full project documentation (German design doc)
- [Albion Online Data API](https://www.albion-online-data.com/api/) — Market data source
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/3.3.x/reference/) — Framework reference