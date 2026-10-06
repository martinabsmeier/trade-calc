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

import de.am.albion.tradecalc.dataprovider.recipe.RecipeJsonMapper;
import de.am.albion.tradecalc.dataprovider.recipe.RecipeLoader;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecipeService}. We stub {@link RecipeLoader} so the
 * service can be exercised without touching the classpath JSON.
 */
class RecipeServiceTest {

    private RecipeService service;
    private RecipeLoader loader;

    /**
     * Mocks {@link RecipeLoader} to return a small list of recipes.
     */
    @BeforeEach
    void setUp() throws IOException {
        loader = mock(RecipeLoader.class);
        Recipe bow = new Recipe("T4_BOW", 4, "Bögen", null,
                List.of(new RecipeIngredient("T4_WOOD", 16, new BigDecimal("120"))));
        Recipe plank = new Recipe("T4_PLANK", 4, null, "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 2, new BigDecimal("100"))));
        when(loader.loadAll()).thenReturn(List.of(bow, plank));
        service = new RecipeService(loader);
    }

    @Test
    void findByItemId_knownItem_returnsRecipe() {
        Optional<Recipe> found = service.findByItemId("T4_BOW");

        assertThat(found).isPresent();
        assertThat(found.get().craftingCategory()).isEqualTo("Bögen");
    }

    @Test
    void findByItemId_isCaseInsensitive() {
        Optional<Recipe> found = service.findByItemId("t4_plank");

        assertThat(found).isPresent();
        assertThat(found.get().refiningCategory()).isEqualTo("Holz");
    }

    @Test
    void findByItemId_unknownItem_returnsEmpty() {
        assertThat(service.findByItemId("T9_ARTEFACT")).isEmpty();
    }

    @Test
    void findByItemId_nullItemId_returnsEmpty() {
        assertThat(service.findByItemId(null)).isEmpty();
    }
}