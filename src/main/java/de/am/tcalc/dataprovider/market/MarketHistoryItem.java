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
package de.am.tcalc.dataprovider.market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * One sell-history series for a single (item, quality) pair in one city — mirror of the
 * `api/v2/stats/history` response objects. Hourly buckets carry the units sold and the average
 * price of that hour.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketHistoryItem(
    @JsonProperty("location") String location,
    @JsonProperty("item_id") String itemId,
    @JsonProperty("quality") int quality,
    @JsonProperty("data") List<MarketHistoryEntry> data
) {

    /** Compact constructor: the data list is defensively copied so no caller can mutate ours. */
    public MarketHistoryItem {
        data = data == null ? List.of() : List.copyOf(data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarketHistoryEntry(
        @JsonProperty("item_count") int itemCount,
        @JsonProperty("avg_price") BigDecimal avgPrice,
        @JsonProperty("timestamp") String timestamp
    ) {
    }
}