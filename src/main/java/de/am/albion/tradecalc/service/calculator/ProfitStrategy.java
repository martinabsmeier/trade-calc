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
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;

import java.util.Optional;

/**
 * One calculation strategy: how aggressively to deviate from the home city in
 * order to maximise profit. Each strategy encodes one
 * {@link CalculationMode}.
 *
 * <p>Strategies are stateless and pure: all inputs are passed in, all
 * external dependencies (prices, bonuses) are reached through the helper
 * instances passed to {@link #calculate}. This keeps the strategies
 * trivially unit-testable without Spring.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
public interface ProfitStrategy {

    /**
     * The {@link CalculationMode} this strategy implements. Used by
     * {@link ProfitCalculationService} to build the mode → strategy dispatch map.
     */
    CalculationMode mode();

    /**
     * Calculates the profit for the given recipe when executed under this
     * strategy. Returns {@link Optional#empty()} when no feasible calculation
     * exists — e.g. a required price is missing for every city.
     *
     * @param recipe the recipe to evaluate
     * @param homeCity the user-selected home city (mode-dependent meaning)
     * @param prices the price source for items involved in the recipe
     * @return the calculated profit, or empty if no feasible result exists
     */
    Optional<ProfitResult> calculate(Recipe recipe, String homeCity, PriceLookup prices);

    /**
     * Resolves the city in which a material should be bought under this
     * strategy. Exposed for reuse by {@code CraftingPlanService} so the plan
     * and the calculation pick the same place to buy.
     *
     * <p>Default implementation returns {@code homeCity}; concrete strategies
     * override based on their mode.</p>
     *
     * @param material the recipe ingredient to purchase
     * @param homeCity the user-selected home city
     * @param prices the price source for the material
     * @return the city to buy in; never {@code null}
     */
    default String buyCityFor(RecipeIngredient material, String homeCity, PriceLookup prices) {
        return homeCity;
    }

    /**
     * Resolves the city in which the recipe's result item should be crafted
     * and sold under this strategy. Exposed for reuse by
     * {@code CraftingPlanService} so the plan's {@code CRAFT} and
     * {@code SELL} steps land in the same city the calculation used.
     *
     * <p>Default implementation returns {@code homeCity}; concrete strategies
     * override based on their mode.</p>
     *
     * @param recipe the recipe being crafted
     * @param homeCity the user-selected home city
     * @param prices the price source for the result item
     * @return the city to craft and sell in; never {@code null}
     */
    default String craftCityFor(Recipe recipe, String homeCity, PriceLookup prices) {
        return homeCity;
    }
}