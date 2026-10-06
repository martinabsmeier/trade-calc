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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * An Albion Online craftable item.
 *
 * <p>Items are uniquely identified by their internal {@code id} (e.g.
 * {@code "T4_BOW"} for a Tier-4 bow). The {@code tier} is the item's tier
 * (1–8) and {@code category} groups items of the same kind (e.g. bows,
 * swords, planks) so that city crafting bonuses can be applied.</p>
 *
 * @param id the unique Albion Online item identifier (e.g. {@code "T4_BOW"})
 * @param tier the item tier (1–8)
 * @param category the crafting category (e.g. {@code "Bows"}); may be {@code null}
 *                 if the item is not crafted
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record Item(
        String id,
        int tier,
        String category) {

    @JsonCreator
    public Item(
            @JsonProperty("id") String id,
            @JsonProperty("tier") int tier,
            @JsonProperty("category") String category) {
        this.id = id;
        this.tier = tier;
        this.category = category;
    }
}