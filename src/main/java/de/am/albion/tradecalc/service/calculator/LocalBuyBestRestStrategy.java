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
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import org.springframework.stereotype.Component;

/**
 * Mode (b): raw materials are bought in the user's home city, but the
 * refining and crafting steps are executed in the cities that offer the best
 * bonus for each step.
 */
@Component
final class LocalBuyBestRestStrategy extends AbstractProfitStrategy {

    LocalBuyBestRestStrategy(CityBonusProvider bonuses) {
        super(bonuses);
    }

    @Override
    public String buyCityFor(RecipeIngredient material, String homeCity, PriceLookup prices) {
        return homeCity;
    }

    @Override
    public String craftCityFor(Recipe recipe, String homeCity, PriceLookup prices) {
        return bonuses.bestCityFor(recipe.craftingCategory(), homeCity);
    }

    @Override
    public CalculationMode mode() {
        return CalculationMode.LOCAL_BUY_BEST_REST;
    }
}