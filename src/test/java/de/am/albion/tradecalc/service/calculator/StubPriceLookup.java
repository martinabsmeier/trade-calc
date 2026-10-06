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
package de.am.albion.tradecalc.service.calculator;

import de.am.albion.tradecalc.domain.model.MarketPrice;

import java.util.HashMap;
import java.util.Map;

/**
 * Map-backed {@link PriceLookup} for unit tests. Build with
 * {@link #builder()}.
 */
final class StubPriceLookup implements PriceLookup {

    private final Map<String, Map<String, MarketPrice>> pricesByItem;

    private StubPriceLookup(Map<String, Map<String, MarketPrice>> pricesByItem) {
        this.pricesByItem = pricesByItem;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public Map<String, MarketPrice> pricesFor(String itemId) {
        return pricesByItem.getOrDefault(itemId, Map.of());
    }

    static final class Builder {
        private final Map<String, Map<String, MarketPrice>> prices = new HashMap<>();

        Builder city(String itemId, MarketPrice price) {
            prices.computeIfAbsent(itemId, k -> new HashMap<>()).put(price.city(), price);
            return this;
        }

        StubPriceLookup build() {
            return new StubPriceLookup(prices);
        }
    }
}