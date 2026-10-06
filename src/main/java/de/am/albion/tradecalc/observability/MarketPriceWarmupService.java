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
package de.am.albion.tradecalc.observability;

import de.am.albion.tradecalc.dataprovider.recipe.RecipeLoader;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pre-populates the {@code marketPrices} cache for every item that appears in
 * the recipe catalogue. Runs once, right after {@link ApplicationReadyEvent},
 * so the very first user query never pays the upstream round-trip.
 *
 * <p>ponytail: the walk is bounded by the recipe catalogue (5–20 items in
 * practice) so we send one upstream request per item rather than batching —
 * Albion's API is per-item anyway and the cache fills in the cheapest order.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceWarmupService {

    private final RecipeLoader recipeLoader;
    private final PriceService priceService;

    /**
     * Warms the cache for every distinct item referenced by a recipe. Failures
     * are logged but never thrown — startup must not fail just because the
     * upstream is flaky; the next user request will retry via the cache miss.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        Set<String> items = collectReferencedItems();
        log.info("Market-price warmup: {} item(s) to fetch", items.size());
        for (String itemId : items) {
            try {
                priceService.getPrices(itemId);
            } catch (Exception e) {
                log.warn("Warmup failed for item {}: {}", itemId, e.toString());
            }
        }
        log.info("Market-price warmup complete");
    }

    /**
     * Collects every distinct item id referenced by the recipe catalogue,
     * deduplicated so each upstream call fires once per item.
     */
    Set<String> collectReferencedItems() {
        List<Recipe> recipes = recipeLoader.loadAll();
        Set<String> ids = new LinkedHashSet<>();
        for (Recipe r : recipes) {
            ids.add(r.itemId());
            for (var material : r.materials()) {
                ids.add(material.itemId());
            }
        }
        return ids;
    }
}