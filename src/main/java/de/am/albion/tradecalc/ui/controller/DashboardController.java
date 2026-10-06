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
package de.am.albion.tradecalc.ui.controller;

import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.service.PriceServiceAdapter;
import de.am.albion.tradecalc.service.ProfitQueryService;
import de.am.albion.tradecalc.ui.dto.DashboardForm;
import de.am.albion.tradecalc.ui.dto.ResultsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Renders the web UI. {@code GET /} shows the dashboard form, {@code POST /}
 * accepts it and redirects to {@code /results} (Post/Redirect/Get so refresh
 * does not re-submit). The results endpoint is also a {@code GET} so the
 * browser can deep-link to it.
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final List<String> CITIES = List.of(
            "Lymhurst", "Fort Sterling", "Martlock", "Bridgewatch", "Thetford");

    private final ProfitQueryService queryService;
    private final PriceServiceAdapter prices;

    /**
     * Dashboard landing page with a fresh, default-populated form.
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("form", new DashboardForm());
        model.addAttribute("cities", CITIES);
        model.addAttribute("modes", CalculationMode.values());
        return "dashboard";
    }

    /**
     * Accepts the dashboard form, runs the calculation and redirects to
     * {@code /results} so a refresh of the result page does not resubmit.
     *
     * <p>ponytail: keeping everything on one controller removes the dual
     * controller pattern from the Phase 8 description. The user-facing flow
     * is identical, the seam is smaller.</p>
     */
    @PostMapping("/")
    public String submit(@ModelAttribute("form") DashboardForm form, RedirectAttributes redirect) {
        if (form.getHomeCity() == null || form.getHomeCity().isBlank()) {
            form.setHomeCity("Lymhurst");
        }
        redirect.addAttribute("mode", form.getMode().name());
        redirect.addAttribute("homeCity", form.getHomeCity());
        redirect.addAttribute("top", form.getTop());
        redirect.addAttribute("includePlan", form.isIncludePlan());
        return "redirect:/results";
    }

    /**
     * Renders the ranked results table. The crafting plan for an item is
     * loaded lazily by HTMX into {@code _plan-drawer.html} when a row is
     * expanded; this keeps the initial response small.
     */
    @GetMapping("/results")
    public String results(@ModelAttribute("form") ResultsViewRequest req, Model model) {
        CalculationMode mode = parseMode(req.mode());
        List<ProfitQueryService.ProfitEntry> entries = queryService.rankTop(
                mode, req.homeCity(), prices, req.top(), req.includePlan());
        model.addAttribute("mode", mode);
        model.addAttribute("homeCity", req.homeCity());
        model.addAttribute("includePlan", req.includePlan());
        model.addAttribute("items", entries);
        model.addAttribute("view", new ResultsView(mode, req.homeCity(), req.includePlan(), entries));
        return "results";
    }

    /**
     * Renders the lazy plan drawer fragment for a single item. Triggered by
     * HTMX ({@code hx-get="/plan?itemId=…&mode=…&homeCity=…"}) when the user
     * expands a row.
     */
    @GetMapping("/plan")
    public String plan(String itemId, String mode, String homeCity, Model model) {
        CalculationMode calculationMode = parseMode(mode);
        String city = (homeCity == null || homeCity.isBlank()) ? "Lymhurst" : homeCity;
        // ponytail: builds the plan from scratch on every drawer open — the
        // cheapest path because the tree walk is small and the cache keeps
        // prices hot. Upgrade to per-item cached plans if this ever shows up
        // in a flame graph.
        queryService.planFor(itemId, calculationMode, city, prices)
                .ifPresentOrElse(
                        plan -> model.addAttribute("plan", plan),
                        () -> model.addAttribute("plan", null));
        model.addAttribute("itemId", itemId);
        return "_plan-drawer";
    }

    private static CalculationMode parseMode(String raw) {
        if (raw == null) {
            return CalculationMode.BEST_OF_ALL;
        }
        return Arrays.stream(CalculationMode.values())
                .filter(m -> m.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElse(CalculationMode.BEST_OF_ALL);
    }

    /**
     * Tiny request bean for the {@code /results} query string. Keeps the
     * controller signature under five parameters.
     */
    public record ResultsViewRequest(String mode, String homeCity, int top, boolean includePlan) {
    }
}