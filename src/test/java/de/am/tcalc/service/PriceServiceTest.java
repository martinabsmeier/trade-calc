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

import static de.am.tcalc.testsupport.MarketHistoryFixtures.entry;
import static de.am.tcalc.testsupport.MarketHistoryFixtures.series;

import de.am.tcalc.config.PriceProperties;
import de.am.tcalc.dataprovider.market.MarketHistoryItem;
import de.am.tcalc.dataprovider.market.MarketPriceHistoryClient;
import de.am.tcalc.domain.ItemPriceStats;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PriceServiceTest {

    // Same values as application.yml — the record has no code-side fallbacks.
    private static final PriceProperties PROPS = new PriceProperties(
        "https://test.local", "/api/v2/stats/history", 28, "1,2,3,4", 1000, 2000, 1, 2500);

    @Test
    void computesUnitsPerDaySummedOverQualitiesAndWeightedPrice() {
        // Spec math: unitsPerDay = Σ item_count ÷ 28 (qualities summed);
        // price = Σ(count×price) ÷ Σcount, volume-weighted, scale 2, HALF_UP.
        MarketPriceHistoryClient stub = (ids, location) -> List.of(
            series("T4_2H_BOW", 1, entry(91, "3289"), entry(2, "3000"), entry(0, "5000"), entry(1, null)),
            series("T4_2H_BOW", 2, entry(10, "1000"))
        );
        PriceService service = new PriceService(stub, PROPS);

        Map<String, ItemPriceStats> stats = service.stats(List.of("T4_2H_BOW"), "Lymhurst");

        assertThat(stats).containsOnlyKeys("T4_2H_BOW");
        ItemPriceStats bow = stats.get("T4_2H_BOW");
        // 91 + 2 + 10 = 103 units → 103/28 = 3.678… → 3.7
        assertThat(bow.unitsPerDay()).isEqualByComparingTo("3.7");
        // Quality 1: (91×3289 + 2×3000) ÷ 93 = 305299/93 = 3282.784… → 3282.78; zero/null entries skipped
        assertThat(bow.priceByQuality().get(1)).isEqualByComparingTo("3282.78");
        // Quality 2: flat 1000.00
        assertThat(bow.priceByQuality().get(2)).isEqualByComparingTo("1000.00");
        assertThat(bow.priceByQuality()).containsOnlyKeys(1, 2);
    }

    @Test
    void statConvenienceReturnsSingleItemOrMissing() {
        MarketPriceHistoryClient stub = (ids, location) -> List.of(series("T4_2H_BOW", 1, entry(28, "100")));
        PriceService service = new PriceService(stub, PROPS);

        assertThat(service.stat("T4_2H_BOW", "Lymhurst").unitsPerDay())
            .isEqualByComparingTo("1.0");
        // Unknown item → null, not a crash.
        assertThat(service.stat("T4_NOT_A_REAL_ITEM", "Lymhurst")).isNull();
    }

    @Test
    void emptyInputShortcircuitsAndZeroVolumeIsAbsent() {
        PriceService service = new PriceService((ids, location) -> List.of(series("T4_2H_BOW", 1, entry(0, "100"))), PROPS);

        assertThat(service.stats(List.of(), "Lymhurst")).isEmpty();
        assertThat(service.stats(null, "Lymhurst")).isEmpty();
        // Item has a series but zero sales volume → no stats entry.
        assertThat(service.stat("T4_2H_BOW", "Lymhurst")).isNull();
    }

    @Test
    void seriesWithoutItemIdIsIgnored() {
        MarketPriceHistoryClient stub = (ids, location) -> List.of(
            new MarketHistoryItem("Lymhurst", null, 1, List.of(entry(5, "100"))));
        PriceService service = new PriceService(stub, PROPS);

        assertThat(service.stats(List.of("T4_2H_BOW"), "Lymhurst")).isEmpty();
    }
}
