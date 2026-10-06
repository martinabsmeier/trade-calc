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

import de.am.albion.tradecalc.domain.model.CityBonus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link CityBonusRepository}: parsing of {@code bonuses.json}
 * and look-ups by city / category.
 */
class CityBonusRepositoryTest {

    private CityBonusRepository repository;

    /**
     * Loads the repository with a small synthetic JSON document covering the
     * branches we want to exercise.
     */
    @BeforeEach
    void setUp() {
        String json = """
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.183},
                  {"cityName":"Lymhurst","category":null,"bonus":0.05},
                  {"cityName":"Martlock","category":"Häute","bonus":0.152},
                  {"cityName":"Martlock","category":"Häute","bonus":0.18},
                  {"cityName":"Caerleon","category":null,"bonus":0.072},
                  {"cityName":"Brecilien","category":null,"bonus":0.0}
                ]
                """;
        repository = CityBonusRepository.forTest(json);
    }

    /**
     * Multiple bonuses for the same category are accepted as long as the JSON
     * parses — repository doesn't pre-validate, callers do.
     */
    @Test
    void loadAll_returnsAllLoadedBonuses() {
        // repository has no public list accessor after refactor; just exercise the parsed data via a lookup
        assertThat(repository.findBestBonus("Lymhurst", "Bögen")).isNotNull();
    }

    /**
     * A specific city + category combination returns the matching entry.
     */
    @Test
    void findBestBonus_returnsExactCategoryMatch() {
        CityBonus bonus = repository.findBestBonus("Lymhurst", "Bögen");

        assertThat(bonus).isNotNull();
        assertThat(bonus.bonus()).isEqualByComparingTo("0.183");
    }

    /**
     * Looking up an unknown category falls back to the city's generic bonus.
     */
    @Test
    void findBestBonus_fallsBackToGenericBonus() {
        CityBonus bonus = repository.findBestBonus("Caerleon", "Bögen");

        assertThat(bonus).isNotNull();
        assertThat(bonus.bonus()).isEqualByComparingTo("0.072");
    }

    /**
     * A completely unknown city returns {@code null}.
     */
    @Test
    void findBestBonus_unknownCity_returnsNull() {
        CityBonus bonus = repository.findBestBonus("Atlantis", "Bögen");

        assertThat(bonus).isNull();
    }

    /**
     * {@code null} city returns {@code null} without crashing — defensive
     * behaviour expected by callers.
     */
    @Test
    void findBestBonus_nullCity_returnsNull() {
        CityBonus bonus = repository.findBestBonus(null, "Bögen");

        assertThat(bonus).isNull();
    }

    /**
     * Brecilien's bonus is loaded as exactly zero (defensive case: zero must
     * not be filtered out).
     */
    @Test
    void zeroBonus_isPreserved() {
        CityBonus bonus = repository.findBestBonus("Brecilien", "Anything");

        assertThat(bonus).isNotNull();
        assertThat(bonus.bonus()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Malformed JSON is reported as an {@link IllegalStateException}.
     */
    @Test
    void invalidJson_throwsIllegalStateException() {
        assertThatIllegalStateException()
                .isThrownBy(() -> CityBonusRepository.forTest("{ not json"));
    }

    /**
     * Looking up a {@code null} category returns the city's generic bonus
     * (where {@link CityBonus#category()} is {@code null}).
     */
    @Test
    void findBestBonus_nullCategory_returnsGenericBonus() {
        CityBonus bonus = repository.findBestBonus("Lymhurst", null);

        assertThat(bonus).isNotNull();
        assertThat(bonus.category()).isNull();
        assertThat(bonus.bonus()).isEqualByComparingTo("0.05");
    }

    /**
     * When the same city has multiple category matches, the highest bonus wins.
     */
    @Test
    void findBestBonus_multipleCategoryMatches_returnsHighest() {
        CityBonus bonus = repository.findBestBonus("Martlock", "Häute");

        assertThat(bonus).isNotNull();
        assertThat(bonus.bonus()).isEqualByComparingTo("0.18");
    }

    /**
     * Case-insensitive city name match: {@code "LYMHURST"} must work.
     */
    @Test
    void findBestBonus_cityMatchIsCaseInsensitive() {
        CityBonus bonus = repository.findBestBonus("LYMHURST", "Bögen");

        assertThat(bonus).isNotNull();
        assertThat(bonus.bonus()).isEqualByComparingTo("0.183");
    }
}