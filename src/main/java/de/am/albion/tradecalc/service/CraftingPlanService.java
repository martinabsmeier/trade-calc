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

import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.domain.model.CraftingPlan;
import de.am.albion.tradecalc.domain.model.CraftingStep;
import de.am.albion.tradecalc.domain.model.Item;
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import de.am.albion.tradecalc.service.calculator.PriceLookup;
import de.am.albion.tradecalc.service.calculator.ProfitStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Builds a {@link CraftingPlan} for a target item under a chosen
 * {@link CalculationMode}: BUY raw materials → REFINE intermediates → CRAFT
 * the result → SELL. Cities are taken from the matching {@link ProfitStrategy}
 * so the plan and the {@link ProfitResult} agree.
 *
 * <p>Step kinds are: {@code BUY} (raw material with no recipe),
 * {@code REFINE} (intermediate whose own recipe is walked first),
 * {@code CRAFT} (the root recipe's output) and {@code SELL} (the root output
 * sold in the craft city). The {@code cost} field is {@code 0} on every step
 * — the monetary total lives on {@link CraftingPlan#profitSummary()}.
 * Step costs in the UI are computed from the {@link ProfitResult} when the
 * plan is rendered.</p>
 */
@Slf4j
@Service
public class CraftingPlanService {

    private final RecipeService recipes;
    private final ProfitCalculationService calculation;

    public CraftingPlanService(RecipeService recipes, ProfitCalculationService calculation) {
        this.recipes = recipes;
        this.calculation = calculation;
    }

    /**
     * Builds the plan for {@code targetItemId}.
     *
     * @param mode         the calculation mode
     * @param targetItemId the item id to craft
     * @param homeCity     the user-selected home city (mode-dependent meaning)
     * @param prices       the price source for items in the plan
     * @return the plan, or empty if the recipe is unknown or the calculation
     *         has no feasible result
     */
    public Optional<CraftingPlan> buildPlan(CalculationMode mode, String targetItemId, String homeCity,
                                            PriceLookup prices) {
        Optional<Recipe> root = recipes.findByItemId(targetItemId);
        if (root.isEmpty()) {
            log.debug("No recipe registered for target item '{}'", targetItemId);
            return Optional.empty();
        }
        Recipe recipe = root.get();
        Optional<ProfitResult> profit = calculation.calculate(mode, recipe, homeCity, prices);
        if (profit.isEmpty()) {
            return Optional.empty();
        }
        ProfitStrategy strategy = calculation.strategyFor(mode);

        List<CraftingStep> steps = new ArrayList<>();
        Deque<String> visited = new ArrayDeque<>();
        for (RecipeIngredient material : recipe.materials()) {
            walk(material, homeCity, prices, visited, steps, strategy);
        }
        String craftCity = strategy.craftCityFor(recipe, homeCity, prices);
        steps.add(step("CRAFT", recipe.itemId(), craftCity, 1));
        steps.add(step("SELL", recipe.itemId(), craftCity, 1));

        return Optional.of(CraftingPlan.builder()
                .targetItem(new Item(recipe.itemId(), recipe.tier(), recipe.craftingCategory()))
                .profitSummary(profit.get())
                .steps(List.copyOf(steps))
                .build());
    }

    /**
     * Walks one material. Leaf (no recipe) → BUY. Intermediate → recurse on
     * its ingredients, then emit REFINE in the picked buy-city.
     *
     * <p>The {@code visited} stack guards against recipe cycles; if a cycle is
     * detected we still emit a REFINE step so the plan does not deadlock —
     * the {@code ponytail:} ceiling here is the lack of cycle detection in
     * the upstream recipe data.</p>
     */
    private void walk(RecipeIngredient material, String homeCity, PriceLookup prices,
                      Deque<String> visited, List<CraftingStep> steps, ProfitStrategy planStrategy) {
        String buyCity = planStrategy.buyCityFor(material, homeCity, prices);
        Optional<Recipe> subRecipe = recipes.findByItemId(material.itemId());

        if (subRecipe.isEmpty()) {
            steps.add(step("BUY", material.itemId(), buyCity, material.quantity()));
            return;
        }
        if (visited.contains(material.itemId())) {
            log.warn("Recipe cycle at '{}' — emitting to unblock", material.itemId());
            steps.add(step("REFINE", material.itemId(), buyCity, material.quantity()));
            return;
        }
        visited.push(material.itemId());
        for (RecipeIngredient nested : subRecipe.get().materials()) {
            walk(nested, homeCity, prices, visited, steps, planStrategy);
        }
        visited.pop();
        steps.add(step("REFINE", material.itemId(), buyCity, material.quantity()));
    }

    private static CraftingStep step(String kind, String itemId, String city, int quantity) {
        return CraftingStep.builder()
                .step(kind)
                .itemId(itemId)
                .city(city)
                .quantity(quantity)
                .cost(BigDecimal.ZERO)
                .build();
    }
}