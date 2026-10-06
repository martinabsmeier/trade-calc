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

import de.am.albion.tradecalc.domain.CalculationMode;
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
 * Tests for the {@link BestOfAllStrategy} (mode c): buy wherever the
 * material is cheapest and craft/sell wherever the crafting bonus is best.
 */
class BestOfAllStrategyTest {

    /**
     * The cheapest city for the material wins the buy, and the city with
     * the highest crafting bonus AND a sell price wins the craft/sell.
     */
    @Test
    void cheapestBuy_bestCraft() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen, Stoff, Möbel", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 8, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder()
                // FortSterling is 9.8% cheaper to refine, Martlock is best to craft.
                .bonus("FortSterling", "Holz", new BigDecimal("0.098"))
                .bonus("Martlock", "Bögen, Stoff, Möbel", new BigDecimal("0.250"))
                .bonus("Lymhurst", "Holz", new BigDecimal("0.000"))
                .bonus("Lymhurst", "Bögen, Stoff, Möbel", new BigDecimal("0.000"))
                .bestCityFor("Bögen, Stoff, Möbel", "Martlock")
                .fallback("Lymhurst")
                .build();
        // FortSterling offers the cheapest buy price for the plank.
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "FortSterling", "720", null))
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "780", null))
                .city("T4_BOW", price("T4_BOW", "Martlock", null, "18450"))
                .build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isPresent();
        ProfitResult r = result.get();
        assertThat(r.city()).isEqualTo("Martlock");
        // per-unit 720 × (1−0.098) = 649.440 ; ×8 = 5195.52 ; ×0.75 = 3896.64
        assertThat(r.totalCost()).isEqualByComparingTo("3896.64");
        assertThat(r.revenue()).isEqualByComparingTo("18450.00");
        assertThat(r.profit()).isEqualByComparingTo("14553.36");
    }

    /**
     * If no city offers a sell price, the result is empty even when the
     * crafting bonus is highest there.
     */
    @Test
    void noSellPriceAnywhere_returnsEmpty() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 1, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "100", null))
                .build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * If the material cannot be bought anywhere, the result is empty.
     */
    @Test
    void noBuyPriceAnywhere_returnsEmpty() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 1, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();
        PriceLookup prices = StubPriceLookup.builder().build();

        Optional<ProfitResult> result = new BestOfAllStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * The strategy reports its mode as {@link CalculationMode#BEST_OF_ALL}.
     */
    @Test
    void mode_isBestOfAll() {
        ProfitStrategy strategy = new BestOfAllStrategy(StubCityBonusProvider.builder().build());

        assertThat(strategy.mode()).isEqualTo(CalculationMode.BEST_OF_ALL);
    }

    private static MarketPrice price(String item, String city, String buy, String sell) {
        return new MarketPrice(item, city, 1,
                buy == null ? null : new BigDecimal(buy),
                sell == null ? null : new BigDecimal(sell),
                Instant.EPOCH);
    }
}