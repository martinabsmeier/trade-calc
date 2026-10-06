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

import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.service.calculator.CityBonusProvider;
import de.am.albion.tradecalc.service.calculator.PriceLookup;
import de.am.albion.tradecalc.service.calculator.ProfitStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@link ProfitCalculationService}: it must dispatch each
 * {@link CalculationMode} to the matching {@link ProfitStrategy}.
 */
class ProfitCalculationServiceTest {

    /**
     * Each {@link CalculationMode} routes to the matching strategy.
     */
    @Test
    void dispatchesEachModeToItsStrategy() {
        ProfitStrategy a = stubFor(CalculationMode.LOCAL_ONLY);
        ProfitStrategy b = stubFor(CalculationMode.LOCAL_BUY_BEST_REST);
        ProfitStrategy c = stubFor(CalculationMode.BEST_OF_ALL);
        ProfitCalculationService service = new ProfitCalculationService(List.of(a, b, c));

        assertThat(service.strategyFor(CalculationMode.LOCAL_ONLY)).isSameAs(a);
        assertThat(service.strategyFor(CalculationMode.LOCAL_BUY_BEST_REST)).isSameAs(b);
        assertThat(service.strategyFor(CalculationMode.BEST_OF_ALL)).isSameAs(c);
    }

    /**
     * If a {@link CalculationMode} is not covered by a strategy, the service
     * fails fast at construction.
     */
    @Test
    void missingStrategy_failsFast() {
        ProfitStrategy a = stubFor(CalculationMode.LOCAL_ONLY);
        ProfitStrategy c = stubFor(CalculationMode.BEST_OF_ALL);

        assertThatThrownBy(() -> new ProfitCalculationService(List.of(a, c)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCAL_BUY_BEST_REST");
    }

    /**
     * Tiny {@link ProfitStrategy} stub that just advertises a given mode.
     * Calculation itself is not exercised here — that is covered by the
     * dedicated strategy unit tests.
     */
    private static ProfitStrategy stubFor(CalculationMode mode) {
        CityBonusProvider noBonuses = new CityBonusProvider() {
            @Override
            public BigDecimal bonusFor(String city, String category) {
                return BigDecimal.ZERO;
            }

            @Override
            public String bestCityFor(String category, String fallback) {
                return fallback;
            }
        };
        return new ProfitStrategy() {
            @Override
            public CalculationMode mode() {
                return mode;
            }

            @Override
            public Optional<ProfitResult> calculate(Recipe recipe, String homeCity, PriceLookup prices) {
                return Optional.empty();
            }
        };
    }
}