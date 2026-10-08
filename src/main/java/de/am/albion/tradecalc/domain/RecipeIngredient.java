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
package de.am.albion.tradecalc.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One ingredient of a {@link Recipe}.
 *
 * @param item item id, e.g. "T4_PLANKS"
 * @param count number of units required
 */
public record RecipeIngredient(String item, int count) {

    @JsonCreator
    public RecipeIngredient(@JsonProperty("item") String item, @JsonProperty("count") int count) {
        this.item = item;
        this.count = count;
    }
}
