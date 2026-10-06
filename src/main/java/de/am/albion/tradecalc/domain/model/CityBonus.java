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
 * Static crafting or refining bonus for a city/category combination.
 *
 * <p>The {@code bonus} is the percentage (as decimal) returned to the crafter
 * when refining or crafting in this city for the given category. A value of
 * {@code 0.152} means a <strong>15.2 %</strong> bonus.</p>
 *
 * <p>If {@link #category} is {@code null} the bonus applies to <em>all</em>
 * categories — used for cities such as Caerleon that only offer a generic bonus.</p>
 *
 * @param cityName the city to which this bonus applies
 * @param category the crafting category (e.g. {@code "Bows"}); {@code null} for a generic bonus
 * @param bonus the bonus as a decimal fraction (e.g. {@code 0.152} for 15.2 %)
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record CityBonus(
        String cityName,
        String category,
        BigDecimal bonus) {
}