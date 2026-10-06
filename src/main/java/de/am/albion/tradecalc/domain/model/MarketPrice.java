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

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Live market price for a single item in a single city at a single quality.
 *
 * <p>All prices are quoted in <strong>silver per item</strong>. Either price
 * may be {@code null} when no buy / sell orders exist on the market.</p>
 *
 * @param itemId the item identifier (e.g. {@code "T4_BOW"})
 * @param city the city the prices were observed in
 * @param quality the item quality (1 = normal, 2 = good, …)
 * @param buyPriceMax maximum buy-order price (silver per unit)
 * @param sellPriceMin minimum sell-order price (silver per unit)
 * @param observedAt timestamp of the observation
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record MarketPrice(
        String itemId,
        String city,
        int quality,
        BigDecimal buyPriceMax,
        BigDecimal sellPriceMin,
        Instant observedAt) {
}