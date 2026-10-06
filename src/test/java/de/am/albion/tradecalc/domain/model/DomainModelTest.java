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
package de.am.albion.tradecalc.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for every Lombok-built domain type. The records carry no logic;
 * these tests simply exercise their generated builders and accessors so JaCoCo
 * counts them as covered.
 */
class DomainModelTest {

    @Test
    void city_exposesName() {
        City city = new City("Lymhurst");
        assertThat(city.name()).isEqualTo("Lymhurst");
    }

    @Test
    void item_isBuiltViaBuilder() {
        Item item = Item.builder()
                .id("T4_BOW")
                .tier(4)
                .category("Bows")
                .build();
        assertThat(item.id()).isEqualTo("T4_BOW");
        assertThat(item.tier()).isEqualTo(4);
        assertThat(item.category()).isEqualTo("Bows");
    }

    @Test
    void marketPrice_isBuiltViaBuilder() {
        MarketPrice price = MarketPrice.builder()
                .itemId("T4_BOW")
                .city("Lymhurst")
                .quality(1)
                .buyPriceMax(new BigDecimal("1500"))
                .sellPriceMin(new BigDecimal("1400"))
                .observedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        assertThat(price.itemId()).isEqualTo("T4_BOW");
        assertThat(price.city()).isEqualTo("Lymhurst");
        assertThat(price.sellPriceMin()).isEqualByComparingTo("1400");
    }

    @Test
    void profitResult_isBuiltViaBuilder() {
        ProfitResult result = ProfitResult.builder()
                .itemId("T4_BOW")
                .city("Lymhurst")
                .totalCost(new BigDecimal("1000"))
                .revenue(new BigDecimal("1500"))
                .profit(new BigDecimal("500"))
                .profitRatio(new BigDecimal("0.50"))
                .build();
        assertThat(result.itemId()).isEqualTo("T4_BOW");
        assertThat(result.profit()).isEqualByComparingTo("500");
        assertThat(result.profitRatio()).isEqualByComparingTo("0.50");
    }

    @Test
    void craftingStep_isBuiltViaBuilder() {
        CraftingStep step = CraftingStep.builder()
                .step("CRAFT")
                .itemId("T4_BOW")
                .city("Lymhurst")
                .quantity(1)
                .cost(new BigDecimal("1000"))
                .build();
        assertThat(step.step()).isEqualTo("CRAFT");
        assertThat(step.cost()).isEqualByComparingTo("1000");
    }

    @Test
    void craftingPlan_isBuiltViaBuilder() {
        Item target = Item.builder().id("T4_BOW").tier(4).build();
        ProfitResult profit = ProfitResult.builder()
                .itemId("T4_BOW").city("Lymhurst")
                .totalCost(BigDecimal.ZERO).revenue(BigDecimal.ZERO)
                .profit(BigDecimal.ZERO).profitRatio(BigDecimal.ZERO)
                .build();
        CraftingPlan plan = CraftingPlan.builder()
                .targetItem(target)
                .profitSummary(profit)
                .steps(List.of())
                .build();
        assertThat(plan.targetItem().id()).isEqualTo("T4_BOW");
        assertThat(plan.steps()).isEmpty();
    }
}