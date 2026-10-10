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
package de.am.tcalc.dataprovider.recipe;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import de.am.tcalc.domain.Recipe;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Reads the static recipe book ({@code classpath:data/recipes.json}) once at startup
 * ({@code @PostConstruct}) into a final list. No {@code @Cacheable} on this loader — see
 * {@code RecipeService} for the per-item cache.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class RecipeLoader {

    static final String CLASSPATH_LOCATION = "classpath:data/recipes.json";

    private final ResourceLoader resourceLoader;
    private final JsonMapper objectMapper;

    /** Immutable list of recipes, loaded once at construction. */
    private List<Recipe> recipes;

    @PostConstruct
    public void load() {
        Resource resource = resourceLoader.getResource(CLASSPATH_LOCATION);
        if (!resource.exists()) {
            throw new IllegalStateException("Recipe book not found at " + CLASSPATH_LOCATION);
        }
        try {
            this.recipes = List.copyOf(objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {}));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + CLASSPATH_LOCATION, ex);
        }
        log.info("Loaded {} recipes from {}", recipes.size(), CLASSPATH_LOCATION);
    }

    /** All recipes; a defensive copy (SpotBugs EI_EXPOSE_REP) — call sites keep it read-only. */
    public List<Recipe> all() {
        return List.copyOf(recipes);
    }
}
