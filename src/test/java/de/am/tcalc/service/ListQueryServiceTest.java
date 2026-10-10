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

    // application.yml values; the record has no code-side fallbacks.
    private static final de.am.tcalc.config.PriceProperties PROPS =
        new de.am.tcalc.config.PriceProperties(
            "https://test.local", "/api/v2/stats/history", 28, "1,2,3,4", 1000, 2000, 1, 2500);

    private final RecipeLoader loader = new RecipeLoader(
        new org.springframework.core.io.DefaultResourceLoader(),
        new tools.jackson.databind.json.JsonMapper());

    private void load() {
        loader.load();
    }

    private PriceService priceService(int units28d, String quality1Price, String quality2Price) {
        return new PriceService(MarketHistoryFixtures.stub(units28d, quality1Price, quality2Price), PROPS);
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

    /** German collation, not Java string order: umlauts sort in place (Fischer-Order). */
    @Test
    void categoriesSortUmlautsAlphabetically() {
        load();
        ListQueryService service = service(priceService(0, "0", "0"));

        List<String> categories = List.copyOf(service.categories());

        // 'ä' sorts like 'a' — plain string order would push Fährtensuche BEHIND Fisch
        assertThat(categories.indexOf("Fackeln")).isLessThan(categories.indexOf("Fährtensuche"));
        assertThat(categories.indexOf("Fährtensuche")).isLessThan(categories.indexOf("Fisch"));
        assertThat(categories.indexOf("Hämmer")).isLessThan(categories.indexOf("Häute"));
        assertThat(categories.subList(0, 4))
            .containsExactly("Arkanstäbe", "Armbrüste", "Äxte", "Bögen");
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

    /** Unrestricted pages fetch NO market data (429 throttle) and sort alphabetically. */
    @Test
    void rowsAllCategoryFetchesNoPricesSortsAlphabeticallyAndCapsAtSize() {
        load();
        // fixture would deliver 20.0 units/day — any nonzero value means a price fetch happened
        ListQueryService service = service(priceService(280, "100", "999"));

        List<ListQueryService.ListRow> rows = service.rows("Brecilien", ListQueryService.ALL, ListQueryService.ALL, 2, 100);

        assertThat(rows).hasSize(100);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.unitsPerDay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(r.price()).isNull();
        });
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rows.get(i - 1).name())
                .isLessThanOrEqualTo(rows.get(i).name());
        }
        assertThat(ListQueryService.isRestricted(null, null)).isFalse();
        assertThat(ListQueryService.isRestricted(ListQueryService.ALL, ListQueryService.ALL)).isFalse();
        assertThat(ListQueryService.isRestricted("Bögen", ListQueryService.ALL)).isTrue();
        assertThat(ListQueryService.isRestricted(ListQueryService.ALL, "Langbögen")).isTrue();
    }

    @Test
    void rowsWithoutMarketDataSortLastWithZeroAndNullPrice() {
        load();
        // stub returns nothing → every recipe has no stats
        ListQueryService service = service(new PriceService(MarketHistoryFixtures.stubEmpty(),
            PROPS));

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