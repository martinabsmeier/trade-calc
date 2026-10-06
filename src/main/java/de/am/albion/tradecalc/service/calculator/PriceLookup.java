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

import java.util.Map;

/**
 * Read-only access to current market prices, keyed by item id. Production
 * callers wire this to {@link de.am.albion.tradecalc.service.PriceService};
 * unit tests use a builder-backed stub.
 */
public interface PriceLookup {

    /**
     * Returns the latest known price per city for the given item. Cities
     * without an active buy- or sell-order may be missing from the map.
     *
     * @param itemId the item id to look up
     * @return map keyed by city, possibly empty
     */
    Map<String, MarketPrice> pricesFor(String itemId);
}