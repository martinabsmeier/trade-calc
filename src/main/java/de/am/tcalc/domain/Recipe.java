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
package de.am.tcalc.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Pure value type describing one crafting recipe: which output item is crafted from which ingredients, plus
 * the crafting parameters (time, focus, silver). Framework-free.
 *
 * @param id item id, e.g. "T4_2H_LONGBOW" — without the dump's {@code @N} enchantment suffix;
 *     enchanted variants share the id and are distinguished by {@code enchantmentLevel}
 * @param tier tier (1..8), null for prototype items
 * @param name human-readable display name (official German localization from the item dump;
 *     English fallback for the few unlocalized prototype entries)
 * @param category crafting category, e.g. "bow", "plate_armor"
 * @param shopCategory top-level shop category, e.g. "weapons"
 * @param shopSub1 first shop subcategory — German display label, e.g. "Bögen"
 * @param shopSub2 second shop subcategory — German display label, e.g. "Langbögen";
 *     null for items without one (bags, satchels …)
 * @param enchantmentLevel null for base recipes, 1..4 for enchanted variants
 * @param refiningCategory the Royal-City bonus category of the dominant refined ingredient
 *     ("Wood", "Fiber", "Hide", "Ore", "Stone"), chosen heuristically by the build script —
 *     the basis for spec "Variante 2" refining-chain routing
 * @param craftingTime time in seconds
 * @param craftingFocus focus points consumed
 * @param silver silver cost
 * @param ingredients list of required input materials
 */
public record Recipe(
    String id,
    Integer tier,
    String name,
    String category,
    String shopCategory,
    String shopSub1,
    String shopSub2,
    Integer enchantmentLevel,
    String refiningCategory,
    double craftingTime,
    int craftingFocus,
    int silver,
    List<RecipeIngredient> ingredients
) {

    @JsonCreator
    public Recipe(
        @JsonProperty("id") String id,
        @JsonProperty("tier") Integer tier,
        @JsonProperty("name") String name,
        @JsonProperty("category") String category,
        @JsonProperty("shopCategory") String shopCategory,
        @JsonProperty("shopSub1") String shopSub1,
        @JsonProperty("shopSub2") String shopSub2,
        @JsonProperty("enchantmentLevel") Integer enchantmentLevel,
        @JsonProperty("refiningCategory") String refiningCategory,
        @JsonProperty("craftingTime") double craftingTime,
        @JsonProperty("craftingFocus") int craftingFocus,
        @JsonProperty("silver") int silver,
        @JsonProperty("ingredients") List<RecipeIngredient> ingredients
    ) {
        this.id = id;
        this.tier = tier;
        this.name = name;
        this.category = category;
        this.shopCategory = shopCategory;
        this.shopSub1 = shopSub1;
        this.shopSub2 = shopSub2;
        this.enchantmentLevel = enchantmentLevel;
        this.refiningCategory = refiningCategory;
        this.craftingTime = craftingTime;
        this.craftingFocus = craftingFocus;
        this.silver = silver;
        this.ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }
}
