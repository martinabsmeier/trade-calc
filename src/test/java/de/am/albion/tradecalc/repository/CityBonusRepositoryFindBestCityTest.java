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
package de.am.albion.tradecalc.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused tests for {@link CityBonusRepository#findBestCity(String, String)}.
 *
 * <p>The method has 22 uncovered branches (8 distinct decision points × 2-3
 * paths each) and is on the hot path of every {@code BEST_OF_ALL}
 * calculation. These tests cover each branch deliberately with a
 * purpose-built JSON fixture.</p>
 */
class CityBonusRepositoryFindBestCityTest {

    private static CityBonusRepository repo(String json) {
        return CityBonusRepository.forTest(json);
    }

    /**
     * A city offering the highest category-specific bonus is selected.
     */
    @Test
    void categoryMatch_winner_isSelected() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.10},
                  {"cityName":"Martlock","category":"Bögen","bonus":0.20},
                  {"cityName":"FortSterling","category":"Bögen","bonus":0.15}
                ]
                """);

        assertThat(r.findBestCity("Bögen", "Lymhurst")).isEqualTo("Martlock");
    }

    /**
     * When two cities tie on the category bonus, the first encountered wins
     * (the implementation keeps {@code best} when {@code bonus <= best.bonus()}).
     */
    @Test
    void categoryMatch_tieKeepsEarlierEntry() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.10},
                  {"cityName":"Martlock","category":"Bögen","bonus":0.10}
                ]
                """);

        assertThat(r.findBestCity("Bögen", "X")).isEqualTo("Lymhurst");
    }

    /**
     * When the same city appears twice for one category the higher bonus wins.
     */
    @Test
    void categoryMatch_sameCityHigherBonusWins() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Martlock","category":"Bögen","bonus":0.10},
                  {"cityName":"Martlock","category":"Bögen","bonus":0.20}
                ]
                """);

        assertThat(r.findBestCity("Bögen", "X")).isEqualTo("Martlock");
    }

    /**
     * Category lookup is case-insensitive: an entry {@code "Häute"} matches
     * the query {@code "häute"}.
     */
    @Test
    void categoryMatch_caseInsensitive() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Martlock","category":"Häute","bonus":0.20}
                ]
                """);

        assertThat(r.findBestCity("häute", "X")).isEqualTo("Martlock");
    }

    /**
     * No category match and no generic bonus → return the fallback.
     */
    @Test
    void noMatch_anywhere_returnsFallback() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.10}
                ]
                """);

        assertThat(r.findBestCity("Unbekannt", "Bridgewatch")).isEqualTo("Bridgewatch");
    }

    /**
     * No category match, but a city with a positive generic bonus exists →
     * the highest generic-bonus city wins.
     */
    @Test
    void noCategoryMatch_fallsBackToHighestPositiveGeneric() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.10},
                  {"cityName":"Martlock","category":null,"bonus":0.05},
                  {"cityName":"FortSterling","category":null,"bonus":0.07}
                ]
                """);

        assertThat(r.findBestCity("Unbekannt", "X")).isEqualTo("FortSterling");
    }

    /**
     * Generic-bonus cities that all have a zero bonus must NOT be selected —
     * the caller is asking for the best, and zero is no improvement over
     * the fallback.
     */
    @Test
    void onlyZeroGenericBonuses_returnsFallback() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.10},
                  {"cityName":"Martlock","category":null,"bonus":0.0},
                  {"cityName":"FortSterling","category":null,"bonus":0.0}
                ]
                """);

        assertThat(r.findBestCity("Unbekannt", "Bridgewatch")).isEqualTo("Bridgewatch");
    }

    /**
     * Passing {@code null} for the category selects the best generic bonus.
     */
    @Test
    void nullCategory_selectsBestGeneric() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Martlock","category":null,"bonus":0.05},
                  {"cityName":"FortSterling","category":null,"bonus":0.07}
                ]
                """);

        assertThat(r.findBestCity(null, "X")).isEqualTo("FortSterling");
    }

    /**
     * With {@code null} category and only category-specific bonuses
     * present (no generic at all), the fallback wins.
     */
    @Test
    void nullCategory_noGenericEntry_returnsFallback() {
        CityBonusRepository r = repo("""
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.10}
                ]
                """);

        assertThat(r.findBestCity(null, "Bridgewatch")).isEqualTo("Bridgewatch");
    }
}