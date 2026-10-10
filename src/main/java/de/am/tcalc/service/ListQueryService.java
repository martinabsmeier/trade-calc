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
package de.am.tcalc.service;

import de.am.tcalc.domain.ItemPriceStats;
import de.am.tcalc.domain.Recipe;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the spec's list view: filter recipes by category/subcategory (German shop labels,
 * "Alle" = no filter), sort by average daily units sold (all qualities summed, no-sales last),
 * page 25/50/100. Market prices come from {@link PriceService} in one batched call per view.
 */
@Service
@RequiredArgsConstructor
public class ListQueryService {

    /** Dropdown pseudo-entry meaning "no filter" (spec: "Alle"). */
    public static final String ALL = "Alle";

    /** Selectable cities in spec order; "Schwarzer Markt" maps to the API's "Black Market". */
    public static final List<String> CITIES = List.of(
        "Fort Sterling", "Lymhurst", "Bridgewatch", "Martlock", "Thetford",
        "Brecilien", "Caerleon", "Schwarzer Markt");

    /** Quality levels the market sells in, with their German labels (default: Normal). */
    public static final Map<Integer, String> QUALITIES = Map.of(
        1, "Normal", 2, "Gut", 3, "Außergewöhnlich", 4, "Hervorragend");

    /** Page sizes the spec offers (default 25). */
    public static final List<Integer> SIZES = List.of(25, 50, 100);

    private final RecipeService recipeService;
    private final PriceService priceService;

    /** Distinct category (shopSub1) labels, in natural order. */
    public Set<String> categories() {
        Set<String> out = new TreeSet<>();
        for (Recipe r : recipeService.all()) {
            if (r.shopSub1() != null) {
                out.add(r.shopSub1());
            }
        }
        return out;
    }

    /**
     * Distinct subcategory (shopSub2) labels for one category, or across all categories.
     * Recipes without a subcategory (bags, satchels …) are listed under "Alle" only.
     */
    public Set<String> subcategories(String category) {
        Set<String> out = new TreeSet<>();
        String want = ALL.equals(category) ? null : category;
        for (Recipe r : recipeService.all()) {
            if (r.shopSub2() == null) {
                continue;
            }
            if (want == null || want.equals(r.shopSub1())) {
                out.add(r.shopSub2());
            }
        }
        return out;
    }

    /**
     * One page of the item list, sorted by daily units sold (desc, no-sales last, then by id).
     *
     * ponytail: prices are fetched for ALL matching recipes, not just the page — needed because
     * the sort key IS the sales volume. Upgrade path: API-side aggregation/sort if the list grows.
     */
    public List<ListRow> rows(String city, String category, String subcategory, int quality, int size) {
        List<Recipe> recipes = matchingRecipes(category, subcategory);
        if (recipes.isEmpty()) {
            return List.of();
        }
        Map<String, ItemPriceStats> stats = pricesOf(recipes, city);
        return sortedPage(recipes.stream().map(r -> row(r, stats.get(marketId(r)), quality)).toList(), size);
    }

    /** Recipes matching the category/subcategory filter ("Alle" = no filter); keeps order. */
    private List<Recipe> matchingRecipes(String category, String subcategory) {
        boolean filterCategory = category != null && !ALL.equals(category);
        boolean filterSub = subcategory != null && !ALL.equals(subcategory);
        return recipeService.all().stream()
            .filter(r -> !(filterCategory && !category.equals(r.shopSub1())))
            .filter(r -> !(filterSub && !subcategory.equals(r.shopSub2())))
            .toList();
    }

    /** Batched market stats for all recipes' market ids in the city (empty map when untraded). */
    private Map<String, ItemPriceStats> pricesOf(List<Recipe> recipes, String city) {
        Set<String> marketIds = recipes.stream().map(ListQueryService::marketId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return priceService.stats(List.copyOf(marketIds), apiLocation(city));
    }

    /** One table row; items without market data get 0 units and no price. */
    private static ListRow row(Recipe recipe, ItemPriceStats stats, int quality) {
        BigDecimal units = stats == null ? BigDecimal.ZERO : stats.unitsPerDay();
        BigDecimal price = stats == null ? null : stats.priceByQuality().get(quality);
        int enchant = recipe.enchantmentLevel() == null ? 0 : recipe.enchantmentLevel();
        return new ListRow(marketId(recipe), recipe.name(), recipe.tier(), enchant, units, price);
    }

    /** Sorts by daily units (desc, no-sales last, then id) and returns the first {@code size} rows. */
    private static List<ListRow> sortedPage(List<ListRow> rows, int size) {
        List<ListRow> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing(ListRow::unitsPerDay, Comparator.reverseOrder())
            .thenComparing(ListRow::marketItemId));
        int to = Math.min(size, sorted.size());
        return sorted.subList(0, to).stream()
            .map(row -> new ListRow(row.marketItemId(), row.name(), row.tier(), row.enchantmentLevel(),
                // Commercial rounding (HALF_UP), re-applied defensively to the service output.
                row.unitsPerDay().setScale(1, RoundingMode.HALF_UP), row.price()))
            .toList();
    }

    /** Market id for a recipe: enchanted variants carry the dump's {@code @level} suffix. */
    public static String marketId(Recipe recipe) {
        Integer enchant = recipe.enchantmentLevel();
        return enchant == null || enchant == 0 ? recipe.id() : recipe.id() + "@" + enchant;
    }

    /** UI city label → API location name ("Schwarzer Markt" → "Black Market"). */
    public static String apiLocation(String city) {
        return "Schwarzer Markt".equals(city) ? "Black Market" : city;
    }

    /**
     * One row of the spec's list table.
     *
     * @param marketItemId dump id incl. enchantment suffix, e.g. "T4_2H_BOW@1"
     * @param name German display name (tier/enchantment already part of it, e.g. "Langbogen des Adepten")
     * @param tier tier of the item, 1..8
     * @param enchantmentLevel 0 for base items, 1..4 for enchanted ones
     * @param unitsPerDay average daily units sold, summed over all qualities (0 when not traded)
     * @param price average price of the requested quality; {@code null} when not traded in it
     */
    public record ListRow(
        String marketItemId,
        String name,
        Integer tier,
        int enchantmentLevel,
        BigDecimal unitsPerDay,
        BigDecimal price
    ) {
    }
}