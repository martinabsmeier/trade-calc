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

import static org.assertj.core.api.Assertions.assertThat;

import de.am.tcalc.dataprovider.recipe.RecipeLoader;
import org.junit.jupiter.api.Test;

class RecipeServiceTest {

    @Test
    void findByItemIdReturnsLongbowWithCorrectIngredients() {
        // Resource loader pointing at the real recipes.json on the classpath.
        RecipeLoader loader = new RecipeLoader(
            new org.springframework.core.io.DefaultResourceLoader(),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
        loader.load();

        RecipeService service = new RecipeService(loader);

        // Spec example: the Longbow must resolve to 32x T4_PLANKS.
        var bow = service.findByItemId("T4_2H_LONGBOW");
        assertThat(bow).isPresent();
        assertThat(bow.get().ingredients())
            .containsExactly(new de.am.tcalc.domain.RecipeIngredient("T4_PLANKS", 32));
        assertThat(bow.get().refiningCategory()).isEqualTo("Wood");

        // Missing ids return Optional.empty() — not a crash.
        assertThat(service.findByItemId("T4_NOT_A_REAL_ITEM")).isEmpty();
    }

    @Test
    void allReturnsFullRecipeSet() {
        // ponytail: kein exakter Snapshot — die Dump-Regeneration ändert die Zahl bei jedem ao-bin-dumps-Update.
        // Ceiling: einmal pro Regeneration grob validieren; Upgrade path: Referenz-Dump pinningen.
        RecipeLoader loader = new RecipeLoader(
            new org.springframework.core.io.DefaultResourceLoader(),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
        loader.load();
        RecipeService service = new RecipeService(loader);

        assertThat(service.all().size()).isGreaterThan(6000);
    }

    @Test
    void variantsOfReturnsEmptyForUnknownId() {
        RecipeLoader loader = new RecipeLoader(
            new org.springframework.core.io.DefaultResourceLoader(),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
        loader.load();
        RecipeService service = new RecipeService(loader);

        // Exercises the null branch of variantsOf.
        assertThat(service.variantsOf("T4_NOT_A_REAL_ITEM")).isEmpty();
    }

    @Test
    void variantsOfReturnsAllRecipesForCraftableItem() {
        RecipeLoader loader = new RecipeLoader(
            new org.springframework.core.io.DefaultResourceLoader(),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
        loader.load();
        RecipeService service = new RecipeService(loader);

        // Longbow has at least the base recipe; verify the non-empty branch.
        assertThat(service.variantsOf("T4_2H_LONGBOW")).isNotEmpty();
    }
}
