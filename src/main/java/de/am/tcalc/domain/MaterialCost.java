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

import java.math.BigDecimal;
import java.util.List;

/**
 * Purchase price of an item's materials in one city (spec "Berechnen", Variante 1). Built by
 * {@link de.am.tcalc.service.calculator.MaterialCostService}; ingredients are priced at quality
 * "Normal".
 *
 * @param itemId recipe id, e.g. "T5_2H_LONGBOW"
 * @param enchantmentLevel enchantment of the requested variant (null/0 = base)
 * @param location city the materials are bought in (UI label)
 * @param lines one line per ingredient, in recipe order
 * @param total Σ line costs (scale 2, HALF_UP); {@code null} when any ingredient has no market
 *     data — a missing price is never silently treated as zero
 */
public record MaterialCost(
    String itemId,
    Integer enchantmentLevel,
    String location,
    List<MaterialCostLine> lines,
    BigDecimal total
) {

    public MaterialCost {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}