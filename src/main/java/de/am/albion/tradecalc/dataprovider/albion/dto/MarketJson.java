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
package de.am.albion.tradecalc.dataprovider.albion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Raw JSON shape of a single element in the response of
 * <code>GET /api/v2/stats/prices/{items}</code> on the
 * <a href="https://www.albion-online-data.com/api/">Albion Online Data API</a>.
 *
 * <p>The upstream API returns one element per {@code (item_id, city, quality)}
 * combination. Prices of {@code 0} (or empty bodies) mean "no active buy / sell
 * order" — callers must treat them as {@code null}, which is what
 * {@link de.am.albion.tradecalc.dataprovider.albion.mapper.MarketJsonMapper}
 * does.</p>
 *
 * <p>This is a <em>data transfer object</em>, not a domain record: it lives in
 * {@code dataprovider.albion.dto} and must not be referenced from
 * {@code service}, {@code repository}, or {@code api}.</p>
 *
 * @param itemId         Albion item identifier (e.g. {@code "T4_BOW"})
 * @param city           city the price applies to (e.g. {@code "Lymhurst"})
 * @param quality        item quality (1 = normal, 2 = good, …)
 * @param sellPriceMin   lowest active sell-order price; {@code 0} if no orders
 * @param buyPriceMax    highest active buy-order price; {@code 0} if no orders
 * @param sellPriceMinDate timestamp of the {@code sell_price_min} observation
 * @param buyPriceMaxDate  timestamp of the {@code buy_price_max} observation
 * @author Martin Absmeier
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketJson(
        @JsonProperty("item_id") String itemId,
        @JsonProperty("city") String city,
        @JsonProperty("quality") int quality,
        @JsonProperty("sell_price_min") BigDecimal sellPriceMin,
        @JsonProperty("buy_price_max") BigDecimal buyPriceMax,
        @JsonProperty("sell_price_min_date") Instant sellPriceMinDate,
        @JsonProperty("buy_price_max_date") Instant buyPriceMaxDate) {
}