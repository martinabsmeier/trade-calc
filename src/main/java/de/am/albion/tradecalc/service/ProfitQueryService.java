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
import de.am.albion.tradecalc.service.calculator.PriceLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates a profit query: walks every recipe, asks
 * {@link ProfitCalculationService} for the best result, sorts by profit
 * descending and trims to the requested {@code topN} entries.
 *
 * <p>{@link CraftingPlan}s are only computed when the caller opts in
 * ({@code includePlan}) — building a plan walks the recipe tree, so on a
 * 5-recipe, 30-step catalogue this is a non-trivial saving.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitQueryService {

    private final RecipeLoader recipes;
    private final ProfitCalculationService calculation;
    private final CraftingPlanService plans;

    /**
     * Computes one {@link ProfitResult} per known recipe under {@code mode}
     * and returns the top {@code topN} by profit, descending.
     *
     * <p>Recipes that have no feasible profit (missing sell price, no
     * bonuses, …) are dropped silently — the response is a best-effort
     * ranking, not an error report. {@code topN <= 0} returns the full list.</p>
     *
     * @param mode         the calculation mode
     * @param homeCity     the user-selected home city
     * @param prices       the price source for items in the recipes
     * @param topN         maximum number of entries to return; {@code <= 0} means no limit
     * @param includePlan  if {@code true}, every returned entry also has its
     *                     {@link CraftingPlan} attached
     * @return immutable list, possibly empty
     */
    public List<ProfitEntry> rankTop(CalculationMode mode, String homeCity, PriceLookup prices,
                                     int topN, boolean includePlan) {
        List<ProfitEntry> entries = new ArrayList<>();
        for (Recipe recipe : recipes.loadAll()) {
            Optional<ProfitResult> profit = calculation.calculate(mode, recipe, homeCity, prices);
            if (profit.isEmpty()) {
                continue;
            }
            Optional<CraftingPlan> plan = includePlan
                    ? plans.buildPlan(mode, recipe.itemId(), homeCity, prices)
                    : Optional.empty();
            entries.add(new ProfitEntry(recipe.itemId(), profit.get(), plan));
        }
        entries.sort(Comparator.comparing((ProfitEntry e) -> e.profit().profit()).reversed());
        if (topN > 0 && entries.size() > topN) {
            entries = entries.subList(0, topN);
        }
        log.debug("rankTop(mode={}, home={}) → {} entries (topN={}, includePlan={})",
                mode, homeCity, entries.size(), topN, includePlan);
        return List.copyOf(entries);
    }

    /**
     * Builds the crafting plan for a single item under the given mode. Used
     * by the lazy drawer endpoint that runs only when a user expands a row.
     *
     * @param itemId    the item id to plan for
     * @param mode      the calculation mode
     * @param homeCity  the user-selected home city
     * @param prices    the price source
     * @return the plan, or empty if the item is unknown / no profit exists
     */
    public Optional<CraftingPlan> planFor(String itemId, CalculationMode mode, String homeCity,
                                          PriceLookup prices) {
        return plans.buildPlan(mode, itemId, homeCity, prices);
    }

    /**
     * One ranked item in a {@link ProfitQueryService#rankTop} response.
     *
     * @param itemId  the item id
     * @param profit  the profit summary for the item under the chosen mode
     * @param plan    optional crafting plan; empty when not requested or
     *                when no plan could be built
     */
    public record ProfitEntry(String itemId, ProfitResult profit, Optional<CraftingPlan> plan) {

        /**
         * Defensive guard: Jackson serialises {@code Optional} as a JSON
         * object instead of {@code null}. Empty plans become {@code null}
         * in the response.
         *
         * @return the plan, or {@code null} when absent
         */
        public CraftingPlan planOrNull() {
            return plan == null ? null : plan.orElse(null);
        }
    }
}