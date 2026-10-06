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

import de.am.albion.tradecalc.dataprovider.recipe.RecipeLoader;
import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.domain.model.CraftingPlan;
import de.am.albion.tradecalc.domain.model.CraftingStep;
import de.am.albion.tradecalc.domain.model.MarketPrice;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import de.am.albion.tradecalc.service.CraftingPlanService;
import de.am.albion.tradecalc.service.ProfitCalculationService;
import de.am.albion.tradecalc.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CraftingPlanService}. The recipe tree mirrors the
 * one in {@code src/main/resources/recipes/index.json} (T4_BOW → 3 intermediates
 * → 3 raw materials) so we exercise the full recursive descent in the happy
 * path. {@link RecipeLoader} is stubbed so the test does not depend on
 * classpath ordering or file contents.
 */
class CraftingPlanServiceTest {

    private CraftingPlanService service;

    @BeforeEach
    void setUp() {
        Recipe bow = new Recipe("T4_BOW", 4, "Bögen, Stoff, Möbel", "Holz",
                List.of(
                        new RecipeIngredient("T4_PLANK", 8, null),
                        new RecipeIngredient("T4_METALBAR", 4, null),
                        new RecipeIngredient("T4_CLOTH", 2, null)));
        Recipe plank = new Recipe("T4_PLANK", 4, null, "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 2, null)));
        Recipe metalbar = new Recipe("T4_METALBAR", 4, null, "Metall",
                List.of(new RecipeIngredient("T4_ORE", 2, null)));
        Recipe cloth = new Recipe("T4_CLOTH", 4, null, "Stoff",
                List.of(new RecipeIngredient("T4_HIDE", 2, null)));

        RecipeLoader loader = mock(RecipeLoader.class);
        when(loader.loadAll()).thenReturn(List.of(bow, plank, metalbar, cloth));

        RecipeService recipes = new RecipeService(loader);
        ProfitCalculationService calculation = new ProfitCalculationService(realStrategies());
        service = new CraftingPlanService(recipes, calculation);
    }

    /**
     * The full T4_BOW tree, under {@code BEST_OF_ALL}: for each root material
     * we walk its sub-recipe (BUY leaves, then REFINE the intermediate)
     * before moving on to the next material. With three intermediates, the
     * sequence is BUY/REFINE ×3, then CRAFT, then SELL.
     */
    @Test
    void bestOfAll_emitsBuyRefineCrafSelltInTopologicalOrder() {
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_WOOD", price("T4_WOOD", "Lymhurst", "100", null))
                .city("T4_ORE", price("T4_ORE", "Lymhurst", "100", null))
                .city("T4_HIDE", price("T4_HIDE", "Lymhurst", "100", null))
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "200", null))
                .city("T4_METALBAR", price("T4_METALBAR", "Lymhurst", "200", null))
                .city("T4_CLOTH", price("T4_CLOTH", "Lymhurst", "200", null))
                .city("T4_BOW", price("T4_BOW", "Lymhurst", null, "5000"))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder()
                .bestCityFor("Bögen, Stoff, Möbel", "Lymhurst")
                .bestCityFor("Holz", "Lymhurst")
                .bestCityFor("Metall", "Lymhurst")
                .bestCityFor("Stoff", "Lymhurst")
                .fallback("Lymhurst")
                .build();
        inject(bonuses);

        Optional<CraftingPlan> plan = service.buildPlan(CalculationMode.BEST_OF_ALL, "T4_BOW", "Lymhurst", prices);

        assertThat(plan).isPresent();
        List<String> kinds = plan.get().steps().stream().map(CraftingStep::step).toList();
        assertThat(kinds).containsExactly("BUY", "REFINE", "BUY", "REFINE", "BUY", "REFINE", "CRAFT", "SELL");
        CraftingStep sell = plan.get().steps().get(7);
        assertThat(sell.city()).isEqualTo("Lymhurst");
        assertThat(sell.itemId()).isEqualTo("T4_BOW");
    }

    /**
     * When the recipe is unknown the service returns empty without asking
     * the strategy for city decisions.
     */
    @Test
    void unknownItem_returnsEmpty() {
        Optional<CraftingPlan> plan = service.buildPlan(CalculationMode.LOCAL_ONLY, "T9_ARTEFACT", "Lymhurst",
                StubPriceLookup.builder().build());

        assertThat(plan).isEmpty();
    }

    /**
     * When the recipe exists but the calculation cannot produce a profit
     * (e.g. no sell price in the chosen city), the service propagates empty.
     */
    @Test
    void noFeasibleProfit_returnsEmpty() {
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_WOOD", price("T4_WOOD", "Lymhurst", "100", null))
                .city("T4_ORE", price("T4_ORE", "Lymhurst", "100", null))
                .city("T4_HIDE", price("T4_HIDE", "Lymhurst", "100", null))
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "200", null))
                .city("T4_METALBAR", price("T4_METALBAR", "Lymhurst", "200", null))
                .city("T4_CLOTH", price("T4_CLOTH", "Lymhurst", "200", null))
                // No T4_BOW sell price → profit is impossible.
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder().fallback("Lymhurst").build();
        inject(bonuses);

        Optional<CraftingPlan> plan = service.buildPlan(CalculationMode.LOCAL_ONLY, "T4_BOW", "Lymhurst", prices);

        assertThat(plan).isEmpty();
    }

    /**
     * The plan always emits the CRAFT and SELL steps in the craft city
     * chosen by the strategy — not the price. With Lymhurst offering the
     * highest Bögen bonus and Fort Sterling offering the highest Holz
     * bonus, the cheapest wood ends up bought in Fort Sterling but the bow
     * is crafted and sold in Lymhurst.
     */
    @Test
    void craftAndSellStep_useCraftCity() {
        PriceLookup prices = StubPriceLookup.builder()
                .city("T4_WOOD", price("T4_WOOD", "FortSterling", "80", null))
                .city("T4_WOOD", price("T4_WOOD", "Lymhurst", "120", null))
                .city("T4_ORE", price("T4_ORE", "Lymhurst", "100", null))
                .city("T4_HIDE", price("T4_HIDE", "Lymhurst", "100", null))
                .city("T4_PLANK", price("T4_PLANK", "Lymhurst", "200", null))
                .city("T4_METALBAR", price("T4_METALBAR", "Lymhurst", "200", null))
                .city("T4_CLOTH", price("T4_CLOTH", "Lymhurst", "200", null))
                .city("T4_BOW", price("T4_BOW", "Lymhurst", null, "5000"))
                .build();
        StubCityBonusProvider bonuses = StubCityBonusProvider.builder()
                .bestCityFor("Bögen, Stoff, Möbel", "Lymhurst")
                .bestCityFor("Holz", "FortSterling")
                .bestCityFor("Metall", "Lymhurst")
                .bestCityFor("Stoff", "Lymhurst")
                .fallback("Lymhurst")
                .build();
        inject(bonuses);

        Optional<CraftingPlan> plan = service.buildPlan(CalculationMode.BEST_OF_ALL, "T4_BOW", "Lymhurst", prices);

        assertThat(plan).isPresent();
        CraftingStep craft = plan.get().steps().get(6);
        CraftingStep sell = plan.get().steps().get(7);
        assertThat(craft.city()).isEqualTo("Lymhurst");
        assertThat(sell.city()).isEqualTo("Lymhurst");
        // Wood is the only material with a cheaper city elsewhere.
        CraftingStep woodBuy = plan.get().steps().stream()
                .filter(s -> s.step().equals("BUY") && s.itemId().equals("T4_WOOD"))
                .findFirst().orElseThrow();
        assertThat(woodBuy.city()).isEqualTo("FortSterling");
    }

    /**
     * Real strategies from the production package. Tests that need a specific
     * bonus configuration re-construct the calculation service through
     * {@link #inject(StubCityBonusProvider)}.
     */
    private List<ProfitStrategy> realStrategies() {
        return List.of(
                new de.am.albion.tradecalc.service.calculator.LocalOnlyStrategy(silentBonus()),
                new de.am.albion.tradecalc.service.calculator.LocalBuyBestRestStrategy(silentBonus()),
                new de.am.albion.tradecalc.service.calculator.BestOfAllStrategy(silentBonus()));
    }

    /**
     * Re-wires the service with bonuses provided by the test. Production
     * strategies are stateless after construction, so this is the cheapest
     * seam that does not require test-only methods on the production types.
     */
    private void inject(StubCityBonusProvider bonuses) {
        RecipeLoader loader = mock(RecipeLoader.class);
        Recipe bow = new Recipe("T4_BOW", 4, "Bögen, Stoff, Möbel", "Holz",
                List.of(
                        new RecipeIngredient("T4_PLANK", 8, null),
                        new RecipeIngredient("T4_METALBAR", 4, null),
                        new RecipeIngredient("T4_CLOTH", 2, null)));
        Recipe plank = new Recipe("T4_PLANK", 4, null, "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 2, null)));
        Recipe metalbar = new Recipe("T4_METALBAR", 4, null, "Metall",
                List.of(new RecipeIngredient("T4_ORE", 2, null)));
        Recipe cloth = new Recipe("T4_CLOTH", 4, null, "Stoff",
                List.of(new RecipeIngredient("T4_HIDE", 2, null)));
        when(loader.loadAll()).thenReturn(List.of(bow, plank, metalbar, cloth));

        ProfitCalculationService real = new ProfitCalculationService(List.of(
                new de.am.albion.tradecalc.service.calculator.LocalOnlyStrategy(bonuses),
                new de.am.albion.tradecalc.service.calculator.LocalBuyBestRestStrategy(bonuses),
                new de.am.albion.tradecalc.service.calculator.BestOfAllStrategy(bonuses)));
        service = new CraftingPlanService(new RecipeService(loader), real);
    }

    /**
     * Bonus provider with every category falling back to {@code homeCity}.
     * Used by the {@link #setUp()} default wiring.
     */
    private static StubCityBonusProvider silentBonus() {
        return StubCityBonusProvider.builder().build();
    }

    private static MarketPrice price(String item, String city, String buy, String sell) {
        return new MarketPrice(item, city, 1,
                buy == null ? null : new BigDecimal(buy),
                sell == null ? null : new BigDecimal(sell),
                Instant.EPOCH);
    }
}