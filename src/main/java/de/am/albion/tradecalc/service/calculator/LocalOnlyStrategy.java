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
 * Mode (a): every action — buying, refining, crafting and selling — happens
 * in the user's selected home city. Simplest mode; ignores any bonuses in
 * other cities.
 */
@Component
final class LocalOnlyStrategy extends AbstractProfitStrategy {

    LocalOnlyStrategy(CityBonusProvider bonuses) {
        super(bonuses);
    }

    @Override
    public String buyCityFor(RecipeIngredient material, String homeCity, PriceLookup prices) {
        return homeCity;
    }

    @Override
    public String craftCityFor(Recipe recipe, String homeCity, PriceLookup prices) {
        return homeCity;
    }

    @Override
    public CalculationMode mode() {
        return CalculationMode.LOCAL_ONLY;
    }
}