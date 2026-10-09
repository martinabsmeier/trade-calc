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
package de.am.tcalc.testsupport;

import de.am.tcalc.dataprovider.market.MarketHistoryItem;
import de.am.tcalc.dataprovider.market.MarketHistoryItem.MarketHistoryEntry;
import de.am.tcalc.dataprovider.market.MarketPriceHistoryClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder-style fixtures for market-history stubs. Domain records carry no Lombok {@code @Builder}
 * (project rule), so test data is built through these factory helpers instead — every place that
 * stubs a {@link MarketPriceHistoryClient} goes through this class.
 */
public final class MarketHistoryFixtures {

    private static final String TS = "2026-10-09T12:00:00";

    private MarketHistoryFixtures() {
    }

    /** One hourly bucket; price null models an unusable entry. */
    public static MarketHistoryEntry entry(int count, String price) {
        return new MarketHistoryEntry(count, price == null ? null : new BigDecimal(price), TS);
    }

    /** One (item, quality) series in Lymhurst by default. */
    public static MarketHistoryItem series(String itemId, int quality, MarketHistoryEntry... entries) {
        return seriesAt("Lymhurst", itemId, quality, entries);
    }

    public static MarketHistoryItem seriesAt(String location, String itemId, int quality,
                                             MarketHistoryEntry... entries) {
        return new MarketHistoryItem(location, itemId, quality, List.of(entries));
    }

    /**
     * Stub client: every requested id answers with one series per price level — level 0 becomes
     * quality 1, level 1 quality 2, and so on (mirrors the real API shape: a series per quality).
     */
    public static MarketPriceHistoryClient stub(int units, String... pricesPerQuality) {
        return (itemIds, location) -> {
            List<MarketHistoryItem> out = new ArrayList<>();
            if (itemIds == null) {
                return out;
            }
            for (String id : itemIds) {
                for (int level = 0; level < pricesPerQuality.length; level++) {
                    out.add(new MarketHistoryItem(location, id, level + 1,
                        List.of(entry(units, pricesPerQuality[level]))));
                }
            }
            return out;
        };
    }

    /** Stub client that knows nothing — mirrors "item not traded". */
    public static MarketPriceHistoryClient stubEmpty() {
        return (itemIds, location) -> List.of();
    }
}