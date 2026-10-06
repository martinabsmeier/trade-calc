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
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.service.calculator.PriceLookup;
import de.am.albion.tradecalc.service.calculator.ProfitStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calculates crafting profit for a list of recipes using the
 * {@link CalculationMode} chosen by the user.
 *
 * <p>Spring injects all {@link ProfitStrategy} beans and this service indexes
 * them by their {@link ProfitStrategy#mode() mode}. Lookup is
 * {@code O(1)} via an {@link EnumMap}; adding a new mode means adding a new
 * {@code @Component} implementing {@link ProfitStrategy} — no changes here.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Service
public class ProfitCalculationService {

    private final Map<CalculationMode, ProfitStrategy> strategies;

    /**
     * Creates the service from the discovered strategy beans.
     *
     * @param strategyBeans every {@link ProfitStrategy} Spring knows about
     */
    public ProfitCalculationService(List<ProfitStrategy> strategyBeans) {
        this.strategies = new EnumMap<>(CalculationMode.class);
        for (ProfitStrategy strategy : strategyBeans) {
            this.strategies.put(strategy.mode(), strategy);
        }
        EnumSet<CalculationMode> missing = EnumSet.allOf(CalculationMode.class);
        missing.removeAll(strategies.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("ProfitCalculationService is missing strategies for: " + missing);
        }
    }

    /**
     * Calculates the profit for a single recipe.
     *
     * @param mode the calculation mode
     * @param recipe the recipe to evaluate
     * @param homeCity the user-selected home city
     * @param prices the price source for items involved in the recipe
     * @return the calculated profit, or empty if no feasible result exists
     */
    public Optional<ProfitResult> calculate(CalculationMode mode, Recipe recipe, String homeCity, PriceLookup prices) {
        return strategies.get(mode).calculate(recipe, homeCity, prices);
    }

    /**
     * Returns the {@link ProfitStrategy} registered for {@code mode}. Used by
     * {@link CraftingPlanService} to share the strategy's buy- and craft-city
     * decisions so the plan and the {@link ProfitResult} agree.
     *
     * @param mode the calculation mode
     * @return the strategy; never {@code null} (the constructor enforces full coverage)
     */
    public ProfitStrategy strategyFor(CalculationMode mode) {
        return strategies.get(mode);
    }
}