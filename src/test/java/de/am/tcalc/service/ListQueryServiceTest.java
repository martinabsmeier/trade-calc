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
package de.am.tcalc.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.am.tcalc.dataprovider.recipe.RecipeLoader;
import de.am.tcalc.testsupport.MarketHistoryFixtures;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Real recipe book on the classpath, stubbed market history (never the real API).
 */
class ListQueryServiceTest {

    private final RecipeLoader loader = new RecipeLoader(
        new org.springframework.core.io.DefaultResourceLoader(),
        new tools.jackson.databind.json.JsonMapper());

    private void load() {
        loader.load();
    }

    private PriceService priceService(int units28d, String quality1Price, String quality2Price) {
        de.am.tcalc.config.PriceProperties props = new de.am.tcalc.config.PriceProperties(null, null, 0, null, 0, 0);
        return new PriceService(MarketHistoryFixtures.stub(units28d, quality1Price, quality2Price), props);
    }

    private ListQueryService service(PriceService priceService) {
        return new ListQueryService(new RecipeService(loader), priceService);
    }

    @Test
    void categoriesAndSubcategoriesComeFromRecipeData() {
        load();
        ListQueryService service = service(priceService(0, "0", "0"));

        assertThat(service.categories()).contains("Bögen", "Essen", "Plattenrüstungen");
        assertThat(service.categories()).doesNotContainNull();
        assertThat(service.categories()).doesNotContain("Alle");

        // Bags have no subcategory — "Alle" lists it, a specific subcategory list must not.
        assertThat(service.subcategories("Taschen")).isEmpty();
        assertThat(service.subcategories("Bögen"))
            .contains("Bögen", "Langbögen", "Avalon-Bögen");
        assertThat(service.subcategories(ListQueryService.ALL))
            .contains("Suppen", "Sicheln", "Gegrillter Fisch");
    }

    @Test
    void rowsFilterByCategoryAndSubcategorySortByUnitsAndPage() {
        load();
        ListQueryService service = service(priceService(280, "100", "999"));

        List<ListQueryService.ListRow> rows = service.rows("Lymhurst", "Bögen", "Langbögen", 1, 25);

        assertThat(rows).hasSizeLessThanOrEqualTo(25);
        assertThat(rows).isNotEmpty();
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rows.get(i - 1).unitsPerDay())
                .isGreaterThanOrEqualTo(rows.get(i).unitsPerDay());
        }
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.marketItemId()).startsWith("T");        // dump ids, e.g. T4_2H_LONGBOW@1
            assertThat(r.unitsPerDay()).isEqualByComparingTo("20.0"); // (280+280) ÷ 28, qualities summed
        });
        // requested quality 1 → price of quality 1, not the higher quality's price
        assertThat(rows).anySatisfy(r -> assertThat(r.price()).isEqualByComparingTo("100"));
        assertThat(rows).anySatisfy(r -> assertThat(r.enchantmentLevel()).isEqualTo(0));
        assertThat(rows).anySatisfy(r -> assertThat(r.enchantmentLevel()).isPositive());
    }

    @Test
    void rowsAllCategoryIncludesItemsWithoutSubcategoryAndCapsAtSize() {
        load();
        ListQueryService service = service(priceService(280, "100", "999"));

        List<ListQueryService.ListRow> rows = service.rows("Brecilien", ListQueryService.ALL, ListQueryService.ALL, 2, 100);

        assertThat(rows).hasSize(100);
        assertThat(rows.stream().anyMatch(r -> r.name().startsWith("Tasche")))
            .as("no-subcategory items are included under 'Alle'")
            .isTrue();
    }

    @Test
    void rowsWithoutMarketDataSortLastWithZeroAndNullPrice() {
        load();
        // stub returns nothing → every recipe has no stats
        ListQueryService service = service(new PriceService(MarketHistoryFixtures.stubEmpty(),
            new de.am.tcalc.config.PriceProperties(null, null, 0, null, 0, 0)));

        List<ListQueryService.ListRow> rows = service.rows("Caerleon", "Bögen", "Alle", 4, 25);

        assertThat(rows).hasSizeLessThanOrEqualTo(25);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.unitsPerDay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(r.price()).isNull();
        });
    }

    @Test
    void schwarzerMarktMapsToApiBlackMarketAndConstantsAreStable() {
        assertThat(ListQueryService.apiLocation("Schwarzer Markt")).isEqualTo("Black Market");
        assertThat(ListQueryService.apiLocation("Lymhurst")).isEqualTo("Lymhurst");
        assertThat(ListQueryService.QUALITIES.get(1)).isEqualTo("Normal");
        assertThat(ListQueryService.SIZES).containsExactly(25, 50, 100);
        assertThat(ListQueryService.CITIES).hasSize(8);
    }
}