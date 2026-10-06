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
package de.am.albion.tradecalc.service;

import de.am.albion.tradecalc.repository.CityBonusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BonusService}: focuses on the lookup logic and the
 * zero-fallback behaviour for cities that offer no bonus for the given category.
 */
class BonusServiceTest {

    private BonusService service;

    /**
     * Builds the repository + service with a small synthetic data set.
     */
    @BeforeEach
    void setUp() {
        String json = """
                [
                  {"cityName":"Lymhurst","category":"Bögen","bonus":0.183},
                  {"cityName":"Caerleon","category":null,"bonus":0.072}
                ]
                """;
        CityBonusRepository repo = CityBonusRepository.forTest(json);
        service = new BonusService(repo);
    }

    /**
     * A known city + category returns the stored bonus as a {@link BigDecimal}.
     */
    @Test
    void getBonus_returnsConfiguredValue() {
        assertThat(service.getBonus("Lymhurst", "Bögen")).isEqualByComparingTo("0.183");
    }

    /**
     * Unknown city returns {@link BigDecimal#ZERO} (never {@code null}).
     */
    @Test
    void getBonus_unknownCity_returnsZero() {
        assertThat(service.getBonus("Atlantis", "Bögen")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Unknown city without a generic bonus still returns {@code ZERO}.
     */
    @Test
    void getBonus_unknownCityNoGeneric_returnsZero() {
        assertThat(service.getBonus("Atlantis", null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}