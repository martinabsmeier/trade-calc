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
     * Each {@link CalculationMode} routes to the matching strategy. The stub
     * strategies encode their mode into the result's {@code city()} field,
     * so dispatch can be observed end-to-end through the public API.
     */
    @Test
    void dispatchesEachModeToItsStrategy() {
        ProfitStrategy a = stubFor(CalculationMode.LOCAL_ONLY);
        ProfitStrategy b = stubFor(CalculationMode.LOCAL_BUY_BEST_REST);
        ProfitStrategy c = stubFor(CalculationMode.BEST_OF_ALL);
        ProfitCalculationService service = new ProfitCalculationService(List.of(a, b, c));

        assertThat(service.calculate(CalculationMode.LOCAL_ONLY, RECIPE, "Lymhurst", EMPTY))
                .map(ProfitResult::city).contains("LOCAL_ONLY");
        assertThat(service.calculate(CalculationMode.LOCAL_BUY_BEST_REST, RECIPE, "Lymhurst", EMPTY))
                .map(ProfitResult::city).contains("LOCAL_BUY_BEST_REST");
        assertThat(service.calculate(CalculationMode.BEST_OF_ALL, RECIPE, "Lymhurst", EMPTY))
                .map(ProfitResult::city).contains("BEST_OF_ALL");
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

    /** Minimal recipe with no materials — every stub returns empty for it. */
    private static final Recipe RECIPE = new Recipe("T4_BOW", 4, null, null, List.of());

    /** Stub {@link PriceLookup} the stubs ignore. */
    private static final PriceLookup EMPTY = itemId -> java.util.Map.of();

    /**
     * Tiny {@link ProfitStrategy} stub that advertises its mode and tags the
     * returned {@link ProfitResult#city()} with it, so dispatch tests can see
     * which strategy the service picked.
     */
    private static ProfitStrategy stubFor(CalculationMode mode) {
        return new ProfitStrategy() {
            @Override
            public CalculationMode mode() {
                return mode;
            }

            @Override
            public Optional<ProfitResult> calculate(Recipe recipe, String homeCity, PriceLookup prices) {
                return Optional.of(ProfitResult.builder()
                        .itemId(recipe.itemId())
                        .city(mode.name())
                        .totalCost(BigDecimal.ZERO)
                        .revenue(BigDecimal.ZERO)
                        .profit(BigDecimal.ZERO)
                        .profitRatio(BigDecimal.ZERO)
                        .build());
            }
        };
    }
}