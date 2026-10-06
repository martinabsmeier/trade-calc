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
 * Tests for the {@link LocalOnlyStrategy} (mode a): every step happens in the
 * home city, so the result depends only on prices and bonuses in that city.
 */
class LocalOnlyStrategyTest {

    /**
     * Recipe with one material. The strategy must use the home city for the
     * buy and the craft.
     */
    @Test
    void singleMaterial_homeCity_usedForBuyAndCraft() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen, Stoff, Möbel", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 8, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder()
                .bonus("Lymhurst", "Holz", new BigDecimal("0.098"))
                .bonus("Lymhurst", "Bögen, Stoff, Möbel", new BigDecimal("0.183"))
                .build();
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "780", null))
                .city("T4_BOW", price("T4_BOW", "Lymhurst", null, "18450"))
                .build();

        Optional<ProfitResult> result = new LocalOnlyStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isPresent();
        ProfitResult r = result.get();
        assertThat(r.itemId()).isEqualTo("T4_BOW");
        assertThat(r.city()).isEqualTo("Lymhurst");
        // per-unit 780 × 0.902 = 703.560 ; ×8 = 5628.480 ; ×(1−0.183)= 4598.47
        assertThat(r.totalCost()).isEqualByComparingTo("4598.47");
        assertThat(r.revenue()).isEqualByComparingTo("18450.00");
        assertThat(r.profit()).isEqualByComparingTo("13851.53");
        assertThat(r.profitRatio()).isGreaterThan(new BigDecimal("2.0"));
    }

    /**
     * A missing buy price for the home city makes the calculation return
     * {@code empty} — the strategy refuses to fabricate a price.
     */
    @Test
    void noBuyPriceInHomeCity_returnsEmpty() {
        Recipe recipe = new Recipe("T4_BOW", 4, "Bögen", "Holz",
                List.of(new RecipeIngredient("T4_PLANK", 1, null)));
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().build();
        PriceLookup prices = StubPriceLookup.builder().build();

        Optional<ProfitResult> result = new LocalOnlyStrategy(bonuses).calculate(recipe, "Lymhurst", prices);

        assertThat(result).isEmpty();
    }

    /**
     * The strategy reports its mode as {@link CalculationMode#LOCAL_ONLY}.
     */
    @Test
    void mode_isLocalOnly() {
        ProfitStrategy strategy = new LocalOnlyStrategy(StubCityBonusProvider.builder().build());

        assertThat(strategy.mode()).isEqualTo(CalculationMode.LOCAL_ONLY);
    }

    private static MarketPrice price(String item, String city, String buy, String sell) {
        return new MarketPrice(item, city, 1,
                buy == null ? null : new BigDecimal(buy),
                sell == null ? null : new BigDecimal(sell),
                Instant.EPOCH);
    }
}