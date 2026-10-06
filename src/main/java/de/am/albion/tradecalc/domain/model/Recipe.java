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

import lombok.Builder;

import java.util.List;

/**
 * A static crafting recipe: which items are required and how many of them, plus
 * an optional refining category so that city refining bonuses can be looked up.
 *
 * <p>A recipe with an empty {@link #materials} list represents a raw resource
 * (no crafting required). The {@link #refiningCategory} may be {@code null}
 * if the recipe cannot be refined (e.g. for a finished weapon).</p>
 *
 * @param itemId the resulting / crafted item
 * @param tier the item tier (1–8)
 * @param craftingCategory the crafting category used to look up city bonuses
 * @param refiningCategory the refining category used to look up city bonuses;
 *                          {@code null} if no refinement is involved
 * @param materials the list of required ingredients (always non-null)
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record Recipe(
        String itemId,
        int tier,
        String craftingCategory,
        String refiningCategory,
        List<RecipeIngredient> materials) {

    /**
     * Canonical constructor: guarantees {@link #materials} is non-null and
     * immutable so callers can safely hand the list to other layers without
     * defensive copies.
     */
    public Recipe {
        materials = materials == null ? List.of() : List.copyOf(materials);
    }
}