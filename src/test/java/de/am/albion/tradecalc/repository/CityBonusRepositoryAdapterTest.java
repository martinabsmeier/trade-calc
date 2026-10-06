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
 * Focused tests for the defensive guards in
 * {@link CityBonusRepositoryAdapter#bonusFor(String, String)}.
 *
 * <p>The happy path (real city, real category, real bonus) is exercised
 * indirectly by every strategy test; this file only pins the early-return
 * branches so a regression in the null handling shows up locally.</p>
 */
class CityBonusRepositoryAdapterTest {

    private static final String BONUSES = """
            [
              {"cityName":"Martlock","category":null,"bonus":0.05},
              {"cityName":"Martlock","category":"Bögen","bonus":0.15}
            ]
            """;

    @Test
    void bonusFor_knownCity_returnsCategoryBonus() {
        CityBonusRepositoryAdapter a = new CityBonusRepositoryAdapter(CityBonusRepository.forTest(BONUSES));

        assertThat(a.bonusFor("Martlock", "Bögen")).isEqualByComparingTo("0.15");
    }

    @Test
    void bonusFor_knownCity_nullCategory_returnsGenericBonus() {
        CityBonusRepositoryAdapter a = new CityBonusRepositoryAdapter(CityBonusRepository.forTest(BONUSES));

        assertThat(a.bonusFor("Martlock", null)).isEqualByComparingTo("0.05");
    }

    @Test
    void bonusFor_unknownCity_returnsZero() {
        CityBonusRepositoryAdapter a = new CityBonusRepositoryAdapter(CityBonusRepository.forTest(BONUSES));

        assertThat(a.bonusFor("Atlantis", null)).isEqualByComparingTo("0");
    }

    @Test
    void bonusFor_nullCity_returnsZero() {
        CityBonusRepositoryAdapter a = new CityBonusRepositoryAdapter(CityBonusRepository.forTest(BONUSES));

        assertThat(a.bonusFor(null, "Bögen")).isEqualByComparingTo("0");
    }
}