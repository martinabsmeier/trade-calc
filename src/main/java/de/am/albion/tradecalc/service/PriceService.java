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
package de.am.albion.tradecalc.service;

import de.am.albion.tradecalc.dataprovider.albion.AlbionDataApiClient;
import de.am.albion.tradecalc.dataprovider.albion.dto.MarketJson;
import de.am.albion.tradecalc.dataprovider.albion.mapper.MarketJsonMapper;
import de.am.albion.tradecalc.domain.model.MarketPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application-level facade over {@link AlbionDataApiClient}. Translates raw
 * upstream responses into domain {@link MarketPrice} records and serves them
 * from the {@code marketPrices} cache.
 *
 * <p>The cache key is the {@code itemId}; one entry holds the full map of
 * city → price for that item at the configured qualities. This keeps the
 * cache size proportional to the number of items queried, not the number of
 * {@code (item, city)} combinations.</p>
 *
 * <p>Upstream entries with {@code 0} prices are normalised to {@code null}
 * by the mapper — they still occupy a slot in the returned map so callers
 * can distinguish "no orders" from "unknown city".</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Service
public class PriceService {

    private final AlbionDataApiClient client;
    private final MarketJsonMapper mapper;

    /**
     * Creates the service.
     *
     * @param client upstream API client
     * @param mapper DTO → domain mapper
     */
    public PriceService(AlbionDataApiClient client, MarketJsonMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Returns the latest market prices for the given item across all configured
     * cities and qualities. Results are cached under {@code marketPrices}.
     *
     * @param itemId the Albion item identifier (e.g. {@code "T4_BOW"})
     * @return immutable map keyed by city name; never {@code null}, possibly empty
     */
    @Cacheable(value = "marketPrices", key = "#itemId")
    public Map<String, MarketPrice> getPrices(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Collections.emptyMap();
        }
        List<MarketJson> response = client.fetchPrices(itemId);
        Map<String, MarketPrice> result = new LinkedHashMap<>();
        for (MarketJson json : response) {
            if (json.itemId() == null || !json.itemId().equalsIgnoreCase(itemId)) {
                continue;
            }
            if (json.city() == null) {
                continue;
            }
            result.put(json.city(), mapper.map(json));
        }
        log.debug("Returning {} market price entries for item {}", result.size(), itemId);
        return Map.copyOf(result);
    }
}