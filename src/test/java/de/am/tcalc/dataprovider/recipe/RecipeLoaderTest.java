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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

class RecipeLoaderTest {

    @Test
    void missingRecipeBookFailsFast() {
        // Resource that never exists → the loader must refuse to start, not silently load nothing.
        org.springframework.core.io.Resource absent =
            new org.springframework.core.io.ByteArrayResource(new byte[0]) {
                @Override public boolean exists() { return false; }
                @Override public String getDescription() { return "absent"; }
            };
        ResourceLoader loader = new ResourceLoader() {
            @Override public Resource getResource(String location) { return absent; }
            @Override public ClassLoader getClassLoader() { return Thread.currentThread().getContextClassLoader(); }
        };

        RecipeLoader recipeLoader = new RecipeLoader(loader, new ObjectMapper());
        assertThatThrownBy(recipeLoader::load).isInstanceOf(IllegalStateException.class);
    }
}