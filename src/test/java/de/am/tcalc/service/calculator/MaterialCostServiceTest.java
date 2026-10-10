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
package de.am.tcalc.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;


import de.am.tcalc.config.PriceProperties;
import de.am.tcalc.dataprovider.market.MarketPriceHistoryClient;
import de.am.tcalc.dataprovider.recipe.RecipeLoader;
import de.am.tcalc.domain.MaterialCost;
import de.am.tcalc.service.PriceService;
import de.am.tcalc.service.RecipeService;
import de.am.tcalc.testsupport.MarketHistoryFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Real recipe book, stubbed market history (never the real API).
 * Spec example (Variante 1): the longbow resolves to 32 × T5_PLANKS.
 */
class MaterialCostServiceTest {

    // application.yml values; the record has no code-side fallbacks.
    private static final PriceProperties TEST_PROPS = new PriceProperties(
        "https://test.local", "/api/v2/stats/history", 28, "1,2,3,4", 1000, 2000, 1, 2500);

    private final RecipeLoader loader = new RecipeLoader(
        new org.springframework.core.io.DefaultResourceLoader(),
        new tools.jackson.databind.json.JsonMapper());

    @Test
    void longbowCostIsCountTimesUnitPriceOfNormalQuality() {
        loader.load();
        MarketPriceHistoryClient stub = (ids, location) -> List.of(
            MarketHistoryFixtures.seriesAt(location, ids.get(0), 1, MarketHistoryFixtures.entry(100, "10")));
        MaterialCostService service = service(stub);

        MaterialCost cost = service.cost("T5_2H_LONGBOW", null, "Thetford");

        assertThat(cost).isNotNull();
        assertThat(cost.location()).isEqualTo("Thetford");
        assertThat(cost.total()).isEqualByComparingTo("320.00"); // 32 × 10
        assertThat(cost.lines()).hasSize(1);
        assertThat(cost.lines().get(0).item()).isEqualTo("T5_PLANKS");
        assertThat(cost.lines().get(0).lineCost()).isEqualByComparingTo("320.00");
    }

    @Test
    void missingIngredientPriceYieldsNoTotal() {
        loader.load();
        // Market knows nothing → unitPrice null → total must be null, not 0.
        MaterialCostService service = service(MarketHistoryFixtures.stubEmpty());

        MaterialCost cost = service.cost("T5_2H_LONGBOW", null, "Lymhurst");

        assertThat(cost).isNotNull();
        assertThat(cost.lines().get(0).unitPrice()).isNull();
        assertThat(cost.lines().get(0).lineCost()).isNull();
        assertThat(cost.total()).isNull();
    }

    @Test
    void enchantmentVariantsMatchByLevel() {
        loader.load();
        MaterialCostService service = service(MarketHistoryFixtures.stubEmpty());

        // T5_2H_LONGBOW exists as base (enchantmentLevel null) and .1..4 variants — pick level 2
        assertThat(service.cost("T5_2H_LONGBOW", 2, "Lymhurst").enchantmentLevel()).isEqualTo(2);
        // unknown recipe → null
        assertThat(service.cost("T4_NOT_A_REAL_ITEM", null, "Lymhurst")).isNull();
    }

    private MaterialCostService service(MarketPriceHistoryClient client) {
        return new MaterialCostService(new RecipeService(loader), new PriceService(client,
            TEST_PROPS));
    }
}