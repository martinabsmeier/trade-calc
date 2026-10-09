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
package de.am.tcalc.api;

import de.am.tcalc.domain.ItemPriceStats;
import de.am.tcalc.domain.MaterialCost;
import de.am.tcalc.service.ListQueryService;
import de.am.tcalc.service.PriceService;
import de.am.tcalc.service.calculator.MaterialCostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves the spec's item-list page. The shell ({@code /}) carries the filter dropdowns; HTMX
 * swaps in the fragments: subcategory options, the sorted table body ({@code /list}) and the
 * "Berechnen" panel ({@code /calc}).
 */
@Controller
@RequiredArgsConstructor
public class ListController {

    public static final int DEFAULT_QUALITY = 1;   // "Normal"
    public static final int DEFAULT_SIZE = 25;

    private final ListQueryService listQueryService;
    private final PriceService priceService;
    private final MaterialCostService materialCostService;

    /** The page shell with all dropdowns; the table body loads via HTMX on page load. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("cities", ListQueryService.CITIES);
        model.addAttribute("categories", listQueryService.categories());
        model.addAttribute("qualities", ListQueryService.QUALITIES);
        model.addAttribute("sizes", ListQueryService.SIZES);
        model.addAttribute("all", ListQueryService.ALL);
        return "index";
    }

    /** Subcategory options for one selected category (HTMX fragment: select options). */
    @GetMapping("/subcats")
    public String subcategories(@RequestParam String category, Model model) {
        model.addAttribute("subcategories", listQueryService.subcategories(category));
        model.addAttribute("all", ListQueryService.ALL);
        return "fragments/subcats";
    }

    /** Sorted item list as a table-body fragment. */
    @GetMapping("/list")
    public String list(
        @RequestParam(defaultValue = "Lymhurst") String city,
        @RequestParam(defaultValue = ListQueryService.ALL) String category,
        @RequestParam(defaultValue = ListQueryService.ALL) String subcategory,
        @RequestParam(defaultValue = "1") int quality,
        @RequestParam(defaultValue = "25") int size,
        Model model) {
        model.addAttribute("rows",
            listQueryService.rows(city, category, subcategory, quality, size));
        return "fragments/list";
    }

    /** "Berechnen" panel for one selected item: Variante 1 material cost (+ Variante 2 pending). */
    @GetMapping("/calc")
    public String calc(
        @RequestParam String itemId,
        @RequestParam(required = false) Integer enchantmentLevel,
        @RequestParam(defaultValue = "Lymhurst") String city,
        @RequestParam(defaultValue = "1") int quality,
        Model model) {
        ItemPriceStats market = priceService.stat(itemId, ListQueryService.apiLocation(city));
        MaterialCost materials = materialCostService.cost(itemId, enchantmentLevel, city);
        model.addAttribute("market", market);
        model.addAttribute("materials", materials);
        model.addAttribute("marketItemId",
            enchantmentLevel == null || enchantmentLevel == 0 ? itemId : itemId + "@" + enchantmentLevel);
        model.addAttribute("quality", quality);
        return "fragments/calc";
    }
}