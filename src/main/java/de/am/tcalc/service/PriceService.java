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

import de.am.tcalc.config.PriceProperties;
import de.am.tcalc.dataprovider.market.MarketHistoryItem;
import de.am.tcalc.dataprovider.market.MarketPriceHistoryClient;
import de.am.tcalc.domain.ItemPriceStats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Turns hourly sell history into the spec's list-view numbers:
 * unitsPerDay = Σ item_count ÷ history window (all qualities summed), price per quality =
 * volume-weighted Σ(item_count × avg_price) ÷ Σ item_count. One batched API call per list page.
 * Monetary math uses BigDecimal (price scale 2, units scale 1, HALF_UP).
 */
@Service
@RequiredArgsConstructor
public class PriceService {

    private final MarketPriceHistoryClient client;
    private final PriceProperties properties;

    /**
     * Batched stats for the list view. Cached on the (ids, location) pair — every page of 25/50/100
     * triggers at most one API call per cache window.
     *
     * @param itemIds exact dump ids to look up
     * @param location city name as spelled in the API (one of the spec's selectable cities)
     * @return stats per item id that the API knows, keyed by item id; unknown items have no entry
     */
    @Cacheable("prices")
    public Map<String, ItemPriceStats> stats(List<String> itemIds, String location) {
        Map<String, ItemPriceStats> out = new LinkedHashMap<>();
        if (itemIds == null || itemIds.isEmpty()) {
            return out;
        }
        List<MarketHistoryItem> history = client.history(itemIds, location);
        // The API returns one series per (item, quality) — a single item arrives as several
        // series objects. Aggregate those into ONE stats entry per item, never overwrite.
        Map<String, List<MarketHistoryItem>> byItem = new LinkedHashMap<>();
        for (MarketHistoryItem series : history) {
            if (series.itemId() == null) {
                continue;
            }
            byItem.computeIfAbsent(series.itemId(), k -> new ArrayList<>()).add(series);
        }
        byItem.forEach((itemId, seriesList) -> {
            ItemPriceStats computed = compute(itemId, seriesList);
            if (computed != null) {
                out.put(itemId, computed);
            }
        });
        return out;
    }

    /**
     * Convenience single-item view (e.g. the "Berechnen" panel). Delegates to the cached
     * {@link #stats(List, String)} with a one-element list — a dedicated cache entry per item.
     *
     * @param itemId exact dump id, e.g. "T4_2H_BOW@1" for an enchanted variant
     * @param location city name as spelled in the API
     * @return stats for the item, or {@code null} when the API knows nothing about it
     */
    public ItemPriceStats stat(String itemId, String location) {
        return stats(List.of(itemId), location).get(itemId);
    }

    /** Per-quality accumulator: total units and Σ(units × price). */
    private record QualitySum(BigDecimal units, BigDecimal value) {

        QualitySum add(BigDecimal unitCount, BigDecimal price) {
            return new QualitySum(units.add(unitCount), value.add(unitCount.multiply(price)));
        }
    }

    private ItemPriceStats compute(String itemId, List<MarketHistoryItem> seriesList) {
        long totalUnits = 0;
        Map<Integer, QualitySum> perQuality = new HashMap<>();
        for (MarketHistoryItem series : seriesList) {
            for (MarketHistoryItem.MarketHistoryEntry e : series.data()) {
                if (e.itemCount() <= 0 || e.avgPrice() == null) {
                    continue;
                }
                BigDecimal units = BigDecimal.valueOf(e.itemCount());
                perQuality.put(series.quality(), perQuality
                    .getOrDefault(series.quality(), new QualitySum(BigDecimal.ZERO, BigDecimal.ZERO))
                    .add(units, e.avgPrice()));
                totalUnits += e.itemCount();
            }
        }
        if (totalUnits == 0) {
            return null;
        }
        // Commercial rounding (HALF_UP) per project convention: units scale 1, money scale 2.
        BigDecimal unitsPerDay = BigDecimal.valueOf(totalUnits)
            .divide(BigDecimal.valueOf(properties.historyDays()), 1, RoundingMode.HALF_UP);
        Map<Integer, BigDecimal> prices = new LinkedHashMap<>();
        perQuality.forEach((quality, sum) -> prices.put(quality,
            sum.value().divide(sum.units(), 2, RoundingMode.HALF_UP)));
        return new ItemPriceStats(itemId, unitsPerDay, prices);
    }
}