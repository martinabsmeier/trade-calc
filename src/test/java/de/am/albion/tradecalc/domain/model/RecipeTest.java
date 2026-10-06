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
package de.am.albion.tradecalc.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Recipe}: focuses on the canonical constructor that
 * guarantees {@link Recipe#materials} is non-null and immutable.
 */
class RecipeTest {

    /**
     * Null materials list is replaced by an empty list — callers can rely on
     * non-null without defensive checks.
     */
    @Test
    void nullMaterials_areReplacedByEmptyList() {
        Recipe r = new Recipe("T4_WOOD", 4, null, null, null);

        assertThat(r.materials()).isNotNull().isEmpty();
    }

    /**
     * The supplied materials list is exposed as an unmodifiable view — any
     * attempt to mutate throws {@link UnsupportedOperationException}.
     */
    @Test
    void materialsList_isImmutable() {
        RecipeIngredient ing = new RecipeIngredient("T4_WOOD", 2, null);
        Recipe r = new Recipe("T4_PLANK", 4, null, "Holz", List.of(ing));

        assertThatThrownBy(() -> r.materials().add(ing))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Recipe exposes all fields via accessor methods.
     */
    @Test
    void exposesAllFields() {
        RecipeIngredient ing = new RecipeIngredient("T4_WOOD", 2, null);
        Recipe r = new Recipe("T4_BOW", 4, "Bögen", "Holz", List.of(ing));

        assertThat(r.itemId()).isEqualTo("T4_BOW");
        assertThat(r.tier()).isEqualTo(4);
        assertThat(r.craftingCategory()).isEqualTo("Bögen");
        assertThat(r.refiningCategory()).isEqualTo("Holz");
        assertThat(r.materials()).hasSize(1);
    }
}