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
package de.am.tcalc.service.calculator;

import de.am.tcalc.domain.MaterialCost;
import de.am.tcalc.domain.MaterialCostLine;
import de.am.tcalc.domain.Recipe;
import de.am.tcalc.domain.RecipeIngredient;
import de.am.tcalc.service.PriceService;
import de.am.tcalc.service.RecipeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Spec "Berechnen", Variante 1: purchase price of an item's materials in the selected city.
 * Every ingredient is priced at quality "Normal" (raw/refined resources are only traded in that
 * quality). Materials are bought at sell orders — no fees apply on the buy side.
 */
@Service
@RequiredArgsConstructor
public class MaterialCostService {

    static final int MATERIAL_QUALITY = 1;

    private final RecipeService recipeService;
    private final PriceService priceService;

    /**
     * @param itemId recipe id, e.g. "T5_2H_LONGBOW"
     * @param enchantmentLevel enchantment of the variant (null/0 = base)
     * @param location city the materials are bought in (UI label, e.g. "Schwarzer Markt")
     * @return the material cost, or {@code null} when the recipe is unknown
     */
    @Cacheable("materialcosts")
    public MaterialCost cost(String itemId, Integer enchantmentLevel, String location) {
        Optional<Recipe> recipe = recipeService.variantsOf(itemId).stream()
            .filter(r -> matchEnchantment(r, enchantmentLevel))
            .findFirst();
        return recipe.map(r -> costOf(r, itemId, enchantmentLevel, location)).orElse(null);
    }

    /** Line costs per ingredient; the total is null as soon as one price is unknown. */
    private MaterialCost costOf(Recipe recipe, String itemId, Integer enchantmentLevel, String location) {
        List<MaterialCostLine> lines = recipe.ingredients().stream()
            .map(ingredient -> line(ingredient, location))
            .toList();
        return new MaterialCost(itemId, enchantmentLevel, location, lines, total(lines));
    }

    /** One ingredient line, priced at quality "Normal" in the city. */
    private MaterialCostLine line(RecipeIngredient ingredient, String location) {
        var stats = priceService.stat(ingredient.item(), location);
        BigDecimal unitPrice = stats == null ? null : stats.priceByQuality().get(MATERIAL_QUALITY);
        BigDecimal lineCost = unitPrice == null
            ? null
            // Commercial rounding (HALF_UP) per project convention, money scale 2.
            : unitPrice.multiply(BigDecimal.valueOf(ingredient.count()))
                .setScale(2, RoundingMode.HALF_UP);
        return new MaterialCostLine(ingredient.item(), ingredient.count(), unitPrice, lineCost);
    }

    /** Σ line costs; a single unknown line cost makes the total unknown — never silently 0. */
    private static BigDecimal total(List<MaterialCostLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (MaterialCostLine line : lines) {
            if (line.lineCost() == null) {
                return null;
            }
            total = total.add(line.lineCost());
        }
        return total;
    }

    private static boolean matchEnchantment(Recipe r, Integer wanted) {
        int level = wanted == null ? 0 : wanted;
        int have = r.enchantmentLevel() == null ? 0 : r.enchantmentLevel();
        return level == have;
    }
}