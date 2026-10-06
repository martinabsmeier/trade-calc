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
package de.am.albion.tradecalc.service;

import de.am.albion.tradecalc.dataprovider.recipe.RecipeLoader;
import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.domain.model.CraftingPlan;
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import de.am.albion.tradecalc.service.calculator.PriceLookup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProfitQueryService}. The recipe catalogue and the
 * profit calculation are stubbed; the test asserts ordering, topN trimming
 * and the {@code includePlan} flag.
 */
class ProfitQueryServiceTest {

    @Test
    void rankTop_sortsByProfitDescAndTrimsToTopN() {
        Recipe bow = bow();
        Recipe sword = sword();
        Recipe axe = axe();

        RecipeLoader recipeLoader = mock(RecipeLoader.class);
        when(recipeLoader.loadAll()).thenReturn(List.of(bow, sword, axe));

        ProfitCalculationService calc = mock(ProfitCalculationService.class);
        when(calc.calculate(any(), any(), any(), any())).thenAnswer(inv -> {
            Recipe r = inv.getArgument(1);
            return Optional.of(profitFor(r.itemId(), profitValue(r.itemId())));
        });
        when(calc.strategyFor(any())).thenReturn(null);

        CraftingPlanService plans = mock(CraftingPlanService.class);
        when(plans.buildPlan(any(), any(), any(), any())).thenReturn(Optional.empty());

        ProfitQueryService query = new ProfitQueryService(recipeLoader, calc, plans);

        // bow = 200, sword = 500, axe = 100 → sword first, then bow, then axe.
        List<ProfitQueryService.ProfitEntry> ranked = query.rankTop(
                CalculationMode.BEST_OF_ALL, "Lymhurst", stubPrices(), 2, false);

        assertThat(ranked).extracting(ProfitQueryService.ProfitEntry::itemId)
                .containsExactly("T4_SWORD", "T4_BOW");
        // Plan must not be built when includePlan=false.
        verify(plans, never()).buildPlan(any(), any(), any(), any());
    }

    @Test
    void rankTop_includePlanTrue_callsPlanService() {
        Recipe bow = bow();
        RecipeLoader recipeLoader = mock(RecipeLoader.class);
        when(recipeLoader.loadAll()).thenReturn(List.of(bow));

        ProfitCalculationService calc = mock(ProfitCalculationService.class);
        when(calc.calculate(any(), any(), any(), any())).thenReturn(Optional.of(profitFor("T4_BOW", "100")));
        when(calc.strategyFor(any())).thenReturn(null);

        CraftingPlanService plans = mock(CraftingPlanService.class);
        when(plans.buildPlan(any(), eq("T4_BOW"), any(), any()))
                .thenReturn(Optional.of(CraftingPlan.builder().build()));

        ProfitQueryService query = new ProfitQueryService(recipeLoader, calc, plans);

        List<ProfitQueryService.ProfitEntry> ranked = query.rankTop(
                CalculationMode.BEST_OF_ALL, "Lymhurst", stubPrices(), 0, true);

        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).planOrNull()).isNotNull();
        verify(plans).buildPlan(eq(CalculationMode.BEST_OF_ALL), eq("T4_BOW"),
                eq("Lymhurst"), any());
    }

    @Test
    void rankTop_dropsRecipesWithoutFeasibleProfit() {
        Recipe bow = bow();
        RecipeLoader recipeLoader = mock(RecipeLoader.class);
        when(recipeLoader.loadAll()).thenReturn(List.of(bow));

        ProfitCalculationService calc = mock(ProfitCalculationService.class);
        when(calc.calculate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(calc.strategyFor(any())).thenReturn(null);

        CraftingPlanService plans = mock(CraftingPlanService.class);

        ProfitQueryService query = new ProfitQueryService(recipeLoader, calc, plans);

        List<ProfitQueryService.ProfitEntry> ranked = query.rankTop(
                CalculationMode.LOCAL_ONLY, "Lymhurst", stubPrices(), 0, false);

        assertThat(ranked).isEmpty();
    }

    private static Recipe bow() {
        return new Recipe("T4_BOW", 4, "Bows", "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 1, null)));
    }

    private static Recipe sword() {
        return new Recipe("T4_SWORD", 4, "Swords", "Metall",
                List.of(new RecipeIngredient("T4_ORE", 1, null)));
    }

    private static Recipe axe() {
        return new Recipe("T4_AXE", 4, "Axes", "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 1, null)));
    }

    private static ProfitResult profitFor(String itemId, String profit) {
        return ProfitResult.builder()
                .itemId(itemId).city("Lymhurst")
                .totalCost(BigDecimal.ZERO).revenue(BigDecimal.ZERO)
                .profit(new BigDecimal(profit)).profitRatio(BigDecimal.ZERO)
                .build();
    }

    private static String profitValue(String itemId) {
        return switch (itemId) {
            case "T4_BOW" -> "200";
            case "T4_SWORD" -> "500";
            case "T4_AXE" -> "100";
            default -> "0";
        };
    }

    private static ProfitResult profitOf(String itemId) {
        return ProfitResult.builder()
                .itemId(itemId).city("Lymhurst")
                .totalCost(BigDecimal.ZERO).revenue(BigDecimal.ZERO)
                .profit(BigDecimal.ZERO).profitRatio(BigDecimal.ZERO)
                .build();
    }

    private static PriceLookup stubPrices() {
        return id -> java.util.Map.of();
    }
}