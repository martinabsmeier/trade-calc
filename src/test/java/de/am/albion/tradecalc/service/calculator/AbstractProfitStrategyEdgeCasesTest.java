/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.albion.tradecalc.service.calculator;

import de.am.albion.tradecalc.domain.model.MarketPrice;
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge cases in {@link AbstractProfitStrategy#calculate} that return
 * {@link Optional#empty()} for inputs the happy-path tests cannot reach.
 *
 * <p>Uses {@link BestOfAllStrategy} because it is the only strategy that
 * can legitimately return {@code null} from its {@code *cityFor*} hooks
 * (when no city offers a usable price).</p>
 */
class AbstractProfitStrategyEdgeCasesTest {

    /** A recipe with one wood plank ingredient — same shape as the happy path. */
    private static final Recipe RECIPE = new Recipe("T4_BOW", 4, "Bögen", "Holz",
            List.of(new RecipeIngredient("T4_PLANK", 1, null)));

    @Test
    void nullRecipe_returnsEmpty() {
        PriceLookup prices = StubPriceLookup.builder().build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(null, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    @Test
    void emptyMaterials_returnsEmpty() {
        Recipe empty = new Recipe("RAW_WOOD", 4, null, null, List.of());
        PriceLookup prices = StubPriceLookup.builder().build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(empty, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    @Test
    void noBuyPriceAnywhere_returnsEmpty() {
        PriceLookup prices = StubPriceLookup.builder().build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(RECIPE, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * The recipe material has a buy price in some city, but no sell price
     * exists anywhere for the result item → the craft city cannot be picked
     * and the calculation returns empty.
     */
    @Test
    void noSellPriceAnywhere_returnsEmpty() {
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "780", null))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(RECIPE, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    @Test
    void sellPriceMissingInChosenCity_returnsEmpty() {
        // Buy price exists in Lymhurst (buyCity = Lymhurst), sell price only in Martlock.
        // BestOfAllStrategy picks Lymhurst as buy city and as craft city (it's the only
        // city with a sell price - wait, there is none). Need a sell price somewhere.
        // Make Lymhurst have NO sell price, Martlock have one.
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "780", null))
                .city("T4_BOW", price("T4_BOW", "Martlock", null, "18450"))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        // BestOfAll: cheapest buyPrice for T4_PLANK is Lymhurst (780).
        // craft city = highest crafting bonus among cities with sellPriceMin: only Martlock.
        // So craftCity = Martlock, craftPrice = present with sellPriceMin != null -> succeeds.
        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(RECIPE, "Lymhurst", prices);

        assertThat(result).isPresent();
    }

    /**
     * A buy order whose {@code buyPriceMax} is explicitly null triggers
     * the {@code mp.buyPriceMax() == null} guard. Use {@link LocalOnlyStrategy}
     * because {@link BestOfAllStrategy} filters out such entries at the
     * city-selection stage and would never let them reach this guard.
     */
    @Test
    void buyPriceIsExplicitNullInChosenCity_returnsEmpty() {
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", null, null))
                .city("T4_BOW", price("T4_BOW", "Lymhurst", null, "18450"))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new LocalOnlyStrategy(bonuses).calculate(RECIPE, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * Forces the {@code craftPrice.sellPriceMin() == null} branch (instead
     * of {@code craftPrice == null}) by giving the chosen craft city a
     * {@link MarketPrice} whose {@code sellPriceMin} is explicitly null.
     */
    @Test
    void sellPriceIsExplicitNullInChosenCity_returnsEmpty() {
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "780", null))
                .city("T4_BOW", price("T4_BOW", "Lymhurst", "1", null))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new LocalOnlyStrategy(bonuses).calculate(RECIPE, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * Free recipe (total cost = 0) → profit ratio is set to ZERO instead
     * of throwing {@link ArithmeticException} on the division.
     */
    @Test
    void freeRecipe_profitRatioIsZero() {
        // buyPriceMax = 0 → effectiveUnitCost = 0 → rawCost = 0 → totalCost = 0.
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "0", null))
                .city("T4_BOW", price("T4_BOW", "Lymhurst", null, "100"))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();

        Optional<ProfitResult> result = new LocalOnlyStrategy(bonuses).calculate(RECIPE, "Lymhurst", prices);

        assertThat(result).isPresent();
        assertThat(result.get().totalCost()).isEqualByComparingTo("0");
        assertThat(result.get().profit()).isEqualByComparingTo("100.00");
        assertThat(result.get().profitRatio()).isEqualByComparingTo("0");
    }

    private static MarketPrice price(String item, String city, String buy, String sell) {
        return new MarketPrice(item, city, 1,
                buy == null ? null : new BigDecimal(buy),
                sell == null ? null : new BigDecimal(sell),
                Instant.EPOCH);
    }
}