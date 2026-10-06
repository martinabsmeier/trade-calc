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
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Mode (c): every step — buying, refining and crafting — happens in whichever
 * city yields the lowest total cost / highest profit. Maximises profit but
 * ignores the player's physical location.
 *
 * <p>The buy-city for each material is the city with the lowest
 * {@code buyPriceMax}. The craft-city is the city with the highest crafting
 * bonus that also has a known {@code sellPriceMin} for the result item.</p>
 */
@Component
final class BestOfAllStrategy extends AbstractProfitStrategy {

    BestOfAllStrategy(CityBonusProvider bonuses) {
        super(bonuses);
    }

    @Override
    public String buyCityFor(RecipeIngredient material, String homeCity, PriceLookup prices) {
        Map<String, MarketPrice> cityPrices = prices.pricesFor(material.itemId());
        return cityPrices.entrySet().stream()
                .filter(e -> e.getValue().buyPriceMax() != null)
                .min((a, b) -> a.getValue().buyPriceMax().compareTo(b.getValue().buyPriceMax()))
                .map(Map.Entry::getKey)
                .orElse(homeCity);
    }

    @Override
    public String craftCityFor(Recipe recipe, String homeCity, PriceLookup prices) {
        Map<String, MarketPrice> cityPrices = prices.pricesFor(recipe.itemId());
        return cityPrices.entrySet().stream()
                .filter(e -> e.getValue().sellPriceMin() != null)
                .max((a, b) -> bonuses.bonusFor(a.getKey(), recipe.craftingCategory())
                        .compareTo(bonuses.bonusFor(b.getKey(), recipe.craftingCategory())))
                .map(Map.Entry::getKey)
                .orElse(homeCity);
    }

    @Override
    public CalculationMode mode() {
        return CalculationMode.BEST_OF_ALL;
    }
}