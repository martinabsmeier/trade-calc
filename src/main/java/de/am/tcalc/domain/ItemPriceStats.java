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

import java.math.BigDecimal;
import java.util.Map;

/**
 * Four-week market statistics for one item in one city, per the spec's list view.
 *
 * @param itemId item id, e.g. "T4_2H_BOW"
 * @param unitsPerDay average units sold per day over the history window, summed over all qualities
 *     (scale 1, HALF_UP)
 * @param priceByQuality volume-weighted average price per quality level (1..4, scale 2, HALF_UP);
 *     a quality with zero sales volume has no entry
 */
public record ItemPriceStats(
    String itemId,
    BigDecimal unitsPerDay,
    Map<Integer, BigDecimal> priceByQuality
) {

    public ItemPriceStats {
        priceByQuality = priceByQuality == null ? Map.of() : Map.copyOf(priceByQuality);
    }
}