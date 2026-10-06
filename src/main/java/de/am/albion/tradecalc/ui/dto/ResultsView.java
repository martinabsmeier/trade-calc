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
package de.am.albion.tradecalc.ui.dto;

import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.service.ProfitQueryService;

import java.util.List;

/**
 * View model for the {@code results} template: the user's selected mode and
 * city plus the list of ranked items. The plan for a single item is fetched
 * lazily via HTMX in {@code _plan-drawer.html}.
 */
public record ResultsView(
        CalculationMode mode,
        String homeCity,
        boolean includePlan,
        List<ProfitQueryService.ProfitEntry> items) {
}