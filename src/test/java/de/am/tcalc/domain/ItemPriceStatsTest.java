/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.tcalc.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ItemPriceStatsTest {

    @Test
    void nullPriceMapBecomesEmptyAndIsImmutable() {
        ItemPriceStats stats = new ItemPriceStats("T4_2H_BOW", BigDecimal.ONE, null);

        assertThat(stats.priceByQuality()).isEmpty();
    }
}