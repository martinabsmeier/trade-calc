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

import java.math.BigDecimal;

/**
 * One ingredient of a {@link Recipe}, identified by its item id and the
 * required quantity.
 *
 * @param itemId the ingredient item id (e.g. {@code "T4_PLANK"})
 * @param quantity how many of the ingredient are needed per craft
 * @param buyPriceMax maximum buy-order price (silver per unit); {@code null}
 *                    if not available
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record RecipeIngredient(
        String itemId,
        int quantity,
        BigDecimal buyPriceMax) {
}