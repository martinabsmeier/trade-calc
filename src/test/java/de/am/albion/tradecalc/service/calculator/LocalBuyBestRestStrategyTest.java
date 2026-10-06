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
 * Tests for the {@link LocalBuyBestRestStrategy} (mode b): buy locally,
 * but craft and sell wherever the crafting bonus is best.
 */
class LocalBuyBestRestStrategyTest {

    /**
     * Buy price is taken from the home city, but the craft / sell price
     * is taken from the city with the highest bonus for the crafting
     * category.
     */
    @Test
    void buyLocal_craftAtBestCity() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen, Stoff, Möbel", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 8, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder()
                .bonus("Lymhurst", "Holz", new BigDecimal("0.000"))
                .bonus("FortSterling", "Bögen, Stoff, Möbel", new BigDecimal("0.250"))
                .bestCityFor("Bögen, Stoff, Möbel", "FortSterling")
                .fallback("Lymhurst")
                .build();
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "780", null))
                .city("T4_BOW", price("T4_BOW", "FortSterling", null, "18450"))
                .build();

        Optional<ProfitResult> result = new LocalBuyBestRestStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isPresent();
        ProfitResult r = result.get();
        assertThat(r.itemId()).isEqualTo("T4_BOW");
        // craft city is the one with the highest crafting bonus, regardless of home city
        assertThat(r.city()).isEqualTo("FortSterling");
        // per-unit 780 ; ×8 = 6240 ; ×0.75 (FortSterling − 25% craft) = 4680
        assertThat(r.totalCost()).isEqualByComparingTo("4680.00");
        assertThat(r.revenue()).isEqualByComparingTo("18450.00");
        assertThat(r.profit()).isEqualByComparingTo("13770.00");
    }

    /**
     * If the material has no entry for the home city, no buy price can be
     * found locally and the result is empty.
     */
    @Test
    void missingBuyPriceInHomeCity_returnsEmpty() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 1, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();
        PriceLookup prices = StubPriceLookup.builder().build();

        Optional<ProfitResult> result = new LocalBuyBestRestStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * The strategy reports its mode as {@link CalculationMode#LOCAL_BUY_BEST_REST}.
     */
    @Test
    void mode_isLocalBuyBestRest() {
        ProfitStrategy strategy = new LocalBuyBestRestStrategy(StubCityBonusProvider.builder().build());

        assertThat(strategy.mode()).isEqualTo(CalculationMode.LOCAL_BUY_BEST_REST);
    }

    private static MarketPrice price(String item, String city, String buy, String sell) {
        return new MarketPrice(item, city, 1,
                buy == null ? null : new BigDecimal(buy),
                sell == null ? null : new BigDecimal(sell),
                Instant.EPOCH);
    }
}