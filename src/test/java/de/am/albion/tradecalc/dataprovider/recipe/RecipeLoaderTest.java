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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the defensive branches in {@link RecipeLoader#RecipeLoader}.
 */
class RecipeLoaderTest {

    @Test
    void happyPath_loadsRecipes() throws Exception {
        RecipeJsonMapper mapper = mock(RecipeJsonMapper.class);
        when(mapper.readList(any())).thenReturn(new Recipe[0]);

        RecipeLoader loader = new RecipeLoader(mapper);

        assertThat(loader.loadAll()).isEmpty();
    }

    @Test
    void mapperThrowsIOException_wrappedAsRecipeLoadException() throws Exception {
        RecipeJsonMapper mapper = mock(RecipeJsonMapper.class);
        when(mapper.readList(any())).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> new RecipeLoader(mapper))
                .isInstanceOf(RecipeLoadException.class)
                .hasMessageContaining("Failed to read recipe resource")
                .hasCauseInstanceOf(IOException.class);
    }
}