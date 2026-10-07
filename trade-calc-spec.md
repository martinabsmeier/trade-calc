# trade-calc — Specification

Profit calculator for crafted items in **Albion Online**, **Europe**
shard.

The application computes the expected profit of turning raw materials
into items through the official refining and crafting recipe chain,
taking Royal City bonuses into account. It exposes:

1. A **ranked list** of every craftable item with expected profit
   (absolute and relative to material cost) — top-down view over
   finished items.
2. An optional **crafting plan** per item: which raw materials to buy
   in which city, where to refine, where to craft, where to sell.
3. A **material planner** (`/materials`) that, for a given home city,
   target material and target quantity, runs an **A/B comparison**:
   "all steps in home city" vs. "buy raw in home, refine and sell in
   the best-bonus city".

All monetary answers use live market prices (`buyPriceMax` for buying,
`sellPriceMin` for selling). All architectural decisions in this
document are final; there are no open questions.

---

## 1. Goals

For a user-selected starting point (home city, tier, calculation mode),
the application returns:

1. A **ranking** of every craftable item with expected profit
   (absolute and relative to material cost) — top-down, finished items.
2. On demand, a **crafting plan** for each item: which raw materials
   to buy in which city, where to refine, where to craft, where to
   sell.
3. A **material planner page** (`/materials`) — bottom-up: home city +
   material + target quantity → A/B comparison "all in home" vs.
   "buy raw in home, carry to best-bonus city, refine and sell there".
   Recipe chain is resolved down to the tier-2 base; the recipe tree
   itself is **not** displayed. See §6.2.1.

All answers use live market prices (`buyPriceMax` for purchase,
`sellPriceMin` for sale).

---

## 2. Data Sources

All configuration and market data are loaded from JSON files on the
classpath and from the Albion market endpoint. **No database.**

### 2.1 Static Data (Classpath)

| File | Content |
|---|---|
| `recipes/index.json` | All crafting and refining recipes. One recipe per output item. |
| `materials.json` | Mapping `itemId → bonus-category`. Source of truth for "category per material" (see §3.2.1). |
| `bonuses.json` | City bonuses, grouped by activity (Crafting, Refining) and category (e.g. `"Wood"`, `"Bows"`). Server-independent — the percentage values per city are identical on `west`, `europe` and `east`, so the shard choice in §2.2 is not reflected here. |

> Note: `recipes/` currently contains a single `index.json`. Splitting
> it into `crafting.json` and `refining.json` is a future refactoring
> option, **not** part of this specification.

### 2.2 Market Data (External)

Source: [`albion-online-data.com`](https://www.albion-online-data.com/api/).
The application calls **only** the current-prices endpoint
(`/api/v2/stats/prices/{items}`). History, charts, and gold endpoints
are intentionally unused — they are out of spec scope.

**Shard:** `europe` (Europe server) — hard-coded in
`AlbionApiProperties`, not user-configurable. Switching to `west` /
`east` via `application.yml` is possible but out of scope.

**Item IDs:** Albion format `T<Tier>_<ITEM>` (e.g. `T4_BAG`,
`T2_HOLZ`, `T5_BOW`). Multiple items are sent comma-separated in
**one** request to stay under the 4096-character URL limit.

**Sample request:**

```
GET https://europe.albion-online-data.com/api/v2/stats/prices/T4_BAG,T5_BOW.json
    ?locations=Caerleon,Bridgewatch,Martlock,Fort%20Sterling,Lymhurst,Thetford
    &qualities=1
Accept-Encoding: gzip
```

- `qualities=1` (Normal) is the default. Higher qualities are not
  requested, since the calculation model operates on Normal items.
- `Accept-Encoding: gzip` is mandatory for long-running services (per
  the API docs, "Server and bandwidth are not free").

**Response:** a JSON array, one entry per `item_id` × `city` ×
`quality`:

| Field | Type | Meaning |
|------|------|---------|
| `item_id` | string | Albion item ID, e.g. `T4_BAG` |
| `city` | string | City name, e.g. `Caerleon` |
| `quality` | int | `1` = Normal |
| `sell_price_min` | int | Lowest active sell-order price |
| `sell_price_max` | int | Highest active sell-order price |
| `buy_price_min` | int | Lowest active buy-order price |
| `buy_price_max` | int | Highest active buy-order price |
| `*_date` | ISO-8601 | Timestamp of the respective order |

Mapping is done by `MarketJson` / `MarketJsonMapper` (historically in
`src/main/java/.../dataprovider/albion/`).

**Sentinel values ("no market"):** When an item has no active orders
in a city, the API still returns an entry with price `0` and date
`0001-01-01T00:00:00`. The mapper **must** treat that as
`Optional.empty()` — **not** as a zero-silver order. The entry is
ignored for calculation; an item without an active market in a city
is excluded from city selection for that item.

The `*_date` fields are carried through but **not** used for
calculation — the cache (15 min TTL, see below) controls how stale a
price may be.

**What we do not use:**

- v1 endpoints (`/api/v1/...`) — not available on `west`; avoid.
- `/api/v2/stats/history/...` and `/api/v2/stats/charts/...` —
  historical time series are out of scope.
- `/api/v2/stats/gold` — gold price is out of scope.
- XML responses — JSON only.

**Rate limits (per API docs):**

- 180 requests / minute
- 300 requests / 5 minutes

The startup warmup bundles all item IDs into **one** request (as long
as the URL stays under 4096 characters) and falls back to multiple
batches on overflow. The retry path in the client (3 attempts,
exponential backoff) handles transient `ResourceAccessException`.

**Caching:** 15 min TTL, cache name `marketPrices`. Convention: cache
key is per `itemId`, value contains all cities. Invalidation is per
item, not per city.

**Item / city lookup sources (official, reference only):**

- Item IDs: [`ao-bin-dumps/formatted/items.json`](https://github.com/ao-data/ao-bin-dumps/blob/master/formatted/items.json)
- City IDs: [`ao-bin-dumps/formatted/world.json`](https://github.com/ao-data/ao-bin-dumps/blob/master/formatted/world.json)

These lists are **not** pulled into spec scope — the `City` enum hard-
coded in the application is sufficient.

---

## 3. Domain Model

### 3.1 Item

An **item** is identified by an Albion ID (`T5_BOW`, `T5_WOOD`, …).
Each item has a tier (2..8) and an enchantment level (`.0`, `.1`,
`.2`, `.3`). The specification covers **only `.0` (Normal) items** in
this build.

### 3.2 Recipe

A recipe describes how to produce `amount` units of an output item
from a list of materials. Example `T5_BOW`:

```json
{
  "itemId": "T5_BOW",
  "tier": 5,
  "amount": 1,
  "materials": [
    { "itemId": "T5_PLANK", "amount": 32 }
  ],
  "refiningCategory": "Bows, Cloth, Furniture"
}
```

Refining and crafting recipes share the same schema. The two
differences:

- For **refining** recipes, `amount` equals the charge size for that
  tier (e.g. 32 for tier 2..4, 4 for some higher tiers, 1 for tier
  8); the output is the next-tier refined resource.
- For **crafting** recipes, `amount` is the number of items produced
  per craft action (typically 1).

A recipe file can contain both refining and crafting recipes. The
presence of `refiningCategory` marks a recipe as **refining**; a
recipe without `refiningCategory` is **crafting**.

`refiningCategory` on the recipe is **redundant** with
`materials.json` (§3.2.1). It is kept for human readability and for
the load-time consistency check. The single source of truth for
"category of material X" is `materials.json`.

> **Albion reality: category per material, not per recipe.** In
> Albion the bonus category (e.g. `"Wood"`, `"Bows"`) is a property of
> the **material itself** (e.g. `T4_WOOD` → category `"Wood"`), not of
> the recipe that consumes the material. If the same material appears
> as an ingredient in several recipes, its category is identical in
> all of them. The model uses a per-material lookup
> (see §3.2.1).

#### 3.2.1 Material-Category Lookup

Bonus category is **not** read from recipes. It comes from a separate
classpath file `materials.json`:

```json
{
  "T2_WOOD":     "Wood",
  "T4_BAR":      "Ore, Metalbars, Weapons, Armor",
  "T5_CLOTH":    "Cloth",
  "T5_BOW":      "Bows, Cloth, Furniture"
}
```

- **Key:** Albion material ID, e.g. `T4_WOOD`.
- **Value:** Bonus category string. Multiple categories per material
  are possible (e.g. `T5_BOW` qualifies for `"Bows"` *and* `"Cloth"`
  *and* `"Furniture"`). In Albion the material receives the
  **highest** of the city bonuses across its categories; the
  "pick highest" rule lives in the service, the JSON stores the
  comma-separated list.
- **Loading:** once at startup by `MaterialCategoryRepository` (analog
  to `CityBonusRepository`), kept in an immutable map. No
  `@Cacheable` is needed because the map is already in memory.
- **Consistency check at load time:**
  - Every material ID that appears as an ingredient in any recipe
    **must** have an entry in `materials.json`. Missing entry →
    `IllegalStateException` at startup, build fails.
  - For every recipe whose `refiningCategory` is set, the value
    **must** match the category in `materials.json` for the recipe's
    output item. Mismatch → `IllegalStateException` at startup.
  - Every entry in `materials.json` should be reachable from at
    least one recipe. Unreachable entries are tolerated but logged
    as warnings (they may be added in advance for upcoming recipes).

**Migration note:** existing recipes with `refiningCategory` are kept
as-is — the value is just moved into `materials.json` (and stays on
the recipe for readability and the consistency check).

### 3.3 City Bonuses

Each city (Lymhurst, Fort Sterling, …) provides a bonus per
**activity** (`CRAFTING` or `REFINING`) and per **material category**.
The JSON structure mirrors `bonuses.json`. Bonuses are decimal
percentages (`0.367` = 36.7 %); precision matches the source file.

### 3.4 Calculation Modes

The user picks one of three modes. The modes differ **only** in how
cities are chosen; the arithmetic is identical.

| Mode | Buy | Refine | Craft | Sell |
|---|---|---|---|---|
| `LOCAL_ONLY` | home city | home city | home city | home city |
| `LOCAL_BUY_BEST_REST` | home city | best city | best city | best city |
| `BEST_OF_ALL` | best city | best city | best city | best city |

"Best city" means: the city with the highest bonus for the relevant
activity and material category, restricted to cities where the
required buy and sell prices are available (non-sentinel).

---

## 4. Calculation

> **Decided:** the application computes **per charge** (one charge =
> one refinement step; no iteration over residue; no input-price
> discount) and rounds the bonus intermediate result per charge
> with **`HALF_UP`**. The half-up rule (`0.5 → 1`) is tabulated in
> §4.2. Both decisions are final. Later verification against the
> official Albion source may adjust the rounding mode
> (`HALF_UP` vs. `floor`) — the **structure** (per-charge,
> output-bonus) does not change.

### 4.1 Bonus Semantics

The bonus semantics follows the Albion standard: **output bonus**.
A charge is refined **once**; the bonus increases the output of
**that** charge. There is no iteration over residue and no input-
price discount.

**Example — T2_HOLZ → … → T5_PLANK in Fort Sterling (bonus 36.7 %
on every plank-refining step), target output 32 T5 planks:**

```
T4_PLANK → T5_PLANK  charge = 2   bonus = 36.7 %
  ceil(32 / 3) = 11 charges   →   11 × 2 = 22 T4_PLANK in
                                 11 × 3 = 33 T5_PLANK out   (+1 surplus)

T3_PLANK → T4_PLANK  charge = 2   bonus = 36.7 %
  ceil(22 / 3) =  8 charges   →    8 × 2 = 16 T3_PLANK in
                                  8 × 3 = 24 T4_PLANK out   (+2 surplus)

T2_PLANK → T3_PLANK  charge = 2   bonus = 36.7 %
  ceil(16 / 3) =  6 charges   →    6 × 2 = 12 T2_PLANK in
                                  6 × 3 = 18 T3_PLANK out   (+2 surplus)

T2_WOOD  → T2_PLANK  charge = 32  bonus = 36.7 %
  ceil(12 / 32) =  1 charge    →    1 × 32 = 32 T2_WOOD  in
                                  1 × 44 = 44 T2_PLANK out   (+32 surplus)

Total: 32 T2_WOOD in  →  33 T5_PLANK + 2 T4 + 2 T3 + 32 T2 planks out
```

The 32 T2_WOOD is a single, full charge — the chain is
**charge-aligned at the base tier** by construction. Intermediate
surpluses (1 T5, 2 T4, 2 T3, 32 T2_PLANK) must be reported by the
calculator so the user can either sell them or roll them into the
next batch; they are **not** silently dropped.

**Crafting bonus (Lymhurst, "Bows" 24.8 %) on T5_BOW,
one charge (1 unit):**

```
1 bow × 24.8 % = 1.248 → rounded (HALF_UP) → 1 bonus bow
1 base bow + 1 bonus bow = 2 T5 bows per charge (instead of 1)
```

> **Historical note.** An earlier draft of this document contained a
> hand-computed example with iterative residue
> (`64 + 23 + 8 + 3 + 1 = 99`) and a value of `46.976 → 50` (instead
> of `46.976 → 47` under `HALF_UP`). That example was replaced by
> the correct per-charge output-bonus semantics above. The current
> computation is a **trade-calc design decision** (§4 banner) and
> may later be verified against the official source.

### 4.2 Rounding

`HALF_UP` rounding applied to the **per-charge bonus intermediate**
(not the final price). Final `profit` and `profitRatio` are
additionally scaled to two decimal places (silver granularity).

`HALF_UP` reference:

| Value | Rounded | Why |
|------|---------|-----|
| `11.744` | `12` | `0.744 > 0.5` |
| `8.441`  | `8`  | `0.441 < 0.5` |
| `2.936`  | `3`  | `0.936 > 0.5` |
| `1.101`  | `1`  | `0.101 < 0.5` |
| `0.367`  | `0`  | `0.367 < 0.5` |
| `0.5`    | `1`  | exactly on the boundary, `HALF_UP` rounds up |

### 4.3 Formula

For one **refining** step (output of one charge):

```
outputPerCharge   = round(chargeSize × refiningBonus) + chargeSize
requiredInput     = ceil(targetOutput / outputPerCharge) × chargeSize
```

For one **crafting** step (output of one charge), the crafting
bonus affects the output count:

```
craftingYieldFactor = 1 + craftingBonus            // e.g. 1.248 for 24.8 %
outputPerCraft      = round(amount × craftingBonus) + amount
                     // equivalent to: amount × craftingYieldFactor (rounded per charge)
```

The two cases are **distinct**:

- **Refining bonus** multiplies the *output* of a charge (more
  refined resource per charge). It does **not** affect the price
  paid for the input.
- **Crafting bonus** multiplies the *output* of a charge. The
  *additional* crafted items are normal, sellable items — therefore
  the **sell revenue** is multiplied by `craftingYieldFactor`. The
  *material cost* is **not** multiplied by the crafting yield factor
  (the input is the recipe's listed material, regardless of bonus).

> Earlier drafts of this document multiplied **both** the cost and
> the revenue by `craftingYieldFactor`. That was wrong: the cost of
> the recipe's input is paid at the input's market price, unaffected
> by the fact that the output is multiplied. The current formula
> applies the factor only to the revenue side.

**Profit and ratio (per item, per profit calculation):**

```
revenue         = sellPrice × outputPerCraft        // crafting bonus only
totalCost       = Σ (materialPrice_i × requiredInput_i)   // refining + crafting inputs
profit          = revenue − totalCost − usageFees
profitRatio     = profit / totalCost
```

Where:

- `usageFees` covers in-game usage costs (focus, nutrition, etc.).
  Set to **zero** in this build. Modelling it is a follow-up issue.
- All `BigDecimal` values are scaled to 2 decimal places (silver
  granularity) on the way out.

### 4.4 Recipe-Chain Resolution and Cycle Protection

The recipe chain is traversed **bottom-up** until the **tier-2 base**
is reached (e.g. T5_BOW → T5_PLANK → T4_PLANK → T3_PLANK → T2_PLANK
→ T2_HOLZ). Tier-2 gatherables have no recipe and terminate the
chain.

Cycle protection: a `visited` set of item IDs prevents infinite
recursion in the (theoretical) case of a cyclic recipe graph. The
current data has no cycles, but the guard is mandatory.

Per-charge, **no iterative residue processing**: each charge yields
exactly once, then the result is either sold or carried into the
next tier's refinement.

---

## 5. Program Structure

Package root: `de.am.albion.tradecalc`. Hexagonal-light layering:
`domain` → `service` → `api` / `ui` → `dataprovider` → `repository`.

| Layer | Content | Framework-free |
|---|---|---|
| `domain` | Records, enums | Yes — no Spring annotations |
| `service` | Application logic, caching | Spring |
| `service/calculator` | `ProfitStrategy` interface + 3 `@Component` implementations | Spring (DI), logic itself framework-free |
| `dataprovider/albion` | RestClient against the market endpoint | Spring |
| `dataprovider/recipe` | Loads `recipes/index.json` once into a `final` field | Spring |
| `repository` | Loads `bonuses.json` and `materials.json` once into `final` fields | Spring |
| `api` | REST endpoints | Spring Web |
| `ui` | Thymeleaf templates (under `resources/templates/`), HTMX fragments | Spring MVC + Thymeleaf |
| `observability` | Request-id filter, warmup listener, Logback MDC | Spring |

Caches (`marketPrices`, `recipes`, `cityBonuses`) are declared in
`application.yml` and activated by `@Cacheable` on the appropriate
service methods. **Do not** add `@Cacheable` to the loaders
themselves — they read once into a `final` field at construction.

---

## 6. API

### 6.1 REST

| Method | Path | Body / Query | Return |
|---|---|---|---|
| `GET` | `/api/v1/prices/{itemId}` | — | Current `MarketPrice` (one row per city, default quality 1) |
| `POST` | `/api/v1/profit/calculate` | `CalculationRequestDto` (mode, homeCity, topN) | `CalculationResponseDto` (list of `ProfitEntryDto` with optional `CraftingPlan`) |

`topN` limits the ranking length; default `50`, max `500`. Values
outside the range return `400`.

Global error handling via `@RestControllerAdvice`. `400` for
validation / type-mismatch errors, `404` for
`NoResourceFoundException` (e.g. `favicon.ico`), `422` for
unprocessable plan requests (no market in any city, missing recipe,
etc.), `500` only for genuine server errors.

### 6.2 Web UI

Thymeleaf + HTMX. Four routes:

- `GET /` — pick home city, tier, mode
- `GET /results` — ranked list (table)
- `GET /plan?itemId=…` — plan drawer fragment (lazy via HTMX; the
  fragment is a full HTML snippet that HTMX swaps into the drawer)
- `GET /materials` — material planner (A/B comparison, see §6.2.1)

#### 6.2.1 Material Planner (`/materials`)

**Use case:** the user picks a home city, a target material (e.g.
`T5_HOLZ` → planks), and a desired **end quantity** (e.g. `960`
T5_PLANK). The page runs an **A/B comparison** of two concrete
plans:

- **Plan A (Home):** every step in the chosen home city. Buy raw
  resources, refine through every tier (T2 → T3 → T4 → T5) in the
  home city, sell the final product in the home city.
- **Plan B (Best per step):** buy raw resources (T2) in the home
  city, then for **each** refinement tier, carry the input to the
  city with the highest refining bonus for that tier's category,
  refine there, and sell the output in the same city.

The user sees both plans **side by side**, with a one-line
recommendation at the top and a per-tier breakdown on demand.

**Sample request:**

```
GET /materials?city=Lymhurst&material=T5_PLANK&quantity=960
```

(`material` is the **final product** the user wants to end up with,
not the raw resource. For wood-to-plank chains, the input is
`T2_HOLZ`, but the parameter names the product the user wants.)

**Page layout (side by side):**

| Column | Plan A (Home) | Plan B (Best per step) |
|---|---|---|
| **Output** | 960 T5_PLANK | 960 T5_PLANK |
| **T2 raw needed** | (computed) | (computed) |
| **Refining path** | T2→T3→T4→T5 in `homeCity` | per-tier `bestCity[tier]` |
| **Total cost** | (computed) | (computed) |
| **Total revenue** | (computed) | (computed) |
| **Profit** | (computed) | (computed) |

**Recommendation block** (always visible, single line):

> "Plan B is **N silver** better (**+M %**). Recommended." — when
> Plan B wins by ≥ 1 % of Plan A.
>
> "Plans are within 1 %; either is fine." — when the difference is
> smaller.
>
> "Plan A is better by N silver." — when Plan A wins.

**Details block** (collapsed by default, expand on click): per-tier
table listing for each tier (T2→T3, T3→T4, T4→T5) the input amount,
output amount, processing city, cost, and revenue.

**Recipe resolution (down to base):**

The service walks the recipe chain iteratively down to the tier-2
base. Example for `T5_PLANK` (target 960 units):

1. **Tier 4 → 5:** recipe `T5_PLANK = 4 × T4_PLANK`. To produce
   960 T5_PLANK, the previous step must deliver at least
   `ceil(960 / outputPerCharge_T5) × chargeSize_T5` T4_PLANK.
2. **Tier 3 → 4:** recipe `T4_PLANK = 4 × T3_PLANK`. Same formula.
3. **Tier 2 → 3:** recipe `T3_PLANK = 4 × T2_PLANK`. Same formula.
4. **Base reached:** `T2_PLANK` is the bottom of the chain;
   `T2_HOLZ` is a gatherable with no recipe.

The per-tier helper functions (`computeYield`,
`requiredInputFor`) live in `CraftingPlanService` (Phase 7) and
are reused here — not duplicated.

**`ceil` on the input is mandatory** so the actual output is at
least the requested quantity. The `CraftingPlanService` already
implements this; the planner delegates.

**Recipe tree is not shown.** Each plan lists only the per-tier
end-state numbers, not the recursive walk. That keeps the
comparison compact.

**Calculation of the two plans (single service method):**

```
# Plan A (home city for every step)
for each tier in (target_tier, target_tier-1, …, 3):
    inputAmount    = requiredInputFor(targetOutput, outputPerCharge(homeCity, tier-1 → tier))
    cost_tier      = buyPriceMax(homeCity, inputItem) × inputAmount
    outputAmount   = effective output of the step (may exceed target)
    revenue_tier   = sellPriceMin(homeCity, outputItem) × outputAmount
    totalCost_A   += cost_tier
    totalRevenue_A+= revenue_tier
profitA = totalRevenue_A - totalCost_A

# Plan B (best city per tier)
for each tier in (target_tier, target_tier-1, …, 3):
    bestCity = argmax over Royal Cities of refiningBonus(city, materialCategory)
    inputAmount    = requiredInputFor(targetOutput, outputPerCharge(bestCity, tier-1 → tier))
    cost_tier      = buyPriceMax(homeCity, inputItem) × inputAmount
    outputAmount   = effective output of the step
    revenue_tier   = sellPriceMin(bestCity, outputItem) × outputAmount
    totalCost_B   += cost_tier
    totalRevenue_B+= revenue_tier
profitB = totalRevenue_B - totalCost_B
```

- Raw materials (tier-2 gatherables) are **always bought in the home
  city** in both plans, since the user can only buy in their home
  city without a transport cost model.
- The per-tier `bestCity` may differ across tiers (e.g. wood is best
  in Fort Sterling, but the tier-3 step might be best in a different
  city for the corresponding material).

**Special cases:**

- **Multiple categories** per material (e.g. `T5_BOW` = `"Bows, Cloth,
  Furniture"`): per city, pick the **highest** of the matching bonuses
  (§3.2.1). The city that supplied the winning bonus is reported per
  step in the details block.
- **Sentinel `0` / `0001-01-01`** (see §2.2): the affected city is
  hidden from selection for that step. If every city is missing for
  a step, the plan is invalid → `422` with the failing step in the
  error body.
- **Crafting with bonus** (e.g. T5_BOW has a crafting bonus in
  addition to its refining bonuses): the crafting yield factor from
  §4.3 applies **only to the revenue** of the final crafting step.
  Material cost is **not** multiplied.
- **Usage fees / market tax** (Albion 2.5 %): not modelled in this
  build, consistent with `/results`. Follow-up issue.

**Data flow:**

`GET /materials?city=…&material=…&quantity=…` →
`MaterialsController.compare(city, material, quantity)` →
`MaterialPlannerService` (new) →
  • `RecipeService` for the recipe chain (cache)
  • `PriceService` for current prices (cache)
  • `CityBonusRepository` for bonuses (cache)
  • `MaterialCategoryRepository` (§3.2.1) for category lookup
→ `MaterialPlanComparisonDto` →
Thymeleaf template `materials.html`.

**Service contract:**

```java
public interface MaterialPlannerService {
    MaterialPlanComparison plan(City homeCity, String outputItemId, int quantity);
}

public record MaterialPlanComparison(
    String outputItemId,             // e.g. "T5_PLANK"
    int requestedQuantity,           // e.g. 960
    City homeCity,                   // input
    MaterialPlan planHome,           // Plan A
    MaterialPlan planBest,           // Plan B
    ComparisonSummary summary        // delta, recommendation
) {}

public record MaterialPlan(
    String label,                    // "Plan A: Home (Lymhurst)" / "Plan B: Best per step"
    List<PlanStep> steps,            // one entry per tier step
    BigDecimal totalCost,            // Σ buy
    BigDecimal totalRevenue,         // Σ sell
    BigDecimal profit                // totalRevenue - totalCost
) {}

public record PlanStep(
    int tier,                        // 2, 3, 4, 5 (the *output* tier of this step)
    String inputItemId,              // e.g. T2_HOLZ
    String outputItemId,             // e.g. T2_PLANK
    int requiredAmount,              // how many of inputItem must be bought
    int effectiveYield,              // how many of outputItem come out
    City processingCity,             // where the step is performed
    BigDecimal cost,                 // buy input × amount
    BigDecimal revenue               // sell output × effectiveYield
) {}

public record ComparisonSummary(
    BigDecimal profitDelta,          // B - A (positive = B is better)
    BigDecimal percentDelta,         // (B - A) / |A| × 100
    String recommendation            // "Plan B is 3,600 silver better (+50 %)."
) {}
```

**No new `CalculationMode`.** Plan A uses `LOCAL_ONLY` semantics
internally, Plan B uses `BEST_OF_ALL` semantics internally. The
modes are not user-selectable here.

**Out of scope:**

- End products with crafting chains across multiple distinct
  material types (e.g. a bow that needs planks *and* metal *and*
  cloth). That belongs in `/results`.
- Recipes with a chain depth greater than the target tier minus 2
  (e.g. complex tier-8 crafts). The current `CraftingPlanService`
  handles only linear refining chains.
- Drag-and-drop or sorting of the detail rows.

---

## 7. Tests

| Suffix | Framework | When |
|---|---|---|
| `*Test` | JUnit 5 + Mockito | Unit tests, no Spring context, fast (<100 ms) |
| `*IT` | `@SpringBootTest` + Failsafe | Integration tests, random port, `@ActiveProfiles("dev")` |

Mocking the external market API: `MockClientHttpRequest` /
`MockClientHttpResponse` over a custom `ClientHttpRequestFactory`.
**No** WireMock, **no** HTTP call to `albion-online-data.com` from
tests.

Coverage gates via JaCoCo on the `BUNDLE` aggregation:
≥ 80 % branch, ≥ 70 % line.

---

## 8. Observability

- **`RequestIdFilter`** (`OncePerRequestFilter`,
  `@Order(HIGHEST_PRECEDENCE)`): reads `X-Request-Id` from the
  request, puts it in MDC, echoes it on the response. If the
  header is missing, a UUID is generated. MDC is set and cleared
  in a `try` / `finally` to prevent thread-pool leakage.
- **`MarketPriceWarmupService`**
  (`@EventListener(ApplicationReadyEvent)`): walks the recipes
  once and primes the `marketPrices` cache. Individual failures
  are logged and swallowed — the warmup phase must not abort
  because of a single missing item.
- **Logback pattern** includes `[%X{requestId:-}]` so empty MDC
  renders no empty brackets.
- **`AlbionDataApiClient`** retry: exponential backoff on
  `ResourceAccessException` (3 attempts, 200 ms initial, doubling).
  After the third failure the exception bubbles up unchanged —
  the cache is **not** cleared automatically; the calling code
  decides. 4xx and 5xx responses are **not** retried; they bubble
  up immediately.

---

## 9. CI

GitHub Actions (`.github/workflows/build.yml`): on `push` and
`pull_request` against `main`. Temurin JDK 21, `mvn -B verify`.
JaCoCo report uploaded as an artifact.

---

## 10. Open Questions

None. The architectural decisions (cache strategy, retry, observability,
UI stack, bonus semantics, rounding mode `HALF_UP`, per-charge
mechanism, material category per material as Albion does — see §3.2.1)
are final. The `/materials` planner is defined as an **A/B comparison**
(Plan A home vs. Plan B best per step, see §6.2.1) and needs no
further clarification.
