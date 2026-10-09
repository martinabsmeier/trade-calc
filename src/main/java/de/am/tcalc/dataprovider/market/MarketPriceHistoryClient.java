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

import java.util.List;

/**
 * Fetches hourly sell history from the Albion Online Data project. One call carries all qualities
 * (1..4) for the given items in the given city — see the spec's data source section.
 */
public interface MarketPriceHistoryClient {

    /**
     * @param itemIds exact dump ids, e.g. {@code ["T4_2H_BOW", "T5_2H_BOW"]} — the API matches these 1:1
     * @param location city name as the API spells it, e.g. "Lymhurst"
     * @return one entry per (item, quality) that the API knows; unknown items are simply absent
     */
    List<MarketHistoryItem> history(List<String> itemIds, String location);
}