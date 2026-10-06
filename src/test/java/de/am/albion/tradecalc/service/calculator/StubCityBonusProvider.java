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
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny in-memory {@link CityBonusProvider} for unit tests. Build with
 * {@link #builder()}.
 */
final class StubCityBonusProvider implements CityBonusProvider {

    private final Map<String, BigDecimal> bonusesByCityCategory;
    private final Map<String, String> bestCityByCategory;
    private final String fallback;

    private StubCityBonusProvider(Map<String, BigDecimal> bonuses, Map<String, String> bestCity, String fallback) {
        this.bonusesByCityCategory = bonuses;
        this.bestCityByCategory = bestCity;
        this.fallback = fallback;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public BigDecimal bonusFor(String cityName, String category) {
        return bonusesByCityCategory.getOrDefault(key(cityName, category), BigDecimal.ZERO);
    }

    @Override
    public String bestCityFor(String category, String fb) {
        return bestCityByCategory.getOrDefault(category == null ? "<null>" : category, fb != null ? fb : fallback);
    }

    private static String key(String cityName, String category) {
        return cityName + "::" + (category == null ? "<null>" : category);
    }

    static final class Builder {
        private final Map<String, BigDecimal> bonuses = new HashMap<>();
        private final Map<String, String> bestCity = new HashMap<>();
        private String fallback = "Lymhurst";

        Builder bonus(String city, String category, BigDecimal bonus) {
            bonuses.put(key(city, category), bonus);
            return this;
        }

        Builder bestCityFor(String category, String city) {
            bestCity.put(category == null ? "<null>" : category, city);
            return this;
        }

        Builder fallback(String city) {
            this.fallback = city;
            return this;
        }

        StubCityBonusProvider build() {
            return new StubCityBonusProvider(bonuses, bestCity, fallback);
        }
    }
}