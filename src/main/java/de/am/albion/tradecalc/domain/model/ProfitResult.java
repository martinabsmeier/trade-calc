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
package de.am.albion.tradecalc.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Result of a single profit calculation: how much it costs to craft the item,
 * how much it can be sold for, and the resulting profit and profit ratio.
 *
 * <p>All values are quoted in <strong>silver</strong>; the profit ratio is a
 * decimal fraction (e.g. {@code 0.25} = 25 % return on cost).</p>
 *
 * @param itemId the calculated item
 * @param city the city in which the item is sold to maximise profit
 * @param totalCost total crafting cost (sum of material costs minus bonuses)
 * @param revenue expected revenue when selling in {@code city}
 * @param profit {@code revenue - totalCost}
 * @param profitRatio {@code profit / totalCost}
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record ProfitResult(
        String itemId,
        String city,
        BigDecimal totalCost,
        BigDecimal revenue,
        BigDecimal profit,
        BigDecimal profitRatio) {
}