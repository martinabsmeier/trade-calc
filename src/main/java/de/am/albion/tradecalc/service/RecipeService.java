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

import de.am.albion.tradecalc.dataprovider.recipe.RecipeLoader;
import de.am.albion.tradecalc.domain.model.Recipe;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Read-side service over {@link RecipeLoader} that exposes cache-friendly
 * look-ups for single recipes and recipe lists.
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Service
public class RecipeService {

    private final RecipeLoader loader;

    /**
     * Creates a new recipe service.
     *
     * @param loader the underlying recipe loader
     */
    public RecipeService(RecipeLoader loader) {
        this.loader = loader;
    }

    /**
     * Looks up a single recipe by its item id.
     *
     * @param itemId the item id (e.g. {@code "T4_BOW"})
     * @return the recipe or {@link Optional#empty()} if no recipe is registered
     */
    @Cacheable("recipes")
    public Optional<Recipe> findByItemId(String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return loader.loadAll().stream()
                .filter(r -> r.itemId().equalsIgnoreCase(itemId))
                .findFirst();
    }
}