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

import java.util.List;

/**
 * A complete step-by-step crafting plan: which raw materials to buy, which
 * refining step to perform where, and finally how to craft the target item.
 *
 * <p>The plan is rendered to the user as a checklist (see UI design doc).</p>
 *
 * @param targetItem the final item produced by following the plan
 * @param profitSummary aggregated profit for the whole plan
 * @param steps the individual steps in the order they must be performed
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record CraftingPlan(
        Item targetItem,
        ProfitResult profitSummary,
        List<CraftingStep> steps) {
}