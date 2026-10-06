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

import java.math.BigDecimal;

/**
 * Read-only access to city bonuses. Implemented by
 * {@link de.am.albion.tradecalc.repository.CityBonusRepository} in production
 * and by a stub in unit tests.
 */
public interface CityBonusProvider {

    /**
     * Returns the bonus (as a decimal fraction, e.g. {@code 0.152} = 15.2 %)
     * the given city offers for the given category, or {@link BigDecimal#ZERO}
     * if the city has no bonus for that category.
     *
     * @param cityName city to look up
     * @param category crafting / refining category, or {@code null} for the
     *                 generic (catch-all) bonus
     * @return the bonus as a decimal fraction; never {@code null}
     */
    BigDecimal bonusFor(String cityName, String category);

    /**
     * Returns the name of the city with the highest bonus for the given
     * category. Falls back to the first city that has a generic bonus when
     * no city matches the category exactly, or to the home city when no
     * city has any relevant bonus at all.
     *
     * @param category category to look up; {@code null} for generic
     * @param fallback city to return when no city offers any bonus for the category
     * @return the city with the highest bonus; never {@code null}
     */
    String bestCityFor(String category, String fallback);
}