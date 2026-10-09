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

import de.am.tcalc.dataprovider.recipe.RecipeLoader;
import de.am.tcalc.domain.Recipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Per-item recipe lookup over a precomputed index (id → variants, built once in the constructor).
 * The {@code recipes} cache declared in {@code application.yml} wraps {@link #findByItemId(String)}
 * only — the loader itself is uncached.
 */
@Service
public class RecipeService {

    private final RecipeLoader loader;
    private final Map<String, List<Recipe>> byId;

    public RecipeService(RecipeLoader loader) {
        this.loader = loader;
        Map<String, List<Recipe>> tmp = new HashMap<>();
        for (Recipe r : loader.all()) {
            tmp.computeIfAbsent(r.id(), k -> new ArrayList<>()).add(r);
        }
        Map<String, List<Recipe>> sorted = new TreeMap<>();
        tmp.forEach((k, v) -> sorted.put(k, Collections.unmodifiableList(v)));
        this.byId = Collections.unmodifiableMap(sorted);
    }

    /**
     * First variant of the item's recipes. An id resolves to base plus enchantment variants with
     * the same id — callers wanting a specific enchantment use {@link #variantsOf(String)}.
     *
     * @param itemId recipe id, e.g. "T4_2H_LONGBOW"
     * @return the base recipe (enchantmentLevel is null), or empty when unknown
     */
    @Cacheable("recipes")
    public Optional<Recipe> findByItemId(String itemId) {
        List<Recipe> list = byId.get(itemId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    public List<Recipe> all() {
        return loader.all();
    }

    public List<Recipe> variantsOf(String itemId) {
        List<Recipe> list = byId.get(itemId);
        return list == null ? List.of() : list;
    }
}
