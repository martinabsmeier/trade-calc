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
package de.am.albion.tradecalc.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link CalculationMode} enum: ensures all three expected
 * modes exist, in the documented order, and that descriptions are present.
 */
class CalculationModeTest {

    /**
     * All three calculation modes from the design doc are present.
     */
    @Test
    void hasAllThreeModes() {
        assertThat(CalculationMode.values())
                .containsExactly(
                        CalculationMode.LOCAL_ONLY,
                        CalculationMode.LOCAL_BUY_BEST_REST,
                        CalculationMode.BEST_OF_ALL);
    }

    /**
     * Every mode has a non-empty description — drives the UI labels.
     */
    @Test
    void descriptionsAreNotBlank() {
        for (CalculationMode mode : CalculationMode.values()) {
            assertThat(mode.getDescription()).isNotBlank();
        }
    }

    /**
     * Round-trip: {@link CalculationMode#valueOf(String)} works for every entry.
     */
    @Test
    void valueOf_findsEachMode() {
        for (CalculationMode mode : CalculationMode.values()) {
            assertThat(CalculationMode.valueOf(mode.name())).isSameAs(mode);
        }
    }
}