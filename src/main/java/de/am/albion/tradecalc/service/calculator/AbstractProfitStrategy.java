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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Shared arithmetic for all profit strategies. Each strategy only needs to
 * decide which cities to use for buying, refining and crafting; this base
 * class does the multiplication and produces the {@link ProfitResult}.
 *
 * <p>All numbers are rounded to two decimal places at output time to match
 * the silver granularity of Albion Online market data.</p>
 */
abstract class AbstractProfitStrategy implements ProfitStrategy {

    /**
     * How many decimal places a profit / cost value carries in the result.
     */
    private static final int SCALE = 2;

    /**
     * Bonus source — must be supplied by the concrete strategy (so each
     * strategy can decide which city to query).
     */
    protected final CityBonusProvider bonuses;

    protected AbstractProfitStrategy(CityBonusProvider bonuses) {
        this.bonuses = bonuses;
    }

    /**
     * Resolves the city in which the material should be bought under this
     * strategy. May be {@code null} if the strategy could not pick one (e.g.
     * no price is available for any city).
     */
    protected abstract String buyCityFor(RecipeIngredient material, String homeCity, PriceLookup prices);

    /**
     * Resolves the city in which the recipe's result item should be crafted
     * and sold.
     */
    protected abstract String craftCityFor(Recipe recipe, String homeCity, PriceLookup prices);

    /**
     * {@inheritDoc}
     *
     * <p>Implementation:
     * <ol>
     *   <li>Compute raw material cost (sum of {@code quantity × buyPriceMax})</li>
     *   <li>Apply the city's refining bonus for each material's category</li>
     *   <li>Apply the craft city's crafting bonus for the result category</li>
     *   <li>Subtract from the sell price in the craft city</li>
     * </ol>
     */
    @Override
    public final Optional<ProfitResult> calculate(Recipe recipe, String homeCity, PriceLookup prices) {
        if (recipe == null || recipe.materials().isEmpty()) {
            return Optional.empty();
        }
        BigDecimal rawCost = BigDecimal.ZERO;
        for (RecipeIngredient material : recipe.materials()) {
            String city = buyCityFor(material, homeCity, prices);
            if (city == null) {
                return Optional.empty();
            }
            BigDecimal buyPrice = buyPriceFor(material.itemId(), city, prices);
            if (buyPrice == null) {
                return Optional.empty();
            }
            BigDecimal refiningBonus = bonuses.bonusFor(city, recipe.refiningCategory());
            BigDecimal effectiveUnitCost = buyPrice.multiply(BigDecimal.ONE.subtract(refiningBonus));
            rawCost = rawCost.add(effectiveUnitCost.multiply(BigDecimal.valueOf(material.quantity())));
        }

        String craftCity = craftCityFor(recipe, homeCity, prices);
        if (craftCity == null) {
            return Optional.empty();
        }
        MarketPrice craftPrice = prices.pricesFor(recipe.itemId()).get(craftCity);
        if (craftPrice == null || craftPrice.sellPriceMin() == null) {
            return Optional.empty();
        }

        BigDecimal craftingBonus = bonuses.bonusFor(craftCity, recipe.craftingCategory());
        BigDecimal totalCost = rawCost.multiply(BigDecimal.ONE.subtract(craftingBonus));
        BigDecimal revenue = craftPrice.sellPriceMin();
        BigDecimal profit = revenue.subtract(totalCost);
        BigDecimal profitRatio = totalCost.signum() == 0
                ? BigDecimal.ZERO
                : profit.divide(totalCost, 4, RoundingMode.HALF_UP);

        return Optional.of(ProfitResult.builder()
                .itemId(recipe.itemId())
                .city(craftCity)
                .totalCost(totalCost.setScale(SCALE, RoundingMode.HALF_UP))
                .revenue(revenue.setScale(SCALE, RoundingMode.HALF_UP))
                .profit(profit.setScale(SCALE, RoundingMode.HALF_UP))
                .profitRatio(profitRatio)
                .build());
    }

    /**
     * Returns the {@code buyPriceMax} for the given item in the given city,
     * or {@code null} if no active buy-order exists in that city.
     */
    private static BigDecimal buyPriceFor(String itemId, String city, PriceLookup prices) {
        Map<String, MarketPrice> cityPrices = prices.pricesFor(itemId);
        MarketPrice mp = cityPrices.get(city);
        return mp == null ? null : mp.buyPriceMax();
    }
}