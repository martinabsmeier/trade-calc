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
package de.am.albion.tradecalc.dataprovider.recipe;

import de.am.albion.tradecalc.domain.model.Recipe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads static {@link Recipe recipes} from {@code recipes/index.json} on the
 * classpath once at construction time. Recipes are reference data and do not
 * change at runtime, so the loaded list is held in a {@code final} field —
 * no Spring cache proxy, no double-IO, no dead {@code exists()} checks.
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Component
public class RecipeLoader {

    private static final String RESOURCE_PATH = "recipes/index.json";
    private final List<Recipe> recipes;

    /**
     * Creates the loader by reading and parsing {@code recipes/index.json}.
     *
     * @param mapper the JSON mapper used for deserialisation
     * @throws RecipeLoadException if the resource is missing or malformed
     */
    public RecipeLoader(RecipeJsonMapper mapper) {
        log.info("Loading recipes from classpath: {}", RESOURCE_PATH);
        try (InputStream in = RecipeLoader.class.getResourceAsStream("/" + RESOURCE_PATH)) {
            if (in == null) {
                throw new RecipeLoadException("Recipe resource not found on classpath: " + RESOURCE_PATH);
            }
            this.recipes = List.of(mapper.readList(in));
        } catch (IOException e) {
            throw new RecipeLoadException("Failed to read recipe resource: " + RESOURCE_PATH, e);
        }
        log.info("Loaded {} recipes from classpath", recipes.size());
    }

    /**
     * Returns every loaded recipe.
     *
     * @return unmodifiable list of recipes; never {@code null}
     */
    public List<Recipe> loadAll() {
        return recipes;
    }
}